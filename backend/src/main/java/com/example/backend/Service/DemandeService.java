package com.example.backend.Service;

import com.example.backend.Models.Demande;
import com.example.backend.Models.Commande;
import com.example.backend.Repository.DemandeRepository;
import java.util.List;
import java.util.Optional;

public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final CommandeService commandeService;

    public DemandeService(DemandeRepository demandeRepository, CommandeService commandeService) {
        this.demandeRepository = demandeRepository;
        this.commandeService = commandeService;
    }

    public List<Demande> getAll() {
        return demandeRepository.findAll();
    }

    public Optional<Demande> getById(Long id) {
        return demandeRepository.findById(id);
    }

    public Demande save(Demande demande) {
        // ensure items link back to demande
        if (demande.getItems() != null) {
            demande.getItems().forEach(i -> i.setDemande(demande));
        }
        return demandeRepository.save(demande);
    }

    public List<Demande> getByStatut(String statut) {
        return demandeRepository.findByStatut(statut);
    }

    public Demande submit(Long id) {
        Demande d = demandeRepository.findById(id).orElseThrow();
        d.setStatut("SOUMISE");
        return demandeRepository.save(d);
    }

    public Commande validateAndCreateCommande(Long id) {
        Demande d = demandeRepository.findById(id).orElseThrow();
        d.setStatut("VALIDEE");
        demandeRepository.save(d);
        // créer commande depuis demande
        return commandeService.createFromDemande(d);
    }

    public Demande reject(Long id, String commentaire) {
        Demande d = demandeRepository.findById(id).orElseThrow();
        d.setStatut("REJETEE");
        return demandeRepository.save(d);
    }
}
