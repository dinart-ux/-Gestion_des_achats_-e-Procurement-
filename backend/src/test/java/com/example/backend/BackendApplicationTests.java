package com.example.backend;

import com.example.backend.Service.AchatWorkflowService;
import com.example.backend.Service.FournisseurService;
import com.example.backend.Models.Utilisateur;
import com.example.backend.Repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:eprocurement-test;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"jwt.secret=test-only-secret-with-more-than-32-bytes"
})
@AutoConfigureMockMvc
class BackendApplicationTests {
	@Autowired
	private MockMvc mvc;

	@Autowired
	private UtilisateurRepository utilisateurRepository;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private AchatWorkflowService workflow;

	@MockitoBean
	private FournisseurService fournisseurService;

	@Test
	void contextLoads() {
		
	}

	@Test
	@Transactional
	void connexionAccepteLeMatriculeEtLemailEtConstruitLePrincipalAvecLeMatricule() throws Exception {
		jdbc.update("INSERT INTO role (code, libelle) VALUES (?, ?)", "DEMANDEUR", "Demandeur test");
		Integer roleId = jdbc.queryForObject("SELECT id FROM role WHERE code = ?", Integer.class, "DEMANDEUR");
		String matricule = "T" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		String email = matricule.toLowerCase() + "@example.test";
		String plainTestPassword = "integration-test-password";

		Utilisateur utilisateur = new Utilisateur();
		utilisateur.setMatricule(matricule);
		utilisateur.setNom("Test");
		utilisateur.setPrenom("Connexion");
		utilisateur.setEmail(email);
		utilisateur.setPassword(passwordEncoder.encode(plainTestPassword));
		utilisateur.setRole(roleId);
		utilisateurRepository.saveAndFlush(utilisateur);

		for (String identifier : List.of(matricule, email)) {
			mvc.perform(post("/api/auth/login")
						.contentType(APPLICATION_JSON)
						.content("{\"identifiant\":\"" + identifier + "\",\"password\":\"" + plainTestPassword + "\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.matricule").value(matricule))
					.andExpect(jsonPath("$.id").value(matricule))
					.andExpect(jsonPath("$.role").value("DEMANDEUR"))
					.andExpect(jsonPath("$.token").isNotEmpty());
		}
	}

	@Test
	@Transactional
	void persistenceUtilisateurUtiliseLaCleNumeriqueEtConserveLesRelationsNumeriques() {
		String matricule = "T" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		Utilisateur utilisateur = new Utilisateur();
		utilisateur.setMatricule(matricule);
		utilisateur.setNom("Test");
		utilisateur.setPrenom("Utilisateur");
		utilisateur.setEmail(matricule.toLowerCase() + "@example.test");
		utilisateur.setPassword("encoded-test-password");
		utilisateur.setRole(817);
		utilisateur.setService(624);
		utilisateur.setManagerId(395);

		Utilisateur saved = utilisateurRepository.saveAndFlush(utilisateur);
		Integer databaseId = saved.getId();

		org.junit.jupiter.api.Assertions.assertNotNull(databaseId);
		org.junit.jupiter.api.Assertions.assertEquals(matricule,
				utilisateurRepository.findByMatricule(matricule).orElseThrow().getMatricule());
		org.junit.jupiter.api.Assertions.assertEquals(matricule,
				utilisateurRepository.findById(databaseId).orElseThrow().getMatricule());
		org.junit.jupiter.api.Assertions.assertEquals(624, saved.getService());
		org.junit.jupiter.api.Assertions.assertEquals(395, saved.getManagerId());

		saved.setEmail("updated-" + matricule.toLowerCase() + "@example.test");
		saved.setActif(false);
		utilisateurRepository.saveAndFlush(saved);

		Utilisateur updated = utilisateurRepository.findById(databaseId).orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals("updated-" + matricule.toLowerCase() + "@example.test",
				updated.getEmail());
		org.junit.jupiter.api.Assertions.assertFalse(updated.isActif());
	}

