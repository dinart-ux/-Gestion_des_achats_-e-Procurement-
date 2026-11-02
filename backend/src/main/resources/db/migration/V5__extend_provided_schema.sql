-- Complète le schéma historique fourni par le projet.
-- Les tables et colonnes existantes restent en place ; les champs métier
-- manquants et les tables de détail sont ajoutés ici.

-- Sur une installation neuve, crée d'abord les huit tables reçues. Les types
-- tableau sont temporairement conservés ici et normalisés plus bas.
CREATE TABLE IF NOT EXISTS public."RECEPTION" (
    "Quantite_Recue" integer[] NOT NULL,
    "Date_Reception" date[] NOT NULL,
    "Id_Recue" varchar(20) PRIMARY KEY
);
CREATE TABLE IF NOT EXISTS public."BON_COMMANDE" (
    "Satut" varchar(20)[] NOT NULL,
    "Referance" varchar(50)[] NOT NULL,
    "Date" date[] NOT NULL,
    "Num_Commande" integer PRIMARY KEY,
    "Id_Recue" varchar
);
CREATE TABLE IF NOT EXISTS public."DEMANDES_ACHATS" (
    "Num_Demande" varchar[] PRIMARY KEY,
    "Titre" varchar[] NOT NULL,
    "Satut_Demande" varchar[] NOT NULL,
    "Coordonnee" varchar[] NOT NULL,
    "Categorie" varchar[] NOT NULL,
    "Condition_Demande" text[] NOT NULL,
    "Date_Demande" date[] NOT NULL,
    "Num_Commande" integer,
    "Num_Offre" integer[]
);
CREATE TABLE IF NOT EXISTS public."CENTRE_COUT" (
    "Nom_Centre" name[] NOT NULL,
    "Montant_Alloue" bigint[] NOT NULL,
    "Montant_Consomme" bigint[] NOT NULL,
    "Id_Centre" integer PRIMARY KEY,
    "Num_Demande" varchar[]
);
CREATE TABLE IF NOT EXISTS public."UTILISATEURS" (
    "Num_Validation" integer,
    "Num_Demande" varchar[],
    "Matricule" varchar(7) PRIMARY KEY,
    "Nom" varchar(50) NOT NULL,
    "Prenom" varchar(75) NOT NULL,
    "Role" varchar(50) NOT NULL,
    "Email" varchar(75) NOT NULL,
    "Service" varchar(50) NOT NULL,
    "Mot_de_Passe" varchar(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS public."FOURNISSEURS" (
    "Nom" name[] NOT NULL,
    "Coordonnee" varchar(50)[] NOT NULL,
    "Conditions_Fournisseur" text[] NOT NULL,
    "Service" varchar(50)[] NOT NULL,
    "Num_Commande" integer,
    "Ref_Fournisseur" varchar(20) PRIMARY KEY
);
CREATE TABLE IF NOT EXISTS public."OFFRE" (
    "Num_Offre" integer[] PRIMARY KEY,
    "Prix_Offre" bigint[] NOT NULL,
    "Condition_Offre" text[] NOT NULL,
    "Num_Commande" integer,
    "Ref_Fournisseur" varchar
);
CREATE TABLE IF NOT EXISTS public."VALIDATION" (
    "Statut_Validation" varchar(50)[] NOT NULL,
    "Commantaire" text[],
    "Date_Validation" date NOT NULL,
    "Niveau" varchar(10)[] NOT NULL,
    "Num_Validation" integer PRIMARY KEY
);

DO $$
DECLARE
    fk RECORD;
BEGIN
    -- Les anciennes clés étrangères reposent sur des colonnes tableau.
    -- On les retire le temps de ramener ces colonnes à une valeur unique.
    FOR fk IN
        SELECT conrelid::regclass AS table_name, conname
        FROM pg_constraint
        WHERE contype = 'f'
          AND conrelid IN (
                to_regclass('public."BON_COMMANDE"'),
                to_regclass('public."CENTRE_COUT"'),
                to_regclass('public."DEMANDES_ACHATS"'),
                to_regclass('public."FOURNISSEURS"'),
                to_regclass('public."OFFRE"'),
                to_regclass('public."UTILISATEURS"'),
                to_regclass('public."VALIDATION"')
          )
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', fk.table_name, fk.conname);
    END LOOP;
END $$;

-- Une demande, une offre et un utilisateur ont chacun une valeur par champ.
-- Le contrôle évite de perdre silencieusement un éventuel deuxième élément.
DO $$
DECLARE
    c RECORD;
BEGIN
    FOR c IN
        SELECT * FROM (VALUES
            ('RECEPTION', 'Quantite_Recue'), ('RECEPTION', 'Date_Reception'),
            ('BON_COMMANDE', 'Satut'), ('BON_COMMANDE', 'Referance'), ('BON_COMMANDE', 'Date'),
            ('CENTRE_COUT', 'Nom_Centre'), ('CENTRE_COUT', 'Montant_Alloue'), ('CENTRE_COUT', 'Montant_Consomme'), ('CENTRE_COUT', 'Num_Demande'),
            ('DEMANDES_ACHATS', 'Num_Demande'), ('DEMANDES_ACHATS', 'Titre'), ('DEMANDES_ACHATS', 'Satut_Demande'),
            ('DEMANDES_ACHATS', 'Coordonnee'), ('DEMANDES_ACHATS', 'Categorie'), ('DEMANDES_ACHATS', 'Condition_Demande'), ('DEMANDES_ACHATS', 'Date_Demande'), ('DEMANDES_ACHATS', 'Num_Offre'),
            ('FOURNISSEURS', 'Nom'), ('FOURNISSEURS', 'Coordonnee'), ('FOURNISSEURS', 'Conditions_Fournisseur'), ('FOURNISSEURS', 'Service'),
            ('OFFRE', 'Num_Offre'), ('OFFRE', 'Prix_Offre'), ('OFFRE', 'Condition_Offre'),
            ('UTILISATEURS', 'Num_Demande'),
            ('VALIDATION', 'Statut_Validation'), ('VALIDATION', 'Commantaire'), ('VALIDATION', 'Niveau')
        ) AS cols(table_name, column_name)
    LOOP
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = c.table_name
            AND column_name = c.column_name AND data_type = 'ARRAY'
        ) THEN
            EXECUTE format(
                'DO $check$ BEGIN IF EXISTS (SELECT 1 FROM public.%I WHERE cardinality(%I) > 1) THEN RAISE EXCEPTION ''Une colonne contient plusieurs valeurs ; migration interrompue pour préserver les données.''; END IF; END $check$',
                c.table_name, c.column_name, c.table_name, c.column_name
            );
        END IF;
    END LOOP;
END $$;

-- Conversion scalaire des colonnes tableau existantes, en conservant leur
-- première valeur après le contrôle de cardinalité ci-dessus.
DO $$
DECLARE
    c RECORD;
BEGIN
    FOR c IN
        SELECT * FROM (VALUES
            ('RECEPTION', 'Quantite_Recue', 'numeric(12,2)', 'numeric'),
            ('RECEPTION', 'Date_Reception', 'date', 'date'),
            ('BON_COMMANDE', 'Satut', 'varchar(20)', 'varchar'),
            ('BON_COMMANDE', 'Referance', 'varchar(50)', 'varchar'),
            ('BON_COMMANDE', 'Date', 'date', 'date'),
            ('CENTRE_COUT', 'Nom_Centre', 'varchar(100)', 'varchar'),
            ('CENTRE_COUT', 'Montant_Alloue', 'bigint', 'bigint'),
            ('CENTRE_COUT', 'Montant_Consomme', 'bigint', 'bigint'),
            ('CENTRE_COUT', 'Num_Demande', 'varchar(50)', 'varchar'),
            ('DEMANDES_ACHATS', 'Num_Demande', 'varchar(50)', 'varchar'),
            ('DEMANDES_ACHATS', 'Titre', 'varchar(200)', 'varchar'),
            ('DEMANDES_ACHATS', 'Satut_Demande', 'varchar(20)', 'varchar'),
            ('DEMANDES_ACHATS', 'Coordonnee', 'varchar(255)', 'varchar'),
            ('DEMANDES_ACHATS', 'Categorie', 'varchar(100)', 'varchar'),
            ('DEMANDES_ACHATS', 'Condition_Demande', 'text', 'text'),
            ('DEMANDES_ACHATS', 'Date_Demande', 'date', 'date'),
            ('DEMANDES_ACHATS', 'Num_Offre', 'integer', 'integer'),
            ('FOURNISSEURS', 'Nom', 'varchar(150)', 'varchar'),
            ('FOURNISSEURS', 'Coordonnee', 'varchar(50)', 'varchar'),
            ('FOURNISSEURS', 'Conditions_Fournisseur', 'text', 'text'),
            ('FOURNISSEURS', 'Service', 'varchar(50)', 'varchar'),
            ('OFFRE', 'Num_Offre', 'integer', 'integer'),
            ('OFFRE', 'Prix_Offre', 'numeric(15,2)', 'numeric'),
            ('OFFRE', 'Condition_Offre', 'text', 'text'),
            ('UTILISATEURS', 'Num_Demande', 'varchar(50)', 'varchar'),
            ('VALIDATION', 'Statut_Validation', 'varchar(50)', 'varchar'),
            ('VALIDATION', 'Commantaire', 'text', 'text'),
            ('VALIDATION', 'Niveau', 'varchar(10)', 'varchar')
        ) AS cols(table_name, column_name, target_type, base_type)
    LOOP
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = c.table_name
              AND column_name = c.column_name AND data_type = 'ARRAY'
        ) THEN
            EXECUTE format(
                'ALTER TABLE public.%I ALTER COLUMN %I TYPE %s USING (%I)[1]::%s',
                c.table_name, c.column_name, c.target_type, c.column_name, c.base_type
            );
        END IF;
    END LOOP;
