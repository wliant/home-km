package com.homekm.file;

import com.homekm.audit.AuditService;
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
import com.homekm.common.RequestContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
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
    private final AuditService auditService;
    private final MimeService mimeService;
    private final AvScanner avScanner;
    private final com.homekm.common.MinioGateway minioGateway;
    private final FileTransformRepository transformRepository;

    public FileService(StoredFileRepository fileRepository, FolderRepository folderRepository,
                       UserRepository userRepository, ChildSafeService childSafeService,
                       MinioClient minioClient, AppProperties appProperties,
                       AuditService auditService, MimeService mimeService, AvScanner avScanner,
                       com.homekm.common.MinioGateway minioGateway,
                       FileTransformRepository transformRepository) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.childSafeService = childSafeService;
        this.minioClient = minioClient;
        this.appProperties = appProperties;
        this.auditService = auditService;
        this.mimeService = mimeService;
        this.avScanner = avScanner;
        this.minioGateway = minioGateway;
        this.transformRepository = transformRepository;
    }

    public PageResponse<FileResponse> list(Long folderId, int page, int size, UserPrincipal principal) {
        var pageable = PageRequest.of(page, Math.min(size, 100));
        var files = principal.isChild()
                ? fileRepository.findByFolderIdAndChildSafeTrueAndDeletedAtIsNullOrderByUploadedAtDesc(folderId, pageable)
                : fileRepository.findByFolderIdAndDeletedAtIsNullOrderByUploadedAtDesc(folderId, pageable);
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

        // Detect & validate MIME via Tika
        byte[] bytes = file.getBytes();
        String detectedMime;
        try (InputStream sniff = new ByteArrayInputStream(bytes)) {
            detectedMime = mimeService.detect(sniff, file.getOriginalFilename());
        }
        mimeService.enforceAllowed(detectedMime, file.getContentType());

        // AV scan (no-op unless ClamAV configured)
        AvScanner.ScanResult scan;
        try (InputStream s = new ByteArrayInputStream(bytes)) {
            scan = avScanner.scan(s);
        }
        if (scan.status() == AvScanner.Status.INFECTED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "FILE_INFECTED: " + scan.detail());
        }

        User owner = userRepository.getReferenceById(principal.getId());
        StoredFile stored = new StoredFile();
        stored.setOwner(owner);
        stored.setFilename(file.getOriginalFilename() != null ? sanitizeFilename(file.getOriginalFilename()) : "upload");
        stored.setMimeType(detectedMime != null ? detectedMime : "application/octet-stream");
        stored.setSizeBytes(file.getSize());
        stored.setMinioKey("pending");
        stored.setClientUploadId(clientUploadId);
        stored.setChildSafe(principal.isChild());
        stored.setScanStatus(scan.status() == AvScanner.Status.CLEAN ? "CLEAN" : "PENDING");
        if (scan.status() == AvScanner.Status.CLEAN) stored.setScannedAt(Instant.now());

        if (folderId != null) {
            Folder folder = folderRepository.findActiveById(folderId)
                    .orElseThrow(() -> new EntityNotFoundException("Folder", folderId));
            stored.setFolder(folder);
            if (!principal.isChild()) {
                boolean safe = childSafeService.resolveChildSafeOnMove(false, folderId);
                stored.setChildSafe(safe);
            }
        }

        fileRepository.save(stored);

        // Build MinIO key and upload (resilience4j-wrapped)
        String folderSegment = folderId != null ? String.valueOf(folderId) : "root";
        String minioKey = principal.getId() + "/" + folderSegment + "/" + stored.getId() + "/" + stored.getFilename();
        stored.setMinioKey(minioKey);

        String bucket = appProperties.getMinio().getBucketName();
        ensureBucketExists(bucket);

        final byte[] payload = bytes;
        minioGateway.run(() -> {
            try (InputStream is = new ByteArrayInputStream(payload)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(minioKey)
                        .stream(is, payload.length, -1)
                        .contentType(stored.getMimeType())
                        .build());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        fileRepository.save(stored);

        if (!stored.isChildSafe()) {
            childSafeService.demoteFolderIfNeeded(folderId, false);
        }

        generateThumbnailAsync(stored.getId(), minioKey, stored.getMimeType(), bucket);
        generateImageVariantsAsync(stored.getId(), bucket);

        return toResponse(stored);
    }

    @Transactional
    public FileResponse rename(Long id, String newFilename, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        StoredFile f = fileRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("File", id));
        if (newFilename == null || newFilename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_FILENAME");
        }
        f.setFilename(sanitizeFilename(newFilename));
        fileRepository.save(f);
        auditService.record(principal.getId(), "FILE_RENAME", "file", String.valueOf(id),
                null, null, RequestContextHelper.currentRequest());
        return toResponse(f);
    }

    @Async
    public void generateImageVariantsAsync(Long fileId, String bucket) {
        StoredFile file = fileRepository.findById(fileId).orElse(null);
        if (file == null || file.getMimeType() == null || !file.getMimeType().startsWith("image/")) return;
        try {
            byte[] bytes;
            try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket).object(file.getMinioKey()).build())) {
                bytes = is.readAllBytes();
            }
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null) return;
            int[] sizes = {320, 960, 1600};
            String[] names = {"thumb", "preview", "display"};
            for (int i = 0; i < sizes.length; i++) {
                int targetW = sizes[i];
                if (source.getWidth() <= targetW && i > 0) continue;
                double scale = Math.min(1.0, (double) targetW / source.getWidth());
                int w = (int) (source.getWidth() * scale);
                int h = (int) (source.getHeight() * scale);
                BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(source.getScaledInstance(w, h, Image.SCALE_SMOOTH), 0, 0, null);
                g.dispose();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(scaled, "JPEG", baos);
                byte[] out = baos.toByteArray();
                String key = file.getMinioKey() + "__" + names[i] + ".jpg";
                try (InputStream is = new ByteArrayInputStream(out)) {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(bucket).object(key).stream(is, out.length, -1)
                            .contentType("image/jpeg").build());
                }
                FileTransform t = transformRepository.findByFileIdAndVariant(fileId, names[i])
                        .orElseGet(FileTransform::new);
                t.setFileId(fileId);
                t.setVariant(names[i]);
                t.setMinioKey(key);
                t.setWidth(w);
                t.setHeight(h);
                t.setSizeBytes(out.length);
                t.setMimeType("image/jpeg");
                transformRepository.save(t);
            }
        } catch (Exception e) {
            log.warn("Image variant generation failed for file {}: {}", fileId, e.getMessage());
        }
    }

    public FileResponse getById(Long id, UserPrincipal principal) {
        StoredFile f = findVisibleFile(id, principal);
        return toResponse(f);
    }

    @Transactional
    public FileResponse update(Long id, FileUpdateRequest req, UserPrincipal principal) {
        StoredFile f = fileRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("File", id));
        if (principal.isChild() && f.getOwner().getId() != principal.getId()) {
            throw new ChildAccountWriteException();
        }
        if (req.filename() != null) f.setFilename(sanitizeFilename(req.filename()));
        if (req.description() != null) f.setDescription(req.description());
        if (!principal.isChild() && req.isChildSafe() != null) f.setChildSafe(req.isChildSafe());
        if (req.folderId() != null) {
            Folder dest = folderRepository.findActiveById(req.folderId())
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
        StoredFile f = fileRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("File", id));
        f.setDeletedAt(Instant.now());
        fileRepository.save(f);
        auditService.record(principal.getId(), "FILE_DELETE", "file", String.valueOf(id),
                null, null, RequestContextHelper.currentRequest());
    }

    @Transactional
    public void restore(Long id, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        StoredFile f = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File", id));
        if (f.getDeletedAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NOT_DELETED");
        }
        f.setDeletedAt(null);
        fileRepository.save(f);
        auditService.record(principal.getId(), "FILE_RESTORE", "file", String.valueOf(id),
                null, null, RequestContextHelper.currentRequest());
    }

    @Transactional
    public FileResponse replaceContent(Long id, MultipartFile file, UserPrincipal principal) throws Exception {
        if (principal.isChild()) throw new ChildAccountWriteException();
        StoredFile f = fileRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("File", id));

        String bucket = appProperties.getMinio().getBucketName();
        String oldKey = f.getMinioKey();
        String oldThumbKey = f.getThumbnailKey();

        String folderSegment = f.getFolder() != null ? String.valueOf(f.getFolder().getId()) : "root";
        String newFilename = file.getOriginalFilename() != null
                ? sanitizeFilename(file.getOriginalFilename()) : f.getFilename();
        String newKey = principal.getId() + "/" + folderSegment + "/" + f.getId() + "/" + newFilename;

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

        f.setFilename(newFilename);
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
            return fileRepository.findByIdAndChildSafeAndDeletedAtIsNull(id, true)
                    .orElseThrow(() -> new EntityNotFoundException("File", id));
        }
        return fileRepository.findActiveById(id).orElseThrow(() -> new EntityNotFoundException("File", id));
    }

    private static String sanitizeFilename(String name) {
        return java.nio.file.Paths.get(name).getFileName().toString();
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
