package com.homekm.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homekm.common.AppProperties;
import com.homekm.file.StoredFile;
import com.homekm.file.StoredFileRepository;
import com.homekm.folder.Folder;
import com.homekm.folder.FolderRepository;
import com.homekm.note.ChecklistItem;
import com.homekm.note.ChecklistItemRepository;
import com.homekm.note.Note;
import com.homekm.note.NoteRepository;
import com.homekm.reminder.Reminder;
import com.homekm.reminder.ReminderRepository;
import com.homekm.search.SavedSearchRepository;
import com.homekm.tag.Tagging;
import com.homekm.tag.TaggingRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * GDPR-style data export. Asynchronously assembles a ZIP containing the
 * caller's data — notes as markdown, side data (folders, tags, reminders,
 * saved searches) as JSON, plus a manifest.json. File blobs are NOT copied
 * into the ZIP at this scale; the manifest lists their MinIO keys and the
 * user can pull them individually if they want bytes too.
 *
 * Lifecycle: PENDING → READY (signed download URL valid for 24h) → EXPIRED
 * (cleanup deletes the ZIP from MinIO).
 */
@Service
public class DataExportService {

    private static final Logger log = LoggerFactory.getLogger(DataExportService.class);

    private final DataExportRepository repository;
    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ReminderRepository reminderRepository;
    private final FolderRepository folderRepository;
    private final StoredFileRepository storedFileRepository;
    private final SavedSearchRepository savedSearchRepository;
    private final TaggingRepository taggingRepository;
    private final MinioClient minioClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public DataExportService(DataExportRepository repository,
                              UserRepository userRepository,
                              NoteRepository noteRepository,
                              ChecklistItemRepository checklistItemRepository,
                              ReminderRepository reminderRepository,
                              FolderRepository folderRepository,
                              StoredFileRepository storedFileRepository,
                              SavedSearchRepository savedSearchRepository,
                              TaggingRepository taggingRepository,
                              MinioClient minioClient,
                              AppProperties appProperties,
                              ObjectMapper objectMapper) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.noteRepository = noteRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.reminderRepository = reminderRepository;
        this.folderRepository = folderRepository;
        this.storedFileRepository = storedFileRepository;
        this.savedSearchRepository = savedSearchRepository;
        this.taggingRepository = taggingRepository;
        this.minioClient = minioClient;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DataExportRequest enqueue(Long userId) {
        DataExportRequest req = new DataExportRequest();
        req.setUserId(userId);
        return repository.save(req);
    }

    /**
     * Process the oldest pending request. Single-row-per-tick keeps the loop
     * predictable; a household-scale deployment never has many concurrent
     * requests.
     */
    @Scheduled(fixedDelayString = "${app.export.poll-millis:30000}")
    public void processPending() {
        var pending = repository.findPending();
        for (DataExportRequest req : pending) {
            try {
                processOne(req);
            } catch (Exception e) {
                log.error("Export {} failed: {}", req.getId(), e.getMessage(), e);
                req.setStatus(DataExportRequest.Status.FAILED);
                req.setErrorMessage(e.getMessage());
                repository.save(req);
            }
        }
    }

