// src/main/java/com/hrm/hrmapi/service/FileStorageService.java
package com.hrm.hrmapi.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;

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

    /** Lưu avatar và trả về path public, ví dụ: /uploads/xxxx.png */
    public String saveAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String original = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot);

        String filename = UUID.randomUUID() + ext.toLowerCase();
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, root.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file " + filename, e);
        }
        return "/uploads/" + filename;
    }
}
