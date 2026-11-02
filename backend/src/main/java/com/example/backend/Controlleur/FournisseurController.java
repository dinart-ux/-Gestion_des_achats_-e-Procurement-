package com.example.backend.Controlleur;

import com.example.backend.DTO.AchatRequests;
import com.example.backend.Service.FournisseurService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/fournisseurs")
public class FournisseurController {
    private final FournisseurService fournisseurs;

    public FournisseurController(FournisseurService fournisseurs) {
        this.fournisseurs = fournisseurs;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DEMANDEUR', 'MANAGER', 'SERVICE_ACHATS', 'ADMIN')")
    public List<Map<String, Object>> getAll() {
        return fournisseurs.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEMANDEUR', 'MANAGER', 'SERVICE_ACHATS', 'ADMIN')")
    public Map<String, Object> getById(@PathVariable int id) {
        return fournisseurs.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SERVICE_ACHATS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody AchatRequests.Fournisseur request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fournisseurs.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVICE_ACHATS', 'ADMIN')")
    public Map<String, Object> update(
            @PathVariable int id,
            @Valid @RequestBody AchatRequests.Fournisseur request) {
        return fournisseurs.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVICE_ACHATS', 'ADMIN')")
    public Map<String, Object> deactivate(@PathVariable int id) {
        return fournisseurs.deactivate(id);
    }
}
