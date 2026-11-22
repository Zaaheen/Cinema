# Showtime room-overlap protection (V8)

## Status

**APPROVED / IMPLEMENTED:** Flyway `V8__protect_showtime_room_overlap.sql` protects one cinema's rooms from conflicting active showtimes on PostgreSQL.

## Constraint semantics

V8 requires the PostgreSQL `btree_gist` extension and adds
`ex_showtimes_active_room_occupied`:

```sql
EXCLUDE USING gist (
    room_id WITH =,
    tsrange(
        starts_at AT TIME ZONE 'UTC',
        (ends_at AT TIME ZONE 'UTC') + INTERVAL '15 minutes',
        '[)'
    ) WITH &&
)
WHERE (status IN ('SCHEDULED', 'ONGOING'))
```

- `SCHEDULED` and `ONGOING` occupy their room.
- `FINISHED` and `CANCELLED` do not block a new active showtime.
- The occupied range ends 15 minutes after the screening ends.
- The half-open range `[)` permits a following active showtime exactly 15 minutes after the prior one ends; a 14-minute turnaround is rejected.
- Different rooms remain independent.
- PostgreSQL enforces this rule for concurrent `INSERT` and `UPDATE`; an exclusion conflict returns SQLSTATE `23P01`.

V8 deliberately adds no mapped columns, no redundant GiST index, no operating-hours or 15-minute-start validation, and no API error mapping. Those application rules, including future `SHOWTIME_ROOM_CONFLICT` HTTP handling and protection of booked showtimes, are deferred to Slice 5.

## Existing-data and deployment policy

The migration performs no data mutation. It does not cancel, delete, reschedule, or rewrite showtimes, bookings, tickets, or payments. If active same-room rows overlap after the approved 15-minute buffer, adding the constraint fails and the Flyway transaction must be investigated; do not repair history automatically.

Run the following **read-only** preflight on the exact target database before approving deployment. It is an operational gate, not part of Flyway:

```sql
-- BLOCKING: active rows whose occupied ranges overlap.
SELECT a.id AS first_showtime_id,
       b.id AS second_showtime_id,
       a.room_id,
       a.starts_at AS first_starts_at,
       a.ends_at AS first_ends_at,
       b.starts_at AS second_starts_at,
       b.ends_at AS second_ends_at
FROM showtimes a
JOIN showtimes b
  ON a.room_id = b.room_id
 AND a.id < b.id
WHERE a.status IN ('SCHEDULED', 'ONGOING')
  AND b.status IN ('SCHEDULED', 'ONGOING')
  AND tsrange(
          a.starts_at AT TIME ZONE 'UTC',
          (a.ends_at AT TIME ZONE 'UTC') + INTERVAL '15 minutes',
          '[)'
      ) && tsrange(
          b.starts_at AT TIME ZONE 'UTC',
          (b.ends_at AT TIME ZONE 'UTC') + INTERVAL '15 minutes',
          '[)'
      );

-- BLOCKING schema/data sanity: this should be empty under ck_showtimes_time.
SELECT id, room_id, starts_at, ends_at, status
FROM showtimes
WHERE starts_at IS NULL OR ends_at IS NULL OR ends_at <= starts_at;

-- INFORMATIONAL/manual review only: history is preserved by V8.
SELECT s.id, s.room_id, s.status, s.starts_at, s.ends_at,
       EXISTS (SELECT 1 FROM bookings b WHERE b.showtime_id = s.id) AS has_booking,
       EXISTS (
           SELECT 1
           FROM booking_seats bs
           JOIN tickets t ON t.booking_seat_id = bs.id
           WHERE bs.showtime_id = s.id
       ) AS has_ticket
FROM showtimes s
ORDER BY s.room_id, s.starts_at;
```

Before migration, confirm that `btree_gist` is allowed and available to the deployment role (including a Supabase extension/privilege preflight if Supabase is the target). `ALTER TABLE ... ADD CONSTRAINT` can acquire a strong lock while it validates existing rows; it is not zero-downtime. Schedule a separately approved maintenance window and run the preflight immediately before deployment.

## Backup and recovery prerequisites

Production deployment needs a separately approved maintenance window, an identified target database, and confirmation of the current Flyway version before any write. Obtain a backup using the target environment's approved mechanism, verify that it completed and can be restored in an isolated environment where feasible, and record its retention location, owner, recovery owner, and escalation contact. Do not assume a backup exists without verification.

Do not edit `flyway_schema_history` to roll back V8. Restoring a backup is a separately approved recovery operation: it must account for writes made after that backup and must be validated in an isolated environment before any production action. Unapproved destructive recovery is prohibited.

## Fresh database versus existing-database upgrade

### Fresh database test procedure

Use an empty, disposable PostgreSQL 16 database only. Confirm the role can install/use `btree_gist`, run Flyway V1–V8, run Flyway validation, verify the exclusion constraint, and run Hibernate `ddl-auto=validate`. This proves the migration chain; it is not a production deployment procedure.

### Existing database deployment procedure

1. Identify and confirm the exact target database; confirm Flyway V1–V7 and capture pre-migration row counts/checksums needed for comparison.
2. Run the read-only preflight above and stop if it finds an active same-room conflict.
3. Verify `btree_gist` availability and deployment-role privileges, the verified backup, and the approved maintenance window.
4. Obtain separate approval for this target, then apply only V8.
5. Stop and escalate on any conflict or unsafe lock condition. Do not cancel, delete, reschedule, or otherwise mutate historical data automatically.

This task does not authorize either procedure against a persistent database.

## Post-migration validation

Run the following read-only checks after an approved deployment. Compare counts/checksums to the baseline captured before migration; the queries alone cannot infer an unchanged historical baseline.

```sql
SELECT version, success
FROM flyway_schema_history
WHERE version = '8';

SELECT extname
FROM pg_extension
WHERE extname = 'btree_gist';

SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conname = 'ex_showtimes_active_room_occupied';

SELECT count(*) AS showtime_count FROM showtimes;
SELECT count(*) AS booking_count FROM bookings;
SELECT count(*) AS ticket_count FROM tickets;

SELECT indexname
FROM pg_indexes
WHERE tablename = 'showtimes'
  AND indexname IN ('ix_showtimes_movie', 'ix_showtimes_room');
```

The returned constraint definition must be GiST exclusion over `room_id` and the UTC-normalized `[)` time range, with a `WHERE` predicate limited to `SCHEDULED` and `ONGOING`. Re-run the active-overlap preflight query above; it must return no rows. Run Flyway validate through the approved deployment tooling, then perform approved application startup/Hibernate schema validation and read-only smoke tests for the public Showtime and Seat Map APIs. Stop and investigate unexpected application errors.

## Failure and recovery

- If preflight finds conflicts, stop deployment and escalate with the affected IDs; automatic deletion, cancellation, or time shifting is prohibited.
- If V8 fails because existing active rows conflict, verify Flyway transaction rollback and that showtime data is unchanged before escalation.
- If extension privileges are insufficient, do not attempt privilege escalation from the application; request the environment owner to complete the approved extension preflight.
- If lock acquisition is unsafe, stop and reschedule within an approved maintenance window. Exclusion-constraint validation can require a strong table lock and is not zero-downtime.
- Production restore must never occur automatically. It requires a separately approved recovery plan, an owner, consideration of post-backup writes, and isolated recovery validation.

## Environment safety

PostgreSQL 16 Testcontainers are permitted for automated tests. Persistent `cinema`/`cinema_dev` databases and Supabase are not authorized for this task. Any real deployment, migration execution, backup restore, or production smoke test requires separate approval. This document contains no connection credentials or secrets.
