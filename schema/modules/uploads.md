# Uploads Module Schema

Purpose
- Store uploaded media metadata and track uploader ownership.

Tables
- `uploads`

Key Relationships
- `uploads.uploaded_by` -> `users.id` (nullable, ON DELETE SET NULL)

Invariants
- `uploads.storage_path` is unique

Migrations
- `V4__uploads_module.sql`
