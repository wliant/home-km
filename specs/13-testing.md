# Testing Strategy

## 1. Principles

Three independent test layers. Each must pass independently in CI.

| Layer | Where | Infra | Purpose |
|-------|-------|-------|---------|
| Unit | Inside each project | None | Business logic in isolation |
| Integration | Inside each project | Testcontainers (real DB + MinIO) / MSW | Full stack per component |
| E2E | `e2e-tests/` Python project | Live running app | End-to-end user scenarios + DB assertions |

---

## 2. Backend Unit Tests

**Framework:** JUnit 5, Mockito, Spring Boot Test  
**Location:** `backend/src/test/java/` (packages mirror main source)  
**Coverage target:** 80% line coverage on all service-layer classes  
**Gradle task:** `./gradlew test`

### Required test coverage

**`ChildSafeService`** (mock repository layer):
- All 11 scenarios from `09-child-safe.md` Section 11 (CS-1 through CS-11)

**`ReminderScheduler`:**
- Non-recurring reminder: `push_sent` set to `true` after send
- `daily` recurrence: `remind_at` advances by 1 day, `push_sent` reset to `false`
- `weekly` recurrence: `remind_at` advances by 7 days
- `monthly` recurrence: `remind_at` advances by 1 calendar month
- `yearly` recurrence: `remind_at` advances by 1 calendar year
- Push endpoint gone (HTTP 410): subscription record is deleted

**`JwtService`:**
- Token generation includes correct claims (`sub`, `email`, `isAdmin`, `isChild`, `exp`)
- Token validation succeeds with valid signature
- Token validation fails with wrong secret
- Token validation fails with expired token (verify `TOKEN_EXPIRED` exception, not generic auth exception)

**`UserService`:**
- Password validation: min 8 chars, requires uppercase, lowercase, digit
- Duplicate email registration returns correct exception
- First registered user gets `is_admin = true`

**`NoteLabel` validation:**
- All 11 valid values pass
- Unknown value throws validation exception with `validValues` list

**MinIO key builder (utility class):**
- Key with folder: `/{userId}/{folderId}/{fileId}/{filename}`
- Key at root: `/{userId}/root/{fileId}/{filename}`
- Filename with spaces: spaces replaced with `_`
- Filename with path separators: separators stripped

---

## 3. Backend Integration Tests

**Framework:** Spring Boot Test (`@SpringBootTest(webEnvironment = RANDOM_PORT)`), Testcontainers  
**Location:** `backend/src/test/java/integration/`  
**Base class:** `AbstractIntegrationTest` — starts PostgreSQL (with pgvector) and MinIO once per test class using Testcontainers `@Container static` pattern  
**Gradle task:** `./gradlew integrationTest` (separate source set)  
**No mocking of external infrastructure** — Testcontainers provides real instances

### Required coverage

| Test | Description |
|------|-------------|
| Auth flow | Register → login → use JWT on `GET /api/auth/me` → verify 200 |
| Invalid login | Wrong password → 401 INVALID_CREDENTIALS |
| Token expiry | Set short expiry, wait, verify 401 TOKEN_EXPIRED |
| Note CRUD | Create, read, update, delete note; verify DB state after each |
| Checklist | Add items, check item, reorder; verify `sort_order` in DB |
| Reminder | Create reminder, advance `remind_at` to past, trigger scheduler directly via test bean, verify `push_sent = true` |
| File upload | Upload file → verify DB row, MinIO object exists, presigned URL is accessible |
| File replace | Upload file, replace binary → verify new size in DB and new MinIO object |
| File delete | Upload file, delete → verify DB row gone, MinIO object gone |
| Thumbnail | Upload PNG → verify `thumbnail_key` in DB, thumbnail object exists in MinIO |
| Child-safe cascade | Create folder, mark child-safe → verify all descendants' DB rows updated |
| Child-safe demotion | Child-safe folder + add unsafe note → verify folder demoted |
| Child content filter | Create adult note (unsafe), call as child user → verify 404 |
| Full-text search | Insert notes with known unique text, search, verify ranked results include expected note |
| Search tag filter | Create notes with tag, search with `tagIds`, verify only tagged notes in results |
| Search folder scope | Create notes in two folders, search with `folderId`, verify only correct folder's notes |
| Admin user management | Admin creates user, updates `isChild`, resets password |

**Transaction strategy:**
- Integration tests that only test DB state: annotate with `@Transactional` to auto-rollback after each test
- Tests that verify MinIO state cannot use DB rollback for MinIO — use `@AfterEach` cleanup that deletes MinIO objects created with a test-specific key prefix

---

## 4. Frontend Unit Tests

**Framework:** Jest, React Testing Library  
**Location:** `frontend/src/__tests__/`  
**Coverage target:** 80% line coverage on all utility functions and custom hooks  
**NPM script:** `npm test -- --watchAll=false`

### Required coverage

