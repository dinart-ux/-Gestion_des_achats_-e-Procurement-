package com.example.backend.Service;

import com.example.backend.DTO.AchatRequests;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Accès SQL aligné sur le script SQL réel. */
@Service
public class AchatWorkflowService {
    private final JdbcTemplate jdbc;

    public AchatWorkflowService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> demandes(String email, String role) {
        int uid = userId(email);
        String sql = """
                SELECT d.id AS id, d.reference AS reference,
                        d.objet AS objet, d.montant_estime AS montant_estime,
                        d.statut AS statut, d.date_creation AS date_creation,
                        u.nom AS demandeur_nom, u.prenom AS demandeur_prenom
                FROM public.demande_achat d
                JOIN public.utilisateurs u ON u.id = d.demandeur_id
                """;
        if (role.equals("ADMIN") || role.equals("SERVICE_ACHATS")) {
            return jdbc.queryForList(sql + " ORDER BY d.date_creation DESC");
        }
        if (role.equals("MANAGER")) {
            return jdbc.queryForList(sql + " WHERE d.demandeur_id = ?"
                    + " OR u.manager_id = ?"
                    + " ORDER BY d.date_creation DESC", uid, uid);
        }
        return jdbc.queryForList(sql + " WHERE d.demandeur_id = ?"
                + " ORDER BY d.date_creation DESC", uid);
    }

    @Transactional
    public Map<String, Object> creerDemande(String email, AchatRequests.Demande request) {
        int demandeurId = userId(email);
        verifierCentrePourUtilisateur(demandeurId, request.centreCoutId());
        BigDecimal total = totalDemande(request.lignes());
        String reference = "DA-" + LocalDate.now().toString().replace("-", "") + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Integer id = jdbc.queryForObject("""
                INSERT INTO public.demande_achat
                    (reference, objet, statut, justification, centre_cout_id, demandeur_id, montant_estime, date_creation)
                VALUES (?, ?, 'BROUILLON', ?, ?, ?, ?, CURRENT_TIMESTAMP)
                RETURNING id
                """, Integer.class, reference, request.objet().trim(),
                request.justification(), request.centreCoutId(), demandeurId, total);
        insererLignes(id, request.lignes());
        historique(id, null, "BROUILLON", demandeurId, "Création du brouillon");
        return detailDemande(id);
    }

    @Transactional
    public Map<String, Object> modifierBrouillon(long id, String email, AchatRequests.Demande request) {
        int uid = userId(email);
        Map<String, Object> current = jdbc.queryForMap("SELECT statut AS statut, demandeur_id AS demandeur FROM public.demande_achat WHERE id=? FOR UPDATE", id);
        verifierProprietaire(current, uid);
        if (!"BROUILLON".equals(current.get("statut"))) throw new IllegalArgumentException("Seul un brouillon peut être modifié.");
        verifierCentrePourUtilisateur(uid, request.centreCoutId());
        BigDecimal total = totalDemande(request.lignes());
        jdbc.update("""
                UPDATE public.demande_achat
                SET centre_cout_id=?, objet=?, justification=?, montant_estime=?
                WHERE id=?
                """, request.centreCoutId(), request.objet().trim(), request.justification(), total, id);
        jdbc.update("DELETE FROM public.ligne_demande WHERE demande_id=?", id);
        insererLignes((int) id, request.lignes());
        return detailDemande(id);
    }

