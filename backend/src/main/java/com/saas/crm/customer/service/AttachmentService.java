package com.saas.crm.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.crm.common.exception.BizException;
import com.saas.crm.common.exception.ErrorCode;
import com.saas.crm.customer.entity.Attachment;
import com.saas.crm.customer.mapper.AttachmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 客户附件（图片/文档）上传与删除。
 * 文件存本地磁盘（生产可替换为对象存储），仅放行白名单类型，UUID 重命名防路径穿越。
 */
@Slf4j
@Service
public class AttachmentService {

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> DOC_EXT = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv");

    private final AttachmentMapper attachmentMapper;
    private final CustomerService customerService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public AttachmentService(AttachmentMapper attachmentMapper, CustomerService customerService) {
        this.attachmentMapper = attachmentMapper;
        this.customerService = customerService;
    }

    public List<Attachment> list(Long currentUserId, boolean isAdmin, Long customerId) {
        customerService.checkAccess(currentUserId, isAdmin, customerId);
        return attachmentMapper.selectList(new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getCustomerId, customerId)
                .orderByDesc(Attachment::getId));
    }

    @Transactional
    public Attachment upload(Long currentUserId, boolean isAdmin, Long customerId, MultipartFile file) {
        customerService.checkAccess(currentUserId, isAdmin, customerId);
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请选择要上传的文件");
        }

        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extractExt(originalName);
        String fileType = classify(ext);

        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadDir, String.valueOf(customerId));
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(storedName).normalize();
            if (!target.startsWith(dir.toAbsolutePath())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "非法文件路径");
            }
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("file storage failed", e);
            throw new BizException(ErrorCode.INTERNAL, "文件保存失败，请稍后重试");
        }

        Attachment attachment = new Attachment();
        attachment.setCustomerId(customerId);
        attachment.setFileName(originalName);
        attachment.setFileType(fileType);
        attachment.setFilePath("/uploads/" + customerId + "/" + storedName);
        attachment.setFileSize(file.getSize());
        attachment.setCreatedBy(currentUserId);
        attachmentMapper.insert(attachment);
        return attachment;
    }

    @Transactional
    public void delete(Long currentUserId, boolean isAdmin, Long customerId, Long attachmentId) {
        customerService.checkAccess(currentUserId, isAdmin, customerId);
        Attachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null || !customerId.equals(attachment.getCustomerId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "附件不存在");
        }
        attachmentMapper.deleteById(attachmentId);
        deletePhysicalFile(attachment.getFilePath());
    }

    private void deletePhysicalFile(String filePath) {
        try {
            String relative = filePath.replaceFirst("^/uploads/", "");
            Path target = Paths.get(uploadDir, relative).normalize();
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("physical file delete failed: {}", filePath, e);
        }
    }

    private String extractExt(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件缺少扩展名");
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!IMAGE_EXT.contains(ext) && !DOC_EXT.contains(ext)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不支持的文件类型: ." + ext);
        }
        return ext;
    }

    private String classify(String ext) {
        if (IMAGE_EXT.contains(ext)) {
            return "image";
        }
        if (DOC_EXT.contains(ext)) {
            return "doc";
        }
        return "other";
    }
}
