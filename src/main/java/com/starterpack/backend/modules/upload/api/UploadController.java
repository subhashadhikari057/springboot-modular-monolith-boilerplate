package com.starterpack.backend.modules.upload.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.starterpack.backend.common.web.PagedResponse;
import com.starterpack.backend.common.web.ResponseDto;
import com.starterpack.backend.modules.upload.api.dto.UploadResponse;
import com.starterpack.backend.modules.upload.api.dto.UploadResultDto;
import com.starterpack.backend.modules.upload.application.UploadService;
import com.starterpack.backend.modules.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/uploads")
@Tag(name = "Uploads", description = "Media upload and management")
@Validated
public class UploadController {
    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @Operation(summary = "Upload files", description = "Uploads one or multiple media files in a single endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Uploaded",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing permission", content = @Content)
    })
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(type = "object", requiredProperties = {"file"})))
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('upload:create')")
    public ResponseEntity<ResponseDto<UploadResultDto>> upload(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @RequestParam(required = false) String folder,
            Authentication authentication
    ) {
        MultipartFile resolvedFile = resolveSingleFile(file, files);

        UUID userId = currentUserId(authentication);
        UploadResponse response = uploadService.uploadOne(resolvedFile, userId, folder);
        UploadResultDto payload = toUploadResult(response);
        URI location = URI.create("/api/uploads/" + response.id());
        return ResponseEntity.created(location).body(new ResponseDto<>("File uploaded successfully", payload));
    }

    @Operation(summary = "Upload multiple files", description = "Uploads multiple media files in a single request.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Uploaded",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file(s)", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing permission", content = @Content)
    })
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(type = "object", requiredProperties = {"files"})))
    @PostMapping(value = "/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('upload:create')")
    public ResponseEntity<ResponseDto<List<UploadResultDto>>> uploadMultiple(
            @RequestPart("files") MultipartFile[] files,
            @RequestParam(required = false) String folder,
            Authentication authentication
    ) {
        if (files.length > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 10 files are allowed per request");
        }

        UUID userId = currentUserId(authentication);
        List<UploadResponse> responses = uploadService.uploadMany(List.of(files), userId, folder);
        List<UploadResultDto> payload = responses.stream().map(this::toUploadResult).toList();
        URI location = responses.isEmpty() ? URI.create("/api/uploads") : URI.create("/api/uploads/" + responses.get(0).id());
        return ResponseEntity.created(location).body(new ResponseDto<>("Files uploaded successfully", payload));
    }

    @Operation(summary = "List uploads", description = "Returns paginated upload metadata.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload list",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Invalid query", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing permission", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasAuthority('upload:read')")
    public PagedResponse<UploadResponse> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 1");
        }
        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and 100");
        }

        String resolvedSortBy = resolveSortBy(sortBy);
        Sort.Direction direction = parseDirection(sortDir);
        return uploadService.list(page - 1, size, resolvedSortBy, direction);
    }

    @Operation(summary = "Get upload", description = "Returns upload metadata by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UploadResponse.class))),
            @ApiResponse(responseCode = "404", description = "Upload not found", content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('upload:read')")
    public UploadResponse get(
            @Parameter(description = "Upload id", example = "f5ef8d31-b3e7-4a03-a4cf-e8cc6b7f84fb")
            @PathVariable UUID id
    ) {
        return uploadService.getById(id);
    }

    @Operation(summary = "Delete upload", description = "Deletes file metadata and the stored file.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Upload deleted", content = @Content),
            @ApiResponse(responseCode = "404", description = "Upload not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('upload:delete')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        uploadService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }

    private String resolveSortBy(String sortBy) {
        return switch (sortBy) {
            case "id", "originalFilename", "contentType", "sizeBytes", "createdAt" -> sortBy;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported sortBy. Allowed: id,originalFilename,contentType,sizeBytes,createdAt"
            );
        };
    }

    private Sort.Direction parseDirection(String sortDir) {
        try {
            return Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sortDir must be asc or desc");
        }
    }

    private UploadResultDto toUploadResult(UploadResponse response) {
        String relativePath = extractRelativePath(response.url());
        String filename = extractFilename(response.url());
        return new UploadResultDto(
                filename,
                response.originalFilename(),
                response.sizeBytes(),
                response.contentType(),
                relativePath
        );
    }

    private MultipartFile resolveSingleFile(MultipartFile file, MultipartFile[] files) {
        if (file != null && !file.isEmpty()) {
            return file;
        }

        if (files != null) {
            for (MultipartFile candidate : files) {
                if (candidate != null && !candidate.isEmpty()) {
                    return candidate;
                }
            }
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Required multipart file is missing. Use key 'file' for /api/uploads or key 'files' for /api/uploads/multiple."
        );
    }

    private String extractRelativePath(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String normalized = url.startsWith("/") ? url.substring(1) : url;
        if (normalized.startsWith("files/")) {
            return normalized.substring("files/".length());
        }
        return normalized;
    }

    private String extractFilename(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        int idx = url.lastIndexOf('/');
        if (idx < 0 || idx == url.length() - 1) {
            return url;
        }
        return url.substring(idx + 1);
    }
}
