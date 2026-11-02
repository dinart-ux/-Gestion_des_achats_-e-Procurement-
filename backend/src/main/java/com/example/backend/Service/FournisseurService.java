package com.example.backend.Service;

import com.example.backend.DTO.AchatRequests;
import com.example.backend.Persistence.CategorieEntity;
import com.example.backend.Persistence.FournisseurCategorieEntity;
import com.example.backend.Persistence.FournisseurEntity;
import com.example.backend.Repository.CategorieRepository;
import com.example.backend.Repository.FournisseurCategorieRepository;
import com.example.backend.Repository.FournisseurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FournisseurService {
    private final FournisseurRepository fournisseurs;
    private final FournisseurCategorieRepository fournisseurCategories;
    private final CategorieRepository categories;
    private final JdbcTemplate jdbc;

    public FournisseurService(
            FournisseurRepository fournisseurs,
            FournisseurCategorieRepository fournisseurCategories,
            CategorieRepository categories,
            JdbcTemplate jdbc) {
        this.fournisseurs = fournisseurs;
        this.fournisseurCategories = fournisseurCategories;
        this.categories = categories;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return fournisseurs.findAllByOrderByRaisonSocialeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(int id) {
        return toResponse(find(id));
    }

    @Transactional
    public Map<String, Object> create(AchatRequests.Fournisseur request) {
        String raisonSociale = request.raisonSociale().trim();
        verrouillerRaisonSociale(raisonSociale);
        if (fournisseurs.existsByRaisonSocialeIgnoreCase(raisonSociale)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un fournisseur porte déjà cette raison sociale.");
        }

        FournisseurEntity fournisseur = new FournisseurEntity();
        fournisseur.setDateCreation(LocalDateTime.now());
        fournisseur.setActif(true);
        apply(fournisseur, request);
        FournisseurEntity saved = fournisseurs.save(fournisseur);
        replaceCategories(saved.getId(), request.categories());
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> update(int id, AchatRequests.Fournisseur request) {
        FournisseurEntity fournisseur = find(id);
        String raisonSociale = request.raisonSociale().trim();
        verrouillerRaisonSociale(raisonSociale);
        if (fournisseurs.existsByRaisonSocialeIgnoreCaseAndIdNot(raisonSociale, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un autre fournisseur porte déjà cette raison sociale.");
        }

        apply(fournisseur, request);
        FournisseurEntity saved = fournisseurs.save(fournisseur);
        replaceCategories(id, request.categories());
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> deactivate(int id) {
        FournisseurEntity fournisseur = find(id);
        fournisseur.setActif(false);
        return toResponse(fournisseurs.save(fournisseur));
    }

    private FournisseurEntity find(int id) {
        return fournisseurs.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Fournisseur introuvable."));
    }

    private void apply(FournisseurEntity entity, AchatRequests.Fournisseur request) {
        entity.setRaisonSociale(request.raisonSociale().trim());
        entity.setNif(blankToNull(request.nif()));
        entity.setStat(blankToNull(request.stat()));
        entity.setAdresse(blankToNull(request.adresse()));
        entity.setTelephone(blankToNull(request.telephone()));
        entity.setEmail(blankToNull(request.email()));
        entity.setContactNom(blankToNull(request.contactNom()));
        entity.setConditionsPaiement(blankToNull(request.conditionsPaiement()));
        entity.setDelaiLivraisonJours(request.delaiLivraisonJours());
    }

    private void replaceCategories(Integer fournisseurId, List<Integer> requestedIds) {
        List<Integer> ids = requestedIds == null ? List.of() : requestedIds.stream().distinct().toList();
        List<CategorieEntity> found = categories.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Une ou plusieurs catégories sélectionnées n'existent pas.");
        }

        fournisseurCategories.deleteAllByFournisseurId(fournisseurId);
        for (Integer categorieId : ids) {
            FournisseurCategorieEntity relation = new FournisseurCategorieEntity();
            relation.setFournisseurId(fournisseurId);
            relation.setCategorieId(categorieId);
            fournisseurCategories.save(relation);
        }
    }

    private Map<String, Object> toResponse(FournisseurEntity entity) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", entity.getId());
        response.put("raison_sociale", entity.getRaisonSociale());
        response.put("nif", entity.getNif());
        response.put("stat", entity.getStat());
        response.put("adresse", entity.getAdresse());
        response.put("telephone", entity.getTelephone());
        response.put("email", entity.getEmail());
        response.put("contact_nom", entity.getContactNom());
        response.put("conditions_paiement", entity.getConditionsPaiement());
        response.put("delai_livraison_jours", entity.getDelaiLivraisonJours());
        response.put("actif", entity.isActif());
        response.put("categories", fournisseurCategories.findAllByFournisseurId(entity.getId()).stream()
                .map(FournisseurCategorieEntity::getCategorieId)
                .toList());
        return response;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private void verrouillerRaisonSociale(String raisonSociale) {
        // Le verrou transactionnel sérialise deux créations concurrentes du même nom,
        // sans ajouter de contrainte risquant de rejeter des doublons historiques.
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(lower(?), 0))",
                Object.class, raisonSociale);
    }
}
