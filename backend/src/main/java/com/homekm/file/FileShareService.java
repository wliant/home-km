package com.homekm.file;

import com.homekm.audit.AuditService;
import com.homekm.auth.UserPrincipal;
import com.homekm.common.AppProperties;
import com.homekm.common.EntityNotFoundException;
import com.homekm.common.MinioGateway;
import com.homekm.common.RequestContextHelper;
import com.homekm.common.TokenHasher;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FileShareService {

    private static final Logger log = LoggerFactory.getLogger(FileShareService.class);

    private final FileShareLinkRepository repo;
    private final StoredFileRepository fileRepo;
    private final AuditService auditService;
    private final MinioClient minioClient;
    private final MinioGateway gateway;
    private final AppProperties appProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    public FileShareService(FileShareLinkRepository repo, StoredFileRepository fileRepo,
                              AuditService auditService, MinioClient minioClient,
                              MinioGateway gateway, AppProperties appProperties) {
        this.repo = repo;
        this.fileRepo = fileRepo;
        this.auditService = auditService;
        this.minioClient = minioClient;
        this.gateway = gateway;
        this.appProperties = appProperties;
    }

    public List<FileShareLink> list(Long fileId) {
        return repo.findByFileIdOrderByCreatedAtDesc(fileId);
    }

    @Transactional
    public IssuedShareLink create(Long fileId, Integer ttlHours, String password,
                                   Integer maxDownloads, UserPrincipal principal) {
        StoredFile f = fileRepo.findActiveById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File", fileId));
        if (!"CLEAN".equals(f.getScanStatus()) && appProperties.getFiles().isRequireScan()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "FILE_NOT_CLEAN");
        }
        String raw = UUID.randomUUID().toString().replace("-", "");
        FileShareLink link = new FileShareLink();
        link.setTokenHash(TokenHasher.sha256(raw));
        link.setFileId(fileId);
        int hours = ttlHours == null ? 168 : Math.max(1, Math.min(ttlHours, 24 * 90));
        link.setExpiresAt(Instant.now().plus(hours, ChronoUnit.HOURS));
        if (password != null && !password.isBlank()) {
            link.setPasswordHash(passwordEncoder.encode(password));
        }
        link.setMaxDownloads(maxDownloads);
        link.setCreatedBy(principal != null ? principal.getId() : null);
        repo.save(link);

        auditService.record(principal != null ? principal.getId() : null,
                "FILE_SHARE_LINK_CREATE", "file_share_link", String.valueOf(link.getId()),
                null, null, RequestContextHelper.currentRequest());

        return new IssuedShareLink(link, raw);
    }

    @Transactional
    public void revoke(Long linkId, UserPrincipal principal) {
        FileShareLink link = repo.findById(linkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LINK_NOT_FOUND"));
        link.setRevokedAt(Instant.now());
        repo.save(link);
        auditService.record(principal != null ? principal.getId() : null,
                "FILE_SHARE_LINK_REVOKE", "file_share_link", String.valueOf(linkId),
                null, null, RequestContextHelper.currentRequest());
    }

    @Transactional
    public ResolvedShare resolve(String rawToken, String suppliedPassword) throws Exception {
        FileShareLink link = repo.findByTokenHash(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LINK_NOT_FOUND"));
        if (link.isRevoked()) throw new ResponseStatusException(HttpStatus.GONE, "LINK_REVOKED");
        if (link.isExpired()) throw new ResponseStatusException(HttpStatus.GONE, "LINK_EXPIRED");
        if (link.isExhausted()) throw new ResponseStatusException(HttpStatus.GONE, "LINK_EXHAUSTED");
        if (link.getPasswordHash() != null) {
            if (suppliedPassword == null || !passwordEncoder.matches(suppliedPassword, link.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PASSWORD_REQUIRED");
            }
        }
        StoredFile f = fileRepo.findActiveById(link.getFileId())
                .orElseThrow(() -> new EntityNotFoundException("File", link.getFileId()));
        if (appProperties.getFiles().isRequireScan() && !"CLEAN".equals(f.getScanStatus())) {
            throw new ResponseStatusException(HttpStatus.GONE, "FILE_NOT_CLEAN");
        }
        link.setDownloadCount(link.getDownloadCount() + 1);
        repo.save(link);
        String url = gateway.call(() -> minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(appProperties.getMinio().getBucketName())
                .object(f.getMinioKey())
                .expiry(15, TimeUnit.MINUTES)
                .build()));
        return new ResolvedShare(f.getFilename(), f.getMimeType(), url);
    }

    public record IssuedShareLink(FileShareLink link, String token) {}
    public record ResolvedShare(String filename, String mimeType, String downloadUrl) {}
}
