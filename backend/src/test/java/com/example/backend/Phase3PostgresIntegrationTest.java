package com.example.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "PG_INTEGRATION_URL", matches = "jdbc:postgresql:.+")
class Phase3PostgresIntegrationTest {
    private static final String START = "2035-01-01";
    private static final String END = "2035-12-31";

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("PG_INTEGRATION_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("PG_INTEGRATION_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv("PG_INTEGRATION_PASSWORD"));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("jwt.secret", () -> "postgres-integration-test-secret-32-bytes-minimum");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    private String suffix;
    private int serviceId;
    private int centreId;
    private int userId;
    private int supplierId;
    private String email;
    private String rejectedReference;
    private String pendingReference;
    private String orderedReference;

    @BeforeEach
    void preparePostgresFixtures() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        email = "phase3-" + suffix + "@example.test";
        rejectedReference = "R-" + suffix;
        pendingReference = "P-" + suffix;
        orderedReference = "O-" + suffix;

        serviceId = jdbc.queryForObject(
                "INSERT INTO service(code, nom) VALUES (?, ?) RETURNING id",
                Integer.class, "IT" + suffix.substring(0, 10), "Phase 3 " + suffix);
        centreId = jdbc.queryForObject(
                "INSERT INTO centre_cout(code, libelle, service_id) VALUES (?, ?, ?) RETURNING id",
                Integer.class, "CC" + suffix.substring(0, 10), "Centre test " + suffix, serviceId);
        userId = jdbc.queryForObject("""
                INSERT INTO utilisateur(nom, prenom, email, mot_de_passe, role_id, service_id)
                VALUES ('Integration', 'Phase3', ?, 'test-only',
                        (SELECT id FROM role WHERE code='ADMIN'), ?) RETURNING id
                """, Integer.class, email, serviceId);
        supplierId = jdbc.queryForObject(
                "INSERT INTO fournisseur(raison_sociale) VALUES (?) RETURNING id",
                Integer.class, "Fournisseur test " + suffix);

        jdbc.update("INSERT INTO budget(centre_cout_id, annee, montant_alloue, seuil_alerte_pct) VALUES (?, 2035, 200, 80)", centreId);
        jdbc.update("""
                INSERT INTO demande_achat(reference, demandeur_id, centre_cout_id, objet, montant_estime,
                                          statut, date_creation, date_soumission)
                VALUES (?, ?, ?, 'Demande rejetee', 25, 'REJETEE', '2035-02-01', '2035-02-02'),
                       (?, ?, ?, 'Demande en cours', 50, 'VALIDEE', '2035-03-01', '2035-03-02'),
                       (?, ?, ?, 'Demande commande', 123.45, 'COMMANDEE', '2034-12-01', '2035-01-02')
                """, rejectedReference, userId, centreId,
                pendingReference, userId, centreId,
                orderedReference, userId, centreId);
        jdbc.update("""
                INSERT INTO bon_commande(numero, demande_id, fournisseur_id, cree_par_id, montant_total, statut, date_emission)
                SELECT ?, id, ?, ?, 123.45, 'GENERE', '2035-01-04 12:00:00'
                FROM demande_achat WHERE reference=?
                """, "BC-" + suffix, supplierId, userId, orderedReference);
    }

    @AfterEach
    void removePostgresFixtures() {
        if (suffix == null) return;
        jdbc.update("DELETE FROM bon_commande WHERE demande_id IN (SELECT id FROM demande_achat WHERE reference IN (?, ?, ?))",
                rejectedReference, pendingReference, orderedReference);
        jdbc.update("DELETE FROM demande_achat WHERE reference IN (?, ?, ?)", rejectedReference, pendingReference, orderedReference);
        jdbc.update("DELETE FROM budget WHERE centre_cout_id=? AND annee=2035", centreId);
        jdbc.update("DELETE FROM utilisateur WHERE id=?", userId);
        jdbc.update("DELETE FROM fournisseur WHERE id=?", supplierId);
        jdbc.update("DELETE FROM centre_cout WHERE id=?", centreId);
        jdbc.update("DELETE FROM service WHERE id=?", serviceId);
    }

    @Test
    void migrationsCalculsBudgetairesEtPermissionsFonctionnentSurPostgres() throws Exception {
        mvc.perform(get("/api/v2/rapports").param("du", START).param("au", END)
                        .with(user(email).roles("SERVICE_ACHATS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicateurs.nombre_demandes").value(2))
                .andExpect(jsonPath("$.indicateurs.demandes_rejetees").value(1))
                .andExpect(jsonPath("$.indicateurs.taux_rejet_pct").value(50.0))
                .andExpect(jsonPath("$.indicateurs.delai_moyen_traitement_jours").value(2.5))
                .andExpect(jsonPath("$.indicateurs.depenses").value(123.45));

        mvc.perform(get("/api/v2/rapports").param("du", START).param("au", END)
                        .with(user(email).roles("MANAGER")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v2/budgets").param("annee", "2035")
                        .with(user(email).roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].montant_engage").value(123.45));

        mvc.perform(get("/api/v2/rapports").param("du", "2036-01-01").param("au", "2036-12-31")
                        .with(user(email).roles("SERVICE_ACHATS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicateurs.nombre_demandes").value(0))
                .andExpect(jsonPath("$.indicateurs.taux_rejet_pct").value(0));
    }
}
