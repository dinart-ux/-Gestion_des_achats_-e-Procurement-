-- Circuit initial à deux étapes : manager puis service achats.
INSERT INTO circuit_validation (nom, service_id, actif)
SELECT 'Circuit standard', NULL, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM circuit_validation WHERE nom = 'Circuit standard' AND service_id IS NULL
);

INSERT INTO niveau_validation (circuit_id, ordre, role_id, seuil_montant)
SELECT c.id, steps.ordre, r.id, 0
FROM circuit_validation c
CROSS JOIN (VALUES (1, 'MANAGER'), (2, 'SERVICE_ACHATS')) AS steps(ordre, role_code)
JOIN role r ON r.code = steps.role_code
WHERE c.nom = 'Circuit standard' AND c.service_id IS NULL
    AND NOT EXISTS (
        SELECT 1 FROM niveau_validation n WHERE n.circuit_id = c.id AND n.ordre = steps.ordre
    );

ALTER TABLE bon_commande
    ADD CONSTRAINT uq_bon_commande_demande UNIQUE (demande_id);
