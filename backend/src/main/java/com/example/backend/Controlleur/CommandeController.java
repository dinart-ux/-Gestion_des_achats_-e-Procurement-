package com.example.backend.Controlleur;

import com.example.backend.Models.Commande;
import com.example.backend.Models.CommandeDTO;
import com.example.backend.Service.CommandeService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Deprecated(forRemoval = true)
public class CommandeController {

    private final CommandeService commandeService;

    public CommandeController(CommandeService commandeService) {
        this.commandeService = commandeService;
    }

    @PreAuthorize("hasAnyRole('DEMANDEUR', 'MANAGER', 'SERVICE_ACHATS', 'ADMIN')")
    @GetMapping("/commandes")
    public List<CommandeDTO> getAll() {
        return commandeService.getAll();
    }

    @PreAuthorize("hasAnyRole('DEMANDEUR', 'MANAGER', 'SERVICE_ACHATS', 'ADMIN')")
    @GetMapping("/commandes/{id}")
    public Commande getById(@PathVariable Long id) {
        return commandeService.getById(id);
    }

    @PreAuthorize("hasAnyRole('SERVICE_ACHATS', 'ADMIN')")
    @PostMapping("/commandes")
    public Commande create(@RequestBody Commande commande) {
        return commandeService.save(commande);
    }
}
