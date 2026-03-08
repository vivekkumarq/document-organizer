package com.vivek.docorganizer.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String filePath;
    private String category;
    private LocalDate expiryDate;
    private Long userId;

    private LocalDateTime uploadedAt;

    public Document() {}

    public Document(String name, String filePath, Long userId, LocalDateTime uploadedAt) {
        this.name = name;
        this.filePath = filePath;
        this.userId = userId;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() { return id; }

    public String getName() { return name; }

    public String getFilePath() { return filePath; }

    public Long getUserId() { return userId; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }

    public void setName(String name) { this.name = name; }

    public void setFilePath(String filePath) { this.filePath = filePath; }

    public void setUserId(Long userId) { this.userId = userId; }

    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}