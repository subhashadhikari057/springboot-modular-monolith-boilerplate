package com.starterpack.backend.modules.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generic message response")
public record MessageResponse(
        @Schema(example = "ok")
        String message
) {
}
