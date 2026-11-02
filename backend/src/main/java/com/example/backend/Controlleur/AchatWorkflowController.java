package com.example.backend.Controlleur;
import com.example.backend.Service.AchatWorkflowService;
import com.example.backend.DTO.AchatRequests;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.Year;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api/v2")
public class AchatWorkflowController {
/*ddd*/

    private boolean roleAutoriseValidations(String role) {
        return List.of("MANAGER", "SERVICE_ACHATS", "ADMIN").contains(role);
    }

    private String getRole(Principal principal) {
    var authentication =
            SecurityContextHolder.getContext().getAuthentication();

    if (principal == null
            || authentication == null
            || !authentication.isAuthenticated()
            || authentication.getAuthorities() == null) {
        throw new AccessDeniedException(
                "Utilisateur non authentifié");
    }

    return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(role -> role != null && !role.isBlank())
            .map(role -> role.startsWith("ROLE_")
                    ? role.substring(5)
                    : role)
            .findFirst()
            .orElseThrow(() ->
                    new AccessDeniedException(
                            "Aucun rôle attribué"));
}
/*ddd*/
    private final AchatWorkflowService achatWorkflowService;

    public AchatWorkflowController(AchatWorkflowService achatWorkflowService) {
        this.achatWorkflowService = achatWorkflowService;
    }

    @GetMapping("/demandes/{id}/offres")
@PreAuthorize("hasAnyRole('DEMANDEUR', 'MANAGER', 'SERVICE_ACHATS', 'ADMIN')")
public ResponseEntity<?> offres(
        @PathVariable long id,
        Principal principal) {
    try {
        String username = principal != null
                ? principal.getName()
                : null;

        String role = getRole(principal);

        return ResponseEntity.ok(
                achatWorkflowService.offresAccessibles(
                        id, username, role
                )
        );

    } catch (AccessDeniedException e) {
        return ResponseEntity.status(403)
                .body(Map.of("error", e.getMessage()));

    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
    }
}

@PostMapping("/demandes/{id}/offres")
@PreAuthorize("hasAnyRole('SERVICE_ACHATS', 'ADMIN')")
public ResponseEntity<?> creerOffre(
        @PathVariable long id,
        @Valid @RequestBody AchatRequests.Offre offre,
        Principal principal) {
    try {
        return ResponseEntity.status(201)
                .body(achatWorkflowService.creerOffre(id, offre));

    } catch (AccessDeniedException e) {
        return ResponseEntity.status(403)
                .body(Map.of("error", e.getMessage()));

    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
    }
}

    @PostMapping("/demandes/{id}/offres/{offreId}/retenir")
@PreAuthorize("hasAnyRole('SERVICE_ACHATS', 'ADMIN')")
public ResponseEntity<?> retenirOffre(
        @PathVariable long id,
        @PathVariable long offreId,
        Principal principal) {

    try {
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Utilisateur non connecté."));
        }

        return ResponseEntity.ok(
                achatWorkflowService.retenirOffreEtCreerCommande(
                        id,
                        offreId,
                        principal.getName()
                )
        );
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
    } catch (AccessDeniedException e) {
        return ResponseEntity.status(403)
                .body(Map.of("message", e.getMessage()));
    }
}

    @PostMapping("/demandes/{id}/offres/{offreId}/refuser")
    @PreAuthorize("hasAnyRole('SERVICE_ACHATS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> refuserOffre(
            @PathVariable long id, @PathVariable long offreId) {
        return ResponseEntity.ok(achatWorkflowService.refuserOffre(id, offreId));
    }
