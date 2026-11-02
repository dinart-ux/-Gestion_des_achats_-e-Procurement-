package com.example.backend.Controlleur;

import com.example.backend.Models.Commande;
import com.example.backend.Models.Demande;
import com.example.backend.Service.DemandeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Deprecated(forRemoval = true)
public class DemandeController {

    private final DemandeService demandeService;

    public DemandeController(DemandeService demandeService) {
        this.demandeService = demandeService;
    }

    @PreAuthorize("hasAnyRole('DEMANDEUR', 'MANAGER', 'SERVICE_ACHATS', 'ADMIN')")
    @GetMapping("/demandes")
    public List<Demande> getAll() {
        return demandeService.getAll();
    }

    @PreAuthorize("hasAnyRole('DEMANDEUR', 'MANAGER', 'SERVICE_ACHATS', 'ADMIN')")
    @GetMapping("/demandes/{id}")
    public ResponseEntity<Demande> getById(@PathVariable Long id) {
        return demandeService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('DEMANDEUR', 'ADMIN')")
    @PostMapping("/demandes")
    public Demande create(@RequestBody Demande demande) {
        return demandeService.save(demande);
    }

    @PreAuthorize("hasAnyRole('DEMANDEUR', 'ADMIN')")
    @PostMapping("/demandes/{id}/submit")
    public Demande submit(@PathVariable Long id) {
        return demandeService.submit(id);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'SERVICE_ACHATS', 'ADMIN')")
    @PostMapping("/demandes/{id}/validate")
    public ResponseEntity<Commande> validate(@PathVariable Long id) {
        Commande c = demandeService.validateAndCreateCommande(id);
        return ResponseEntity.ok(c);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping("/demandes/{id}/reject")
    public Demande reject(@PathVariable Long id, @RequestBody(required = false) String commentaire) {
        return demandeService.reject(id, commentaire);
    }
}
