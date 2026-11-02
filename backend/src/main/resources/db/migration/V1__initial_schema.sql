
-- ============================================================
-- PROJET E-PROCUREMENT : SCHEMA INITIAL
-- PostgreSQL
-- ============================================================

-- 1. CREATION DES TYPES ENUM

CREATE TYPE statut_demande AS ENUM (
    'BROUILLON',
    'SOUMISE',
    'VALIDEE',
    'REJETEE',
    'COMMANDEE',
    'RECEPTIONNEE'
);

CREATE TYPE decision_validation AS ENUM (
    'EN_ATTENTE',
    'VALIDEE',
    'REJETEE',
    'PRECISION_DEMANDEE'
);

CREATE TYPE statut_offre AS ENUM (
    'RECUE',
    'RETENUE',
    'REFUSEE'
);

CREATE TYPE statut_bc AS ENUM (
    'GENERE',
    'ENVOYE',
    'PARTIELLEMENT_RECU',
    'RECU',
    'ANNULE'
);

CREATE TYPE type_article AS ENUM (
    'ARTICLE',
    'SERVICE'
);

-- ============================================================
-- 2. ROLES
-- ============================================================

CREATE TABLE role (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

-- ============================================================
-- 3. SERVICES
-- ============================================================

CREATE TABLE service (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    nom VARCHAR(100) NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

-- ============================================================
-- 4. UTILISATEURS
-- ============================================================

CREATE TABLE utilisateurs (
    id SERIAL PRIMARY KEY,
    matricule VARCHAR(20) UNIQUE,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(255) NOT NULL,
    telephone VARCHAR(30),
    role_id INT NOT NULL REFERENCES role(id),
    service_id INT REFERENCES service(id),
    manager_id INT REFERENCES utilisateurs(id),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 5. CENTRES DE COUT
-- ============================================================

CREATE TABLE centre_cout (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    service_id INT NOT NULL REFERENCES service(id),
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

-- ============================================================
-- 6. BUDGETS
-- ============================================================

CREATE TABLE budget (
    id SERIAL PRIMARY KEY,
    centre_cout_id INT NOT NULL REFERENCES centre_cout(id),
    annee INT NOT NULL,
    montant_alloue NUMERIC(15,2) NOT NULL
        CHECK (montant_alloue >= 0),
    montant_engage NUMERIC(15,2) NOT NULL DEFAULT 0
        CHECK (montant_engage >= 0),
    seuil_alerte_pct NUMERIC(5,2) NOT NULL DEFAULT 80
        CHECK (seuil_alerte_pct BETWEEN 0 AND 100),
    UNIQUE (centre_cout_id, annee)
);

-- ============================================================
-- 7. ALERTES BUDGETAIRES
-- ============================================================

CREATE TABLE alerte_budget (
    id SERIAL PRIMARY KEY,
    budget_id INT NOT NULL
        REFERENCES budget(id) ON DELETE CASCADE,
    type_alerte VARCHAR(20) NOT NULL,
    message VARCHAR(255) NOT NULL,
    date_alerte TIMESTAMP NOT NULL DEFAULT NOW(),
    traitee BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================
-- 8. CATEGORIES
-- ============================================================

CREATE TABLE categorie (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- ============================================================
-- 9. FOURNISSEURS
-- ============================================================

CREATE TABLE fournisseur (
    id SERIAL PRIMARY KEY,
    raison_sociale VARCHAR(150) NOT NULL,
    nif VARCHAR(30),
    stat VARCHAR(30),
    adresse VARCHAR(255),
    telephone VARCHAR(30),
    email VARCHAR(150),
    contact_nom VARCHAR(100),
    conditions_paiement VARCHAR(255),
    delai_livraison_jours INT,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 10. FOURNISSEURS ET CATEGORIES
-- ============================================================

CREATE TABLE fournisseur_categorie (
    fournisseur_id INT NOT NULL
        REFERENCES fournisseur(id) ON DELETE CASCADE,
    categorie_id INT NOT NULL
        REFERENCES categorie(id) ON DELETE CASCADE,
    PRIMARY KEY (fournisseur_id, categorie_id)
);

-- ============================================================
-- 11. DEMANDES D'ACHAT
-- ============================================================

CREATE TABLE demande_achat (
    id SERIAL PRIMARY KEY,
    reference VARCHAR(30) NOT NULL UNIQUE,
    demandeur_id INT NOT NULL REFERENCES utilisateurs(id),
    centre_cout_id INT NOT NULL REFERENCES centre_cout(id),
    objet VARCHAR(200) NOT NULL,
    justification TEXT,
    montant_estime NUMERIC(15,2) NOT NULL DEFAULT 0
        CHECK (montant_estime >= 0),
    statut statut_demande NOT NULL DEFAULT 'BROUILLON',
    date_creation TIMESTAMP NOT NULL DEFAULT NOW(),
    date_soumission TIMESTAMP
);

-- ============================================================
-- 12. LIGNES DES DEMANDES
-- ============================================================

CREATE TABLE ligne_demande (
    id SERIAL PRIMARY KEY,
    demande_id INT NOT NULL
        REFERENCES demande_achat(id) ON DELETE CASCADE,
    categorie_id INT REFERENCES categorie(id),
    type type_article NOT NULL DEFAULT 'ARTICLE',
    designation VARCHAR(200) NOT NULL,
    quantite NUMERIC(12,2) NOT NULL CHECK (quantite > 0),
    prix_unitaire_estime NUMERIC(15,2) NOT NULL DEFAULT 0
        CHECK (prix_unitaire_estime >= 0)
);

-- ============================================================
-- 13. HISTORIQUE DES STATUTS
-- ============================================================

CREATE TABLE historique_statut (
    id SERIAL PRIMARY KEY,
    demande_id INT NOT NULL
        REFERENCES demande_achat(id) ON DELETE CASCADE,
    ancien_statut statut_demande,
    nouveau_statut statut_demande NOT NULL,
    utilisateur_id INT REFERENCES utilisateurs(id),
    commentaire VARCHAR(255),
    date_changement TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 14. CIRCUITS DE VALIDATION
-- ============================================================

CREATE TABLE circuit_validation (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    service_id INT REFERENCES service(id),
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

-- ============================================================
-- 15. NIVEAUX DE VALIDATION
-- ============================================================

CREATE TABLE niveau_validation (
    id SERIAL PRIMARY KEY,
    circuit_id INT NOT NULL
        REFERENCES circuit_validation(id) ON DELETE CASCADE,
    ordre INT NOT NULL CHECK (ordre > 0),
    role_id INT NOT NULL REFERENCES role(id),
    seuil_montant NUMERIC(15,2) NOT NULL DEFAULT 0
        CHECK (seuil_montant >= 0),
    UNIQUE (circuit_id, ordre)
);

-- ============================================================
-- 16. VALIDATIONS
-- ============================================================

CREATE TABLE validation (
    id SERIAL PRIMARY KEY,
    demande_id INT NOT NULL
        REFERENCES demande_achat(id) ON DELETE CASCADE,
    niveau_id INT NOT NULL REFERENCES niveau_validation(id),
    valideur_id INT REFERENCES utilisateurs(id),
    decision decision_validation NOT NULL DEFAULT 'EN_ATTENTE',
    commentaire TEXT,
    date_decision TIMESTAMP,
    UNIQUE (demande_id, niveau_id)
);

-- ============================================================
-- 17. OFFRES DES FOURNISSEURS
-- ============================================================

CREATE TABLE offre (
    id SERIAL PRIMARY KEY,
    demande_id INT NOT NULL
        REFERENCES demande_achat(id) ON DELETE CASCADE,
    fournisseur_id INT NOT NULL REFERENCES fournisseur(id),
    montant_total NUMERIC(15,2) NOT NULL
        CHECK (montant_total >= 0),
    delai_livraison_jours INT,
    conditions VARCHAR(255),
    statut statut_offre NOT NULL DEFAULT 'RECUE',
    date_offre DATE NOT NULL DEFAULT CURRENT_DATE,
    UNIQUE (demande_id, fournisseur_id)
);

-- ============================================================
-- 18. BONS DE COMMANDE
-- ============================================================

CREATE TABLE bon_commande (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) NOT NULL UNIQUE,
    demande_id INT NOT NULL REFERENCES demande_achat(id),
    offre_id INT REFERENCES offre(id),
    fournisseur_id INT NOT NULL REFERENCES fournisseur(id),
    cree_par_id INT NOT NULL REFERENCES utilisateurs(id),
    montant_total NUMERIC(15,2) NOT NULL
        CHECK (montant_total >= 0),
    statut statut_bc NOT NULL DEFAULT 'GENERE',
    date_emission TIMESTAMP NOT NULL DEFAULT NOW(),
    date_livraison_prevue DATE
);

-- ============================================================
-- 19. LIGNES DES BONS DE COMMANDE
-- ============================================================

CREATE TABLE ligne_bon_commande (
    id SERIAL PRIMARY KEY,
    bon_commande_id INT NOT NULL
        REFERENCES bon_commande(id) ON DELETE CASCADE,
    ligne_demande_id INT REFERENCES ligne_demande(id),
    designation VARCHAR(200) NOT NULL,
    quantite NUMERIC(12,2) NOT NULL CHECK (quantite > 0),
    prix_unitaire NUMERIC(15,2) NOT NULL
        CHECK (prix_unitaire >= 0)
);

-- ============================================================
-- 20. RECEPTIONS
-- ============================================================

CREATE TABLE reception (
    id SERIAL PRIMARY KEY,
    bon_commande_id INT NOT NULL REFERENCES bon_commande(id),
    receptionnaire_id INT NOT NULL REFERENCES utilisateurs(id),
    date_reception TIMESTAMP NOT NULL DEFAULT NOW(),
    type_reception VARCHAR(10) NOT NULL DEFAULT 'TOTALE'
        CHECK (type_reception IN ('PARTIELLE', 'TOTALE')),
    observation VARCHAR(255)
);

-- ============================================================
-- 21. LIGNES DE RECEPTION
-- ============================================================

CREATE TABLE ligne_reception (
    id SERIAL PRIMARY KEY,
    reception_id INT NOT NULL
        REFERENCES reception(id) ON DELETE CASCADE,
    ligne_bc_id INT NOT NULL REFERENCES ligne_bon_commande(id),
    quantite_recue NUMERIC(12,2) NOT NULL
        CHECK (quantite_recue > 0)
);

-- ============================================================
-- 22. INDEX POUR AMELIORER LES RECHERCHES
-- ============================================================

CREATE INDEX idx_demande_statut
    ON demande_achat(statut);

CREATE INDEX idx_demande_demandeur
    ON demande_achat(demandeur_id);

CREATE INDEX idx_demande_cc
    ON demande_achat(centre_cout_id);

CREATE INDEX idx_validation_decision
    ON validation(decision);

CREATE INDEX idx_bc_fournisseur
    ON bon_commande(fournisseur_id);

CREATE INDEX idx_bc_statut
    ON bon_commande(statut);

-- ============================================================
-- 23. INSERTION DES ROLES PAR DEFAUT
-- ============================================================

INSERT INTO role (code, libelle) VALUES
    ('DEMANDEUR', 'Demandeur'),
    ('MANAGER', 'Manager'),
    ('SERVICE_ACHATS', 'Service achats'),
    ('ADMIN', 'Administrateur')
ON CONFLICT (code) DO NOTHING;
