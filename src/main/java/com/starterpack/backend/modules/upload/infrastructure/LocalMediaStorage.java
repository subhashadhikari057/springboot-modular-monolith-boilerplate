package com.starterpack.backend.modules.upload.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

import com.starterpack.backend.config.UploadProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LocalMediaStorage implements MediaStorage {
    private final UploadProperties uploadProperties;

    public LocalMediaStorage(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public StoredMedia store(MultipartFile file, String folder) {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename().trim();
        String extension = extractExtension(original);
        String stored = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        String sanitizedFolder = sanitizeFolder(folder);
        Path baseDir = Paths.get(uploadProperties.getLocal().getBaseDir()).toAbsolutePath().normalize();
        Path targetDir = sanitizedFolder == null ? baseDir : baseDir.resolve(sanitizedFolder).normalize();
        Path target = targetDir.resolve(stored).normalize();

        try {
            Files.createDirectories(targetDir);
            file.transferTo(target);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store uploaded file");
        }

        String pathForDb = target.toString();
        String publicBaseUrl = normalizePublicBaseUrl(uploadProperties.getLocal().getPublicBaseUrl());
        String publicUrl = sanitizedFolder == null
                ? publicBaseUrl + "/" + stored
                : publicBaseUrl + "/" + sanitizedFolder + "/" + stored;

        return new StoredMedia(
                stored,
                pathForDb,
                file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                file.getSize(),
                extension,
                publicUrl,
                "local"
        );
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(Path.of(storagePath));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete uploaded file");
        }
    }

    private String extractExtension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizePublicBaseUrl(String publicBaseUrl) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return "/files";
        }
        return publicBaseUrl.startsWith("/") ? publicBaseUrl : "/" + publicBaseUrl;
    }

    private String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return null;
        }

        String normalized = folder.trim().replace("\\", "/");
        normalized = normalized.replaceAll("/+", "/");
        normalized = normalized.replaceAll("^/+", "").replaceAll("/+$", "");

        if (normalized.isBlank()) {
            return null;
        }

        if (normalized.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid folder path");
        }

        if (!normalized.matches("[a-zA-Z0-9/_-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder can contain only letters, numbers, slash, underscore, and dash");
        }

        return normalized;
    }
}