/*@PostMapping("/demandes/{id}/offres/{offreId}/commande")
@PreAuthorize("hasAnyRole('SERVICE_ACHATS', 'ADMIN')")
public ResponseEntity<Map<String, Object>> creerCommandeDepuisOffre(
        @PathVariable long id,
        @PathVariable long offreId,
        Principal principal) {

    String username = principal != null
            ? principal.getName()
            : null;

    if (username == null) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Utilisateur non authentifié."
        );
    }

    return ResponseEntity.ok(
            achatWorkflowService.creerCommande(id, offreId, username)
    );
}*/
@PostMapping("/demandes/{id}/offres/{offreId}/commande")
public ResponseEntity<?> creerCommandeDepuisOffre(
        @PathVariable("id") long demandeId,
        @PathVariable("offreId") long offreId,
        Principal principal) {
    try {
        String username = principal != null
                ? principal.getName()
                : null;

        String role = getRole(principal);

        return ResponseEntity.ok(
                achatWorkflowService.creerCommande(
                        demandeId,
                        offreId,
                        username
                )
        );
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
    } catch (AccessDeniedException e) {
        return ResponseEntity.status(403)
                .body(Map.of("message", e.getMessage()));
    }
}

    @GetMapping("/demandes")
    public ResponseEntity<List<Map<String, Object>>> demandes(
        Principal principal) {

    String username = principal != null ? principal.getName() : null;
    String role = getRole(principal);

    return ResponseEntity.ok(
        achatWorkflowService.demandes(username, role)
    );
    }

    @PostMapping("/demandes")
    @PreAuthorize("hasRole('DEMANDEUR')")
    public ResponseEntity<?> creerDemande(
            @Valid @RequestBody AchatRequests.Demande demandeDto,
            Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return ResponseEntity.status(201).body(
                achatWorkflowService.creerDemande(username, demandeDto));
    }

    @PutMapping("/demandes/{id}")
    @PreAuthorize("hasRole('DEMANDEUR')")
    public ResponseEntity<?> modifierBrouillon(
            @PathVariable long id,
            @Valid @RequestBody AchatRequests.Demande demandeDto,
            Principal principal) {
        try {
            return ResponseEntity.ok(
                    achatWorkflowService.modifierBrouillon(id, principal.getName(), demandeDto));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/demandes/{id}")
    @PreAuthorize("hasRole('DEMANDEUR')")
    public ResponseEntity<?> supprimerBrouillon(
            @PathVariable long id,
            Principal principal) {
        try {
            achatWorkflowService.supprimerBrouillon(id, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
@GetMapping("/demandes/{id}")
public ResponseEntity<?> detailDemande(
        @PathVariable long id,
        Principal principal) {
    try {
        String username = principal != null ? principal.getName() : null;
        String role = getRole(principal);

        return ResponseEntity.ok(
            achatWorkflowService.demandeAccessible(id, username, role)
        );
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
        return ResponseEntity.notFound().build();
    } catch (org.springframework.security.access.AccessDeniedException e) {
        return ResponseEntity.status(403)
            .body(Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", e.getMessage()));
    }
}

@PostMapping("/demandes/{id}/soumettre")
@PreAuthorize("hasRole('DEMANDEUR')")
public ResponseEntity<?> soumettreDemande(
        @PathVariable long id,
        Principal principal) {
    try {
        String username = principal != null ? principal.getName() : null;

        if (username == null) {
            return ResponseEntity.status(401)
                .body(Map.of("error", "Utilisateur non connecté."));
        }

        return ResponseEntity.ok(
            achatWorkflowService.soumettre(id, username)
        );

    } catch (org.springframework.security.access.AccessDeniedException e) {
        return ResponseEntity.status(403)
            .body(Map.of("error", e.getMessage()));

    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", e.getMessage()));
    }
}
@PostMapping("/demandes/{id}/decision")
@PreAuthorize("hasAnyRole('MANAGER', 'SERVICE_ACHATS', 'ADMIN')")
public ResponseEntity<?> deciderDemande(
        @PathVariable long id,
        @Valid @RequestBody AchatRequests.Decision request,
        Principal principal) {

    try {
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Utilisateur non connecté."));
        }

        String username = principal.getName();
        String role = getRole(principal);

        return ResponseEntity.ok(
                achatWorkflowService.decider(
                        id,
                        username,
                        role,
                        request
                )
        );

    } catch (AccessDeniedException e) {
        return ResponseEntity.status(403)
                .body(Map.of("error", e.getMessage()));

    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
    }
}
    
    // ==================== GESTION DES SERVICES ====================

    @GetMapping("/services")
    public ResponseEntity<?> services(Principal principal) {
        String role = getRole(principal);
        return ResponseEntity.ok(
                achatWorkflowService.servicesAccessibles(principal.getName(), role)
        );
    }

    @PostMapping("/services")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> creerService(
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        try {
            verifierAdmin(principal);

            return ResponseEntity.ok(
                    achatWorkflowService.creerService(payload)
            );
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Impossible de créer le service."
                    ));
        }
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> modifierService(
            @PathVariable int id,
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        try {
            verifierAdmin(principal);

            return ResponseEntity.ok(
                    achatWorkflowService.modifierService(id, payload)
            );
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Impossible de modifier le service."
                    ));
        }
    }

    @DeleteMapping("/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> supprimerService(
            @PathVariable int id,
            Principal principal) {
        try {
            verifierAdmin(principal);

            return ResponseEntity.ok(
                    achatWorkflowService.supprimerService(id)
            );
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Impossible de supprimer le service."
                    ));
        }
    }

    private void verifierAdmin(Principal principal) {
        String role = getRole(principal);

        if (!"ADMIN".equals(role)) {
            throw new AccessDeniedException(
                    "Seul l'administrateur peut gérer les services."
            );
        }
    }


    
