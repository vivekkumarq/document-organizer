package com.vivek.docorganizer.service;

import com.vivek.docorganizer.entity.Document;
import com.vivek.docorganizer.entity.User;
import com.vivek.docorganizer.repository.DocumentRepository;
import com.vivek.docorganizer.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    private final UserRepository userRepository;

    public DocumentService(DocumentRepository documentRepository,
                           UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    public List<Document> getDocumentsByUser(Long userId) {
        return documentRepository.findByUserId(userId);
    }

    public void uploadFile(MultipartFile file, String email) throws Exception {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String storagePath = "E:/Vivek2025project/docorganizer/storage/";

        File folder = new File(storagePath);

        if (!folder.exists()) {
            folder.mkdirs();
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        File destination = new File(storagePath + fileName);

        file.transferTo(destination);

        Document document = new Document(
                file.getOriginalFilename(),
                destination.getAbsolutePath(),
                user.getId(),
                LocalDateTime.now()
        );

        documentRepository.save(document);
    }

    public Document getDocument(Long id) {

        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }
}