    @Transactional
    public void supprimerBrouillon(long id, String email) {
        int uid = userId(email);
        Map<String, Object> current = jdbc.queryForMap("SELECT statut AS statut, demandeur_id AS demandeur FROM public.demande_achat WHERE id=? FOR UPDATE", id);
        verifierProprietaire(current, uid);
        if (!"BROUILLON".equals(current.get("statut"))) throw new IllegalArgumentException("Seul un brouillon peut être supprimé.");
        Boolean validationsExistantes = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM public.validation WHERE demande_id=?)",
                Boolean.class, id);
        if (Boolean.TRUE.equals(validationsExistantes)) {
            throw new IllegalArgumentException(
                    "Un brouillon ayant déjà un historique de validation ne peut pas être supprimé. Corrigez-le puis resoumettez-le.");
        }
        jdbc.update("DELETE FROM public.demande_achat WHERE id=?", id);
    }

    public List<Map<String, Object>> listerUtilisateurs() {
    return jdbc.queryForList("""
            SELECT u.id AS id, u.matricule AS matricule, u.nom AS nom, u.prenom AS prenom,u.email AS email, u.actif AS actif, r.code AS role, s.nom AS service
            FROM public.utilisateurs u
            LEFT JOIN public.role r ON r.id = u.role_id
            LEFT JOIN public.service s ON s.id = u.service_id
            ORDER BY u.id DESC
            """);
}

    @Transactional
    public Map<String, Object> creerUtilisateur(Map<String, Object> payload) {
        String matricule = (String) payload.get("matricule");
        String nom = (String) payload.get("nom");
        String prenom = (String) payload.get("prenom");
        String email = (String) payload.get("email");
        //mot de passe temporaire par défaut
        String motDePasse = payload.get("motDePasse") != null ? (String) payload.get("motDePasse") : "Pass1234@";
        Integer roleId = ((Number) payload.get("roleId")).intValue();
        Integer serviceId = payload.get("serviceId") != null ? ((Number) payload.get("serviceId")).intValue() : null;

        Integer id = jdbc.queryForObject("""
                INSERT INTO public.utilisateurs (matricule, nom, prenom, email, mot_de_passe, role_id, service_id, actif)
                VALUES (?, ?, ?, ?, ?, ?, ?, true)
                RETURNING id
                """, Integer.class, matricule, nom, prenom, email, motDePasse, roleId, serviceId);

        return Map.of("id", id, "message", "Utilisateur créé avec succès");
    }

    public Map<String, Object> detailDemande(long id) {
        Map<String, Object> result = jdbc.queryForMap("""
                SELECT d.id AS id, d.reference AS reference,
                        d.objet AS objet, d.justification AS justification,
                        d.montant_estime AS montant_estime, d.statut AS statut,
                        d.date_creation AS date_creation, d.date_soumission AS date_soumission,
                        d.demandeur_id AS demandeur_id, d.centre_cout_id AS centre_cout_id,
                        u.nom AS demandeur_nom, u.prenom AS demandeur_prenom,
                        cc.code AS centre_cout_code, cc.libelle AS centre_cout
                FROM public.demande_achat d
                JOIN public.utilisateurs u ON u.id = d.demandeur_id
                JOIN public.centre_cout cc ON cc.id = d.centre_cout_id
                WHERE d.id=?
                """, id);
        result.put("lignes", jdbc.queryForList("""
                SELECT l.id AS id, l.categorie_id AS categorie_id,
                    l.type AS type, l.designation AS designation,
                    l.quantite AS quantite, l.prix_unitaire_estime AS prix_unitaire_estime,
                    c.nom AS categorie
                FROM public.ligne_demande l 
                LEFT JOIN public.categorie c ON c.id = l.categorie_id
                WHERE l.demande_id=? ORDER BY l.id
                """, id));
        result.put("historique", jdbc.queryForList("""
                SELECT h.ancien_statut AS ancien_statut, h.nouveau_statut AS nouveau_statut,
                    h.commentaire AS commentaire, h.date_changement AS date_changement,
                    u.nom AS utilisateur_nom, u.prenom AS utilisateur_prenom
                FROM public.historique_statut h
                LEFT JOIN public.utilisateurs u ON u.id = h.utilisateur_id
                WHERE h.demande_id=? ORDER BY h.date_changement, h.id
                """, id));
        Boolean validationsExistantes = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM public.validation WHERE demande_id=?)",
                Boolean.class, id);
        result.put("peut_supprimer", !Boolean.TRUE.equals(validationsExistantes));
        return result;
    }

    public Map<String, Object> demandeAccessible(long id, String email, String role) {
        Map<String, Object> demande = detailDemande(id);
        if (role.equals("ADMIN") || role.equals("SERVICE_ACHATS")) return demande;
        int uid = userId(email);
        int demandeur = ((Number) demande.get("demandeur_id")).intValue();
        if (uid == demandeur) return demande;
        if (role.equals("MANAGER") && Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM public.utilisateurs d
                    WHERE d.id=? AND d.manager_id=?)
                """, Boolean.class, demandeur, uid))) return demande;
        throw new org.springframework.security.access.AccessDeniedException("Accès refusé à cette demande.");
    }

    @Transactional
    public Map<String, Object> soumettre(long id, String email) {
        int uid = userId(email);
        Map<String, Object> demande = jdbc.queryForMap("SELECT statut AS statut, demandeur_id AS demandeur FROM public.demande_achat WHERE id=? FOR UPDATE", id);
        verifierProprietaire(demande, uid);
        if (!"BROUILLON".equals(demande.get("statut"))) throw new IllegalArgumentException("Seul un brouillon peut être soumis.");
        int count = jdbc.queryForObject("SELECT count(*) FROM public.ligne_demande WHERE demande_id=?", Integer.class, id);
        if (count == 0) throw new IllegalArgumentException("Ajoutez au moins une ligne avant de soumettre la demande.");
        Integer derniereTentative = jdbc.queryForObject(
                "SELECT COALESCE(MAX(tentative), 0) FROM public.validation WHERE demande_id=?",
                Integer.class, id);
        int tentative = (derniereTentative == null ? 0 : derniereTentative) + 1;
        if (tentative > 1) {
            Integer precisionsDemandees = jdbc.queryForObject("""
                    SELECT count(*) FROM public.validation
                    WHERE demande_id=? AND tentative=? AND decision='PRECISION_DEMANDEE'
                    """, Integer.class, id, tentative - 1);
            if (precisionsDemandees == null || precisionsDemandees == 0) {
                throw new IllegalArgumentException(
                        "La demande possède déjà une tentative de validation qui ne s'est pas terminée par une demande de précision.");
            }
        }
        Integer centreId = jdbc.queryForObject("SELECT centre_cout_id FROM public.demande_achat WHERE id=?", Integer.class, id);
        Integer serviceId = jdbc.queryForObject("SELECT service_id FROM public.centre_cout WHERE id=?", Integer.class, centreId);
        verifierManagerResponsable(uid, id, serviceId);
        Integer circuit;
        try {
            circuit = jdbc.queryForObject("""
                    SELECT id FROM public.circuit_validation
                    WHERE actif AND (service_id IS NULL OR service_id=?)
                    ORDER BY (service_id IS NULL), id DESC LIMIT 1
                    """, Integer.class, serviceId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Aucun circuit de validation actif n'est configuré pour le service de cette demande.");
        }
        Integer managerLevel;
        try {
            managerLevel = jdbc.queryForObject("""
                    SELECT n.id
                    FROM public.niveau_validation n
                    JOIN public.role r ON r.id=n.role_id
                    WHERE n.circuit_id=? AND r.code='MANAGER'
                    ORDER BY n.ordre LIMIT 1
                    """, Integer.class, circuit);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Le circuit actif ne comporte aucun niveau MANAGER. La demande ne peut pas être soumise.");
        }
        BigDecimal amount = jdbc.queryForObject("SELECT montant_estime FROM public.demande_achat WHERE id=?", BigDecimal.class, id);
        List<Integer> levels = jdbc.queryForList("""
                SELECT n.id
                FROM public.niveau_validation n
                WHERE n.circuit_id=? AND (n.seuil_montant<=? OR n.id=?)
                ORDER BY CASE WHEN n.id=? THEN 0 ELSE 1 END, n.ordre
                """, Integer.class, circuit, amount, managerLevel, managerLevel);
        if (levels.isEmpty()) throw new IllegalArgumentException("Aucun circuit de validation ne correspond à cette demande.");
        for (Integer level : levels) {
            jdbc.update("""
                    INSERT INTO public.validation (demande_id, niveau_id, tentative, decision, date_decision)
                    VALUES (?, ?, ?, 'EN_ATTENTE', NULL)
                    """, id, level, tentative);
        }
        jdbc.update("UPDATE public.demande_achat SET statut='SOUMISE', date_soumission=now() WHERE id=?", id);
        historique(id, "BROUILLON", "SOUMISE", uid, "Demande soumise");
        return detailDemande(id);
    }

    private void verifierManagerResponsable(int demandeurId, long demandeId, Integer serviceCentre) {
        Map<String, Object> demandeur = jdbc.queryForMap("""
                SELECT manager_id, service_id
                FROM public.utilisateurs
                WHERE id=?
                """, demandeurId);
        Integer managerId = (Integer) demandeur.get("manager_id");
        Integer serviceDemandeur = (Integer) demandeur.get("service_id");
        if (managerId == null) {
            throw new IllegalArgumentException("Aucun Manager responsable n'est associé à votre compte. Contactez l'administrateur avant de soumettre une demande.");
        }
        Boolean valide = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.utilisateurs manager
                    JOIN public.role role_manager ON role_manager.id=manager.role_id
                    WHERE manager.id=? AND manager.actif=TRUE
                      AND role_manager.code='MANAGER'
                      AND manager.service_id=?
                      AND manager.service_id=?
                      AND manager.id<>?
                )
                """, Boolean.class, managerId, serviceDemandeur, serviceCentre, demandeurId);
        if (!Boolean.TRUE.equals(valide)) {
            throw new IllegalArgumentException("Le Manager associé à votre compte est absent, inactif ou ne respecte pas le rôle et le service requis. Contactez l'administrateur.");
        }
    }

    public List<Map<String, Object>> validationsEnAttente(
            String email, String role, Integer serviceId,
            BigDecimal montantMin, BigDecimal montantMax, String demandeur) {
        int uid = userId(email);
        return jdbc.queryForList("""
                SELECT d.id AS demande_id, d.reference AS reference,
                    d.objet AS objet, d.montant_estime AS montant_estime,
                    d.date_soumission AS date_soumission,
                    v.id AS validation_id, v.tentative AS tentative, n.ordre AS niveau,
                    n.id=(SELECT nm.id FROM public.niveau_validation nm
                        JOIN public.role rm ON rm.id=nm.role_id
                        WHERE nm.circuit_id=n.circuit_id AND rm.code='MANAGER'
                        ORDER BY nm.ordre LIMIT 1) AS manager_obligatoire,
                    u.nom AS demandeur_nom, u.prenom AS demandeur_prenom,
                    s.id AS service_id, s.nom AS service_nom
                FROM public.validation v
                JOIN public.niveau_validation n ON n.id=v.niveau_id
                JOIN public.demande_achat d ON d.id=v.demande_id
                JOIN public.utilisateurs u ON u.id=d.demandeur_id
                JOIN public.centre_cout cc ON cc.id=d.centre_cout_id
                JOIN public.service s ON s.id=cc.service_id
                WHERE d.statut='SOUMISE' AND v.decision='EN_ATTENTE'
                    AND v.tentative=(SELECT MAX(active.tentative) FROM public.validation active WHERE active.demande_id=v.demande_id)
                    AND (?='ADMIN' OR (n.role_id IN (SELECT id FROM public.role WHERE code = ?) AND (SELECT service_id FROM public.utilisateurs WHERE id=?)=s.id))
                    AND (?<>'MANAGER' OR u.manager_id=?)
                    AND (CAST(? AS INTEGER) IS NULL OR s.id=?)
                    AND (CAST(? AS NUMERIC) IS NULL OR d.montant_estime>=?)
                    AND (CAST(? AS NUMERIC) IS NULL OR d.montant_estime<=?)
                    AND (CAST(? AS TEXT) IS NULL OR concat_ws(' ', u.prenom, u.nom) ILIKE '%' || ? || '%')
                    AND NOT EXISTS (SELECT 1 FROM public.validation p
                    JOIN public.niveau_validation pn ON pn.id=p.niveau_id
                    WHERE p.demande_id=v.demande_id AND p.tentative=v.tentative AND p.decision='EN_ATTENTE'
                      AND (CASE WHEN pn.id=(SELECT nm.id FROM public.niveau_validation nm
                            JOIN public.role rm ON rm.id=nm.role_id
                            WHERE nm.circuit_id=pn.circuit_id AND rm.code='MANAGER'
                            ORDER BY nm.ordre LIMIT 1) THEN 0 ELSE pn.ordre END)
                        < (CASE WHEN n.id=(SELECT nm.id FROM public.niveau_validation nm
                            JOIN public.role rm ON rm.id=nm.role_id
                            WHERE nm.circuit_id=n.circuit_id AND rm.code='MANAGER'
                            ORDER BY nm.ordre LIMIT 1) THEN 0 ELSE n.ordre END))
                ORDER BY d.date_soumission, d.id
                """, role, role, uid, role, uid, serviceId, serviceId,
                montantMin, montantMin, montantMax, montantMax,
                demandeur, demandeur);
    }

    public List<Map<String, Object>> validationsTraitees(
            String email, String role, Integer serviceId,
            BigDecimal montantMin, BigDecimal montantMax, String demandeur) {
        int uid = userId(email);
        return jdbc.queryForList("""
                SELECT d.id AS demande_id, d.reference AS reference,
                    d.objet AS objet, d.montant_estime AS montant_estime,
                    v.id AS validation_id, v.tentative AS tentative,
                    v.tentative=(SELECT MAX(active.tentative) FROM public.validation active WHERE active.demande_id=v.demande_id) AS tentative_active,
                    n.ordre AS niveau,
                    n.id=(SELECT nm.id FROM public.niveau_validation nm
                        JOIN public.role rm ON rm.id=nm.role_id
                        WHERE nm.circuit_id=n.circuit_id AND rm.code='MANAGER'
                        ORDER BY nm.ordre LIMIT 1) AS manager_obligatoire,
                    v.decision AS decision, v.commentaire AS commentaire,
                    v.date_decision AS date_decision,
                    u.nom AS demandeur_nom, u.prenom AS demandeur_prenom,
                    s.id AS service_id, s.nom AS service_nom
                FROM public.validation v
                JOIN public.niveau_validation n ON n.id=v.niveau_id
                JOIN public.demande_achat d ON d.id=v.demande_id
                JOIN public.utilisateurs u ON u.id=d.demandeur_id
                JOIN public.centre_cout cc ON cc.id=d.centre_cout_id
                JOIN public.service s ON s.id=cc.service_id
                WHERE (v.decision<>'EN_ATTENTE' OR v.tentative <
                    (SELECT MAX(active.tentative) FROM public.validation active WHERE active.demande_id=v.demande_id))
                    AND (?='ADMIN' OR (n.role_id IN (SELECT id FROM public.role WHERE code = ?) AND (SELECT service_id FROM public.utilisateurs WHERE id=?)=s.id))
                    AND (?<>'MANAGER' OR (u.manager_id=? AND v.valideur_id=?))
                    AND (CAST(? AS INTEGER) IS NULL OR s.id=?)
                    AND (CAST(? AS NUMERIC) IS NULL OR d.montant_estime>=?)
                    AND (CAST(? AS NUMERIC) IS NULL OR d.montant_estime<=?)
                    AND (CAST(? AS TEXT) IS NULL OR concat_ws(' ', u.prenom, u.nom) ILIKE '%' || ? || '%')
                ORDER BY v.date_decision DESC, d.id DESC
                """, role, role, uid, role, uid, uid,
                serviceId, serviceId, montantMin, montantMin,
                montantMax, montantMax, demandeur, demandeur);
    }

    @Transactional
    public Map<String, Object> decider(long demandeId, String email, String role, AchatRequests.Decision request) {
        String decision = request.decision().toUpperCase();
        if (!List.of("VALIDEE", "REJETEE", "PRECISION_DEMANDEE").contains(decision)) throw new IllegalArgumentException("Décision invalide.");
        if (!"VALIDEE".equals(decision) && (request.commentaire() == null || request.commentaire().isBlank()))
            throw new IllegalArgumentException("Un commentaire est obligatoire pour un rejet ou une demande de précision.");
        int uid = userId(email);
        Map<String, Object> demandeVerrouillee = jdbc.queryForMap(
                "SELECT statut FROM public.demande_achat WHERE id=? FOR UPDATE", demandeId);
        if (!"SOUMISE".equals(demandeVerrouillee.get("statut"))) {
            throw new IllegalArgumentException("Seule une demande soumise peut être traitée.");
        }
        if (role.equals("MANAGER") && !Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM public.demande_achat d JOIN public.utilisateurs u ON u.id=d.demandeur_id
                WHERE d.id=? AND u.manager_id=?)
                """, Boolean.class, demandeId, uid))) {
            throw new org.springframework.security.access.AccessDeniedException("Cette demande n'appartient pas à votre périmètre de validation.");
        }
        
        Map<String, Object> pending;
        try {
            pending = jdbc.queryForMap("""
                SELECT v.id AS id, n.ordre AS ordre
                FROM public.validation v JOIN public.niveau_validation n ON n.id=v.niveau_id
                JOIN public.centre_cout cc ON cc.id=(SELECT centre_cout_id FROM public.demande_achat WHERE id=v.demande_id)
                JOIN public.utilisateurs valideur ON valideur.id=?
                WHERE v.demande_id=? AND v.decision='EN_ATTENTE'
                    AND v.tentative=(SELECT MAX(active.tentative) FROM public.validation active WHERE active.demande_id=v.demande_id)
                    AND (n.role_id=valideur.role_id OR ?='ADMIN')
                    AND (?='ADMIN' OR valideur.service_id=cc.service_id)
                    AND NOT EXISTS (SELECT 1 FROM public.validation p
                    JOIN public.niveau_validation pn ON pn.id=p.niveau_id
                    WHERE p.demande_id=v.demande_id AND p.tentative=v.tentative AND p.decision='EN_ATTENTE'
                      AND (CASE WHEN pn.id=(SELECT nm.id FROM public.niveau_validation nm
                            JOIN public.role rm ON rm.id=nm.role_id
                            WHERE nm.circuit_id=pn.circuit_id AND rm.code='MANAGER'
                            ORDER BY nm.ordre LIMIT 1) THEN 0 ELSE pn.ordre END)
                        < (CASE WHEN n.id=(SELECT nm.id FROM public.niveau_validation nm
                            JOIN public.role rm ON rm.id=nm.role_id
                            WHERE nm.circuit_id=n.circuit_id AND rm.code='MANAGER'
                            ORDER BY nm.ordre LIMIT 1) THEN 0 ELSE n.ordre END))
                ORDER BY CASE WHEN n.id=(SELECT nm.id FROM public.niveau_validation nm
                        JOIN public.role rm ON rm.id=nm.role_id
                        WHERE nm.circuit_id=n.circuit_id AND rm.code='MANAGER'
                        ORDER BY nm.ordre LIMIT 1) THEN 0 ELSE n.ordre END
                LIMIT 1 FOR UPDATE OF v
                """, uid, demandeId, role, role);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Aucune étape de validation accessible pour votre rôle et votre service.");
        }
        
        String decisionDb = switch (decision) {
    case "VALIDEE" -> "VALIDEE";
    case "REJETEE" -> "REJETEE";
    case "PRECISION_DEMANDEE" -> "PRECISION_DEMANDEE";
    default -> throw new IllegalArgumentException("Décision invalide.");
};

jdbc.update("""
    UPDATE public.validation
    SET decision = CAST(? AS public.decision_validation),
        valideur_id = ?,
        commentaire = ?,
        date_decision = now()
    WHERE id = ?
    """,
    decisionDb,
    uid,
    request.commentaire(),
    pending.get("id")
);
        String newStatus = switch (decision) {
            case "REJETEE" -> "REJETEE";
            case "PRECISION_DEMANDEE" -> "BROUILLON";
            default -> jdbc.queryForObject(
                "SELECT count(*) FROM public.validation v WHERE demande_id=? AND decision='EN_ATTENTE' AND tentative=(SELECT MAX(active.tentative) FROM public.validation active WHERE active.demande_id=v.demande_id)",
                Integer.class,
                demandeId
            ) == 0 ? "VALIDEE" : "SOUMISE";
        };
        String oldStatus = (String) demandeVerrouillee.get("statut");
        jdbc.update("""
    UPDATE public.demande_achat
    SET statut = CAST(? AS public.statut_demande)
    WHERE id = ?
    """,
    newStatus,
    demandeId
);
        historique(demandeId, oldStatus, newStatus, uid, request.commentaire());
            return detailDemande(demandeId);
    }

    
public List<Map<String, Object>> categories() {
    return jdbc.queryForList("""
        SELECT id, nom, description, actif
        FROM public.categorie
        ORDER BY nom
        """);
}

@Transactional
public Map<String, Object> creerCategorie(
        String nom, String description) {

    if (nom == null || nom.isBlank()) {
        throw new IllegalArgumentException(
            "Le nom de la catégorie est obligatoire."
        );
    }

    String nomPropre = nom.trim();
    if (nomPropre.length() > 100 || (description != null && description.length() > 255)) {
        throw new IllegalArgumentException("Le nom de la catégorie est limité à 100 caractères et sa description à 255.");
    }
    verrouillerNom("categorie:" + nomPropre);

    Integer existe = jdbc.queryForObject(
        "SELECT COUNT(*) FROM public.categorie WHERE LOWER(nom) = LOWER(?)",
        Integer.class,
        nomPropre
    );

    if (existe != null && existe > 0) {
        throw new IllegalArgumentException(
            "Cette catégorie existe déjà."
        );
    }

    jdbc.update("""
        INSERT INTO public.categorie (nom, description, actif)
        VALUES (?, ?, TRUE)
        """,
        nomPropre,
        description == null || description.isBlank()
            ? null : description.trim()
    );

    return jdbc.queryForMap("""
        SELECT id, nom, description, actif
        FROM public.categorie
        WHERE LOWER(nom) = LOWER(?)
        """, nomPropre);
}

@Transactional
public Map<String, Object> modifierCategorie(
        Integer id, String nom, String description) {

    if (nom == null || nom.isBlank()) {
        throw new IllegalArgumentException(
            "Le nom de la catégorie est obligatoire."
        );
    }

    String nomPropre = nom.trim();
    if (nomPropre.length() > 100 || (description != null && description.length() > 255)) {
        throw new IllegalArgumentException("Le nom de la catégorie est limité à 100 caractères et sa description à 255.");
    }
    verrouillerNom("categorie:" + nomPropre);

    Integer existe = jdbc.queryForObject(
        "SELECT COUNT(*) FROM public.categorie WHERE id = ?",
        Integer.class,
        id
    );

    if (existe == null || existe == 0) {
        throw new IllegalArgumentException(
            "Catégorie introuvable."
        );
    }

    Integer doublon = jdbc.queryForObject("""
        SELECT COUNT(*)
        FROM public.categorie
        WHERE LOWER(nom) = LOWER(?) AND id <> ?
        """,
        Integer.class,
        nomPropre,
        id
    );

    if (doublon != null && doublon > 0) {
        throw new IllegalArgumentException(
            "Une autre catégorie porte déjà ce nom."
        );
    }

    jdbc.update("""
        UPDATE public.categorie
        SET nom = ?, description = ?
        WHERE id = ?
        """,
        nom.trim(),
        description == null || description.isBlank()
            ? null : description.trim(),
        id
    );

    return jdbc.queryForMap("""
        SELECT id, nom, description, actif
        FROM public.categorie
        WHERE id = ?
        """, id);
}

@Transactional
public void desactiverCategorie(Integer id) {
    int lignes = jdbc.update("""
        UPDATE public.categorie
        SET actif = FALSE
        WHERE id = ? AND actif = TRUE
        """, id);

    if (lignes == 0) {
        throw new IllegalArgumentException(
            "Catégorie introuvable ou déjà désactivée."
        );
    }
}

@Transactional
public void reactiverCategorie(Integer id) {
    int lignes = jdbc.update("""
        UPDATE public.categorie
        SET actif = TRUE
        WHERE id = ? AND actif = FALSE
        """, id);

    if (lignes == 0) {
        throw new IllegalArgumentException(
            "Catégorie introuvable ou déjà active."
        );
    }
}


    public List<Map<String, Object>> offres(long demandeId) {
        return jdbc.queryForList("""
                SELECT o.id AS id, f.id AS fournisseur_id,
                        f.raison_sociale AS fournisseur, o.montant_total AS montant_total,
                        o.delai_livraison_jours AS delai_livraison_jours,
                        o.conditions AS conditions, o.statut AS statut,
                        o.date_offre AS date_offre,
                        f.conditions_paiement AS conditions_paiement,
                        d.montant_estime AS montant_estime,
                        o.montant_total-d.montant_estime AS ecart_montant
                FROM public.offre o
                JOIN public.fournisseur f ON f.id=o.fournisseur_id
                JOIN public.demande_achat d ON d.id=o.demande_id
                WHERE o.demande_id=? ORDER BY o.montant_total
                """, demandeId);
    }

    public List<Map<String, Object>> offresAccessibles(long demandeId, String email, String role) {
        demandeAccessible(demandeId, email, role);
        return offres(demandeId);
    }

    @Transactional
    public Map<String, Object> creerOffre(long demandeId, AchatRequests.Offre o) {
        verrouillerDemandePourOffre(demandeId);
        if (Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM public.offre WHERE demande_id=? AND statut='RETENUE')",
                Boolean.class, demandeId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une offre a déjà été retenue pour cette demande.");
        }
        Boolean fournisseurActif = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM public.fournisseur WHERE id=? AND actif=TRUE)",
                Boolean.class, o.fournisseurId());
        if (!Boolean.TRUE.equals(fournisseurActif)) {
            throw new IllegalArgumentException("Le fournisseur sélectionné est introuvable ou inactif.");
        }
        Boolean offreExistante = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM public.offre WHERE demande_id=? AND fournisseur_id=?)",
                Boolean.class, demandeId, o.fournisseurId());
        if (Boolean.TRUE.equals(offreExistante)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une offre de ce fournisseur existe déjà pour cette demande.");
        }
        Integer id = jdbc.queryForObject("""
                INSERT INTO public.offre
                    (montant_total, conditions, fournisseur_id, demande_id,
                    delai_livraison_jours, statut, date_offre)
                VALUES (?, ?, ?, ?, ?, 'RECUE', CURRENT_DATE) RETURNING id
                """, Integer.class, o.montantTotal(), valueOrEmpty(o.conditions()),
                o.fournisseurId(), demandeId, o.delaiLivraisonJours());
        return jdbc.queryForMap("SELECT id AS id, demande_id AS demande_id, montant_total AS montant_total, statut AS statut FROM public.offre WHERE id=?", id);
    }

    @Transactional
    public Map<String, Object> retenirOffre(long demandeId, long offreId) {
        verrouillerDemandePourOffre(demandeId);
        Map<String, Object> offer = verifierOffreAppartient(demandeId, offreId);
        String statutOffre = offer.get("statut").toString();
        if ("RETENUE".equals(statutOffre)) {
            return jdbc.queryForMap("""
                    SELECT id AS id, demande_id AS demande_id, montant_total AS montant_total,
                           fournisseur_id AS fournisseur_id, statut AS statut
                    FROM public.offre WHERE id=? AND demande_id=?
                    """, offreId, demandeId);
        }
        if (!"RECUE".equals(statutOffre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une offre refusée ne peut pas être retenue.");
        }
        if (Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM public.offre WHERE demande_id=? AND statut='RETENUE')",
                Boolean.class, demandeId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une autre offre est déjà retenue pour cette demande.");
        }
        jdbc.update("""
                UPDATE public.offre
                SET statut=CASE WHEN id=? THEN 'RETENUE'::public.statut_offre
                                ELSE 'REFUSEE'::public.statut_offre END
                WHERE demande_id=? AND statut='RECUE'
                """, offreId, demandeId);
        return jdbc.queryForMap("""
                SELECT id AS id, demande_id AS demande_id, montant_total AS montant_total,
                       fournisseur_id AS fournisseur_id, statut AS statut
                FROM public.offre WHERE id=? AND demande_id=?
                """, offreId, demandeId);
    }
    @Transactional
public Map<String, Object> retenirOffreEtCreerCommande(
        long demandeId,
        long offreId,
        String email) {

    retenirOffre(demandeId, offreId);

    return creerCommande(demandeId, offreId, email);
}

    @Transactional
    public Map<String, Object> refuserOffre(long demandeId, long offreId) {
        verrouillerDemandePourOffre(demandeId);
        Map<String, Object> offer = verifierOffreAppartient(demandeId, offreId);
        String statut = offer.get("statut").toString();
        if ("RETENUE".equals(statut)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une offre déjà retenue ne peut pas être refusée après sélection.");
        }
        if ("RECUE".equals(statut)) {
            jdbc.update("UPDATE public.offre SET statut='REFUSEE' WHERE id=? AND demande_id=?",
                    offreId, demandeId);
        }
        return jdbc.queryForMap("""
                SELECT id AS id, demande_id AS demande_id, montant_total AS montant_total,
                       fournisseur_id AS fournisseur_id, statut AS statut
                FROM public.offre WHERE id=? AND demande_id=?
                """, offreId, demandeId);
    }

    private void verrouillerDemandePourOffre(long demandeId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT statut FROM public.demande_achat WHERE id=? FOR UPDATE", demandeId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable.");
        }
        if (!"VALIDEE".equals(rows.get(0).get("statut").toString())) {
            throw new IllegalArgumentException("La demande doit être validée pour gérer ses offres.");
        }
        if (commandeExiste(demandeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Les offres ne peuvent plus être modifiées après la création d'une commande.");
        }
    }

    private Map<String, Object> verifierOffreAppartient(long demandeId, long offreId) {
        List<Map<String, Object>> offers = jdbc.queryForList("""
                SELECT id AS id, demande_id AS demande_id, statut AS statut
                FROM public.offre WHERE id=? AND demande_id=? FOR UPDATE
                """, offreId, demandeId);
        if (offers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Cette offre n'appartient pas à la demande sélectionnée.");
        }
        return offers.get(0);
    }

    public List<Map<String, Object>> commandes(String email, String role) {
        String sql = """
                SELECT b.id AS id, b.numero AS numero,
                    b.demande_id AS demande_id, f.id AS fournisseur_id,
                    f.raison_sociale AS fournisseur, b.montant_total AS montant_total,
                    b.statut AS statut, b.date_emission AS date_emission,
                    b.date_livraison_prevue AS date_livraison_prevue
                FROM public.bon_commande b
                JOIN public.fournisseur f ON f.id=b.fournisseur_id
                """;
        if (role.equals("ADMIN") || role.equals("SERVICE_ACHATS")) return jdbc.queryForList(sql + " ORDER BY b.date_emission DESC");
        return jdbc.queryForList(sql + " JOIN public.demande_achat d ON d.id=b.demande_id"
                + " WHERE d.demandeur_id=? ORDER BY b.date_emission DESC", userId(email));
    }

    
@Transactional
public Map<String, Object> creerCommande(
        long demandeId,
        long offreId,
        String email) {

    Map<String, Object> demande = jdbc.queryForMap("""
            SELECT statut AS statut, reference AS reference
            FROM public.demande_achat
            WHERE id = ?
            FOR UPDATE
            """, demandeId);

    // Vérifier d'abord si une commande existe déjà.
    List<Map<String, Object>> commandesExistantes = jdbc.queryForList("""
            SELECT id AS id,
                   numero AS numero,
                   demande_id AS demande_id,
                   offre_id AS offre_id,
                   montant_total AS montant_total,
                   statut AS statut,
                   date_emission AS date_emission
            FROM public.bon_commande
            WHERE demande_id = ?
            """, demandeId);

    if (!commandesExistantes.isEmpty()) {
        Map<String, Object> existante = commandesExistantes.get(0);
        Number offreExistante = (Number) existante.get("offre_id");

        if (offreExistante != null
                && offreExistante.longValue() == offreId) {
            return existante;
        }

        throw new IllegalArgumentException(
                "Un bon de commande existe déjà pour cette demande "
                + "avec une autre offre."
        );
    }

    if (!"VALIDEE".equals(demande.get("statut").toString())) {
        throw new IllegalArgumentException(
                "La demande doit être validée avant la commande."
        );
    }

    Map<String, Object> offer = jdbc.queryForMap("""
            SELECT montant_total AS montant,
                   fournisseur_id AS fournisseur
            FROM public.offre
            WHERE id = ?
              AND demande_id = ?
              AND statut = 'RETENUE'
            FOR UPDATE
            """, offreId, demandeId);

    String number = "BC-"
            + LocalDate.now().toString().replace("-", "")
            + "-"
            + UUID.randomUUID().toString()
                    .substring(0, 8).toUpperCase();

    BigDecimal total = new BigDecimal(
            offer.get("montant").toString()
    );

    Integer fournisseurId =
            ((Number) offer.get("fournisseur")).intValue();

    int userIdVal = userId(email);

    Integer id = jdbc.queryForObject("""
        INSERT INTO public.bon_commande
            (numero, date_emission, demande_id, offre_id,
             fournisseur_id, cree_par_id, montant_total, statut)
        VALUES (?, CURRENT_DATE, ?, ?, ?, ?, ?, 'GENERE')
        RETURNING id
        """,
        Integer.class,
        number,
        demandeId,
        offreId,
        fournisseurId,
        userIdVal,
        total
);

jdbc.update("""
        INSERT INTO public.ligne_bon_commande
            (bon_commande_id, ligne_demande_id,
             designation, quantite, prix_unitaire)
        SELECT ?, l.id, l.designation, l.quantite,
               CASE
                   WHEN d.montant_estime > 0
                   THEN l.prix_unitaire_estime
                        * (? / d.montant_estime)
                   ELSE 0
               END
        FROM public.ligne_demande l
        JOIN public.demande_achat d
          ON d.id = l.demande_id
        WHERE l.demande_id = ?
        """,
        id, total, demandeId
);

// Engagement du budget avec le montant réel de l'offre retenue
engagerBudget(demandeId, total);

jdbc.update("""
        UPDATE public.demande_achat
        SET statut = 'COMMANDEE'
        WHERE id = ?
        """, demandeId);

historique(
        demandeId,
        "VALIDEE",
        "COMMANDEE",
        userIdVal,
        "Commande " + number + " générée"
);

return jdbc.queryForMap("""
        SELECT id AS id,
            numero AS numero,
            demande_id AS demande_id,
            montant_total AS montant_total,
            statut AS statut,
            date_emission AS date_emission
        FROM public.bon_commande
        WHERE id = ?
        """, id);
}



public List<Map<String, Object>> lignesCommande(
        long commandeId,
        String email,
        String role) {

    if (!role.equals("ADMIN")
            && !role.equals("SERVICE_ACHATS")
            && !Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(
                    SELECT 1
                    FROM public.bon_commande b
                    JOIN public.demande_achat d ON d.id = b.demande_id
                    WHERE b.id = ? AND d.demandeur_id = ?
                )
                """,
                Boolean.class,
                commandeId,
                userId(email))) ) {

        throw new org.springframework.security.access.AccessDeniedException(
            "Accès refusé à cette commande."
        );
    }

    return jdbc.queryForList("""
        SELECT
            l.id AS id,
            l.ligne_demande_id AS ligne_demande_id,
            l.designation AS designation,
            l.quantite AS quantite,
            l.prix_unitaire AS prix_unitaire,
            COALESCE((
                SELECT SUM(lr.quantite_recue)
                FROM public.ligne_reception lr
                JOIN public.reception r ON r.id = lr.reception_id
                WHERE lr.ligne_bc_id = l.id
                  AND r.bon_commande_id = l.bon_commande_id
            ), 0) AS quantite_deja_recue,
            l.quantite - COALESCE((
                SELECT SUM(lr.quantite_recue)
                FROM public.ligne_reception lr
                JOIN public.reception r ON r.id = lr.reception_id
                WHERE lr.ligne_bc_id = l.id
                  AND r.bon_commande_id = l.bon_commande_id
            ), 0) AS quantite_restante
        FROM public.ligne_bon_commande l
        WHERE l.bon_commande_id = ?
        ORDER BY l.id
        """, commandeId);
}


    @Transactional
    public Map<String, Object> receptionner(long commandeId, String email, AchatRequests.Reception reception) {
        Map<String, Object> order = jdbc.queryForMap("SELECT statut AS statut FROM public.bon_commande WHERE id=? FOR UPDATE", commandeId);
        if (List.of("RECU", "ANNULE").contains(order.get("statut").toString())) throw new IllegalArgumentException("Cette commande ne peut plus recevoir de livraison.");
        
        Map<Integer, BigDecimal> quantities = new LinkedHashMap<>();
        for (var line : reception.lignes()) quantities.merge(line.ligneCommandeId(), line.quantiteRecue(), BigDecimal::add);
        BigDecimal sum = BigDecimal.ZERO;
        
        for (var entry : quantities.entrySet()) {

    Integer ligneCommandeId = entry.getKey();
    BigDecimal quantiteRecue = entry.getValue();

    BigDecimal ordered = jdbc.queryForObject(
        """
        SELECT quantite
        FROM public.ligne_bon_commande
        WHERE id = ? AND bon_commande_id = ?
        """,
        BigDecimal.class,
        ligneCommandeId,
        commandeId
    );

    if (ordered == null) {
        throw new IllegalArgumentException(
            "La ligne de commande " + ligneCommandeId
            + " n'appartient pas à la commande " + commandeId + "."
        );
    }

    BigDecimal already = jdbc.queryForObject(
        """
        SELECT COALESCE(SUM(lr.quantite_recue), 0)
        FROM public.ligne_reception lr
        JOIN public.reception r ON r.id = lr.reception_id
        WHERE lr.ligne_bc_id = ?
          AND r.bon_commande_id = ?
        """,
        BigDecimal.class,
        ligneCommandeId,
        commandeId
    );

    if (already == null) {
        already = BigDecimal.ZERO;
    }

    BigDecimal totalRecu = already.add(quantiteRecue);

    if (quantiteRecue.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException(
            "La quantité reçue doit être supérieure à zéro."
        );
    }

    if (totalRecu.compareTo(ordered) > 0) {
        throw new IllegalArgumentException(
            "Quantité excessive pour la ligne " + ligneCommandeId
            + " : commandée = " + ordered
            + ", déjà reçue = " + already
            + ", nouvelle réception = " + quantiteRecue
            + ", total = " + totalRecu + "."
        );
    }

    sum = sum.add(quantiteRecue);
}

        int userIdVal = userId(email);
        Integer receptionId = jdbc.queryForObject("""
                INSERT INTO public.reception (bon_commande_id, receptionnaire_id, date_reception, type_reception, observation)
                VALUES (?, ?, CURRENT_DATE, 'TOTALE', ?) RETURNING id
                """, Integer.class, commandeId, userIdVal, reception.observation());

        for (var entry : quantities.entrySet()) {
            jdbc.update("INSERT INTO public.ligne_reception (reception_id, ligne_bc_id, quantite_recue) VALUES (?, ?, ?)", receptionId, entry.getKey(), entry.getValue());
        }

        int open = jdbc.queryForObject("""
                SELECT count(*) FROM public.ligne_bon_commande l WHERE l.bon_commande_id=? AND l.quantite>
                (SELECT COALESCE(SUM(r.quantite_recue),0) FROM public.ligne_reception r WHERE r.ligne_bc_id=l.id)
                """, Integer.class, commandeId);
        
        
        
            String status = open == 0 ? "RECU" : "PARTIELLEMENT_RECU";

            jdbc.update(
                "UPDATE public.bon_commande SET statut = CAST(? AS public.statut_bc) WHERE id = ?",
                status,
                commandeId
            );


        
        if (status.equals("RECU")) {
            Integer requestId = jdbc.queryForObject("SELECT demande_id FROM public.bon_commande WHERE id=?", Integer.class, commandeId);
            jdbc.update("UPDATE public.demande_achat SET statut='RECEPTIONNEE' WHERE id=?", requestId);
            historique(requestId, "COMMANDEE", "RECEPTIONNEE", userIdVal, "Réception complète enregistrée");
        }
        return jdbc.queryForMap("SELECT id AS id, bon_commande_id AS commande_id, date_reception AS date_reception, observation AS observation FROM public.reception WHERE id=?", receptionId);
    }

    