END $$;

-- Colonnes nécessaires au workflow, ajoutées sans supprimer les informations
-- existantes dans les tables fournies.
ALTER TABLE public."UTILISATEURS"
    ADD COLUMN IF NOT EXISTS "Id_Utilisateur" integer GENERATED BY DEFAULT AS IDENTITY,
    ADD COLUMN IF NOT EXISTS "Manager_Matricule" varchar(7);
CREATE UNIQUE INDEX IF NOT EXISTS uq_utilisateurs_id_utilisateur ON public."UTILISATEURS" ("Id_Utilisateur");
CREATE UNIQUE INDEX IF NOT EXISTS uq_utilisateurs_email_ci ON public."UTILISATEURS" (lower("Email"));

ALTER TABLE public."DEMANDES_ACHATS"
    ADD COLUMN IF NOT EXISTS "Id_Demande" integer GENERATED BY DEFAULT AS IDENTITY,
    ADD COLUMN IF NOT EXISTS "Matricule_Demandeur" varchar(7),
    ADD COLUMN IF NOT EXISTS "Montant_Estime" numeric(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "Justification" text,
    ADD COLUMN IF NOT EXISTS "Date_Soumission" timestamp;
ALTER TABLE public."DEMANDES_ACHATS" ADD COLUMN IF NOT EXISTS "Id_Centre" integer;
CREATE UNIQUE INDEX IF NOT EXISTS uq_demandes_achats_id_demande ON public."DEMANDES_ACHATS" ("Id_Demande");

ALTER TABLE public."CENTRE_COUT"
    ADD COLUMN IF NOT EXISTS "Code_Centre" varchar(20),
    ADD COLUMN IF NOT EXISTS "Service" varchar(50);

ALTER TABLE public."VALIDATION"
    ADD COLUMN IF NOT EXISTS "Id_Demande" integer,
    ADD COLUMN IF NOT EXISTS "Id_Niveau" integer,
    ADD COLUMN IF NOT EXISTS "Matricule_Valideur" varchar(7),
    ADD COLUMN IF NOT EXISTS "Date_Decision" timestamp;
ALTER TABLE public."VALIDATION" ALTER COLUMN "Date_Validation" SET DEFAULT CURRENT_DATE;

ALTER TABLE public."OFFRE"
    ADD COLUMN IF NOT EXISTS "Id_Demande" integer,
    ADD COLUMN IF NOT EXISTS "Delai_Livraison_Jours" integer,
    ADD COLUMN IF NOT EXISTS "Statut_Offre" varchar(20) NOT NULL DEFAULT 'RECUE',
    ADD COLUMN IF NOT EXISTS "Date_Offre" date NOT NULL DEFAULT CURRENT_DATE;

ALTER TABLE public."BON_COMMANDE"
    ADD COLUMN IF NOT EXISTS "Id_Demande" integer,
    ADD COLUMN IF NOT EXISTS "Id_Offre" integer,
    ADD COLUMN IF NOT EXISTS "Ref_Fournisseur" varchar(20),
    ADD COLUMN IF NOT EXISTS "Matricule_Createur" varchar(7),
    ADD COLUMN IF NOT EXISTS "Montant_Total" numeric(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "Date_Livraison_Prevue" date;

ALTER TABLE public."RECEPTION"
    ADD COLUMN IF NOT EXISTS "Num_Commande" integer,
    ADD COLUMN IF NOT EXISTS "Matricule_Receptionnaire" varchar(7),
    ADD COLUMN IF NOT EXISTS "Observation" varchar(255);

CREATE TABLE IF NOT EXISTS public."LIGNE_DEMANDE" (
    "Id_Ligne" integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    "Id_Demande" integer NOT NULL REFERENCES public."DEMANDES_ACHATS" ("Id_Demande") ON DELETE CASCADE,
    "Categorie" varchar(100),
    "Type" varchar(20) NOT NULL DEFAULT 'ARTICLE' CHECK ("Type" IN ('ARTICLE','SERVICE')),
    "Designation" varchar(200) NOT NULL,
    "Quantite" numeric(12,2) NOT NULL CHECK ("Quantite" > 0),
    "Prix_Unitaire_Estime" numeric(15,2) NOT NULL DEFAULT 0 CHECK ("Prix_Unitaire_Estime" >= 0)
);

CREATE TABLE IF NOT EXISTS public."CATEGORIE" (
    "Id_Categorie" integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    "Nom" varchar(100) NOT NULL UNIQUE,
    "Description" varchar(255)
);
CREATE TABLE IF NOT EXISTS public."FOURNISSEUR_CATEGORIE" (
    "Id_Fournisseur" integer NOT NULL,
    "Id_Categorie" integer NOT NULL REFERENCES public."CATEGORIE" ("Id_Categorie") ON DELETE CASCADE,
    PRIMARY KEY ("Id_Fournisseur", "Id_Categorie")
);

ALTER TABLE public."FOURNISSEURS" ADD COLUMN IF NOT EXISTS "Id_Fournisseur" integer GENERATED BY DEFAULT AS IDENTITY;
CREATE UNIQUE INDEX IF NOT EXISTS uq_fournisseurs_id ON public."FOURNISSEURS" ("Id_Fournisseur");

CREATE TABLE IF NOT EXISTS public."HISTORIQUE_STATUT" (
    "Id_Historique" integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    "Id_Demande" integer NOT NULL REFERENCES public."DEMANDES_ACHATS" ("Id_Demande") ON DELETE CASCADE,
    "Ancien_Statut" varchar(20),
    "Nouveau_Statut" varchar(20) NOT NULL,
    "Matricule_Utilisateur" varchar(7),
    "Commentaire" varchar(255),
    "Date_Changement" timestamp NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public."CIRCUIT_VALIDATION" (
    "Id_Circuit" integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    "Nom" varchar(100) NOT NULL,
    "Service" varchar(50),
    "Actif" boolean NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS public."NIVEAU_VALIDATION" (
    "Id_Niveau" integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    "Id_Circuit" integer NOT NULL REFERENCES public."CIRCUIT_VALIDATION" ("Id_Circuit") ON DELETE CASCADE,
    "Ordre" integer NOT NULL CHECK ("Ordre" > 0),
    "Role" varchar(50) NOT NULL,
    "Seuil_Montant" numeric(15,2) NOT NULL DEFAULT 0,
    UNIQUE ("Id_Circuit", "Ordre")
);

INSERT INTO public."CIRCUIT_VALIDATION" ("Nom", "Service", "Actif")
SELECT 'Circuit standard', NULL, true
WHERE NOT EXISTS (
    SELECT 1 FROM public."CIRCUIT_VALIDATION" WHERE "Nom" = 'Circuit standard' AND "Service" IS NULL
);
INSERT INTO public."NIVEAU_VALIDATION" ("Id_Circuit", "Ordre", "Role", "Seuil_Montant")
SELECT c."Id_Circuit", steps.ordre, steps.role_code, 0
FROM public."CIRCUIT_VALIDATION" c
CROSS JOIN (VALUES (1, 'MANAGER'), (2, 'SERVICE_ACHATS')) AS steps(ordre, role_code)
WHERE c."Nom" = 'Circuit standard' AND c."Service" IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM public."NIVEAU_VALIDATION" n
      WHERE n."Id_Circuit" = c."Id_Circuit" AND n."Ordre" = steps.ordre
  );

CREATE TABLE IF NOT EXISTS public."BUDGET" (
    "Id_Budget" integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    "Id_Centre" integer NOT NULL REFERENCES public."CENTRE_COUT" ("Id_Centre"),
    "Annee" integer NOT NULL,
    "Montant_Alloue" numeric(15,2) NOT NULL CHECK ("Montant_Alloue" >= 0),
    "Montant_Engage" numeric(15,2) NOT NULL DEFAULT 0 CHECK ("Montant_Engage" >= 0),
    "Seuil_Alerte_Pct" numeric(5,2) NOT NULL DEFAULT 80 CHECK ("Seuil_Alerte_Pct" BETWEEN 0 AND 100),
    UNIQUE ("Id_Centre", "Annee")
);

CREATE TABLE IF NOT EXISTS public."ALERTE_BUDGET" (
    "Id_Alerte" integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    "Id_Budget" integer NOT NULL REFERENCES public."BUDGET" ("Id_Budget") ON DELETE CASCADE,
    "Type_Alerte" varchar(20) NOT NULL,
    "Message" varchar(255) NOT NULL,
    "Date_Alerte" timestamp NOT NULL DEFAULT now(),
    "Traitee" boolean NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS public."LIGNE_BON_COMMANDE" (
    "Id_Ligne_BC" integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    "Num_Commande" integer NOT NULL REFERENCES public."BON_COMMANDE" ("Num_Commande") ON DELETE CASCADE,
    "Id_Ligne_Demande" integer REFERENCES public."LIGNE_DEMANDE" ("Id_Ligne"),
    "Designation" varchar(200) NOT NULL,
    "Quantite" numeric(12,2) NOT NULL CHECK ("Quantite" > 0),
    "Prix_Unitaire" numeric(15,2) NOT NULL CHECK ("Prix_Unitaire" >= 0)
);

CREATE TABLE IF NOT EXISTS public."LIGNE_RECEPTION" (
    "Id_Ligne_Reception" integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    "Id_Recue" varchar(20) NOT NULL REFERENCES public."RECEPTION" ("Id_Recue") ON DELETE CASCADE,
    "Id_Ligne_BC" integer NOT NULL REFERENCES public."LIGNE_BON_COMMANDE" ("Id_Ligne_BC"),
    "Quantite_Recue" numeric(12,2) NOT NULL CHECK ("Quantite_Recue" > 0)
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_bc_reception') THEN
        ALTER TABLE public."BON_COMMANDE" ADD CONSTRAINT fk_bc_reception
            FOREIGN KEY ("Id_Recue") REFERENCES public."RECEPTION" ("Id_Recue") ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_centre_demande') THEN
        ALTER TABLE public."CENTRE_COUT" ADD CONSTRAINT fk_centre_demande
            FOREIGN KEY ("Num_Demande") REFERENCES public."DEMANDES_ACHATS" ("Num_Demande") ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_demande_commande') THEN
        ALTER TABLE public."DEMANDES_ACHATS" ADD CONSTRAINT fk_demande_commande
            FOREIGN KEY ("Num_Commande") REFERENCES public."BON_COMMANDE" ("Num_Commande") ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_demande_offre') THEN
        ALTER TABLE public."DEMANDES_ACHATS" ADD CONSTRAINT fk_demande_offre
            FOREIGN KEY ("Num_Offre") REFERENCES public."OFFRE" ("Num_Offre") ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_fournisseur_commande') THEN
        ALTER TABLE public."FOURNISSEURS" ADD CONSTRAINT fk_fournisseur_commande
            FOREIGN KEY ("Num_Commande") REFERENCES public."BON_COMMANDE" ("Num_Commande") ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_offre_fournisseur') THEN
        ALTER TABLE public."OFFRE" ADD CONSTRAINT fk_offre_fournisseur
            FOREIGN KEY ("Ref_Fournisseur") REFERENCES public."FOURNISSEURS" ("Ref_Fournisseur") ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_utilisateur_ancienne_demande') THEN
        ALTER TABLE public."UTILISATEURS" ADD CONSTRAINT fk_utilisateur_ancienne_demande
            FOREIGN KEY ("Num_Demande") REFERENCES public."DEMANDES_ACHATS" ("Num_Demande") ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_utilisateur_ancienne_validation') THEN
        ALTER TABLE public."UTILISATEURS" ADD CONSTRAINT fk_utilisateur_ancienne_validation
            FOREIGN KEY ("Num_Validation") REFERENCES public."VALIDATION" ("Num_Validation") ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_utilisateur_demandeur') THEN
        ALTER TABLE public."DEMANDES_ACHATS" ADD CONSTRAINT fk_utilisateur_demandeur
            FOREIGN KEY ("Matricule_Demandeur") REFERENCES public."UTILISATEURS" ("Matricule");
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_demande_centre') THEN
        ALTER TABLE public."DEMANDES_ACHATS" ADD CONSTRAINT fk_demande_centre
            FOREIGN KEY ("Id_Centre") REFERENCES public."CENTRE_COUT" ("Id_Centre");
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_validation_niveau') THEN
        ALTER TABLE public."VALIDATION" ADD CONSTRAINT fk_validation_niveau
            FOREIGN KEY ("Id_Niveau") REFERENCES public."NIVEAU_VALIDATION" ("Id_Niveau");
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_fournisseur_categorie_fournisseur') THEN
        ALTER TABLE public."FOURNISSEUR_CATEGORIE" ADD CONSTRAINT fk_fournisseur_categorie_fournisseur
            FOREIGN KEY ("Id_Fournisseur") REFERENCES public."FOURNISSEURS" ("Id_Fournisseur") ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_validation_demande') THEN
        ALTER TABLE public."VALIDATION" ADD CONSTRAINT fk_validation_demande
            FOREIGN KEY ("Id_Demande") REFERENCES public."DEMANDES_ACHATS" ("Id_Demande") ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_validation_valideur') THEN
        ALTER TABLE public."VALIDATION" ADD CONSTRAINT fk_validation_valideur
            FOREIGN KEY ("Matricule_Valideur") REFERENCES public."UTILISATEURS" ("Matricule");
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_offre_demande') THEN
        ALTER TABLE public."OFFRE" ADD CONSTRAINT fk_offre_demande
            FOREIGN KEY ("Id_Demande") REFERENCES public."DEMANDES_ACHATS" ("Id_Demande") ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_commande_demande') THEN
        ALTER TABLE public."BON_COMMANDE" ADD CONSTRAINT fk_commande_demande
            FOREIGN KEY ("Id_Demande") REFERENCES public."DEMANDES_ACHATS" ("Id_Demande");
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_commande_offre') THEN
        ALTER TABLE public."BON_COMMANDE" ADD CONSTRAINT fk_commande_offre
            FOREIGN KEY ("Id_Offre") REFERENCES public."OFFRE" ("Num_Offre");
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_commande_fournisseur') THEN
        ALTER TABLE public."BON_COMMANDE" ADD CONSTRAINT fk_commande_fournisseur
            FOREIGN KEY ("Ref_Fournisseur") REFERENCES public."FOURNISSEURS" ("Ref_Fournisseur");
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_commande_createur') THEN
        ALTER TABLE public."BON_COMMANDE" ADD CONSTRAINT fk_commande_createur
            FOREIGN KEY ("Matricule_Createur") REFERENCES public."UTILISATEURS" ("Matricule");
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reception_commande') THEN
        ALTER TABLE public."RECEPTION" ADD CONSTRAINT fk_reception_commande
            FOREIGN KEY ("Num_Commande") REFERENCES public."BON_COMMANDE" ("Num_Commande");
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_receptionnaire') THEN
        ALTER TABLE public."RECEPTION" ADD CONSTRAINT fk_receptionnaire
            FOREIGN KEY ("Matricule_Receptionnaire") REFERENCES public."UTILISATEURS" ("Matricule");
    END IF;
END $$;

DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT * FROM (VALUES
        ('BON_COMMANDE', 'Num_Commande'), ('OFFRE', 'Num_Offre'), ('VALIDATION', 'Num_Validation')
    ) AS cols(table_name, column_name)
    LOOP
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public'
                   AND table_name=c.table_name AND column_name=c.column_name AND is_identity='NO'
                   AND column_default IS NULL) THEN
            EXECUTE format('ALTER TABLE public.%I ALTER COLUMN %I ADD GENERATED BY DEFAULT AS IDENTITY', c.table_name, c.column_name);
        END IF;
    END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_validation_demande_niveau ON public."VALIDATION" ("Id_Demande", "Id_Niveau");

CREATE UNIQUE INDEX IF NOT EXISTS uq_offre_num_offre_scalar ON public."OFFRE" ("Num_Offre");
CREATE UNIQUE INDEX IF NOT EXISTS uq_offre_demande_fournisseur ON public."OFFRE" ("Id_Demande", "Ref_Fournisseur");
CREATE INDEX IF NOT EXISTS idx_demandes_statut ON public."DEMANDES_ACHATS" ("Satut_Demande");
CREATE INDEX IF NOT EXISTS idx_validation_demande ON public."VALIDATION" ("Id_Demande");
CREATE INDEX IF NOT EXISTS idx_historique_demande ON public."HISTORIQUE_STATUT" ("Id_Demande", "Date_Changement");
