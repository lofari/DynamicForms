# SQLite Persistence for Backend

**Date**: 2026-02-09
**Status**: Approved

## Goal

Replace the in-memory `ConcurrentHashMap` stores (`FormStore`, `SubmissionStore`) with SQLite-backed persistence using Exposed (JetBrains' Kotlin SQL framework). All API contracts remain unchanged; the admin panel requires zero modifications.

## Schema

```sql
CREATE TABLE forms (
    form_id    TEXT PRIMARY KEY,
    title      TEXT NOT NULL,
    json_data  TEXT NOT NULL   -- Full serialized FormDefinition JSON
);

CREATE TABLE submissions (
    id            TEXT PRIMARY KEY,   -- UUID
    form_id       TEXT NOT NULL,
    values_json   TEXT NOT NULL,      -- Serialized Map<String, String>
    submitted_at  INTEGER NOT NULL,   -- Epoch millis
    FOREIGN KEY (form_id) REFERENCES forms(form_id) ON DELETE CASCADE
);
```

Forms are stored as JSON blobs rather than normalized columns because the polymorphic `FormElement` hierarchy would require many join tables for minimal query benefit.

Idempotency keys remain in-memory (`ConcurrentHashMap`). They are short-lived deduplication guards, not business data.

## Files Changed

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Add `exposed` and `sqlite-jdbc` versions + libraries |
| `backend/build.gradle.kts` | Add Exposed + SQLite JDBC dependencies |
| `backend/.../storage/DatabaseFactory.kt` | **New** - init SQLite connection, create tables |
| `backend/.../storage/FormTable.kt` | **New** - Exposed table definition for forms |
| `backend/.../storage/SubmissionTable.kt` | **New** - Exposed table definition for submissions |
| `backend/.../storage/FormStore.kt` | Rewrite: query Exposed tables instead of ConcurrentHashMap |
| `backend/.../storage/SubmissionStore.kt` | Rewrite: query Exposed tables instead of ConcurrentHashMap |
| `backend/.../Application.kt` | Call `DatabaseFactory.init()` before creating stores |
| `backend/.../routes/AdminRoutesTest.kt` | Use in-memory SQLite for test isolation |
| `backend/.../routes/FormRoutesTest.kt` | Use in-memory SQLite for test isolation |

## Seed Data

On first run, if the `forms` table is empty, seed forms are loaded from classpath `/forms/*.json` files (same behavior as current `FormStore.init`). This preserves the existing developer experience.

## Test Strategy

Tests use in-memory SQLite (`jdbc:sqlite::memory:`) so each `testApplication` block starts clean. `DatabaseFactory.init()` accepts a JDBC URL parameter, defaulting to file-based for production and in-memory for tests.

## What Stays the Same

- All REST API request/response contracts
- Admin panel (zero changes)
- Validation, CORS, call logging
- Idempotency key handling (in-memory)
- Route function signatures (still accept `FormStore` / `SubmissionStore`)
