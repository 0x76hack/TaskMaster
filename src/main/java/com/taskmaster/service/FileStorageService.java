package com.taskmaster.service;

import com.taskmaster.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService implements StorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not initialize upload directory", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Cannot upload an empty file.");
        }
        String originalFileName = StringUtils.cleanPath(Objects.toString(file.getOriginalFilename(), ""));
        if (originalFileName.isBlank() || originalFileName.contains("..")
                || originalFileName.indexOf('/') >= 0 || originalFileName.indexOf('\\') >= 0) {
            throw new BadRequestException("Invalid filename.");
        }
        try {
            String extension = "";
            int extensionIndex = originalFileName.lastIndexOf('.');
            if (extensionIndex > 0 && extensionIndex < originalFileName.length() - 1) {
                String candidate = originalFileName.substring(extensionIndex);
                if (candidate.length() <= 16 && candidate.matches("\\.[A-Za-z0-9]+")) {
                    extension = candidate.toLowerCase();
                }
            }
            String fileName = UUID.randomUUID() + extension;
            Path target = fileStorageLocation.resolve(fileName).normalize();
            if (!target.getParent().equals(fileStorageLocation)) {
                throw new BadRequestException("Invalid upload path.");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.CREATE_NEW);
            return fileName;
        } catch (IOException ex) {
            throw new BadRequestException("Could not store uploaded file. Please try again.", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.matches("[A-Za-z0-9-]+(?:\\.[A-Za-z0-9]+)?")) {
            throw new BadRequestException("Invalid file identifier.");
        }
        try {
            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            if (!filePath.startsWith(fileStorageLocation)) {
                throw new BadRequestException("Invalid file path.");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable() && Files.isRegularFile(filePath)) {
                return resource;
            }
            throw new BadRequestException("File not found.");
        } catch (MalformedURLException ex) {
            throw new BadRequestException("File not found.", ex);
        }
    }
}
