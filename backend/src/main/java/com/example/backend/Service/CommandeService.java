package com.example.backend.Service;

import com.example.backend.Models.Commande;
import com.example.backend.Models.CommandeDTO;
import com.example.backend.Repository.CommandeRepository;
import java.util.List;
import java.util.stream.Collectors;

public class CommandeService {

    private final CommandeRepository commandeRepository;

    public CommandeService(CommandeRepository commandeRepository) {
        this.commandeRepository = commandeRepository;
    }

    public List<CommandeDTO> getAll() {
        return commandeRepository.findAll().stream()
                .map(c -> new CommandeDTO(c.getId(), c.getDateCommande(), c.getStatut(), c.getMontantTotal()))
                .collect(Collectors.toList());
    }

    public Commande getById(Long id) {
        return commandeRepository.findById(id).orElse(null);
    }

    public Commande save(Commande commande) {
        return commandeRepository.save(commande);
    }

    public Commande createFromDemande(com.example.backend.Models.Demande demande) {
        Commande cmd = new Commande();
        cmd.setUtilisateur(demande.getCreateur());
        cmd.setStatut("EN_ATTENTE");
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        if (demande.getItems() != null) {
            for (com.example.backend.Models.DemandeItem di : demande.getItems()) {
                com.example.backend.Models.CommandeItem ci = new com.example.backend.Models.CommandeItem();
                ci.setCommande(cmd);
                ci.setProduit(di.getProduit());
                ci.setQuantite(di.getQuantite());
                ci.setPrixUnitaire(di.getPrixUnitaire());
                cmd.getItems().add(ci);
                total = total.add(ci.getPrixUnitaire().multiply(new java.math.BigDecimal(ci.getQuantite())));
            }
        }
        cmd.setMontantTotal(total);
        return commandeRepository.save(cmd);
    }
}
