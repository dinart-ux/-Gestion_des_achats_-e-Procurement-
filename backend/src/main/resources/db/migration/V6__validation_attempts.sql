-- Keep each approval round when a requester resubmits after clarification.
-- Existing validation rows belong to the first attempt.
ALTER TABLE public.validation
    ADD COLUMN IF NOT EXISTS tentative INTEGER NOT NULL DEFAULT 1;

DO $$
DECLARE
    c RECORD;
BEGIN
    -- Remove only the old unique constraint on (demande_id, niveau_id).
    -- The catalog lookup also handles databases where PostgreSQL generated a
    -- different constraint name.
    FOR c IN
        SELECT con.conname
        FROM pg_constraint con
        WHERE con.conrelid = 'public.validation'::regclass
            AND con.contype = 'u'
            AND (
                SELECT array_agg(att.attname::text ORDER BY att.attname::text)
                FROM unnest(con.conkey) AS key(attnum)
                JOIN pg_attribute att
                ON att.attrelid = con.conrelid
                AND att.attnum = key.attnum
            ) = ARRAY['demande_id', 'niveau_id']::text[]
    LOOP
        EXECUTE format('ALTER TABLE public.validation DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;

DO $$
DECLARE
    i RECORD;
BEGIN
    -- Also handle a manually-created standalone unique index on the old pair.
    -- Indexes backing other constraints are excluded and are never dropped here.
    FOR i IN
        SELECT ns.nspname AS schema_name, idx.relname AS index_name
        FROM pg_index ix
        JOIN pg_class idx ON idx.oid = ix.indexrelid
        JOIN pg_namespace ns ON ns.oid = idx.relnamespace
        WHERE ix.indrelid = 'public.validation'::regclass
          AND ix.indisunique
          AND NOT EXISTS (SELECT 1 FROM pg_constraint con WHERE con.conindid = ix.indexrelid)
          AND (
              SELECT array_agg(att.attname::text ORDER BY att.attname::text)
              FROM unnest(ix.indkey) WITH ORDINALITY AS key(attnum, position)
              JOIN pg_attribute att
                ON att.attrelid = ix.indrelid
               AND att.attnum = key.attnum
              WHERE key.position <= ix.indnkeyatts
          ) = ARRAY['demande_id', 'niveau_id']::text[]
    LOOP
        EXECUTE format('DROP INDEX %I.%I', i.schema_name, i.index_name);
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint con
        WHERE con.conrelid = 'public.validation'::regclass
          AND con.contype = 'u'
          AND (
              SELECT array_agg(att.attname::text ORDER BY att.attname::text)
              FROM unnest(con.conkey) AS key(attnum)
              JOIN pg_attribute att
                ON att.attrelid = con.conrelid
               AND att.attnum = key.attnum
          ) = ARRAY['demande_id', 'niveau_id', 'tentative']::text[]
    ) THEN
        ALTER TABLE public.validation
            ADD CONSTRAINT uq_validation_demande_niveau_tentative
            UNIQUE (demande_id, niveau_id, tentative);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.validation'::regclass
          AND conname = 'ck_validation_tentative_positive'
    ) THEN
        ALTER TABLE public.validation
            ADD CONSTRAINT ck_validation_tentative_positive CHECK (tentative > 0);
    END IF;
END $$;