    @Transactional
    protected void processOne(DataExportRequest req) throws Exception {
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IllegalStateException("user gone"));

        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(zipBytes)) {
            // manifest
            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "displayName", user.getDisplayName(),
                    "isAdmin", user.isAdmin(),
                    "isChild", user.isChild(),
                    "createdAt", user.getCreatedAt().toString()));
            manifest.put("exportedAt", Instant.now().toString());
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest));
            zip.closeEntry();

            // Per-item collections — built up while we walk owned notes/files.
            List<Map<String, Object>> notesIndex = new ArrayList<>();
            List<Map<String, Object>> checklistItems = new ArrayList<>();
            List<Map<String, Object>> reminders = new ArrayList<>();
            List<Map<String, Object>> noteTaggings = new ArrayList<>();
            List<Map<String, Object>> filesMetadata = new ArrayList<>();
            List<Map<String, Object>> fileTaggings = new ArrayList<>();

            // Notes as markdown — one file per note under notes/<id>-<slug>.md
            for (Note n : noteRepository.findAll()) {
                if (n.getOwner() == null || !user.getId().equals(n.getOwner().getId())) continue;
                if (n.getDeletedAt() != null) continue;
                String slug = n.getTitle() == null ? "untitled"
                        : n.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
                if (slug.length() > 60) slug = slug.substring(0, 60);
                String name = "notes/" + n.getId() + "-" + slug + ".md";
                StringBuilder md = new StringBuilder();
                md.append("# ").append(n.getTitle() != null ? n.getTitle() : "(untitled)").append("\n\n");
                md.append("- **Label:** ").append(n.getLabel()).append("\n");
                md.append("- **Created:** ").append(n.getCreatedAt()).append("\n");
                md.append("- **Updated:** ").append(n.getUpdatedAt()).append("\n");
                if (n.getFolder() != null) md.append("- **Folder ID:** ").append(n.getFolder().getId()).append("\n");
                md.append("\n---\n\n");
                if (n.getBody() != null) md.append(n.getBody());
                zip.putNextEntry(new ZipEntry(name));
                zip.write(md.toString().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();

                notesIndex.add(noteSummary(n));
                for (ChecklistItem ci : checklistItemRepository.findByNoteIdOrderBySortOrder(n.getId())) {
                    checklistItems.add(checklistEntry(ci, n.getId()));
                }
                for (Reminder r : reminderRepository.findByNoteId(n.getId())) {
                    reminders.add(reminderEntry(r, n.getId()));
                }
                for (Tagging t : taggingRepository.findByEntityTypeAndEntityId("note", n.getId())) {
                    noteTaggings.add(taggingEntry(t));
                }
            }

            // Files — metadata only; blobs stay in MinIO under the listed key.
            for (StoredFile f : storedFileRepository.findAll()) {
                if (f.getOwner() == null || !user.getId().equals(f.getOwner().getId())) continue;
                if (f.getDeletedAt() != null) continue;
                filesMetadata.add(fileEntry(f));
                for (Tagging t : taggingRepository.findByEntityTypeAndEntityId("file", f.getId())) {
                    fileTaggings.add(taggingEntry(t));
                }
            }

            // Folders are household-shared — include the live tree so referenced
            // folder IDs in notes/files resolve outside this install.
            List<Map<String, Object>> folders = new ArrayList<>();
            for (Folder f : folderRepository.findAllActive()) folders.add(folderEntry(f));

            writeJson(zip, "notes-index.json", notesIndex);
            writeJson(zip, "checklist-items.json", checklistItems);
            writeJson(zip, "reminders.json", reminders);
            writeJson(zip, "note-tags.json", noteTaggings);
            writeJson(zip, "files-metadata.json", filesMetadata);
            writeJson(zip, "file-tags.json", fileTaggings);
            writeJson(zip, "folders.json", folders);
            writeJson(zip, "saved-searches.json",
                    savedSearchRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
        }

        byte[] payload = zipBytes.toByteArray();
        String bucket = appProperties.getMinio().getBucketName();
        String key = "exports/" + user.getId() + "/" + req.getId() + ".zip";
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket).object(key)
                .stream(new ByteArrayInputStream(payload), payload.length, -1)
                .contentType("application/zip")
                .build());

        req.setStatus(DataExportRequest.Status.READY);
        req.setMinioKey(key);
        req.setSizeBytes((long) payload.length);
        req.setReadyAt(Instant.now());
        req.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        repository.save(req);
        log.info("Export {} ready ({} bytes) for user {}", req.getId(), payload.length, user.getId());
    }

    public String presignedDownloadUrl(DataExportRequest req) throws Exception {
        if (req.getStatus() != DataExportRequest.Status.READY || req.getMinioKey() == null) {
            return null;
        }
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(appProperties.getMinio().getBucketName())
                .object(req.getMinioKey())
                .expiry(15, TimeUnit.MINUTES)
                .build());
    }

    /** Sweep expired ZIPs from MinIO + flip their state. Runs hourly. */
    @Scheduled(fixedDelayString = "${app.export.cleanup-millis:3600000}")
    @Transactional
    public void cleanupExpired() {
        var expired = repository.findExpired(Instant.now());
        for (DataExportRequest req : expired) {
            try {
                if (req.getMinioKey() != null) {
                    minioClient.removeObject(io.minio.RemoveObjectArgs.builder()
                            .bucket(appProperties.getMinio().getBucketName())
                            .object(req.getMinioKey())
                            .build());
                }
            } catch (Exception e) {
                log.warn("Could not delete export blob {}: {}", req.getMinioKey(), e.getMessage());
            }
            req.setStatus(DataExportRequest.Status.EXPIRED);
            req.setMinioKey(null);
            repository.save(req);
        }
    }

    private void writeJson(ZipOutputStream zip, String name, Object payload) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload));
        zip.closeEntry();
    }

    private static Map<String, Object> noteSummary(Note n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("title", n.getTitle());
        m.put("label", n.getLabel());
        m.put("folderId", n.getFolder() != null ? n.getFolder().getId() : null);
        m.put("isChildSafe", n.isChildSafe());
        m.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
        m.put("updatedAt", n.getUpdatedAt() != null ? n.getUpdatedAt().toString() : null);
        return m;
    }

    private static Map<String, Object> checklistEntry(ChecklistItem ci, Long noteId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ci.getId());
        m.put("noteId", noteId);
        m.put("text", ci.getText());
        m.put("isChecked", ci.isChecked());
        m.put("createdAt", ci.getCreatedAt() != null ? ci.getCreatedAt().toString() : null);
        return m;
    }

    private static Map<String, Object> reminderEntry(Reminder r, Long noteId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("noteId", noteId);
        m.put("remindAt", r.getRemindAt() != null ? r.getRemindAt().toString() : null);
        m.put("recurrence", r.getRecurrence());
        m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        return m;
    }

    private static Map<String, Object> taggingEntry(Tagging t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("entityType", t.getEntityType());
        m.put("entityId", t.getEntityId());
        m.put("tagId", t.getTag() != null ? t.getTag().getId() : null);
        m.put("tagName", t.getTag() != null ? t.getTag().getName() : null);
        return m;
    }

    private static Map<String, Object> fileEntry(StoredFile f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.getId());
        m.put("filename", f.getFilename());
        m.put("mimeType", f.getMimeType());
        m.put("minioKey", f.getMinioKey());
        m.put("description", f.getDescription());
        m.put("isChildSafe", f.isChildSafe());
        m.put("uploadedAt", f.getUploadedAt() != null ? f.getUploadedAt().toString() : null);
        m.put("updatedAt", f.getUpdatedAt() != null ? f.getUpdatedAt().toString() : null);
        return m;
    }

    private static Map<String, Object> folderEntry(Folder f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.getId());
        m.put("parentId", f.getParent() != null ? f.getParent().getId() : null);
        m.put("name", f.getName());
        m.put("description", f.getDescription());
        return m;
    }
}
