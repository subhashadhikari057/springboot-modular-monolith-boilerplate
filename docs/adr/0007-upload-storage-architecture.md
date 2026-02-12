# ADR 0007: Upload Storage Architecture

## Status
Accepted

## Context
The system needs media upload support with metadata queryability and future storage-provider flexibility.

## Decision
Use a storage abstraction (`MediaStorage`) with current local-disk implementation (`LocalMediaStorage`).  
Persist upload metadata in `uploads` table and expose public path references.

## Alternatives Considered
1. Store files in DB as BLOB
2. Hardcode local filesystem logic directly in service/controller

## Consequences/Tradeoffs
- Pros: clear separation between metadata and storage operations; extensible for S3/minio later.
- Cons: local storage is not horizontally scalable without shared storage strategy.

