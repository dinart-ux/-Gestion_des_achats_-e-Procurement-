package com.example.backend.Service;

import com.example.backend.DTO.UtilisateurRequest;
import com.example.backend.DTO.UtilisateurUpdateRequest;
import com.example.backend.Models.Utilisateur;
import com.example.backend.Repository.UtilisateurRepository;
import com.example.backend.Repository.RoleRepository;
import com.example.backend.Repository.ManagerRepository;
import com.example.backend.Controlleur.UtilisateurController;
import com.example.backend.security.LegacyAwarePasswordEncoder;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class UtilisateurServiceTest {

        private final UtilisateurRepository utilisateurs =
        mock(UtilisateurRepository.class);

        private final RoleRepository roles =
        mock(RoleRepository.class);

        private final ManagerRepository managers = mock(ManagerRepository.class);

        private final UtilisateurAuthStateService authStateService =
                mock(UtilisateurAuthStateService.class);

        private final LegacyAwarePasswordEncoder passwordEncoder =new LegacyAwarePasswordEncoder();

        private final UtilisateurService utilisateurService =
                new UtilisateurService(
                        utilisateurs,
                        roles,
                        managers,
                        passwordEncoder,
                        authStateService,
                        false
                );

        @Test
        void creationHashLeMotDePasseEtRetourneUnDto() {

                when(utilisateurs.existsByEmailIgnoreCase("amina@example.test"))
                        .thenReturn(false);

                when(utilisateurs.existsByMatricule("M-10"))
                        .thenReturn(false);

                when(roles.findIdByCode("DEMANDEUR"))
                        .thenReturn(Optional.of(1));
                when(roles.findCodeById(1)).thenReturn(Optional.of("DEMANDEUR"));
                when(managers.serviceExists(1)).thenReturn(true);
                when(managers.isActiveManagerOfService(20, 1)).thenReturn(true);

                when(utilisateurs.save(any(Utilisateur.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));

                var response = utilisateurService.create(
                        new UtilisateurRequest(
                                "M-10",
                                "Diallo",
                                "Amina",
                                " amina@example.test ",
                                "mot-de-passe-123",
                                "DEMANDEUR",
                                "1",
                                20,
                                "+33123456789"
                        )
                );

                assertEquals("amina@example.test", response.email());
                assertEquals("M-10", response.id());
                assertEquals("M-10", response.matricule());
                assertEquals("1", response.role());
                assertEquals("DEMANDEUR", response.roleCode());
                assertEquals("+33123456789", response.telephone());

                ArgumentCaptor<Utilisateur> captor =
                        ArgumentCaptor.forClass(Utilisateur.class);

                verify(utilisateurs).save(captor.capture());

                Utilisateur saved = captor.getValue();

                assertEquals(1, saved.getRole());
                assertEquals(20, saved.getManagerId());
                assertEquals("+33123456789", saved.getTelephone());
                assertNotEquals("mot-de-passe-123", saved.getPassword());

                assertTrue(
                        passwordEncoder.matches(
                                "mot-de-passe-123",
                                saved.getPassword()
                        )
                );
        }

        @Test
        void deuxDemandeursPeuventAvoirDesManagersDifferentsDansLeMemeService() {
                when(utilisateurs.existsByEmailIgnoreCase("a@example.test")).thenReturn(false);
                when(utilisateurs.existsByEmailIgnoreCase("b@example.test")).thenReturn(false);
                when(utilisateurs.existsByMatricule("A-1")).thenReturn(false);
                when(utilisateurs.existsByMatricule("B-1")).thenReturn(false);
                when(roles.findIdByCode("DEMANDEUR")).thenReturn(Optional.of(1));
                when(managers.serviceExists(1)).thenReturn(true);
                when(managers.isActiveManagerOfService(20, 1)).thenReturn(true);
                when(managers.isActiveManagerOfService(21, 1)).thenReturn(true);
                when(utilisateurs.save(any(Utilisateur.class))).thenAnswer(invocation -> invocation.getArgument(0));

                utilisateurService.create(new UtilisateurRequest("A-1", "Nom", "A", "a@example.test",
                        "mot-de-passe-123", "DEMANDEUR", "1", 20, null));
                utilisateurService.create(new UtilisateurRequest("B-1", "Nom", "B", "b@example.test",
                        "mot-de-passe-123", "DEMANDEUR", "1", 21, null));

                ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
                verify(utilisateurs, org.mockito.Mockito.times(2)).save(captor.capture());
                assertEquals(java.util.List.of(20, 21), captor.getAllValues().stream()
                        .map(Utilisateur::getManagerId).toList());
        }

        @Test
        void refuseManagerAbsentOuInvalidePourUnDemandeur() {
                when(utilisateurs.existsByEmailIgnoreCase("a@example.test")).thenReturn(false);
                when(utilisateurs.existsByMatricule("A-1")).thenReturn(false);
                when(roles.findIdByCode("DEMANDEUR")).thenReturn(Optional.of(1));
                when(managers.serviceExists(1)).thenReturn(true);
                when(managers.isActiveManagerOfService(99, 1)).thenReturn(false);

                var sansManager = new UtilisateurRequest("A-1", "Nom", "A", "a@example.test",
                        "mot-de-passe-123", "DEMANDEUR", "1", null, null);
                org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                        () -> utilisateurService.create(sansManager));

                var managerIncorrect = new UtilisateurRequest("A-1", "Nom", "A", "a@example.test",
                        "mot-de-passe-123", "DEMANDEUR", "1", 99, null);
                org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                        () -> utilisateurService.create(managerIncorrect));
                verify(utilisateurs, org.mockito.Mockito.never()).save(any(Utilisateur.class));
        }

        @Test
        void lesAutresRolesNeDoiventPasEtreAssociesAUnManager() {
                when(utilisateurs.existsByEmailIgnoreCase("achats@example.test")).thenReturn(false);
                when(utilisateurs.existsByMatricule("ACH-1")).thenReturn(false);
                when(roles.findIdByCode("SERVICE_ACHATS")).thenReturn(Optional.of(3));
                when(managers.serviceExists(1)).thenReturn(true);
                when(utilisateurs.save(any(Utilisateur.class))).thenAnswer(invocation -> invocation.getArgument(0));

                utilisateurService.create(new UtilisateurRequest("ACH-1", "Nom", "A", "achats@example.test",
                        "mot-de-passe-123", "SERVICE_ACHATS", "1", null, null));

                ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
                verify(utilisateurs).save(captor.capture());
                assertEquals(null, captor.getValue().getManagerId());
        }

        @Test
        void laModificationConserveLesInformationsEtValideLeManagerDuDemandeur() {
                Utilisateur existant = new Utilisateur();
                existant.setMatricule("M-30");
                existant.setNom("Ancien nom");
                existant.setPrenom("Amina");
                existant.setEmail("amina@example.test");
                existant.setPassword("hash-existant");
                existant.setRole(1);
                existant.setService(1);
                existant.setManagerId(20);
                when(utilisateurs.findByMatricule("M-30")).thenReturn(Optional.of(existant));
                when(utilisateurs.existsByEmailIgnoreCaseAndMatriculeNot("amina@example.test", "M-30"))
                        .thenReturn(false);
                when(roles.findIdByCode("DEMANDEUR")).thenReturn(Optional.of(1));
                when(managers.isActiveManagerOfService(21, 1)).thenReturn(true);
                when(managers.serviceExists(1)).thenReturn(true);
                when(utilisateurs.save(any(Utilisateur.class))).thenAnswer(invocation -> invocation.getArgument(0));

                utilisateurService.update("M-30", new UtilisateurUpdateRequest(
                        "Nouveau nom", "Amina", "amina@example.test", "DEMANDEUR", "1", 21, "+33987654321"));

                assertEquals("M-30", existant.getMatricule());
                assertEquals("hash-existant", existant.getPassword());
                assertEquals("Nouveau nom", existant.getNom());
                assertEquals(21, existant.getManagerId());
                assertEquals("+33987654321", existant.getTelephone());
        }

        @Test
        void lesRoutesDeGestionRestentReserveesAuxAdministrateurs() {
                var authorization = UtilisateurController.class
                        .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
                assertEquals("hasRole('ADMIN')", authorization.value());
        }
}       
