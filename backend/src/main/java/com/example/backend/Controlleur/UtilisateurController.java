package com.example.backend.Controlleur;

import com.example.backend.DTO.ChangeRoleRequest;
import com.example.backend.DTO.UtilisateurRequest;
import com.example.backend.DTO.UtilisateurResponse;
import com.example.backend.DTO.UtilisateurUpdateRequest;
import com.example.backend.Service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;
import com.example.backend.DTO.ManagerOption;

@RestController
@RequestMapping("/api/utilisateurs")
@PreAuthorize("hasRole('ADMIN')")
public class UtilisateurController {
    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    public List<UtilisateurResponse> getAll() {
        return utilisateurService.getAll();
    }

    @GetMapping("/managers")
    public List<ManagerOption> getManagersByService(@org.springframework.web.bind.annotation.RequestParam Integer serviceId) {
        return utilisateurService.getManagersByService(serviceId);
    }

    @GetMapping("/{id}")
    public UtilisateurResponse getById(@PathVariable String id) {
        return utilisateurService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurResponse create(@Valid @RequestBody UtilisateurRequest request) {
        return utilisateurService.create(request);
    }

    @PutMapping("/{id}")
    public UtilisateurResponse update(@PathVariable String id,
            @Valid @RequestBody UtilisateurUpdateRequest request) {
        return utilisateurService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public UtilisateurResponse deactivate(@PathVariable String id, Authentication authentication) {
        return utilisateurService.deactivate(id, authentication.getName());
    }

    @PatchMapping("/{id}/role")
    public UtilisateurResponse changerRole(@PathVariable String id,
            @Valid @RequestBody ChangeRoleRequest request) {
        return utilisateurService.changerRole(id, request.roleCode());
    }
}
