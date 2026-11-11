package com.example.backend.Service;

import com.example.backend.DTO.UtilisateurRequest;
import com.example.backend.DTO.UtilisateurResponse;
import com.example.backend.DTO.UtilisateurUpdateRequest;
import com.example.backend.DTO.ManagerOption;
import com.example.backend.Models.Utilisateur;
import com.example.backend.Repository.UtilisateurRepository;
import com.example.backend.Repository.RoleRepository;
import com.example.backend.Repository.ManagerRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UtilisateurService {

        private final UtilisateurRepository utilisateurs;
        private final RoleRepository roles;
        private final ManagerRepository managers;
        private final PasswordEncoder passwordEncoder;
        private final UtilisateurAuthStateService authStateService;
        private final boolean authStateEnabled;

        public UtilisateurService(
                UtilisateurRepository utilisateurs,
                RoleRepository roles,
                ManagerRepository managers,
                PasswordEncoder passwordEncoder,
                UtilisateurAuthStateService authStateService,
                @Value("${app.security.auth-state.enabled:false}") boolean authStateEnabled) {
        this.utilisateurs = utilisateurs;
        this.roles = roles;
        this.managers = managers;
        this.passwordEncoder = passwordEncoder;
        this.authStateService = authStateService;
        this.authStateEnabled = authStateEnabled;
        }

        @Transactional(readOnly = true)
        public List<ManagerOption> getManagersByService(Integer serviceId) {
                if (serviceId == null || serviceId < 1) {
                        throw new IllegalArgumentException("Sélectionnez un service valide.");
                }
                return managers.findActifsByService(serviceId);
        }

        @Transactional(readOnly = true)
        public List<UtilisateurResponse> getAll() {
        return utilisateurs.findAll()
                .stream()
                .map(this::reponse)
                .toList();
        }

        @Transactional(readOnly = true)
        public UtilisateurResponse getById(String matricule) {
        return reponse(trouverUtilisateur(matricule));
        }

        @Transactional
        public UtilisateurResponse create(UtilisateurRequest request) {
        String matricule = request.matricule().trim();
        String email = request.email().trim();

        if (utilisateurs.existsByEmailIgnoreCase(email)) {
                throw new IllegalArgumentException(
                "Un utilisateur possède déjà cet email.");
        }

        if (utilisateurs.existsByMatricule(matricule)) {
                throw new IllegalArgumentException(
                "Un utilisateur possède déjà ce matricule.");
        }

        Integer roleId = roles.findIdByCode(request.roleCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rôle introuvable : " + request.roleCode()));

        Integer serviceId = parseServiceId(request.service());
        verifierService(serviceId);
        Integer managerId = managerPourDemandeur(request.roleCode(), request.managerId(), serviceId);

        Utilisateur utilisateur = new Utilisateur();

        utilisateur.setMatricule(matricule);
        utilisateur.setNom(request.nom().trim());
        utilisateur.setPrenom(request.prenom().trim());
        utilisateur.setEmail(email);
        utilisateur.setTelephone(blankToNull(request.telephone()));
        utilisateur.setPassword(
                passwordEncoder.encode(request.password()));
        utilisateur.setRole(roleId);

        utilisateur.setService(serviceId);
        utilisateur.setManagerId(managerId);

        Utilisateur saved = utilisateurs.save(utilisateur);
        if (authStateEnabled) authStateService.ensureState(saved.getId());
        return reponse(saved);
    }

    @Transactional
    public UtilisateurResponse changerRole(
            String matricule, String roleCode) {

        Utilisateur utilisateur = trouverUtilisateur(matricule);

        Integer roleId = roles.findIdByCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rôle introuvable : " + roleCode));

        if ("DEMANDEUR".equalsIgnoreCase(roleCode)
                && (utilisateur.getManagerId() == null
                || !managers.isActiveManagerOfService(utilisateur.getManagerId(), utilisateur.getService()))) {
                throw new IllegalArgumentException(
                        "Affectez un Manager actif du même service avant de donner le rôle DEMANDEUR.");
        }

        utilisateur.setRole(roleId);

        return reponse(utilisateurs.save(utilisateur));
    }

        @Transactional
        public UtilisateurResponse update(String matricule, UtilisateurUpdateRequest request) {
        Utilisateur utilisateur = trouverUtilisateur(matricule);
        String email = request.email().trim();
        if (utilisateurs.existsByEmailIgnoreCaseAndMatriculeNot(email, matricule)) {
                throw new IllegalArgumentException("Un utilisateur possède déjà cet email.");
        }
        Integer roleId = roles.findIdByCode(request.roleCode())
                .orElseThrow(() -> new IllegalArgumentException("Rôle introuvable : " + request.roleCode()));
        Integer serviceId = parseServiceId(request.service());
        verifierService(serviceId);
        Integer managerId = utilisateur.getManagerId();
        if ("DEMANDEUR".equalsIgnoreCase(request.roleCode())) {
                managerId = request.managerId() != null ? request.managerId() : managerId;
                managerId = managerPourDemandeur(request.roleCode(), managerId, serviceId);
        }
        utilisateur.setNom(request.nom().trim());
        utilisateur.setPrenom(request.prenom().trim());
        utilisateur.setEmail(email);
        utilisateur.setTelephone(blankToNull(request.telephone()));
        utilisateur.setRole(roleId);
        utilisateur.setService(serviceId);
        utilisateur.setManagerId(managerId);
        return reponse(utilisateurs.save(utilisateur));
        }

        private void verifierService(Integer serviceId) {
                if (!managers.serviceExists(serviceId)) {
                        throw new IllegalArgumentException("Service introuvable.");
                }
        }

        private UtilisateurResponse reponse(Utilisateur utilisateur) {
                String roleCode = utilisateur.getRole() == null ? null
                        : roles.findCodeById(utilisateur.getRole()).orElse(null);
                return UtilisateurResponse.from(utilisateur, roleCode);
        }

        private Integer managerPourDemandeur(String roleCode, Integer managerId, Integer serviceId) {
                if (!"DEMANDEUR".equalsIgnoreCase(roleCode)) return null;
                if (managerId == null) {
                        throw new IllegalArgumentException("Un Manager responsable est obligatoire pour un Demandeur.");
                }
                if (!managers.isActiveManagerOfService(managerId, serviceId)) {
                        throw new IllegalArgumentException(
                                "Le Manager sélectionné doit être actif, avoir le rôle MANAGER et appartenir au même service.");
                }
                return managerId;
        }

        private String blankToNull(String value) {
                return value == null || value.isBlank() ? null : value.trim();
        }

        private Integer parseServiceId(String service) {
                try {
                        int serviceId = Integer.parseInt(service.trim());
                        if (serviceId < 1) throw new NumberFormatException();
                        return serviceId;
                } catch (NumberFormatException exception) {
                        throw new IllegalArgumentException("L'identifiant du service doit être un nombre valide.");
                }
        }

        @Transactional
        public UtilisateurResponse deactivate(String matricule, String currentMatricule) {
        if (matricule.equals(currentMatricule)) {
                throw new IllegalArgumentException("Vous ne pouvez pas désactiver votre propre compte administrateur.");
        }
        Utilisateur utilisateur = trouverUtilisateur(matricule);
        utilisateur.setActif(false);
        return reponse(utilisateurs.save(utilisateur));
        }

    private Utilisateur trouverUtilisateur(String matricule) {
        return utilisateurs.findByMatricule(matricule)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Utilisateur introuvable."));
    }
}