public List<Map<String, Object>> centresCout(String identifiant) {
    int uid = userId(identifiant);

    // Récupérer le service de l'utilisateur connecté.
    List<Integer> servicesUtilisateur = jdbc.queryForList(
            """
            SELECT service_id
            FROM public.utilisateurs
            WHERE id = ?
            """,
            Integer.class,
            uid
    );

    if (servicesUtilisateur.isEmpty()
            || servicesUtilisateur.get(0) == null) {
        throw new IllegalArgumentException(
                "Votre compte n'est associé à aucun service."
        );
    }

    int serviceId = servicesUtilisateur.get(0);

    // Afficher uniquement les centres de coût de ce service.
    return jdbc.queryForList(
            """
            SELECT c.id AS id,
                    c.code AS code,
                    c.libelle AS libelle,
                    s.nom AS service
            FROM public.centre_cout c
            JOIN public.service s ON s.id = c.service_id
            WHERE c.service_id = ?
            ORDER BY c.id
            """,
            serviceId
    );
}

@Transactional
public Map<String, Object> creerCentreCout(Map<String, Object> payload) {
    String code = payload.get("code") != null
            ? payload.get("code").toString().trim()
            : "";

    String libelle = payload.get("libelle") != null
            ? payload.get("libelle").toString().trim()
            : "";

    Object serviceValue = payload.get("serviceId");

    if (code.isBlank() || libelle.isBlank() || serviceValue == null) {
        throw new IllegalArgumentException(
                "Le code, le libellé et le service sont obligatoires."
        );
    }
    if (code.length() > 20 || libelle.length() > 100) {
        throw new IllegalArgumentException("Le code est limité à 20 caractères et le libellé à 100.");
    }
    verrouillerNom("centre:" + code);
    Integer codeExiste = jdbc.queryForObject(
            "SELECT COUNT(*) FROM public.centre_cout WHERE lower(code)=lower(?)",
            Integer.class, code);
    if (codeExiste != null && codeExiste > 0) {
        throw new IllegalArgumentException("Un centre de coût porte déjà ce code.");
    }

    int serviceId;
    try {
        serviceId = Integer.parseInt(serviceValue.toString());
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Le service sélectionné est invalide.");
    }

    if (serviceId <= 0) {
        throw new IllegalArgumentException("Le service sélectionné est invalide.");
    }

    Integer serviceExiste = jdbc.queryForObject(
            "SELECT COUNT(*) FROM public.service WHERE id = ?",
            Integer.class,
            serviceId
    );

    if (serviceExiste == null || serviceExiste == 0) {
        throw new IllegalArgumentException("Le service sélectionné n'existe pas.");
    }

    Integer id = jdbc.queryForObject("""
            INSERT INTO public.centre_cout (code, libelle, service_id)
            VALUES (?, ?, ?)
            RETURNING id
            """, Integer.class, code, libelle, serviceId);

    return jdbc.queryForMap("""
            SELECT c.id AS id, c.code AS code,
            c.libelle AS libelle, s.nom AS service
            FROM public.centre_cout c
            JOIN public.service s ON s.id = c.service_id
            WHERE c.id = ?
            """, id);
    }
    

    // ==================== GESTION DES SERVICES ====================

    @Transactional
    public Map<String, Object> creerService(Map<String, Object> payload) {
        String nom = payload.get("nom") == null
                ? ""
                : payload.get("nom").toString().trim();

        if (nom.isBlank()) {
            throw new IllegalArgumentException(
                    "Le nom du service est obligatoire."
            );
        }
        if (nom.length() > 100) {
            throw new IllegalArgumentException("Le nom du service ne peut pas dépasser 100 caractères.");
        }

        verifierNomServiceDisponible(nom, null);

        // Générer automatiquement un code à partir du nom.
        String code = nom
                .toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (code.isBlank()) {
            throw new IllegalArgumentException(
                    "Impossible de générer le code du service."
            );
        }
        if (code.length() > 20) {
            throw new IllegalArgumentException("Ce nom génère un code de plus de 20 caractères. Raccourcissez le nom du service.");
        }

        // Éviter les codes déjà utilisés.
        Integer compteur = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM public.service
                WHERE UPPER(code) = UPPER(?)
                """,
                Integer.class,
                code
        );

        if (compteur != null && compteur > 0) {
            throw new IllegalArgumentException("Ce nom génère un code déjà utilisé. Choisissez un autre nom de service.");
        }

        Integer id = jdbc.queryForObject(
                """
                INSERT INTO public.service (code, nom)
                VALUES (?, ?)
                RETURNING id
                """,
                Integer.class,
                code,
                nom
        );

        return jdbc.queryForMap(
                """
                SELECT id, code, nom
                FROM public.service
                WHERE id = ?
                """,
                id
        );
    }

    public List<Map<String, Object>> services() {
        return jdbc.queryForList(
                """
                SELECT id, code, nom
                FROM public.service
                ORDER BY nom
                """
        );
    }

    public List<Map<String, Object>> servicesAccessibles(String email, String role) {
        if (!"MANAGER".equals(role)) return services();
        int uid = userId(email);
        return jdbc.queryForList("""
                SELECT DISTINCT s.id, s.code, s.nom
                FROM public.service s
                JOIN public.utilisateurs u ON u.service_id = s.id
                WHERE u.manager_id = ?
                ORDER BY s.nom
                """, uid);
    }

    @Transactional
    public Map<String, Object> modifierService(
            int id,
            Map<String, Object> payload) {

        String nom = payload.get("nom") == null
                ? ""
                : payload.get("nom").toString().trim();

        if (nom.isBlank()) {
            throw new IllegalArgumentException(
                    "Le nom du service est obligatoire."
            );
        }
        if (nom.length() > 100) {
            throw new IllegalArgumentException("Le nom du service ne peut pas dépasser 100 caractères.");
        }

        Integer existe = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM public.service
                WHERE id = ?
                """,
                Integer.class,
                id
        );

        if (existe == null || existe == 0) {
            throw new IllegalArgumentException(
                    "Le service demandé n'existe pas."
            );
        }

        verifierNomServiceDisponible(nom, id);

        jdbc.update(
                """
                UPDATE public.service
                SET nom = ?
                WHERE id = ?
                """,
                nom,
                id
        );

        return jdbc.queryForMap(
                """
                SELECT id, code, nom
                FROM public.service
                WHERE id = ?
                """,
                id
        );
    }

    @Transactional
    public Map<String, Object> supprimerService(int id) {

        Integer existe = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM public.service
                WHERE id = ?
                """,
                Integer.class,
                id
        );

        if (existe == null || existe == 0) {
            throw new IllegalArgumentException(
                    "Le service demandé n'existe pas."
            );
        }

        Integer centresUtilisateurs = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM public.centre_cout
                WHERE service_id = ?
                """,
                Integer.class,
                id
        );

        Integer utilisateurs = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM public.utilisateurs
                WHERE service_id = ?
                """,
                Integer.class,
                id
        );

        if ((centresUtilisateurs != null && centresUtilisateurs > 0)
                || (utilisateurs != null && utilisateurs > 0)) {
            throw new IllegalArgumentException(
                    "Impossible de supprimer ce service : "
                    + "il est utilisé par un centre de coût "
                    + "ou par un utilisateur."
            );
        }

        jdbc.update(
                """
                DELETE FROM public.service
                WHERE id = ?
                """,
                id
        );

        return Map.of(
                "id", id,
                "message", "Service supprimé avec succès."
        );
    }

    private void verifierNomServiceDisponible(
            String nom,
            Integer idExclu) {

        verrouillerNom("service:" + nom);

        Integer compteur;

        if (idExclu == null) {
            compteur = jdbc.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM public.service
                    WHERE LOWER(TRIM(nom)) = LOWER(TRIM(?))
                    """,
                    Integer.class,
                    nom
            );
        } else {
            compteur = jdbc.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM public.service
                    WHERE LOWER(TRIM(nom)) = LOWER(TRIM(?))
                    AND id <> ?
                    """,
                    Integer.class,
                    nom,
                    idExclu
            );
        }

        if (compteur != null && compteur > 0) {
            throw new IllegalArgumentException(
                    "Un service portant ce nom existe déjà."
            );
        }
    }

    private void verrouillerNom(String valeur) {
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(lower(?), 0)) IS NULL",
                Boolean.class, valeur);
    }

    public List<Map<String, Object>> budgets(int year, String email, String role) {
        return budgets(year, null, email, role);
    }

    public List<Map<String, Object>> budgets(int year, Integer centreCoutId, String email, String role) {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("L'année budgétaire doit être comprise entre 2000 et 2100.");
        }
        if (!List.of("ADMIN", "MANAGER", "SERVICE_ACHATS").contains(role)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "La consultation des budgets est réservée aux responsables autorisés.");
        }
        return jdbc.queryForList("""
                SELECT c.id AS centre_cout_id, b.id AS budget_id,
                        c.code AS code,
                        c.libelle AS libelle, b.annee AS annee,
                        b.montant_alloue AS montant_alloue, b.montant_engage AS montant_engage,
                        b.seuil_alerte_pct AS seuil_alerte_pct,
                        b.montant_alloue - b.montant_engage AS solde_disponible,
                        CASE WHEN b.montant_alloue>0 THEN round(b.montant_engage*100/b.montant_alloue,2) ELSE 0 END AS consommation_pct,
                        (b.montant_alloue>0 AND b.montant_engage*100/b.montant_alloue>=b.seuil_alerte_pct) AS alerte_seuil
                FROM public.centre_cout c 
                JOIN public.service s ON s.id = c.service_id
                LEFT JOIN public.budget b ON b.centre_cout_id=c.id AND b.annee=?
                WHERE (?<>'MANAGER' OR s.id=(SELECT u.service_id FROM public.utilisateurs u WHERE u.id=?))
                  AND (CAST(? AS INTEGER) IS NULL OR c.id=?)
                ORDER BY c.id
                """, year, role, userId(email), centreCoutId, centreCoutId);
    }

    public List<Map<String, Object>> centresCoutActifsPourBudget() {
        return jdbc.queryForList("""
                SELECT id AS centre_cout_id, code, libelle
                FROM public.centre_cout
                WHERE actif=true
                ORDER BY code, libelle
                """);
    }

    @Transactional
    public Map<String, Object> enregistrerBudget(AchatRequests.Budget budget) {
        if (budget.annee() < 2000 || budget.annee() > 2100) {
            throw new IllegalArgumentException("L'année budgétaire doit être comprise entre 2000 et 2100.");
        }
        List<Integer> centreActif = jdbc.queryForList("""
                SELECT id FROM public.centre_cout
                WHERE id=? AND actif=true
                FOR SHARE
                """, Integer.class, budget.centreCoutId());
        if (centreActif.isEmpty()) {
            throw new IllegalArgumentException("Le centre de coût sélectionné n'existe pas.");
        }
        jdbc.update("""
                INSERT INTO public.budget (centre_cout_id, annee, montant_alloue, montant_engage, seuil_alerte_pct)
                VALUES (?, ?, ?, 0, ?)
                ON CONFLICT (centre_cout_id, annee) DO UPDATE
                SET montant_alloue=EXCLUDED.montant_alloue, seuil_alerte_pct=EXCLUDED.seuil_alerte_pct
                """, budget.centreCoutId(), budget.annee(), budget.montantAlloue(), budget.seuilAlertePct());
        return jdbc.queryForMap("""
                SELECT b.id AS budget_id, b.centre_cout_id AS centre_cout_id,
                       b.annee AS annee, b.montant_alloue AS montant_alloue,
                       b.montant_engage AS montant_engage, b.seuil_alerte_pct AS seuil_alerte_pct
                FROM public.budget b WHERE b.centre_cout_id=? AND b.annee=?
                """, budget.centreCoutId(), budget.annee());
    }

    public Map<String, Object> rapport(LocalDate du, LocalDate au) {
        if (du.isAfter(au)) throw new IllegalArgumentException("La date de début doit précéder la date de fin.");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("indicateurs", jdbc.queryForMap("""
                SELECT COUNT(*) AS nombre_demandes,
                       COUNT(*) FILTER (WHERE d.statut='REJETEE') AS demandes_rejetees,
                       COALESCE(round(100.0*COUNT(*) FILTER (WHERE d.statut='REJETEE')/NULLIF(COUNT(*),0),2),0) AS taux_rejet_pct,
                       (SELECT round(avg(extract(epoch FROM (b.date_emission-d2.date_soumission))/86400.0)::numeric,2)
                        FROM public.demande_achat d2 JOIN public.bon_commande b ON b.demande_id=d2.id
                        WHERE d2.date_soumission IS NOT NULL AND b.date_emission>=? AND b.date_emission<=?) AS delai_moyen_traitement_jours,
                       (SELECT COALESCE(SUM(montant_total),0) FROM public.bon_commande WHERE date_emission>=? AND date_emission<=?) AS depenses
                FROM public.demande_achat d WHERE d.date_creation>=? AND d.date_creation<=?
                """, du, au, du, au, du, au));
        result.put("parStatut", jdbc.queryForList("SELECT statut AS statut, count(*) AS nombre FROM public.demande_achat WHERE date_creation>=? AND date_creation<=? GROUP BY statut ORDER BY statut", du, au));
        result.put("parService", jdbc.queryForList("""
                SELECT s.nom AS service, count(DISTINCT d.id) AS demandes,
                       COALESCE(sum(b.montant_total),0) AS depenses
                FROM public.bon_commande b 
                JOIN public.demande_achat d ON d.id=b.demande_id
                JOIN public.centre_cout c ON c.id=d.centre_cout_id
                JOIN public.service s ON s.id=c.service_id
                WHERE b.date_emission>=? AND b.date_emission<=? GROUP BY s.nom ORDER BY depenses DESC
                """, du, au));
        result.put("parFournisseur", jdbc.queryForList("""
                SELECT f.raison_sociale AS fournisseur, count(b.id) AS commandes,
                       COALESCE(sum(b.montant_total),0) AS depenses
                FROM public.bon_commande b JOIN public.fournisseur f ON f.id=b.fournisseur_id
                WHERE b.date_emission>=? AND b.date_emission<=? GROUP BY f.raison_sociale ORDER BY depenses DESC
                """, du, au));
        result.put("parCategorie", jdbc.queryForList("""
                SELECT COALESCE(cat.nom, 'Sans catégorie') AS categorie,
                       count(DISTINCT b.id) AS commandes,
                       COALESCE(sum(lb.quantite*lb.prix_unitaire),0) AS depenses
                FROM public.bon_commande b 
                JOIN public.ligne_bon_commande lb ON lb.bon_commande_id=b.id
                LEFT JOIN public.ligne_demande l ON l.id=lb.ligne_demande_id
                LEFT JOIN public.categorie cat ON cat.id=l.categorie_id
                WHERE b.date_emission>=? AND b.date_emission<=? GROUP BY cat.nom ORDER BY depenses DESC
                """, du, au));
        result.put("du", du);
        result.put("au", au);
        return result;
    }

    private BigDecimal totalDemande(List<AchatRequests.LigneDemande> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (var line : lines) {
            total = total.add(line.quantite().multiply(line.prixUnitaireEstime())
                    .setScale(2, RoundingMode.HALF_UP));
        }
        if (total.compareTo(new BigDecimal("9999999999999.99")) > 0) {
            throw new IllegalArgumentException("Le montant total dépasse la limite autorisée pour une demande.");
        }
        return total;
    }

    /*private void insererLignes(int requestId, List<AchatRequests.LigneDemande> lines) {
        for (var line : lines) {
            jdbc.update("INSERT INTO public.ligne_demande (demande_id, categorie_id, type, designation, quantite, prix_unitaire_estime) VALUES (?, ?, ?, ?, ?, ?)",
                    requestId, line.categorieId(), line.type().toUpperCase(), line.designation().trim(), line.quantite(), line.prixUnitaireEstime());
        }
    }*/
    private void insererLignes(
        int requestId,
        List<AchatRequests.LigneDemande> lines) {

    if (lines == null || lines.isEmpty()) {
        throw new IllegalArgumentException(
            "La demande doit contenir au moins une ligne."
        );
    }

    for (var line : lines) {
        if (line == null) {
            throw new IllegalArgumentException(
                "Une ligne de demande est vide."
            );
        }

        if (line.type() == null ||
                !(line.type().equalsIgnoreCase("ARTICLE")
                || line.type().equalsIgnoreCase("SERVICE"))) {
            throw new IllegalArgumentException(
                "Le type doit être ARTICLE ou SERVICE."
            );
        }

        if (line.designation() == null ||
                line.designation().isBlank()) {
            throw new IllegalArgumentException(
                "La désignation est obligatoire."
            );
        }

        if (line.quantite() == null ||
                line.quantite().signum() <= 0) {
            throw new IllegalArgumentException(
                "La quantité doit être supérieure à zéro."
            );
        }

        if (line.prixUnitaireEstime() == null ||
                line.prixUnitaireEstime().signum() < 0) {
            throw new IllegalArgumentException(
                "Le prix unitaire estimé ne peut pas être négatif."
            );
        }

        if (line.categorieId() != null) {
            if (line.categorieId() <= 0) {
                throw new IllegalArgumentException("La catégorie sélectionnée est invalide.");
            }
            Boolean categorieExiste = jdbc.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM public.categorie WHERE id = ?)",
                    Boolean.class, line.categorieId());
            if (!Boolean.TRUE.equals(categorieExiste)) {
                throw new IllegalArgumentException("La catégorie sélectionnée n'existe plus. Choisissez une catégorie valide.");
            }
        }

        try {
            jdbc.update("""
                INSERT INTO public.ligne_demande
                    (demande_id, categorie_id, type, designation,
                     quantite, prix_unitaire_estime)
                VALUES (?, ?, ?::public.type_article, ?, ?, ?)
                """,
                requestId,
                line.categorieId(),
                line.type().toUpperCase(java.util.Locale.ROOT),
                line.designation().trim(),
                line.quantite(),
                line.prixUnitaireEstime()
            );
        } catch (org.springframework.dao.DataAccessException e) {
            throw new IllegalStateException(
                "Impossible d'enregistrer une ligne de demande. Vérifiez les champs et la catégorie sélectionnée.", e
            );
        }
    }
}
    /*ddd */

   
private void verifierCentrePourUtilisateur(int userId, int centreId) {
    List<Integer> servicesUtilisateur = jdbc.queryForList(
            """
            SELECT service_id
            FROM public.utilisateurs
            WHERE id = ?
            """,
            Integer.class,
            userId
    );

    if (servicesUtilisateur.isEmpty()
            || servicesUtilisateur.get(0) == null) {
        throw new IllegalArgumentException(
                "Votre compte n'est associé à aucun service."
        );
    }

    List<Integer> servicesCentre = jdbc.queryForList(
            """
            SELECT service_id
            FROM public.centre_cout
            WHERE id = ?
            """,
            Integer.class,
            centreId
    );

    if (servicesCentre.isEmpty()
            || servicesCentre.get(0) == null) {
        throw new IllegalArgumentException(
                "Le centre de coût sélectionné est introuvable."
        );
    }

    if (!servicesUtilisateur.get(0).equals(servicesCentre.get(0))) {
        throw new IllegalArgumentException(
                "Le centre de coût doit appartenir à votre service."
        );
    }
}


    private void verifierProprietaire(Map<String, Object> row, int uid) {
        int ownerId = ((Number) row.get("demandeur")).intValue();
        if (ownerId != uid) throw new org.springframework.security.access.AccessDeniedException("Seul le demandeur peut modifier cette demande.");
    }

    private void verifierDemandeValidee(long id) {
        String status = jdbc.queryForObject("SELECT statut FROM public.demande_achat WHERE id=?", String.class, id);
        if (!"VALIDEE".equals(status)) throw new IllegalArgumentException("La demande doit être validée.");
    }

    private boolean commandeExiste(long requestId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM public.bon_commande WHERE demande_id=?)", Boolean.class, requestId));
    }

    private void engagerBudget(long requestId, BigDecimal montantOffre) {
    if (montantOffre == null || montantOffre.signum() <= 0) {
        throw new IllegalArgumentException(
            "Le montant de l'offre retenue est invalide."
        );
    }

    int updated = jdbc.update("""
        UPDATE public.budget b
        SET montant_engage = b.montant_engage + ?
        FROM public.demande_achat d
        WHERE d.id = ?
            AND b.centre_cout_id = d.centre_cout_id
            AND b.annee = EXTRACT(YEAR FROM CURRENT_DATE)
            AND b.montant_engage + ? <= b.montant_alloue
        """,
        montantOffre,
        requestId,
        montantOffre
    );

    if (updated != 1) {
        throw new IllegalArgumentException(
            "Budget insuffisant ou budget introuvable pour ce centre de coût."
        );
    }

    jdbc.update("""
        INSERT INTO public.alerte_budget
            (budget_id, type_alerte, message)
        SELECT b.id, 'SEUIL',
                'Le seuil budgétaire est atteint ou dépassé.'
        FROM public.budget b
        JOIN public.demande_achat d
            ON d.centre_cout_id = b.centre_cout_id
        WHERE d.id = ?
            AND b.annee = EXTRACT(YEAR FROM CURRENT_DATE)
            AND b.montant_alloue > 0
            AND b.montant_engage * 100.0 / b.montant_alloue
                >= b.seuil_alerte_pct
            AND NOT EXISTS (
                SELECT 1
                FROM public.alerte_budget a
                WHERE a.budget_id = b.id
                AND a.traitee = false
            )
        """, requestId);
}

    private int userId(String identifiant) {
    if (identifiant == null || identifiant.isBlank()) {
        throw new IllegalArgumentException("Identifiant utilisateur manquant.");
    }

    List<Integer> ids = jdbc.queryForList("""
        SELECT id
        FROM public.utilisateurs
        WHERE lower(matricule) = lower(?)
        OR lower(email) = lower(?)
        """, Integer.class, identifiant.trim(), identifiant.trim());

    if (ids.isEmpty()) {
        throw new IllegalArgumentException("Utilisateur introuvable.");
    }

    if (ids.size() > 1) {
        throw new IllegalStateException(
            "Plusieurs utilisateurs correspondent à cet identifiant."
        );
    }

    return ids.get(0);
    }

    private void historique(long id, String oldStatus, String newStatus, int userId, String comment) {
        jdbc.update("INSERT INTO public.historique_statut (demande_id, ancien_statut, nouveau_statut, utilisateur_id, commentaire) VALUES (?, ?::statut_demande, ?::statut_demande, ?, ?)", 
                id, oldStatus, newStatus, userId, comment);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
    
}
