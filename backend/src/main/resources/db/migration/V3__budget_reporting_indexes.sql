ALTER TABLE budget
    ADD CONSTRAINT ck_budget_seuil_alerte_pct CHECK (seuil_alerte_pct BETWEEN 0 AND 100);

CREATE INDEX idx_budget_annee ON budget(annee);
CREATE INDEX idx_offre_demande_statut ON offre(demande_id, statut);
CREATE INDEX idx_reception_commande ON reception(bon_commande_id, date_reception);
CREATE INDEX idx_bc_date_emission ON bon_commande(date_emission);
