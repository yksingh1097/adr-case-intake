package dev.adrintake.controller;

import dev.adrintake.model.DocumentDetail;
import dev.adrintake.model.DocumentSummary;
import dev.adrintake.service.DocumentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/documents")

    public List<DocumentSummary> list() {
        return documentService.list();
    }

    @GetMapping("/documents/{id}")
    public DocumentDetail get(@PathVariable String id) {
        return documentService.get(id);
    }
}