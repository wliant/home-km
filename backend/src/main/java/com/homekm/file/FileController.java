package com.homekm.file;

import com.homekm.auth.UserPrincipal;
import com.homekm.common.PageResponse;
import com.homekm.common.Pagination;
import com.homekm.file.dto.FileResponse;
import com.homekm.file.dto.FileUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<FileResponse>> list(
            @RequestParam(required = false) Long folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.list(folderId, Pagination.clampPage(page), Pagination.clampSize(size), principal));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String clientUploadId,
            @AuthenticationPrincipal UserPrincipal principal) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileService.upload(file, folderId, clientUploadId, principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileResponse> getById(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.getById(id, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FileResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody FileUpdateRequest req,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.update(id, req, principal));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FileResponse> patch(@PathVariable Long id,
                                                @Valid @RequestBody FileUpdateRequest req,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.update(id, req, principal));
    }

    @PutMapping(value = "/{id}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> replaceContent(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws Exception {
        return ResponseEntity.ok(fileService.replaceContent(id, file, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        fileService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long id,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        fileService.restore(id, principal);
        return ResponseEntity.ok().build();
    }
}
