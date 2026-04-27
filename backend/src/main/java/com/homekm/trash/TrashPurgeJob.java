package com.homekm.trash;

import com.homekm.common.AppProperties;
import com.homekm.file.StoredFileRepository;
import com.homekm.folder.FolderRepository;
import com.homekm.note.NoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class TrashPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(TrashPurgeJob.class);

    private final NoteRepository noteRepository;
    private final StoredFileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final AppProperties appProperties;

    public TrashPurgeJob(NoteRepository noteRepository, StoredFileRepository fileRepository,
                         FolderRepository folderRepository, AppProperties appProperties) {
        this.noteRepository = noteRepository;
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.appProperties = appProperties;
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredTrash() {
        int retentionDays = appProperties.getTrash().getRetentionDays();
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("Starting trash purge (retention={}d, cutoff={})", retentionDays, cutoff);
        int notes = noteRepository.purgeDeletedBefore(cutoff);
        int files = fileRepository.purgeDeletedBefore(cutoff);
        int folders = folderRepository.purgeDeletedBefore(cutoff);
        log.info("Trash purge complete: {} notes, {} files, {} folders removed", notes, files, folders);
    }
}
