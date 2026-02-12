package com.starterpack.backend.modules.upload.application;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.starterpack.backend.common.web.PageMeta;
import com.starterpack.backend.common.web.PagedResponse;
import com.starterpack.backend.config.UploadProperties;
import com.starterpack.backend.modules.upload.api.dto.UploadResponse;
import com.starterpack.backend.modules.upload.domain.UploadMedia;
import com.starterpack.backend.modules.upload.infrastructure.MediaStorage;
import com.starterpack.backend.modules.upload.infrastructure.UploadMediaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UploadService {
    private final UploadMediaRepository uploadMediaRepository;
    private final MediaStorage mediaStorage;
    private final UploadProperties uploadProperties;

    public UploadService(
            UploadMediaRepository uploadMediaRepository,
            MediaStorage mediaStorage,
            UploadProperties uploadProperties
    ) {
        this.uploadMediaRepository = uploadMediaRepository;
        this.mediaStorage = mediaStorage;
        this.uploadProperties = uploadProperties;
    }

    public UploadResponse uploadOne(MultipartFile file, UUID uploadedBy, String folder) {
        validateFile(file);

        MediaStorage.StoredMedia stored = mediaStorage.store(file, folder);
        UploadMedia media = new UploadMedia();
        media.setOriginalFilename(safeOriginalName(file.getOriginalFilename()));
        media.setStoredFilename(stored.storedFilename());
        media.setContentType(stored.contentType());
        media.setExtension(stored.extension());
        media.setSizeBytes(stored.sizeBytes());
        media.setStoragePath(stored.storagePath());
        media.setStorageProvider(stored.storageProvider());
        media.setPublicUrl(stored.publicUrl());
        media.setUploadedBy(uploadedBy);

        return UploadResponse.from(uploadMediaRepository.save(media));
    }

    public List<UploadResponse> uploadMany(List<MultipartFile> files, UUID uploadedBy, String folder) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "files must not be empty");
        }

        return files.stream()
                .map(file -> uploadOne(file, uploadedBy, folder))
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<UploadResponse> list(int page, int size, String sortBy, Sort.Direction sortDirection) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<UploadMedia> uploads = uploadMediaRepository.findAll(pageable);
        List<UploadResponse> items = uploads.getContent().stream().map(UploadResponse::from).toList();
        return new PagedResponse<>(items, PageMeta.from(uploads));
    }

    @Transactional(readOnly = true)
    public UploadResponse getById(UUID id) {
        UploadMedia media = uploadMediaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload not found"));
        return UploadResponse.from(media);
    }

    public void delete(UUID id) {
        UploadMedia media = uploadMediaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload not found"));

        mediaStorage.delete(media.getStoragePath());
        uploadMediaRepository.delete(media);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file must not be empty");
        }

        if (file.getSize() > uploadProperties.getMaxFileSizeBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file exceeds max allowed size");
        }

        List<String> allow = uploadProperties.getAllowedContentTypes();
        if (allow == null || allow.isEmpty()) {
            return;
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        Set<String> normalized = allow.stream().map(v -> v.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());

        if (!normalized.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported content type");
        }
    }

    private String safeOriginalName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "file";
        }
        String name = originalFilename.trim();
        return name.replace("\\", "_").replace("/", "_");
    }
}
