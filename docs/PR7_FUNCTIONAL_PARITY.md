# PR #7 functional parity — Phase B1.5

Status: **PARTIAL**. This report describes the main-based integration worktree, not a merged or deployed release. The owner reported manual Browser QA **7/7 PASS** for Phase B1.5. Browser automation and independent visual parity verification remain **NOT VERIFIED**.

| Capability | Integration status | Evidence and remaining check |
| --- | --- | --- |
| Public trailer playback | IMPLEMENTED; owner-reported manual QA PASS | Movie Detail derives an 11-character ID only from a canonical validated YouTube URL and opens a `youtube-nocookie.com` iframe in a native dialog. Closing or pressing Escape unmounts the iframe; an external YouTube link remains available. Browser automation was unavailable to this agent. |
| Admin movie management | IMPLEMENTED; owner-reported manual QA PASS | ADMIN list, create and edit routes use backend read/write APIs. Browser automation was unavailable to this agent. |
| Cloudinary poster upload | IMPLEMENTED in backend; live upload NOT VERIFIED | Admin poster API and isolated tests exist. No real Cloudinary upload was performed in B1.5. |
| Genre management | IMPLEMENTED | V7 preserves `movies.genre` and adds normalized genre relations; Admin movie APIs expose canonical genres. The isolated main V1–V3 → V8 upgrade test passed. |
| Admin Showtime API | IMPLEMENTED | Backend API and PostgreSQL concurrency/migration tests passed in the full backend suite. This does not establish race safety for a future booking writer. |
| Admin Showtime Schedule Editor | DEFERRED | Slice 6E has not been implemented. PR #7 functional parity is incomplete. |

Verification on 2026-11-22: Gradle full backend suite 93 tests passed, 0 failures/errors/skips; the isolated PostgreSQL 16 main-schema upgrade test passed. Frontend lint and build passed. Lint reported seven existing warnings and Vite reported a large-bundle warning. Manual Browser QA 7/7 PASS was reported by the owner; it was not independently observed by this agent. No persistent database, Cloudinary upload or deployment was used.
