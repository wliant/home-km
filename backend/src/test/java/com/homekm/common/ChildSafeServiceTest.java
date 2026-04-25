package com.homekm.common;

import com.homekm.file.StoredFileRepository;
import com.homekm.folder.Folder;
import com.homekm.folder.FolderRepository;
import com.homekm.note.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChildSafeServiceTest {

    @Mock FolderRepository folderRepository;
    @Mock NoteRepository noteRepository;
    @Mock StoredFileRepository fileRepository;

    @InjectMocks ChildSafeService childSafeService;

    @Nested
    class CascadeMarkSafe {

        // CS-1: marking a folder safe cascades to all descendants + their notes/files
        @Test
        void cascadesMarkSafeToDescendantsNotesAndFiles() {
            when(folderRepository.findDescendantIds(10L)).thenReturn(List.of(20L, 30L));

            childSafeService.cascadeMarkSafe(10L);

            verify(folderRepository).markChildSafeByIds(List.of(20L, 30L, 10L));
            verify(noteRepository).markChildSafeByFolderIds(List.of(20L, 30L, 10L));
            verify(fileRepository).markChildSafeByFolderIds(List.of(20L, 30L, 10L));
        }

        // CS-1: folder with no descendants still marks itself
        @Test
        void leafFolder_marksItself() {
            when(folderRepository.findDescendantIds(5L)).thenReturn(List.of());

            childSafeService.cascadeMarkSafe(5L);

            verify(folderRepository).markChildSafeByIds(List.of(5L));
        }
    }

    @Nested
    class DemoteFolderIfNeeded {

        // CS-2: unsafe item in a safe folder demotes that folder
        @Test
        void demotesFolder_whenItemIsUnsafe() {
            childSafeService.demoteFolderIfNeeded(10L, false);

            verify(folderRepository).updateChildSafe(10L, false);
        }

        // CS-3 variant: safe item does NOT demote folder
        @Test
        void doesNotDemote_whenItemIsSafe() {
            childSafeService.demoteFolderIfNeeded(10L, true);

            verify(folderRepository, never()).updateChildSafe(any(), anyBoolean());
        }

        // CS-2: no folder (root item) is a no-op
        @Test
        void noOp_whenFolderIdIsNull() {
            childSafeService.demoteFolderIfNeeded(null, false);

            verify(folderRepository, never()).updateChildSafe(any(), anyBoolean());
        }
    }

    @Nested
    class ResolveChildSafeOnMove {

        private Folder safeFolder;
        private Folder unsafeFolder;

        @BeforeEach
        void setUp() {
            safeFolder = new Folder();
            safeFolder.setChildSafe(true);

            unsafeFolder = new Folder();
            unsafeFolder.setChildSafe(false);
        }

        // CS-4: unsafe item moved to safe folder becomes safe
        @Test
        void unsafeItem_movesToSafeFolder_becomesChildSafe() {
            when(folderRepository.findById(1L)).thenReturn(Optional.of(safeFolder));

            boolean result = childSafeService.resolveChildSafeOnMove(false, 1L);

            assertThat(result).isTrue();
        }

        // CS-3: safe item moved to unsafe folder stays safe
        @Test
        void safeItem_movesToUnsafeFolder_remainsChildSafe() {
            when(folderRepository.findById(2L)).thenReturn(Optional.of(unsafeFolder));

            boolean result = childSafeService.resolveChildSafeOnMove(true, 2L);

            assertThat(result).isTrue();
        }

        // CS-5: unsafe item stays unsafe in unsafe folder
        @Test
        void unsafeItem_movesToUnsafeFolder_remainsUnsafe() {
            when(folderRepository.findById(3L)).thenReturn(Optional.of(unsafeFolder));

            boolean result = childSafeService.resolveChildSafeOnMove(false, 3L);

            assertThat(result).isFalse();
        }

        // Moving to root (no folder) preserves current state
        @Test
        void movingToRoot_preservesCurrentState() {
            assertThat(childSafeService.resolveChildSafeOnMove(true, null)).isTrue();
            assertThat(childSafeService.resolveChildSafeOnMove(false, null)).isFalse();
        }

        // Unknown destination folder preserves current state (graceful fallback)
        @Test
        void unknownDestinationFolder_preservesCurrentState() {
            when(folderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(childSafeService.resolveChildSafeOnMove(true, 99L)).isTrue();
            assertThat(childSafeService.resolveChildSafeOnMove(false, 99L)).isFalse();
        }
    }
}
