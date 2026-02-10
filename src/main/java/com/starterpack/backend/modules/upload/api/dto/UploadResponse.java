package com.starterpack.backend.modules.upload.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.starterpack.backend.modules.upload.domain.UploadMedia;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uploaded file metadata")
public record UploadResponse(
        @Schema(example = "f5ef8d31-b3e7-4a03-a4cf-e8cc6b7f84fb")
        UUID id,
        @Schema(example = "avatar.png")
        String originalFilename,
        @Schema(example = "image/png")
        String contentType,
        @Schema(example = "png")
        String extension,
        @Schema(example = "34567")
        long sizeBytes,
        @Schema(example = "/files/3889ce2c-9285-48d3-a95c-d2ddc8396d29.png")
        String url,
        OffsetDateTime createdAt
) {
    public static UploadResponse from(UploadMedia media) {
        return new UploadResponse(
                media.getId(),
                media.getOriginalFilename(),
                media.getContentType(),
                media.getExtension(),
                media.getSizeBytes(),
                media.getPublicUrl(),
                media.getCreatedAt()
        );
    }
}
