package com.starterpack.backend.modules.upload.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uploaded media result")
public record UploadResultDto(
        @Schema(example = "2adde33e-d222-43d6-8444-4e1eb85b6dcd.png")
        String filename,
        @Schema(example = "avatar.png")
        String originalName,
        @Schema(example = "34567")
        long size,
        @Schema(example = "image/png")
        String mimeType,
        @Schema(example = "2adde33e-d222-43d6-8444-4e1eb85b6dcd.png")
        String relativePath
) {
}
