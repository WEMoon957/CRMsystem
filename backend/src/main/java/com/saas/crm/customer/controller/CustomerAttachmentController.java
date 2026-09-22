package com.saas.crm.customer.controller;

import com.saas.crm.auth.security.CurrentPrincipal;
import com.saas.crm.common.dto.ApiResponse;
import com.saas.crm.customer.entity.Attachment;
import com.saas.crm.customer.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 客户附件（图片/文档）上传与删除。
 */
@RestController
@RequestMapping("/api/customers/{customerId}/attachments")
@RequiredArgsConstructor
public class CustomerAttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping
    public ApiResponse<List<Attachment>> list(@AuthenticationPrincipal CurrentPrincipal principal,
                                              @PathVariable Long customerId) {
        return ApiResponse.ok(attachmentService.list(principal.userId(), principal.isAdmin(), customerId));
    }

    @PostMapping
    public ApiResponse<Attachment> upload(@AuthenticationPrincipal CurrentPrincipal principal,
                                          @PathVariable Long customerId,
                                          @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(attachmentService.upload(principal.userId(), principal.isAdmin(), customerId, file));
    }

    @DeleteMapping("/{attachmentId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CurrentPrincipal principal,
                                    @PathVariable Long customerId,
                                    @PathVariable Long attachmentId) {
        attachmentService.delete(principal.userId(), principal.isAdmin(), customerId, attachmentId);
        return ApiResponse.ok();
    }
}
