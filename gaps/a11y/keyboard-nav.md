# Keyboard navigation pass

| Field | Value |
|---|---|
| Category | Functional · Accessibility |
| Priority | P1 |
| Size | M |
| Status | Closed |

**Current state:** Most controls inherit native keyboard behavior, but custom components (folder tree, tag autocomplete, file grid) have no consistent arrow-key navigation, no skip-link, and no global shortcut for search.

**Gap:** Mouse-only experience for several core flows.

**Proposed direction:** Audit each custom widget against the WAI-ARIA Authoring Practices for its pattern (treeview, combobox, listbox, menu). Add a `Skip to content` link. Implement global shortcuts: `Cmd/Ctrl+K` for search, `g n` for notes, `g f` for files. Document shortcuts in a help overlay (`?` to open).

**References:** `frontend/src/components/AppLayout.tsx`, `frontend/src/components/TagAutocomplete.tsx`, `frontend/src/features/folders/`, `frontend/src/features/files/`, `specs/14-frontend-architecture.md`
