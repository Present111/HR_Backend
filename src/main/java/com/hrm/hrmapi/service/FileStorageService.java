// src/main/java/com/hrm/hrmapi/service/FileStorageService.java
package com.hrm.hrmapi.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.io.IOException;

@Service
public class FileStorageService {

    private final Path root = Paths.get("uploads");

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(root);
        } catch (Exception e) {
            throw new RuntimeException("Could not create upload folder: " + root.toAbsolutePath(), e);
        }
    }

    /** Lưu avatar vào /uploads/avatars/YYYY/MM/..., trả về path public, ví dụ: /uploads/avatars/2025/10/xxxx.png */
    public String saveAvatar(MultipartFile file) {
        return store(file, "avatars");
    }

    /**
     * Lưu file vào /uploads/<subdir>/YYYY/MM/
     * @param file   multipart file
     * @param subdir ví dụ: "docs/employeeId" hoặc "contracts/emp123"
     * @return public path bắt đầu bằng "/uploads/...", ví dụ: /uploads/docs/emp123/2025/10/<uuid>.pdf
     */
    public String store(MultipartFile file, String subdir) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }

        // Làm sạch subdir, chặn path traversal
        String safeSubdir = sanitizeSubdir(subdir);

        // Tạo cây thư mục: <root>/<safeSubdir>/<yyyy>/<mm>
        LocalDate now = LocalDate.now();
        Path targetDir = root;
        if (!safeSubdir.isEmpty()) {
            targetDir = targetDir.resolve(safeSubdir);
        }
        targetDir = targetDir
                .resolve(String.valueOf(now.getYear()))
                .resolve(String.format("%02d", now.getMonthValue()))
                .normalize()
                .toAbsolutePath();

        try {
            if (!targetDir.startsWith(root.toAbsolutePath())) {
                throw new RuntimeException("Invalid storage path");
            }
            Files.createDirectories(targetDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare upload directory", e);
        }

        // Lấy ext
        String original = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot).toLowerCase();

        String filename = UUID.randomUUID() + ext;

        Path target = targetDir.resolve(filename).normalize();
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file " + filename, e);
        }

        // Trả về public path: /uploads/<safeSubdir>/<yyyy>/<mm>/<filename>
        StringBuilder publicPath = new StringBuilder("/uploads");
        if (!safeSubdir.isEmpty()) publicPath.append("/").append(safeSubdir);
        publicPath.append("/").append(now.getYear())
                .append("/").append(String.format("%02d", now.getMonthValue()))
                .append("/").append(filename);

        return publicPath.toString();
    }

    private String sanitizeSubdir(String subdir) {
        if (!StringUtils.hasText(subdir)) return "";
        // chuẩn hoá: bỏ leading '/', thay '\' -> '/', loại bỏ '..'
        String s = subdir.replace('\\', '/').trim();
        if (s.startsWith("/")) s = s.substring(1);
        // loại bỏ các đoạn '..'
        Path p = Paths.get(s).normalize();
        String cleaned = p.toString().replace('\\', '/');
        if (cleaned.contains("..")) {
            throw new IllegalArgumentException("Invalid subdir");
        }
        return cleaned;
    }

    public boolean deleteIfLocal(String urlOrPath) {
        if (!StringUtils.hasText(urlOrPath)) return false;

        String p = urlOrPath.trim();

        // Bỏ qua URL ngoài (S3/GCS/http…)
        String lower = p.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return false;
        }

        // Chấp nhận "/uploads/..." hoặc "uploads/..."
        if (p.startsWith("/")) p = p.substring(1);
        if (!p.startsWith("uploads/")) {
            // Không phải trong thư mục uploads => bỏ qua
            return false;
        }

        // Lấy phần sau "uploads/" để ghép với root
        String rel = p.substring("uploads/".length());

        // Chuẩn hoá để tránh path traversal
        Path target = root.resolve(rel).normalize().toAbsolutePath();

        // Đảm bảo vẫn nằm trong root uploads
        if (!target.startsWith(root.toAbsolutePath())) {
            throw new RuntimeException("Invalid delete path");
        }

        try {
            return Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + target, e);
        }
    }
}
