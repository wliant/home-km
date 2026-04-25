package com.homekm.file;

import com.homekm.auth.User;
import com.homekm.auth.UserPrincipal;
import com.homekm.auth.UserRepository;
import com.homekm.common.AppProperties;
import com.homekm.common.ChildAccountWriteException;
import com.homekm.common.ChildSafeService;
import com.homekm.common.EntityNotFoundException;
import com.homekm.common.PageResponse;
import com.homekm.file.dto.FileResponse;
import com.homekm.file.dto.FileUpdateRequest;
import com.homekm.folder.Folder;
import com.homekm.folder.FolderRepository;
import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final StoredFileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final ChildSafeService childSafeService;
    private final MinioClient minioClient;
    private final AppProperties appProperties;

    public FileService(StoredFileRepository fileRepository, FolderRepository folderRepository,
                       UserRepository userRepository, ChildSafeService childSafeService,
                       MinioClient minioClient, AppProperties appProperties) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.childSafeService = childSafeService;
        this.minioClient = minioClient;
        this.appProperties = appProperties;
    }

    public PageResponse<FileResponse> list(Long folderId, int page, int size, UserPrincipal principal) {
        var pageable = PageRequest.of(page, Math.min(size, 100));
        var files = principal.isChild()
                ? fileRepository.findByFolderIdAndChildSafeTrueOrderByUploadedAtDesc(folderId, pageable)
                : fileRepository.findByFolderIdOrderByUploadedAtDesc(folderId, pageable);
        return PageResponse.of(files.map(f -> toResponse(f)));
    }

    @Transactional
    public FileResponse upload(MultipartFile file, Long folderId, String clientUploadId,
                                UserPrincipal principal) throws Exception {
        // Idempotency check
        if (clientUploadId != null) {
            var existing = fileRepository.findByOwnerIdAndClientUploadId(principal.getId(), clientUploadId);
            if (existing.isPresent()) return toResponse(existing.get());
        }

        User owner = userRepository.getReferenceById(principal.getId());
        StoredFile stored = new StoredFile();
        stored.setOwner(owner);
        stored.setFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload");
        stored.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        stored.setSizeBytes(file.getSize());
        stored.setMinioKey("pending");
        stored.setClientUploadId(clientUploadId);
        stored.setChildSafe(principal.isChild());

        if (folderId != null) {
            Folder folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new EntityNotFoundException("Folder", folderId));
            stored.setFolder(folder);
            if (!principal.isChild()) {
                boolean safe = childSafeService.resolveChildSafeOnMove(false, folderId);
                stored.setChildSafe(safe);
            }
        }

        fileRepository.save(stored);

        // Build MinIO key and upload
        String folderSegment = folderId != null ? String.valueOf(folderId) : "root";
        String minioKey = principal.getId() + "/" + folderSegment + "/" + stored.getId() + "/" + stored.getFilename();
        stored.setMinioKey(minioKey);

        String bucket = appProperties.getMinio().getBucketName();
        ensureBucketExists(bucket);

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(minioKey)
                    .stream(is, file.getSize(), -1)
                    .contentType(stored.getMimeType())
                    .build());
        }

        fileRepository.save(stored);

        if (!stored.isChildSafe()) {
            childSafeService.demoteFolderIfNeeded(folderId, false);
        }

        generateThumbnailAsync(stored.getId(), minioKey, stored.getMimeType(), bucket);

        return toResponse(stored);
    }

    public FileResponse getById(Long id, UserPrincipal principal) {
        StoredFile f = findVisibleFile(id, principal);
        return toResponse(f);
    }

    @Transactional
    public FileResponse update(Long id, FileUpdateRequest req, UserPrincipal principal) {
        StoredFile f = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File", id));
        if (principal.isChild() && f.getOwner().getId() != principal.getId()) {
            throw new ChildAccountWriteException();
        }
        if (req.filename() != null) f.setFilename(req.filename());
        if (req.description() != null) f.setDescription(req.description());
        if (!principal.isChild() && req.isChildSafe() != null) f.setChildSafe(req.isChildSafe());
        if (req.folderId() != null) {
            Folder dest = folderRepository.findById(req.folderId())
                    .orElseThrow(() -> new EntityNotFoundException("Folder", req.folderId()));
            f.setFolder(dest);
            boolean safe = childSafeService.resolveChildSafeOnMove(f.isChildSafe(), req.folderId());
            f.setChildSafe(safe);
        }
        fileRepository.save(f);
        return toResponse(f);
    }

    @Transactional
    public void delete(Long id, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        StoredFile f = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File", id));
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(appProperties.getMinio().getBucketName())
                    .object(f.getMinioKey()).build());
            if (f.getThumbnailKey() != null) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(appProperties.getMinio().getBucketName())
                        .object(f.getThumbnailKey()).build());
            }
        } catch (Exception e) {
            log.warn("Failed to delete MinIO object {}: {}", f.getMinioKey(), e.getMessage());
        }
        fileRepository.delete(f);
    }

    @Transactional
    public FileResponse replaceContent(Long id, MultipartFile file, UserPrincipal principal) throws Exception {
        if (principal.isChild()) throw new ChildAccountWriteException();
        StoredFile f = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File", id));

        String bucket = appProperties.getMinio().getBucketName();
        String oldKey = f.getMinioKey();
        String oldThumbKey = f.getThumbnailKey();

        String folderSegment = f.getFolder() != null ? String.valueOf(f.getFolder().getId()) : "root";
        String newKey = principal.getId() + "/" + folderSegment + "/" + f.getId() + "/" +
                (file.getOriginalFilename() != null ? file.getOriginalFilename() : f.getFilename());

        ensureBucketExists(bucket);
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(newKey)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType() != null ? file.getContentType() : f.getMimeType())
                    .build());
        }

        // remove old objects
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(oldKey).build());
            if (oldThumbKey != null) {
                minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(oldThumbKey).build());
            }
        } catch (Exception e) {
            log.warn("Could not remove old MinIO object {}: {}", oldKey, e.getMessage());
        }

        if (file.getOriginalFilename() != null) f.setFilename(file.getOriginalFilename());
        if (file.getContentType() != null) f.setMimeType(file.getContentType());
        f.setSizeBytes(file.getSize());
        f.setMinioKey(newKey);
        f.setThumbnailKey(null);
        fileRepository.save(f);

        String mimeType = f.getMimeType();
        generateThumbnailAsync(f.getId(), newKey, mimeType, bucket);

        return toResponse(f);
    }

    @Async
    public void generateThumbnailAsync(Long fileId, String minioKey, String mimeType, String bucket) {
        if (!mimeType.startsWith("image/")) return;
        try {
            byte[] imageBytes;
            try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket).object(minioKey).build())) {
                imageBytes = is.readAllBytes();
            }

            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) return;

            int w = original.getWidth(), h = original.getHeight();
            double scale = Math.min(256.0 / w, 256.0 / h);
            int tw = (int) (w * scale), th = (int) (h * scale);

            BufferedImage thumb = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original.getScaledInstance(tw, th, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumb, "JPEG", baos);
            byte[] thumbBytes = baos.toByteArray();

            String thumbKey = minioKey + "_thumb.jpg";
            try (InputStream is = new ByteArrayInputStream(thumbBytes)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket).object(thumbKey)
                        .stream(is, thumbBytes.length, -1)
                        .contentType("image/jpeg").build());
            }

            fileRepository.findById(fileId).ifPresent(f -> {
                f.setThumbnailKey(thumbKey);
                fileRepository.save(f);
            });
        } catch (Exception e) {
            log.warn("Thumbnail generation failed for {}: {}", minioKey, e.getMessage());
        }
    }

    private FileResponse toResponse(StoredFile f) {
        String bucket = appProperties.getMinio().getBucketName();
        long expiry = appProperties.getPresignedUrlExpiryMinutes();
        String downloadUrl = presignedUrl(bucket, f.getMinioKey(), expiry);
        String thumbUrl = f.getThumbnailKey() != null ? presignedUrl(bucket, f.getThumbnailKey(), expiry) : null;
        return FileResponse.from(f, downloadUrl, thumbUrl);
    }

    private String presignedUrl(String bucket, String key, long expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET).bucket(bucket).object(key)
                    .expiry((int) expiryMinutes, TimeUnit.MINUTES).build());
        } catch (Exception e) {
            log.warn("Could not generate presigned URL for {}: {}", key, e.getMessage());
            return null;
        }
    }

    private StoredFile findVisibleFile(Long id, UserPrincipal principal) {
        if (principal.isChild()) {
            return fileRepository.findByIdAndChildSafe(id, true)
                    .orElseThrow(() -> new EntityNotFoundException("File", id));
        }
        return fileRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("File", id));
    }

    private void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            log.warn("Could not verify/create bucket {}: {}", bucket, e.getMessage());
        }
    }
}