	@Test
	void consultationBudgetEstReserveeAuxRolesAutorises() throws Exception {
		when(workflow.budgets(2035, null, "admin@example.test", "ADMIN"))
				.thenReturn(List.of(Map.of("centre_cout_id", 4, "budget_id", 9)));
		when(workflow.budgets(2035, null, "manager@example.test", "MANAGER"))
				.thenReturn(List.of(Map.of("centre_cout_id", 4, "budget_id", 9)));
		when(workflow.budgets(2035, null, "achats@example.test", "SERVICE_ACHATS"))
				.thenReturn(List.of(Map.of("centre_cout_id", 4, "budget_id", 9)));

		mvc.perform(get("/api/v2/budgets").param("annee", "2035")
					.with(user("admin@example.test").roles("ADMIN")))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v2/budgets").param("annee", "2035").param("centreCoutId", "4")
					.with(user("admin@example.test").roles("ADMIN")))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v2/budgets").param("annee", "2035")
					.with(user("manager@example.test").roles("MANAGER")))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v2/budgets").param("annee", "2035")
					.with(user("achats@example.test").roles("SERVICE_ACHATS")))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v2/budgets").param("annee", "2035")
					.with(user("demandeur@example.test").roles("DEMANDEUR")))
				.andExpect(status().isForbidden());
	}

	@Test
	void seulsAdminPeutChargerLesCentresActifsDuFormulaireBudget() throws Exception {
		when(workflow.centresCoutActifsPourBudget()).thenReturn(List.of(Map.of("code", "IT", "libelle", "Informatique")));

		mvc.perform(get("/api/v2/budgets/centres-cout")
					.with(user("admin@example.test").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].code").value("IT"));
		mvc.perform(get("/api/v2/budgets/centres-cout")
					.with(user("manager@example.test").roles("MANAGER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void seulsAdminPeutCreerOuModifierUnBudget() throws Exception {
		String body = """
				{"centreCoutId":4,"annee":2035,"montantAlloue":1000,"seuilAlertePct":80}
				""";
		when(workflow.enregistrerBudget(any()))
				.thenReturn(Map.of("centre_cout_id", 4, "annee", 2035, "montant_alloue", 1000));

		mvc.perform(put("/api/v2/budgets").with(user("admin@example.test").roles("ADMIN"))
					.contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.annee").value(2035));

		mvc.perform(put("/api/v2/budgets").with(user("manager@example.test").roles("MANAGER"))
					.contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
		mvc.perform(put("/api/v2/budgets").with(user("achats@example.test").roles("SERVICE_ACHATS"))
					.contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
		mvc.perform(put("/api/v2/budgets").with(user("demandeur@example.test").roles("DEMANDEUR"))
					.contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
		verify(workflow).enregistrerBudget(any());
	}

	@Test
	void budgetRefuseMontantOuSeuilInvalide() throws Exception {
		mvc.perform(put("/api/v2/budgets").with(user("admin@example.test").roles("ADMIN"))
					.contentType(APPLICATION_JSON)
					.content("""
							{"centreCoutId":4,"annee":2035,"montantAlloue":-1,"seuilAlertePct":101}
							"""))
				.andExpect(status().isBadRequest());
		mvc.perform(put("/api/v2/budgets").with(user("admin@example.test").roles("ADMIN"))
					.contentType(APPLICATION_JSON)
					.content("""
							{"centreCoutId":4,"annee":2035,"montantAlloue":0,"seuilAlertePct":80}
							"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void demandeurPeutListerSesDemandes() throws Exception {
		when(workflow.demandes("demandeur@example.test", "DEMANDEUR"))
				.thenReturn(List.of(Map.of("reference", "DA-TEST", "statut", "BROUILLON")));

		mvc.perform(get("/api/v2/demandes").with(user("demandeur@example.test").roles("DEMANDEUR")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].reference").value("DA-TEST"))
				.andExpect(jsonPath("$[0].statut").value("BROUILLON"));
	}

	@Test
	void creationDemandeValideLeDtoEtRetourneCreated() throws Exception {
		when(workflow.creerDemande(eq("demandeur@example.test"), any()))
				.thenReturn(Map.of("id", 42, "statut", "BROUILLON"));

		mvc.perform(post("/api/v2/demandes")
					.with(user("demandeur@example.test").roles("DEMANDEUR"))
					.contentType(APPLICATION_JSON)
					.content("""
							{"centreCoutId":1,"objet":"Fournitures","lignes":[{"type":"ARTICLE","designation":"Papier","quantite":2,"prixUnitaireEstime":4.5}]}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(42));

		verify(workflow).creerDemande(eq("demandeur@example.test"), any());
	}

	@Test
	void seulUnDemandeurPeutCreerUneDemande() throws Exception {
		mvc.perform(post("/api/v2/demandes")
					.with(user("manager@example.test").roles("MANAGER"))
					.contentType(APPLICATION_JSON)
					.content("""
							{"centreCoutId":1,"objet":"Achat","lignes":[{"type":"ARTICLE","designation":"Papier","quantite":1,"prixUnitaireEstime":2}]}
							"""))
				.andExpect(status().isForbidden());

		verify(workflow, never()).creerDemande(any(), any());
	}

	@Test
	void seulsLesDemandeursPeuventModifierOuSupprimerLeursBrouillonsViaLesRoutesActives() throws Exception {
		String body = """
				{"centreCoutId":1,"objet":"Fournitures corrigées","lignes":[{"type":"ARTICLE","designation":"Papier","quantite":2,"prixUnitaireEstime":4.5}]}
				""";
		when(workflow.modifierBrouillon(eq(42L), eq("demandeur@example.test"), any()))
				.thenReturn(Map.of("id", 42, "statut", "BROUILLON"));

		mvc.perform(put("/api/v2/demandes/42")
					.with(user("manager@example.test").roles("MANAGER"))
					.contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
		mvc.perform(delete("/api/v2/demandes/42")
					.with(user("manager@example.test").roles("MANAGER")))
				.andExpect(status().isForbidden());
		verify(workflow, never()).modifierBrouillon(anyLong(), any(), any());
		verify(workflow, never()).supprimerBrouillon(anyLong(), any());

		mvc.perform(put("/api/v2/demandes/42")
					.with(user("demandeur@example.test").roles("DEMANDEUR"))
					.contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isOk());
		mvc.perform(delete("/api/v2/demandes/42")
					.with(user("demandeur@example.test").roles("DEMANDEUR")))
				.andExpect(status().isNoContent());
		verify(workflow).modifierBrouillon(eq(42L), eq("demandeur@example.test"), any());
		verify(workflow).supprimerBrouillon(42L, "demandeur@example.test");
	}

	@Test
	void seulsLesValideursPeuventDeciderUneDemande() throws Exception {
		mvc.perform(post("/api/v2/demandes/42/decision")
					.with(user("demandeur@example.test").roles("DEMANDEUR"))
					.contentType(APPLICATION_JSON)
					.content("{\"decision\":\"VALIDEE\"}"))
				.andExpect(status().isForbidden());

		verify(workflow, never()).decider(anyLong(), any(), any(), any());
	}

	@Test
	void lesMutationsDesReferentielsSontReserveesAuxAdministrateurs() throws Exception {
		mvc.perform(post("/api/v2/services")
					.with(user("achats@example.test").roles("SERVICE_ACHATS"))
					.contentType(APPLICATION_JSON)
					.content("{\"code\":\"TEST\",\"nom\":\"Service test\"}"))
				.andExpect(status().isForbidden());

		mvc.perform(post("/api/v2/categories")
					.with(user("manager@example.test").roles("MANAGER"))
					.contentType(APPLICATION_JSON)
					.content("{\"nom\":\"Categorie test\"}"))
				.andExpect(status().isForbidden());

		verify(workflow, never()).creerService(any());
		verify(workflow, never()).creerCategorie(any(), any());
	}

	@Test
	void endpointsPrincipauxExigentUneAuthentificationEtLeBonRole() throws Exception {
		mvc.perform(get("/api/v2/demandes")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v2/validations/en-attente")
				.with(user("demandeur@example.test").roles("DEMANDEUR")))
				.andExpect(status().isForbidden());
	}

	@Test
	void rapportEstReserveAuServiceAchatsEtAuxAdministrateurs() throws Exception {
		when(workflow.rapport(java.time.LocalDate.parse("2035-01-01"), java.time.LocalDate.parse("2035-12-31")))
				.thenReturn(Map.of("total", 0));

		mvc.perform(get("/api/v2/rapports")
					.param("du", "2035-01-01").param("au", "2035-12-31")
					.with(user("manager@example.test").roles("MANAGER")))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/v2/rapports")
					.param("du", "2035-01-01").param("au", "2035-12-31")
					.with(user("achats@example.test").roles("SERVICE_ACHATS")))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v2/rapports")
					.param("du", "2035-01-01").param("au", "2035-12-31")
					.with(user("admin@example.test").roles("ADMIN")))
				.andExpect(status().isOk());
	}

	@Test
	void creationAccepteUnBrouillonSansLigneAvantSoumission() throws Exception {
		mvc.perform(post("/api/v2/demandes")
					.with(user("demandeur@example.test").roles("DEMANDEUR"))
					.contentType(APPLICATION_JSON)
					.content("{\"centreCoutId\":1,\"objet\":\"Fournitures\",\"lignes\":[]}"))
				.andExpect(status().isCreated());
	}

	@Test
	void creationAccepteUneQuantiteNulleEnBrouillonEtRefuseUnObjetTropLong() throws Exception {
		mvc.perform(post("/api/v2/demandes")
					.with(user("demandeur@example.test").roles("DEMANDEUR"))
					.contentType(APPLICATION_JSON)
					.content("""
							{"centreCoutId":1,"objet":"Achat","lignes":[{"type":"ARTICLE","designation":"Papier","quantite":0,"prixUnitaireEstime":2}]}
							"""))
				.andExpect(status().isCreated());

		mvc.perform(post("/api/v2/demandes")
					.with(user("demandeur@example.test").roles("DEMANDEUR"))
					.contentType(APPLICATION_JSON)
					.content("""
							{"centreCoutId":0,"objet":"%s","lignes":[{"type":"ARTICLE","designation":"Papier","quantite":1,"prixUnitaireEstime":2}]}
							""".formatted("x".repeat(201))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void uneErreurInterneNestPasTransformeeEnBadRequest() throws Exception {
		when(workflow.creerDemande(eq("demandeur@example.test"), any()))
				.thenThrow(new IllegalStateException("Erreur SQL de test"));

		mvc.perform(post("/api/v2/demandes")
					.with(user("demandeur@example.test").roles("DEMANDEUR"))
					.contentType(APPLICATION_JSON)
					.content("""
							{"centreCoutId":1,"objet":"Fournitures","lignes":[{"type":"ARTICLE","designation":"Papier","quantite":2,"prixUnitaireEstime":4.5}]}
							"""))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.message").value("Une erreur interne est survenue. Consultez les logs du serveur."));
	}

	@Test
	void seulsAchatsEtAdminPeuventCreerUnFournisseur() throws Exception {
		mvc.perform(post("/api/v2/fournisseurs")
					.with(user("demandeur@example.test").roles("DEMANDEUR"))
					.contentType(APPLICATION_JSON)
					.content("""
							{"raisonSociale":"Fournisseur test","categories":[]}
							"""))
				.andExpect(status().isForbidden());

		when(fournisseurService.create(any())).thenReturn(Map.of("id", 9, "raison_sociale", "Fournisseur test"));
		mvc.perform(post("/api/v2/fournisseurs")
					.with(user("achats@example.test").roles("SERVICE_ACHATS"))
					.contentType(APPLICATION_JSON)
					.content("""
							{"raisonSociale":"Fournisseur test","categories":[]}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(9));

		mvc.perform(post("/api/v2/fournisseurs")
					.with(user("achats@example.test").roles("SERVICE_ACHATS"))
					.contentType(APPLICATION_JSON)
					.content("""
							{"raisonSociale":"Fournisseur valide","email":"pas-une-adresse","categories":[]}
							"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void modificationFournisseurEstReserveeAuxAchatsEtAdmin() throws Exception {
		String supplier = """
				{"raisonSociale":"Fournisseur test","categories":[]}
				""";
		mvc.perform(put("/api/v2/fournisseurs/9")
					.with(user("manager@example.test").roles("MANAGER"))
					.contentType(APPLICATION_JSON)
					.content(supplier))
				.andExpect(status().isForbidden());

		when(fournisseurService.update(eq(9), any()))
				.thenReturn(Map.of("id", 9, "raison_sociale", "Fournisseur test"));
		mvc.perform(put("/api/v2/fournisseurs/9")
					.with(user("admin@example.test").roles("ADMIN"))
					.contentType(APPLICATION_JSON)
					.content(supplier))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(9));
	}

	@Test
	void seulsAchatsEtAdminPeuventEnregistrerUneOffreEtLeDtoRefuseLesValeursInvalides() throws Exception {
		String validOffer = """
				{"fournisseurId":2,"montantTotal":1250.50,"delaiLivraisonJours":4,"conditions":"Paiement à 30 jours"}
				""";
		mvc.perform(post("/api/v2/demandes/12/offres")
					.with(user("manager@example.test").roles("MANAGER"))
					.contentType(APPLICATION_JSON)
					.content(validOffer))
				.andExpect(status().isForbidden());

		when(workflow.creerOffre(eq(12L), any()))
				.thenReturn(Map.of("id", 14, "demande_id", 12, "statut", "RECUE"));
		mvc.perform(post("/api/v2/demandes/12/offres")
					.with(user("achats@example.test").roles("SERVICE_ACHATS"))
					.contentType(APPLICATION_JSON)
					.content(validOffer))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.statut").value("RECUE"));

		mvc.perform(post("/api/v2/demandes/12/offres")
					.with(user("achats@example.test").roles("SERVICE_ACHATS"))
					.contentType(APPLICATION_JSON)
					.content("""
							{"fournisseurId":2,"montantTotal":1250.50,"delaiLivraisonJours":-1}
							"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void choixEtRefusOffreSontDesRoutesReserveesAuxAchatsEtAdmin() throws Exception {
		when(workflow.retenirOffreEtCreerCommande(
        eq(12L),
        eq(14L),
        any()
			)).thenReturn(Map.of(
				"id", 14,
				"statut", "RETENUE"
			));
		when(workflow.refuserOffre(12L, 14L)).thenReturn(Map.of("id", 14, "statut", "REFUSEE"));

		mvc.perform(post("/api/v2/demandes/12/offres/14/retenir")
					.with(user("demandeur@example.test").roles("DEMANDEUR")))
				.andExpect(status().isForbidden());

		mvc.perform(post("/api/v2/demandes/12/offres/14/retenir")
					.with(user("achats@example.test").roles("SERVICE_ACHATS")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statut").value("RETENUE"));

		mvc.perform(post("/api/v2/demandes/12/offres/14/refuser")
					.with(user("admin@example.test").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statut").value("REFUSEE"));
	}
}