@GetMapping("/centres-cout")
public ResponseEntity<?> centresCout(Principal principal) {
    try {
        getRole(principal);

        String identifiant = principal.getName();

        return ResponseEntity.ok(
                achatWorkflowService.centresCout(identifiant)
        );
    } catch (AccessDeniedException e) {
        return ResponseEntity.status(403)
                .body(Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.internalServerError()
                .body(Map.of(
                        "error",
                        "Impossible de charger les centres de coût."
                ));
    }
}

    @PostMapping("/centres-cout")
    @PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> creerCentreCout(
        @RequestBody Map<String, Object> payload,
        Principal principal) {
    try {
        String role = getRole(principal);

        if (!"ADMIN".equals(role)) {
            throw new AccessDeniedException(
                    "Seul l'administrateur peut créer un centre de coût."
            );
        }

        return ResponseEntity.ok(
                achatWorkflowService.creerCentreCout(payload)
        );
    } catch (AccessDeniedException e) {
        return ResponseEntity.status(403)
                .body(Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error",
                        e.getMessage() != null
                                ? e.getMessage()
                                : "Impossible de créer le centre de coût."
                ));
    }
}
    /*@GetMapping("validations/en-attente")
    public ResponseEntity<List<Map<String, Object>>> validEnAttente(Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(achatWorkflowService.validationsEnAttente(username));
    }*/
    @GetMapping("/validations/en-attente")
    @PreAuthorize("hasAnyRole('MANAGER', 'SERVICE_ACHATS', 'ADMIN')")
    public ResponseEntity<?> validEnAttente(
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) BigDecimal montantMin,
            @RequestParam(required = false) BigDecimal montantMax,
            @RequestParam(required = false) String demandeur,
            Principal principal) {
        if (montantMin != null && montantMin.signum() < 0
                || montantMax != null && montantMax.signum() < 0
                || montantMin != null && montantMax != null && montantMin.compareTo(montantMax) > 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "La plage de montants est invalide."));
        }
        String role = getRole(principal);
        if (!roleAutoriseValidations(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux responsables de validation."));
        }
        String username = principal.getName();
        return ResponseEntity.ok(achatWorkflowService.validationsEnAttente(
                username, role, serviceId, montantMin, montantMax,
                demandeur == null || demandeur.isBlank() ? null : demandeur.trim()));
    }

    @GetMapping("/validations/traitees")
    @PreAuthorize("hasAnyRole('MANAGER', 'SERVICE_ACHATS', 'ADMIN')")
    public ResponseEntity<?> validationsTraitees(
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) BigDecimal montantMin,
            @RequestParam(required = false) BigDecimal montantMax,
            @RequestParam(required = false) String demandeur,
            Principal principal) {
        if (montantMin != null && montantMin.signum() < 0
                || montantMax != null && montantMax.signum() < 0
                || montantMin != null && montantMax != null && montantMin.compareTo(montantMax) > 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "La plage de montants est invalide."));
        }
        String role = getRole(principal);
        if (!roleAutoriseValidations(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux responsables de validation."));
        }
        String username = principal.getName();
        return ResponseEntity.ok(achatWorkflowService.validationsTraitees(
                username, role, serviceId, montantMin, montantMax,
                demandeur == null || demandeur.isBlank() ? null : demandeur.trim()));
    }

@GetMapping("/commandes")
public ResponseEntity<List<Map<String, Object>>> commandes(
        Principal principal) {

    String username = principal != null ? principal.getName() : null;
    String role = getRole(principal);

    return ResponseEntity.ok(
            achatWorkflowService.commandes(username, role)
    );
}

/*@PostMapping("/demandes/{id}/offres/{offreId}/commande")
@PreAuthorize("hasAnyRole('SERVICE_ACHATS', 'ADMIN')")
public ResponseEntity<?> creerCommande(
        @PathVariable long id,
        @PathVariable long offreId,
        Principal principal) {
    try {
        return ResponseEntity.status(201).body(
                achatWorkflowService.creerCommande(
                        id,
                        offreId,
                        principal.getName()
                )
        );
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Offre retenue introuvable."));
    }
}*/

