package com.starterpack.backend.modules.upload.infrastructure;

import org.springframework.web.multipart.MultipartFile;

public interface MediaStorage {
    StoredMedia store(MultipartFile file, String folder);

    void delete(String storagePath);

    record StoredMedia(
            String storedFilename,
            String storagePath,
            String contentType,
            long sizeBytes,
            String extension,
            String publicUrl,
            String storageProvider
    ) {
    }
}
