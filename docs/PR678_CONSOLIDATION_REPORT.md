# PR #6 + #7 + #8 consolidation — Phase C1

Status: **IMPLEMENTED locally, runtime Browser QA NOT VERIFIED**. This branch starts at PR #8 commit `81a55328f594467704c31c9466f8ade5005c3bf4`. It has not been committed, pushed, merged, or deployed.

## Sources and inventory

| Source | Relevant functionality |
| --- | --- |
| PR #6 `feature/add-film` (`de755e453f3b816c22a5a49a60855970bb1ca044`) | Admin movie create/update, Cloudinary poster upload, first Admin movie form, public trailer link. |
| PR #7 `feature/add-movie-trailer` (`95b1741080ba38b2ccfbfc2673461973c0b5d0ab`) | Contains PR #6; adds genres, showtime management, room scheduling UI, trailer-related fields. Its V5/V6 migrations conflict by version with the applied PR #8 history. |
| PR #8 `codex/main-based-integration-slice6d` (`81a55328f594467704c31c9466f8ade5005c3bf4`) | Public Home/Movies/Movie Detail, embedded trailer, Promotions, read-only Seat Map, Admin movie list/create/edit, protected movie/poster/trailer and showtime APIs, pricing, V4–V8 migrations and PostgreSQL tests. |

No separate SRS file is present in this checkout. The relevant `docs/API_CONTRACT.md`, `docs/ERD.md`, `docs/PLAN.md`, and code were reviewed. This change does not decide booking, price, payment, role, or security policies.

## Functional parity matrix

| Capability | Status against PR #8 baseline | Phase C1 result |
| --- | --- | --- |
| Admin Movie create/update/list and canonical genres | ALREADY INTEGRATED | Preserved existing API and form. |
| Backend Cloudinary PNG poster handling | ALREADY INTEGRATED | Preserved API, validation and mock-based backend tests; no live upload. |
| Backend YouTube URL validation/clear | ALREADY INTEGRATED | Preserved canonical URL policy. |
| Public Movie Detail trailer playback | ALREADY INTEGRATED | Existing validated ID and `youtube-nocookie.com` dialog retained. |
| Admin Showtime CRUD, overlap and booking-history safeguards | ALREADY INTEGRATED | Existing APIs, V8 exclusion constraint, locking and tests retained. |
| Admin poster/trailer form controls | MISSING | Added edit-page upload, existing-value display, trailer save/clear, preliminary validation and feedback. Movie must be saved first to obtain an ID. Browser QA remains outstanding. |
| Admin Showtime Schedule Editor | MISSING | Added date/room/time editor and schedule list within Admin Movie edit. Writes call existing protected showtime API. Conflict and booking-history responses are shown. Browser QA remains outstanding. |
| Admin room selection data | MISSING | Added read-only `GET /api/v1/admin/rooms` with ADMIN authorization, DTO, OpenAPI summary and H2 authorization test. |
| Public catalog, Promotions, Seat Map, pricing and login/guards | ALREADY INTEGRATED | No source changes to these flows. Full backend suite and frontend build passed; browser regression remains outstanding. |
| PR #7's normalized genre V5 / overlap V6 migrations | INCOMPATIBLE | Excluded. PR #8's V5/V6 remain untouched; compatible behavior already exists through V7/V8. |
| PR #7 bulk showtime/create-with-movie request shape | INCOMPATIBLE | Excluded. Current movie creation and individual showtime APIs remain authoritative. |
| PR #7 Schedule Editor as-is | PARTIALLY INTEGRATED | Useful date/room scheduling workflow adapted. The remote undeclared `setScheduleError`, double-applied overlap buffer, obsolete `hasBookings` field, and missing room API were not copied. Backend is the overlap authority. |

## Changed files

- Backend room read API: `AdminRoomController.java`, `AdminRoomResponse.java`, `AdminShowtimeService.java`, `AdminShowtimeServiceImpl.java`, `AdminShowtimeApiTests.java`.
- Frontend API and validation: `adminMovies.js`, `adminShowtimes.js`, `adminMovieMedia.js`, `showtimeSchedule.js`, `adminMovieIntegration.test.mjs`.
- Frontend UI: `AdminMovieFormPage.jsx`, `AdminMovieMediaPanel.jsx`, `AdminMovieScheduleEditor.jsx`, `index.css`.
- Documentation: `API_CONTRACT.md`, this report.

## Phase C1 implementation

