package com.vivek.docorganizer.controller;

import com.vivek.docorganizer.entity.Document;
import com.vivek.docorganizer.entity.User;
import com.vivek.docorganizer.repository.UserRepository;
import com.vivek.docorganizer.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository;

    public DocumentController(DocumentService documentService,
                              UserRepository userRepository) {
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    @GetMapping("/user/{userId}")
    public List<Document> getUserDocuments(@PathVariable Long userId) {
        return documentService.getDocumentsByUser(userId);
    }

    @GetMapping("/my")
    public List<Document> myDocuments() {

        String email = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return documentService.getDocumentsByUser(user.getId());
    }

    @PostMapping("/upload")
    public String uploadDocument(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) throws Exception {

        String email = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        documentService.uploadFile(file, email);

        return "File uploaded successfully";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) throws Exception {

        var document = documentService.getDocument(id);

        File file = new File(document.getFilePath());

        Resource resource = new UrlResource(file.toURI());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getName() + "\"")
                .body(resource);
    }
}