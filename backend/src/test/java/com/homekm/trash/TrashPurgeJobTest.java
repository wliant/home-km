package com.homekm.trash;

import com.homekm.common.AppProperties;
import com.homekm.file.StoredFileRepository;
import com.homekm.folder.FolderRepository;
import com.homekm.note.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrashPurgeJobTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private StoredFileRepository storedFileRepository;

    @Mock
    private FolderRepository folderRepository;

    private AppProperties appProperties;
    private TrashPurgeJob purgeJob;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getTrash().setRetentionDays(14);
        appProperties.getJwt().setSecret("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");

        purgeJob = new TrashPurgeJob(noteRepository, storedFileRepository,
                folderRepository, appProperties);
    }

    @Test
    void purgeExpiredTrash_usesConfiguredRetention() {
        when(noteRepository.purgeDeletedBefore(any())).thenReturn(0);
        when(storedFileRepository.purgeDeletedBefore(any())).thenReturn(0);
        when(folderRepository.purgeDeletedBefore(any())).thenReturn(0);

        Instant before = Instant.now().minus(14, ChronoUnit.DAYS);
        purgeJob.purgeExpiredTrash();
        Instant after = Instant.now().minus(14, ChronoUnit.DAYS);

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(noteRepository).purgeDeletedBefore(captor.capture());

        Instant cutoff = captor.getValue();
        // The cutoff should be approximately 14 days ago (within a small tolerance)
        assertThat(cutoff).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }

    @Test
    void purgeExpiredTrash_callsAllThreeRepositories() {
        when(noteRepository.purgeDeletedBefore(any())).thenReturn(2);
        when(storedFileRepository.purgeDeletedBefore(any())).thenReturn(3);
        when(folderRepository.purgeDeletedBefore(any())).thenReturn(1);

        purgeJob.purgeExpiredTrash();

        verify(noteRepository).purgeDeletedBefore(any(Instant.class));
        verify(storedFileRepository).purgeDeletedBefore(any(Instant.class));
        verify(folderRepository).purgeDeletedBefore(any(Instant.class));
    }
}