- `GET /api/v1/admin/rooms` returns all database rooms as `{id,name,status}`, ordered by name then ID. Anonymous gets 401; MEMBER and EMPLOYEE get 403. Only ACTIVE rooms are selectable in the UI. No room write API was added.
- After movie creation, the UI opens that movie's edit page so media and schedule controls are immediately available. The Admin movie edit form has a separate media section. Poster upload sends multipart `file` to the existing API; the backend validates PNG content and Cloudinary configuration. Trailer save sends `{trailerUrl: string}`; clear sends `{trailerUrl: null}`. The frontend never receives Cloudinary credentials.
- The schedule editor appears after the movie form on edit pages. It reads all room schedules for the selected date and can create, edit, or cancel only the current movie's eligible showtimes. It uses `Asia/Ho_Chi_Minh`, 15-minute input slots and the backend's 08:00–23:00 rule. It disables edits when the movie duration has unsaved changes, the movie status is unsupported, the showtime is not future SCHEDULED, or history is reported. Backend validation remains authoritative, including overlap and booking history.
- Schedule mutations are individual API calls; no bulk creation or booking writer was added. Loading, empty and error states are shown.

## Flyway compatibility

PR #8's V1–V8 files were not modified. No V9 migration is needed for the read-only room endpoint or frontend additions. The full backend suite includes PostgreSQL 16 Testcontainers migration tests for fresh schema and the GitHub main V1–V3 upgrade with representative existing data, Flyway validate and repeated migrate checks. No persistent database or Supabase was accessed for this task.

## Test evidence (2026-11-22)

| Check | Result |
| --- | --- |
| `backend/gradlew.bat compileJava compileTestJava test --tests com.cinema.AdminShowtimeApiTests` | PASS; new ADMIN room response/authorization assertion passed. |
| `backend/gradlew.bat compileJava test` | PASS; final Gradle XML: 30 classes, 94 tests, 0 failures, 0 errors, 0 skipped. PostgreSQL tests use `postgres:16-alpine` Testcontainers; standard API tests use in-memory H2. |
| `frontend/npm run lint` | PASS; seven pre-existing warnings in unrelated pages. No warning remains in Phase C1 files. |
| `frontend/npm run build` | PASS; Vite reports existing bundle-size warning over 500 kB. |
| `node --test frontend/src/lib/adminMovieIntegration.test.mjs` | PASS; 3 isolated tests for poster input, trailer URL, and time boundaries. |
| `git diff --check` | PASS. |

Frontend dependency installation used `npm ci --offline` in this isolated worktree; lockfile was unchanged. Browser interaction, runtime authorization, live Cloudinary upload, and Stitch visual parity are **NOT VERIFIED** by these automated checks.

## Manual Browser QA checklist

Use a separate disposable PostgreSQL QA database and legitimate ADMIN account. Do not use `cinema`, `cinema_dev`, Supabase, or live Cloudinary credentials for this checklist. Test 1440×900, 768×1024 and 390×844; inspect Console and Network for unexpected failures.

1. Anonymous/MEMBER/EMPLOYEE cannot access `/admin/movies`; ADMIN can. Confirm `GET /admin/rooms` returns 200 only for ADMIN.
2. Open `/admin/movies`; search, filter, paginate, then create a movie. Verify Home/Movies/Movie Detail, embedded trailer close/Escape, Promotions and read-only Seat Map still work.
3. Open `/admin/movies/{id}/edit`; verify current poster and trailer are displayed. Save a valid YouTube URL, clear it, and check the value after reload. Try invalid host/query and confirm an error without data change.
4. Use a mocked Cloudinary backend response in isolated QA to test PNG upload success; try JPEG, oversized and unavailable-provider errors. A real Cloudinary upload remains separately unverified.
5. On the edit page, choose a future date, ACTIVE room and 15-minute start. Create a showtime, reload and verify response. Edit and cancel an eligible showtime. Confirm an overlapping room request shows conflict and a showtime with booking history cannot be changed.
6. Verify loading, empty, network error and retry for rooms and showtimes. Test keyboard focus, table horizontal scroll and action controls on desktop/tablet/mobile.
7. Check all writes in the disposable QA database only; inspect the browser Network tab for expected methods/status codes and no leaked tokens or Cloudinary credentials.

## Remaining gaps and readiness

**Runtime Browser QA NOT VERIFIED**, including visual parity, interaction behavior, ADMIN session handling, and live provider behavior. The new read-only room API has authorization coverage in H2; PostgreSQL integration coverage comes from the full Testcontainers suite but the room endpoint itself was not separately exercised on PostgreSQL. The future real booking writer must cooperate with the existing showtime row-lock protocol; this phase does not prove end-to-end booking race safety.

Phase C2 recommendation: review this uncommitted diff, perform disposable QA browser checks, then separately authorize staging/commit/push or PR update. Do not claim full production acceptance until those checks pass.