| Test | Description |
|------|-------------|
| `ChildSafeBadge` | Renders "Child Safe" badge when `isChildSafe = true`; nothing when `false` |
| `ChecklistItem` | Toggle `isChecked` updates local state; calls API mutation |
| Offline banner | Banner appears when `navigator.onLine = false`; disappears on `true` |
| Tag autocomplete | Filters suggestions by prefix; shows "Create new tag" when no match |
| Note label selector | Shows all 11 valid label values; selects correctly |
| `useOfflineQueue` hook | Returns correct `pendingCount` from IndexedDB mock |
| JWT decode utility | Extracts `isChild`, `isAdmin` from token claims |

---

## 5. Frontend Integration Tests

**Framework:** Jest + React Testing Library + MSW (Mock Service Worker)  
**Location:** `frontend/src/__tests__/integration/`  
**Setup:** `frontend/src/__tests__/msw/handlers.ts` — MSW handler definitions for all API endpoints with realistic fixture data

### Required coverage

| Test | Description |
|------|-------------|
| Login → redirect | Valid credentials → redirect to `/folders` |
| Login → error | Invalid credentials → error message shown |
| Notes list | Renders paginated notes, shows label badges |
| Label filter | Select "Recipe" label → only recipe notes shown |
| File upload progress | Upload file → progress bar shown → success state |
| Offline queue state | `navigator.onLine = false` → file added to queue, banner shown |
| Search results | Search query → results from notes, files, folders shown |
| Tag filter in search | Select tag → MSW returns filtered results → correct notes shown |
| Child view | Child user JWT → non-child-safe items not rendered (MSW returns only safe fixtures) |

---

## 6. E2E Tests (`e2e-tests/`)

### Project structure

```
e2e-tests/
  conftest.py              # Fixtures
  .env.example             # Configuration template
  requirements.txt         # Dependencies
  tests/
    test_auth.py
    test_notes.py
    test_checklists.py
    test_files.py
    test_search.py
    test_child_safe.py
    test_reminders.py
```

### Configuration

`.env.example`:
```
E2E_BASE_URL=http://localhost:3000
E2E_ADMIN_EMAIL=admin@test.local
E2E_ADMIN_PASSWORD=TestPassword1
E2E_CHILD_EMAIL=child@test.local
E2E_CHILD_PASSWORD=TestPassword1
E2E_DB_DSN=postgresql://homekm:password@localhost:5432/homekm
```

Load with `python-dotenv`. All values must be overridable by environment variables (for CI).

### Dependencies (`requirements.txt`)

```
playwright>=1.40
pytest>=7.4
pytest-playwright>=0.4
psycopg2-binary>=2.9
python-dotenv>=1.0
```

### `conftest.py` fixtures

```python
@pytest.fixture(scope="session")
def browser_context(playwright):
    browser = playwright.chromium.launch()
    context = browser.new_context(base_url=E2E_BASE_URL)
    yield context
    browser.close()

@pytest.fixture
def admin_page(browser_context):
    page = browser_context.new_page()
    # Login as admin, store JWT
    yield page
    page.close()

@pytest.fixture
def child_page(browser_context):
    page = browser_context.new_page()
    # Login as child user
    yield page
    page.close()

@pytest.fixture
def db():
    conn = psycopg2.connect(E2E_DB_DSN)
    yield conn
    conn.close()

@pytest.fixture(autouse=True)
def cleanup_db(db):
    yield
    # Delete all test data created during the test
    # Use naming convention: test data titles start with "E2E_TEST_"
    cursor = db.cursor()
    cursor.execute("DELETE FROM notes WHERE title LIKE 'E2E_TEST_%'")
    cursor.execute("DELETE FROM files WHERE filename LIKE 'E2E_TEST_%'")
    cursor.execute("DELETE FROM folders WHERE name LIKE 'E2E_TEST_%'")
    cursor.execute("DELETE FROM tags WHERE name LIKE 'E2E_TEST_%'")
    cursor.execute("DELETE FROM users WHERE email LIKE '%@e2e.test'")
    db.commit()
```

### Test matrix

#### `test_auth.py`

| Test | Steps | Assertions |
|------|-------|-----------|
| `test_login_success` | Navigate to `/login`, enter valid credentials, submit | UI redirected to `/folders`; localStorage contains JWT |
| `test_login_invalid_password` | Enter wrong password | Error message "Invalid credentials" visible |
| `test_admin_create_user` | Login as admin, navigate to `/admin/users`, create new child user | UI shows new user; DB: `SELECT is_child FROM users WHERE email = ?` returns `true` |

#### `test_notes.py`

| Test | Steps | Assertions |
|------|-------|-----------|
| `test_create_note` | Create note with title "E2E_TEST_create", body "test body" | Note appears in list; DB: row exists with correct title and body |
| `test_edit_note` | Create note, edit title and body | Updated content shown; DB: `updated_at` changed |
| `test_delete_note` | Create note, delete it | Note gone from list; DB: no row with that title |
| `test_label_filter` | Create notes with labels "recipe" and "todo", apply recipe filter | Only recipe note shown; todo note not visible |

