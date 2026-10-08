# NOTES

## Summary of changes

1. **SQL `AND`/`OR` precedence (backend + `db/`):** the filter read `archived AND title OR description AND status`. `AND` binds tighter, so archived rows leaked in and status was ignored for title matches. Added parentheses: `archived AND (title OR description) AND status`; also fixed in the two `db/` SQL files.
2. **Removed `Thread.sleep(100–1000ms)`** in `TaskController` — a fake delay pinned a Tomcat thread per request; empty search went ~1025 ms → ~50 ms.
3. **Input validation:** bad `status`, `page=0`, `pageSize=0` and huge pages (int overflow in `subList`) returned 500; now HTTP 400, offset math uses `long`.
4. **Race conditions in `useTasks`:** stale responses overwrote newer results and `loading`/`error` got stuck. Added `AbortController`, `setError(null)` per fetch, `setLoading(false)` in `catch`.
5. **Pagination reset:** changing search/status kept the old page — handlers now `setPage(1)`.
6. **Request storm:** one request per keystroke — added a 300 ms debounce.

## What I chose not to change and why

In-memory pagination (fetch all, then `subList`) is fine at this size; SQL pagination would change the repository contract. Sort stays `created_at DESC`. No auth or write endpoints: out of scope. The Oracle file does not run locally; only its identical precedence bug was fixed.

## Biggest remaining risk

Pagination loads every matching row, so latency and heap grow with table size — first thing to move into SQL (`LIMIT/OFFSET` + `COUNT`). No mutations exist, so validation and transactions are untested.

## Tools / AI used

Used GenAI to review code, draft candidates (debounce hook) and check query semantics; I chose the final fixes and the 300 ms delay myself. Verified with the API, a frontend build, and the browser smoke test.

**Assumptions:** invalid input fails with HTTP 400 + JSON, not silent clamping; `pageSize` capped at 100.
