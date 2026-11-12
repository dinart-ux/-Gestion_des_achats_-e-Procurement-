-- Autorise les brouillons incomplets ; le passage à SOUMISE reste protégé en base.
ALTER TABLE public.demande_achat ALTER COLUMN centre_cout_id DROP NOT NULL;
ALTER TABLE public.demande_achat ALTER COLUMN objet DROP NOT NULL;
ALTER TABLE public.demande_achat ADD COLUMN revision BIGINT NOT NULL DEFAULT 0 CHECK (revision >= 0);
ALTER TABLE public.demande_achat ADD COLUMN cle_brouillon VARCHAR(36);
CREATE UNIQUE INDEX uq_demande_achat_cle_brouillon
    ON public.demande_achat (demandeur_id, cle_brouillon) WHERE cle_brouillon IS NOT NULL;

ALTER TABLE public.ligne_demande ALTER COLUMN designation DROP NOT NULL;
ALTER TABLE public.ligne_demande ALTER COLUMN quantite DROP NOT NULL;
ALTER TABLE public.ligne_demande ALTER COLUMN prix_unitaire_estime DROP NOT NULL;
ALTER TABLE public.ligne_demande DROP CONSTRAINT ligne_demande_quantite_check;

CREATE OR REPLACE FUNCTION public.verifier_demande_complete_avant_soumission()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.statut <> 'BROUILLON'::public.statut_demande AND
       (NEW.objet IS NULL OR btrim(NEW.objet) = '' OR NEW.centre_cout_id IS NULL OR
        NOT EXISTS (SELECT 1 FROM public.ligne_demande l WHERE l.demande_id = NEW.id) OR
        EXISTS (SELECT 1 FROM public.ligne_demande l WHERE l.demande_id = NEW.id AND
            (l.designation IS NULL OR btrim(l.designation) = '' OR l.quantite IS NULL OR
             l.quantite <= 0 OR l.prix_unitaire_estime IS NULL OR l.prix_unitaire_estime < 0))) THEN
        RAISE EXCEPTION 'La demande doit être complète avant sa soumission.' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_demande_complete_avant_soumission
BEFORE UPDATE OF statut ON public.demande_achat
FOR EACH ROW EXECUTE FUNCTION public.verifier_demande_complete_avant_soumission();

CREATE OR REPLACE FUNCTION public.verifier_ligne_brouillon_incomplet()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent_statut public.statut_demande;
BEGIN
    IF TG_OP = 'DELETE' THEN
        SELECT statut INTO parent_statut FROM public.demande_achat WHERE id = OLD.demande_id FOR UPDATE;
        IF parent_statut <> 'BROUILLON'::public.statut_demande THEN
            RAISE EXCEPTION 'Les lignes d''une demande soumise ne peuvent pas être supprimées.' USING ERRCODE = '23514';
        END IF;
        RETURN OLD;
    END IF;
    SELECT statut INTO parent_statut FROM public.demande_achat WHERE id = NEW.demande_id FOR UPDATE;
    IF parent_statut IS NULL THEN
        RAISE EXCEPTION 'La demande associée est introuvable.' USING ERRCODE = '23503';
    END IF;
    IF parent_statut <> 'BROUILLON'::public.statut_demande AND
       (NEW.designation IS NULL OR btrim(NEW.designation) = '' OR NEW.quantite IS NULL OR NEW.quantite <= 0 OR
        NEW.prix_unitaire_estime IS NULL OR NEW.prix_unitaire_estime < 0) THEN
        RAISE EXCEPTION 'Une ligne incomplète est autorisée uniquement en brouillon.' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_ligne_incomplete_uniquement_brouillon
BEFORE INSERT OR UPDATE OR DELETE ON public.ligne_demande
FOR EACH ROW EXECUTE FUNCTION public.verifier_ligne_brouillon_incomplet();
