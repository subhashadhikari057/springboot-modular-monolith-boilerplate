package com.starterpack.backend.modules.users.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create a new user")
public record CreateUserRequest(
        @Schema(example = "Jane Doe")
        @NotBlank
        @Size(max = 120)
        String name,

        @Schema(example = "jane.doe@example.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(example = "Str0ngP@ssword")
        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @Schema(example = "+1-415-555-0132")
        @Size(max = 30)
        String phone,

        @Schema(example = "https://cdn.example.com/avatars/jane.png")
        @Size(max = 500)
        String image,

        @Schema(description = "Role id to assign. Defaults to USER if omitted.", example = "3")
        Integer roleId
) {
}