@PostMapping("/commandes/{commandeId}/receptions")
@PreAuthorize("hasAnyRole('SERVICE_ACHATS', 'ADMIN')")
public ResponseEntity<?> enregistrerReception(
        @PathVariable long commandeId,
        @Valid @RequestBody AchatRequests.Reception reception,
        Principal principal) {
    try {
        return ResponseEntity.status(201).body(
                achatWorkflowService.receptionner(
                        commandeId,
                        principal.getName(),
                        reception
                )
        );
    } catch (org.springframework.security.access.AccessDeniedException e) {
        return ResponseEntity.status(403)
                .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(
                Map.of(
                        "error",
                        e.getMessage() != null
                                ? e.getMessage()
                                : "Impossible d'enregistrer la réception."
                )
        );
    }
}

@GetMapping("/commandes/{commandeId}/lignes")
@PreAuthorize("hasAnyRole('DEMANDEUR', 'MANAGER', 'SERVICE_ACHATS', 'ADMIN')")
public ResponseEntity<?> lignesCommande(
        @PathVariable long commandeId,
        Principal principal) {
    try {
        String username = principal != null
                ? principal.getName()
                : null;

        String role = getRole(principal);

        return ResponseEntity.ok(
                achatWorkflowService.lignesCommande(
                        commandeId,
                        username,
                        role
                )
        );
    } catch (AccessDeniedException e) {
        return ResponseEntity.status(403)
                .body(Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
    }
}



@GetMapping("/budgets")
@PreAuthorize("hasAnyRole('MANAGER', 'SERVICE_ACHATS', 'ADMIN')")
public ResponseEntity<List<Map<String, Object>>> budgets(
        @RequestParam(required = false) Integer annee,
        @RequestParam(required = false) Integer centreCoutId,
        Principal principal) {

    String username = principal != null ? principal.getName() : null;
    String role = getRole(principal);

    int anneeRecherchee = annee != null
            ? annee
            : Year.now().getValue();

    return ResponseEntity.ok(
            achatWorkflowService.budgets(
                    anneeRecherchee, centreCoutId, username, role
            )
    );
}

@GetMapping("/budgets/centres-cout")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<List<Map<String, Object>>> centresCoutActifsPourBudget() {
    return ResponseEntity.ok(achatWorkflowService.centresCoutActifsPourBudget());
}

@PutMapping("/budgets")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> enregistrerBudget(
        @Valid @RequestBody AchatRequests.Budget budget) {
    try {
        return ResponseEntity.ok(achatWorkflowService.enregistrerBudget(budget));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

@GetMapping("/rapports")
@PreAuthorize("hasAnyRole('SERVICE_ACHATS', 'ADMIN')")
public ResponseEntity<Map<String, Object>> rapport(
        @RequestParam LocalDate du,
        @RequestParam LocalDate au) {

    return ResponseEntity.ok(
            achatWorkflowService.rapport(du, au)
    );
}
@PostMapping("/categories")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> creerCategorie(
        @RequestBody Map<String, String> donnees,
        Principal principal) {
    try {
        verifierAdmin(principal);

        String nom = donnees.get("nom");
        String description = donnees.get("description");

        if (nom == null || nom.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Le nom de la catégorie est obligatoire."
                    ));
        }

        return ResponseEntity.ok(
                achatWorkflowService.creerCategorie(
                        nom.trim(),
                        description == null ? "" : description.trim()
                )
        );

    } catch (AccessDeniedException e) {
        return ResponseEntity.status(403)
                .body(Map.of("message", e.getMessage()));

    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "message",
                        e.getMessage() != null
                                ? e.getMessage()
                                : "Impossible de créer la catégorie."
                ));
    }
}

@PutMapping("/categories/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> modifierCategorie(
        @PathVariable Integer id,
        @RequestBody Map<String, String> donnees,
        Principal principal) {
    verifierAdmin(principal);

    try {
        return ResponseEntity.ok(
            achatWorkflowService.modifierCategorie(
                id,
                donnees.get("nom"),
                donnees.get("description")
            )
        );
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", e.getMessage()));
    }
}

@PutMapping("/categories/{id}/desactiver")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> desactiverCategorie(
        @PathVariable Integer id,
        Principal principal) {
    verifierAdmin(principal);

    try {
        achatWorkflowService.desactiverCategorie(id);
        return ResponseEntity.ok(
            Map.of("message", "Catégorie désactivée avec succès.")
        );
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", e.getMessage()));
    }
}

@PutMapping("/categories/{id}/reactiver")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> reactiverCategorie(
        @PathVariable Integer id,
        Principal principal) {
    verifierAdmin(principal);

    try {
        achatWorkflowService.reactiverCategorie(id);
        return ResponseEntity.ok(
            Map.of("message", "Catégorie réactivée avec succès.")
        );
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", e.getMessage()));
    }
}


}
