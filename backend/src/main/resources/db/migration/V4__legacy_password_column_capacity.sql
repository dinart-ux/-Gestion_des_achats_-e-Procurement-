-- Keep the legacy UTILISATEURS table usable with BCrypt hashes (60 characters).
-- Existing password values are preserved; the application upgrades legacy values
-- to BCrypt after a successful login.
DO $$
BEGIN
    IF to_regclass('public."UTILISATEURS"') IS NOT NULL THEN
        ALTER TABLE public."UTILISATEURS"
            ALTER COLUMN "Mot_de_Passe" TYPE VARCHAR(255);
    END IF;
END $$;