#### `test_checklists.py`

| Test | Steps | Assertions |
|------|-------|-----------|
| `test_add_checklist_item` | Open note, add checklist item "E2E_TEST_item" | Item appears in checklist; DB: `checklist_items` row exists |
| `test_check_item` | Add item, click checkbox | Checkbox shows checked; DB: `is_checked = true` |
| `test_reorder_items` | Add 3 items, drag-and-drop to reorder | Items in new order; DB: `sort_order` values updated |

#### `test_files.py`

| Test | Steps | Assertions |
|------|-------|-----------|
| `test_upload_file` | Upload text file "E2E_TEST_document.txt" | File appears in list; DB: `files` row with correct filename and `size_bytes > 0`; MinIO: object exists at `minio_key` from DB |
| `test_presigned_download` | Upload file, click download | Browser navigates to presigned URL; response status 200 |
| `test_upload_image_thumbnail` | Upload PNG image | DB: `thumbnail_key` is not null; MinIO: thumbnail object exists |
| `test_replace_file_content` | Upload file, replace with different file | DB: `size_bytes` updated; MinIO: object at same key has new content |

#### `test_search.py`

| Test | Steps | Assertions |
|------|-------|-----------|
| `test_search_by_note_title` | Create note with unique title "E2E_TEST_xq9k7", search "xq9k7" | Note appears in results |
| `test_search_by_file_name` | Upload file "E2E_TEST_unique_xq9k8.txt", search "xq9k8" | File appears in results |
| `test_search_tag_filter` | Create two notes, tag one with "E2E_TEST_tag", search with that tag | Only tagged note in results |
| `test_search_folder_scope` | Create notes in two folders, search scoped to one folder | Only target folder's notes shown |

#### `test_child_safe.py`

| Test | Steps | Assertions |
|------|-------|-----------|
| `test_child_cannot_see_unsafe_note` | Admin creates note with `isChildSafe=false`, login as child | Note not visible in list or by direct URL |
| `test_child_sees_safe_note` | Admin creates note with `isChildSafe=true`, login as child | Note visible |
| `test_folder_cascade_marks_note_safe` | Create unsafe note in folder, admin marks folder child-safe | DB: `notes.is_child_safe = true`; child can see note |
| `test_adding_unsafe_note_demotes_folder` | Create child-safe folder, add unsafe note to it | DB: `folders.is_child_safe = false` |
| `test_child_cannot_delete` | Login as child, try to DELETE any note via API direct call | Returns 403 |

#### `test_reminders.py`

| Test | Steps | Assertions |
|------|-------|-----------|
| `test_create_reminder` | Create note, add reminder | DB: `reminders` row with correct `remind_at` and `push_sent = false` |
| `test_reminder_fires` | Create reminder, update `remind_at` to 5 seconds in the past via DB, call `POST /api/test/trigger-scheduler` (test profile only), wait | DB: `push_sent = true` for non-recurring reminder |
| `test_recurring_reminder_advances` | Create recurring weekly reminder, trigger scheduler | DB: `remind_at` advanced by 7 days, `push_sent = false` |

### Test profile scheduler endpoint

Register `POST /api/test/trigger-scheduler` only when Spring profile `test` is active:

```java
@Profile("test")
@RestController
@RequestMapping("/api/test")
public class TestController {
    @PostMapping("/trigger-scheduler")
    public ResponseEntity<Void> triggerScheduler(ReminderScheduler scheduler) {
        scheduler.processReminders();
        return ResponseEntity.noContent().build();
    }
}
```

This endpoint does not require authentication (only reachable in test environments).

---

## 7. CI Pipeline

### Stage 1 — Unit tests (no infra)
```bash
# Backend
cd backend && ./gradlew test

# Frontend
cd frontend && npm test -- --watchAll=false --coverage
```

### Stage 2 — Integration tests (Testcontainers starts infra)
```bash
cd backend && ./gradlew integrationTest
```

### Stage 3 — E2E tests (requires live environment)

Run only on `main` branch push or manually triggered:
```bash
cd e2e-tests
pip install -r requirements.txt
playwright install chromium
pytest tests/ -v --tb=short
```

Configure via env vars pointing to the deployed test instance.

---

## 8. Screen-reader testing

Manual matrix lives in `SR_TESTING.md` at the repo root: top-10 user flows tested against VoiceOver (macOS/iOS) and NVDA (Windows) before every release. Regressions of the structural properties (single main landmark, labelled inputs, polite live region for route announcements, skip link first in tab order) are pinned by `e2e/tests/a11y-sr.spec.ts`.

`e2e/tests/a11y.spec.ts` continues to own the WCAG-checkable defects via axe-core; the SR suite covers what axe alone cannot enforce.

---

## 9. Load testing

k6 scenarios live under `tests/load/`. `LOAD-TESTING.md` documents how to run them, the canonical baseline numbers, and the SLO they back-stop (`docs/slo.md`). They run on-demand against a staging stack — not in main CI — and re-run before each release so regressions surface against committed baselines.
