package dev.adrintake.service;


import dev.adrintake.exception.DocumentNotFoundException;
import dev.adrintake.model.DocumentDetail;
import dev.adrintake.model.DocumentSummary;
import dev.adrintake.repository.DocumentRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository repository;

    public DocumentService(DocumentRepository repository) {
        this.repository = repository;
    }

    public List<DocumentSummary> list() {
        return repository.findAll();
    }

    public DocumentDetail get(String id) {
        return repository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
    }
}