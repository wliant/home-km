# Drag-and-drop reorganization

| Field | Value |
|---|---|
| Category | Functional · Folders |
| Priority | P1 |
| Size | M |

**Current state:** Moving items requires navigating to the item, opening "Move", and picking a target folder. The sidebar folder tree is read-only.

**Gap:** No drag-and-drop, the most natural way to reorganize a tree of folders.

**Proposed direction:** Use `@dnd-kit/core` (lighter than react-dnd, better touch support) to allow dragging notes/files/subfolders onto folder targets in both the sidebar tree and the main list. Reuse the bulk-move endpoint (`bulk-move.md`). Touch devices: long-press to start drag.

**References:** `frontend/src/components/AppLayout.tsx`, `frontend/src/features/folders/FolderPage.tsx`, `frontend/package.json`, `specs/03-folders.md`
