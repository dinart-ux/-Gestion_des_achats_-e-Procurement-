-- Jeu de démonstration pour la base locale uniquement.
-- Exécution recommandée : psql ... -v ON_ERROR_STOP=1 -c "SET app.seed_environment='development'" -f development-demo.sql
-- Ce script n'efface ni ne met à jour les comptes/données existants.
BEGIN;

DO $$
BEGIN
    IF current_database() <> 'e-procurement'
       OR current_setting('app.seed_environment', true) IS DISTINCT FROM 'development' THEN
        RAISE EXCEPTION 'Seed refusé : sélectionner la base e-procurement et app.seed_environment=development.';
    END IF;
    IF EXISTS (
        SELECT 1 FROM utilisateurs
        WHERE (matricule IN ('2026007','2026008','2026009','2026010','2026011','2026012','2026013','2026014')
               AND email NOT IN ('demo.admin@example.test','demo.demandeur1@example.test','demo.demandeur2@example.test',
                                 'demo.demandeur3@example.test','demo.manager1@example.test','demo.manager2@example.test',
                                 'demo.achats1@example.test','demo.achats2@example.test'))
           OR (email IN ('demo.admin@example.test','demo.demandeur1@example.test','demo.demandeur2@example.test',
                         'demo.demandeur3@example.test','demo.manager1@example.test','demo.manager2@example.test',
                         'demo.achats1@example.test','demo.achats2@example.test')
               AND matricule NOT IN ('2026007','2026008','2026009','2026010','2026011','2026012','2026013','2026014'))
    ) THEN
        RAISE EXCEPTION 'Un matricule ou e-mail de démonstration appartient déjà à un autre compte.';
    END IF;
END $$;

INSERT INTO service(code, nom) VALUES
 ('DEMO-INFO','Informatique - démonstration'),
 ('DEMO-COMPTA','Comptabilité - démonstration'),
 ('DEMO-RH','Ressources humaines - démonstration'),
 ('DEMO-LOG','Logistique - démonstration'),
 ('DEMO-ADMIN','Administration - démonstration')
ON CONFLICT (code) DO NOTHING;

INSERT INTO categorie(nom, description) VALUES
 ('DEMO - Fournitures de bureau','Fournitures et consommables de démonstration'),
 ('DEMO - Formation','Formations professionnelles de démonstration'),
 ('DEMO - Transport','Transport et logistique de démonstration'),
 ('DEMO - Prestations de services','Prestations fictives de démonstration')
ON CONFLICT (nom) DO NOTHING;

INSERT INTO centre_cout(code, libelle, service_id)
SELECT v.code, v.libelle, s.id
FROM (VALUES
 ('DEMO-CC-INFO','Informatique - investissements', 'DEMO-INFO'),
 ('DEMO-CC-ALERTE','Informatique - budget proche du seuil', 'DEMO-INFO'),
 ('DEMO-CC-COMPTA','Comptabilité - fonctionnement', 'DEMO-COMPTA'),
 ('DEMO-CC-RH','Ressources humaines - formation', 'DEMO-RH'),
 ('DEMO-CC-LOG','Logistique - exploitation', 'DEMO-LOG'),
 ('DEMO-CC-ADMIN','Administration générale', 'DEMO-ADMIN')
) AS v(code,libelle,service_code)
JOIN service s ON s.code=v.service_code
ON CONFLICT (code) DO NOTHING;

-- Un seul hash BCrypt généré par BCryptPasswordEncoder (Spring Security).
-- Les matricules 2026001 à 2026006 existent déjà et sont laissés intacts.
INSERT INTO utilisateurs(matricule,nom,prenom,email,mot_de_passe,telephone,role_id,service_id,manager_id)
SELECT v.matricule,v.nom,v.prenom,v.email,
       '$2a$10$esA6YJvghaoeizXrf6aSJuzH1GrIxi12ZO5QSE5h0rozrUfHaJdAq',
       v.telephone,r.id,s.id,NULL
FROM (VALUES
 ('2026007','Martin','Alexandre','demo.admin@example.test','+33 1 55 00 26 01','ADMIN','DEMO-ADMIN'),
 ('2026008','Bernard','Camille','demo.demandeur1@example.test','+33 1 55 00 26 02','DEMANDEUR','DEMO-INFO'),
 ('2026009','Petit','Lina','demo.demandeur2@example.test','+33 1 55 00 26 03','DEMANDEUR','DEMO-COMPTA'),
 ('2026010','Moreau','Yanis','demo.demandeur3@example.test','+33 1 55 00 26 04','DEMANDEUR','DEMO-RH'),
 ('2026011','Garcia','Nora','demo.manager1@example.test','+33 1 55 00 26 05','MANAGER','DEMO-INFO'),
 ('2026012','Roux','Hugo','demo.manager2@example.test','+33 1 55 00 26 06','MANAGER','DEMO-COMPTA'),
 ('2026013','Fournier','Sarah','demo.achats1@example.test','+33 1 55 00 26 07','SERVICE_ACHATS','DEMO-ADMIN'),
 ('2026014','Girard','Adam','demo.achats2@example.test','+33 1 55 00 26 08','SERVICE_ACHATS','DEMO-ADMIN')
) AS v(matricule,nom,prenom,email,telephone,role_code,service_code)
JOIN role r ON r.code=v.role_code
JOIN service s ON s.code=v.service_code
WHERE NOT EXISTS (SELECT 1 FROM utilisateurs u WHERE u.matricule=v.matricule OR lower(u.email)=lower(v.email));

-- Responsables d'équipe, uniquement pour les nouveaux comptes de démonstration.
UPDATE utilisateurs demandeur SET manager_id=manager.id
FROM utilisateurs manager
WHERE (demandeur.matricule,manager.matricule) IN (('2026008','2026011'),('2026009','2026012'),('2026010','2026011'))
  AND demandeur.email IN ('demo.demandeur1@example.test','demo.demandeur2@example.test','demo.demandeur3@example.test')
  AND demandeur.manager_id IS DISTINCT FROM manager.id;

INSERT INTO fournisseur(raison_sociale,nif,stat,adresse,telephone,email,contact_nom,conditions_paiement,delai_livraison_jours)
SELECT v.* FROM (VALUES
 ('DEMO Numérique SAS','NIF-DEMO-26001','STAT-DEMO-26001','12 rue des Ateliers, 75011 Paris','+33 1 44 00 10 01','contact@demo-numerique.example.test','Élodie Caron','30 jours fin de mois',12),
 ('DEMO Bureautique SARL','NIF-DEMO-26002','STAT-DEMO-26002','8 avenue du Parc, 69003 Lyon','+33 4 72 00 10 02','ventes@demo-bureautique.example.test','Mehdi Laurent','30 jours',5),
 ('DEMO Formation Conseil','NIF-DEMO-26003','STAT-DEMO-26003','21 rue des Écoles, 31000 Toulouse','+33 5 61 00 10 03','bonjour@demo-formation.example.test','Julie Robert','45 jours',20),
 ('DEMO Mobilité Pro','NIF-DEMO-26004','STAT-DEMO-26004','4 quai de la Gare, 44000 Nantes','+33 2 40 00 10 04','pro@demo-mobilite.example.test','Karim Lopez','30 jours',3),
 ('DEMO Services & Maintenance','NIF-DEMO-26005','STAT-DEMO-26005','16 boulevard Central, 59000 Lille','+33 3 20 00 10 05','achats@demo-services.example.test','Inès Fontaine','30 jours',10)
) AS v(raison_sociale,nif,stat,adresse,telephone,email,contact_nom,conditions_paiement,delai_livraison_jours)
WHERE NOT EXISTS (SELECT 1 FROM fournisseur f WHERE f.raison_sociale=v.raison_sociale);

INSERT INTO fournisseur_categorie(fournisseur_id,categorie_id)
SELECT f.id,c.id FROM (VALUES
 ('DEMO Numérique SAS','Materiel informatique'),('DEMO Numérique SAS','Maintenance informatique'),
 ('DEMO Bureautique SARL','DEMO - Fournitures de bureau'),('DEMO Bureautique SARL','Mobilier de bureau'),
 ('DEMO Formation Conseil','DEMO - Formation'),('DEMO Mobilité Pro','DEMO - Transport'),
 ('DEMO Services & Maintenance','DEMO - Prestations de services'),('DEMO Services & Maintenance','Maintenance informatique')
) v(fournisseur,categorie)
JOIN fournisseur f ON f.raison_sociale=v.fournisseur
JOIN categorie c ON c.nom=v.categorie
ON CONFLICT DO NOTHING;

INSERT INTO circuit_validation(nom,service_id,actif)
SELECT 'DEMO - Circuit standard',NULL,TRUE
WHERE NOT EXISTS (SELECT 1 FROM circuit_validation WHERE nom='DEMO - Circuit standard' AND service_id IS NULL);

INSERT INTO niveau_validation(circuit_id,ordre,role_id,seuil_montant)
SELECT c.id,v.ordre,r.id,v.seuil
FROM circuit_validation c
CROSS JOIN (VALUES (1,'MANAGER',0::numeric),(2,'SERVICE_ACHATS',5000::numeric)) v(ordre,role_code,seuil)
JOIN role r ON r.code=v.role_code
WHERE c.nom='DEMO - Circuit standard' AND c.service_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM niveau_validation n WHERE n.circuit_id=c.id AND n.ordre=v.ordre);

INSERT INTO budget(centre_cout_id,annee,montant_alloue,montant_engage,seuil_alerte_pct)
SELECT c.id,2026,v.alloue,0,v.seuil
FROM (VALUES
 ('DEMO-CC-INFO',120000::numeric,80::numeric),('DEMO-CC-COMPTA',45000,75),
 ('DEMO-CC-ALERTE',4000,80),
 ('DEMO-CC-RH',60000,80),('DEMO-CC-LOG',90000,85),('DEMO-CC-ADMIN',30000,80)
) v(code,alloue,seuil)
JOIN centre_cout c ON c.code=v.code
ON CONFLICT (centre_cout_id,annee) DO NOTHING;

-- Demandes : les références DEMO- rendent chaque insertion répétable.
INSERT INTO demande_achat(reference,demandeur_id,centre_cout_id,objet,justification,montant_estime,statut,date_creation,date_soumission)
SELECT v.reference,u.id,c.id,v.objet,v.justification,v.montant,v.statut,v.created,v.submitted
FROM (VALUES
 ('DEMO-DA-2026-001','2026008','DEMO-CC-INFO','Renouvellement de postes développeurs','Remplacement de postes arrivés en fin de garantie.',8400::numeric,'BROUILLON'::statut_demande,'2026-10-01'::timestamp,NULL::timestamp),
 ('DEMO-DA-2026-002','2026008','DEMO-CC-INFO','Écrans pour l’équipe support','Équipement de deux postes de support.',2400,'SOUMISE','2026-10-03',CURRENT_TIMESTAMP),
 ('DEMO-DA-2026-003','2026009','DEMO-CC-COMPTA','Mobilier ergonomique','Amélioration de trois postes comptables.',3600,'VALIDEE','2026-09-20', '2026-09-21'),
 ('DEMO-DA-2026-004','2026010','DEMO-CC-RH','Formation gestion des achats','Formation de l’équipe RH aux procédures internes.',4800,'REJETEE','2026-09-12','2026-09-13'),
 ('DEMO-DA-2026-005','2026008','DEMO-CC-INFO','Maintenance réseau annuelle','Maintenance préventive des équipements réseau.',7200,'COMMANDEE','2026-08-15','2026-08-16'),
 ('DEMO-DA-2026-006','2026009','DEMO-CC-COMPTA','Fournitures du quatrième trimestre','Réassort de papier et consommables.',950,'RECEPTIONNEE','2026-08-18','2026-08-19'),
 ('DEMO-DA-2026-007','2026008','DEMO-CC-INFO','Livraison partielle de stations d’accueil','Déploiement progressif des nouveaux postes.',3200,'COMMANDEE','2026-09-01','2026-09-02'),
 ('DEMO-DA-2026-008','2026008','DEMO-CC-ALERTE','Équipement réseau pour le laboratoire','Remplacement urgent du matériel réseau du laboratoire.',5000,'VALIDEE','2026-10-06','2026-10-07')
) v(reference,matricule,centre,objet,justification,montant,statut,created,submitted)
JOIN utilisateurs u ON u.matricule=v.matricule
JOIN centre_cout c ON c.code=v.centre
WHERE NOT EXISTS (SELECT 1 FROM demande_achat d WHERE d.reference=v.reference);

INSERT INTO ligne_demande(demande_id,categorie_id,type,designation,quantite,prix_unitaire_estime)
SELECT d.id,c.id,v.type::type_article,v.designation,v.quantite,v.prix
FROM (VALUES
 ('DEMO-DA-2026-001','Materiel informatique','ARTICLE','Ordinateur portable professionnel',4::numeric,1800::numeric),('DEMO-DA-2026-001','Materiel informatique','ARTICLE','Station d’accueil USB-C',4,300),
 ('DEMO-DA-2026-002','Materiel informatique','ARTICLE','Écran 27 pouces',4,600),
 ('DEMO-DA-2026-003','Mobilier de bureau','ARTICLE','Fauteuil ergonomique',3,1200),
 ('DEMO-DA-2026-004','DEMO - Formation','SERVICE','Journée de formation achats',2,2400),
 ('DEMO-DA-2026-005','Maintenance informatique','SERVICE','Forfait annuel de maintenance réseau',1,7200),
 ('DEMO-DA-2026-006','DEMO - Fournitures de bureau','ARTICLE','Carton de papier A4',10,35),('DEMO-DA-2026-006','DEMO - Fournitures de bureau','ARTICLE','Pack de cartouches',2,300),
 ('DEMO-DA-2026-007','Materiel informatique','ARTICLE','Station d’accueil USB-C',8,400),
 ('DEMO-DA-2026-008','Materiel informatique','ARTICLE','Commutateur réseau administrable',1,5000)
) v(reference,categorie,type,designation,quantite,prix)
JOIN demande_achat d ON d.reference=v.reference
JOIN categorie c ON c.nom=v.categorie
WHERE NOT EXISTS (SELECT 1 FROM ligne_demande l WHERE l.demande_id=d.id AND l.designation=v.designation);

-- Historique explicite, cohérent avec le statut courant de chaque demande.
INSERT INTO historique_statut(demande_id,ancien_statut,nouveau_statut,utilisateur_id,commentaire,date_changement)
SELECT d.id,v.ancien::statut_demande,v.nouveau::statut_demande,u.id,v.commentaire,v.date_changement
FROM (VALUES
 ('DEMO-DA-2026-001',NULL::text,'BROUILLON','2026008','Création du brouillon de démonstration','2026-10-01'::timestamp),
 ('DEMO-DA-2026-002','BROUILLON','SOUMISE','2026008','Demande soumise pour validation','2026-10-03'),
 ('DEMO-DA-2026-003','BROUILLON','SOUMISE','2026009','Demande soumise pour validation','2026-09-21'),('DEMO-DA-2026-003','SOUMISE','VALIDEE','2026012','Demande validée par les deux niveaux','2026-09-23'),
 ('DEMO-DA-2026-004','BROUILLON','SOUMISE','2026010','Demande soumise pour validation','2026-09-13'),('DEMO-DA-2026-004','SOUMISE','REJETEE','2026011','Budget de formation non disponible sur cette période','2026-09-14'),
 ('DEMO-DA-2026-005','BROUILLON','SOUMISE','2026008','Demande soumise pour validation','2026-08-16'),('DEMO-DA-2026-005','SOUMISE','VALIDEE','2026011','Validation manager','2026-08-17'),('DEMO-DA-2026-005','VALIDEE','COMMANDEE','2026013','Commande de démonstration créée','2026-08-20'),
 ('DEMO-DA-2026-006','BROUILLON','SOUMISE','2026009','Demande soumise pour validation','2026-08-19'),('DEMO-DA-2026-006','SOUMISE','VALIDEE','2026012','Demande validée','2026-08-20'),('DEMO-DA-2026-006','VALIDEE','COMMANDEE','2026013','Commande de démonstration créée','2026-08-22'),('DEMO-DA-2026-006','COMMANDEE','RECEPTIONNEE','2026009','Réception complète enregistrée','2026-08-25'),
 ('DEMO-DA-2026-007','BROUILLON','SOUMISE','2026008','Demande soumise pour validation','2026-09-02'),('DEMO-DA-2026-007','SOUMISE','VALIDEE','2026011','Demande validée','2026-09-04'),('DEMO-DA-2026-007','VALIDEE','COMMANDEE','2026013','Commande créée, livraison en plusieurs fois','2026-09-06'),
 ('DEMO-DA-2026-008','BROUILLON','SOUMISE','2026008','Demande soumise pour validation','2026-10-07'),('DEMO-DA-2026-008','SOUMISE','VALIDEE','2026011','Besoin urgent validé, budget dédié dépassé','2026-10-08')
) v(reference,ancien,nouveau,matricule,commentaire,date_changement)
JOIN demande_achat d ON d.reference=v.reference
JOIN utilisateurs u ON u.matricule=v.matricule
WHERE NOT EXISTS (SELECT 1 FROM historique_statut h WHERE h.demande_id=d.id AND h.nouveau_statut=v.nouveau::statut_demande);

-- Les validations sont alignées sur les deux niveaux configurés.
INSERT INTO validation(demande_id,niveau_id,valideur_id,decision,commentaire,date_decision)
SELECT d.id,n.id,u.id,v.decision::decision_validation,v.commentaire,v.date_decision
FROM (VALUES
 ('DEMO-DA-2026-002',1,'2026011','EN_ATTENTE',NULL::text,NULL::timestamp),('DEMO-DA-2026-002',2,'2026013','EN_ATTENTE',NULL,NULL),
 ('DEMO-DA-2026-003',1,'2026012','VALIDEE','Conforme au besoin du service.','2026-09-22'),('DEMO-DA-2026-003',2,'2026013','VALIDEE','Offres comparées, budget compatible.','2026-09-23'),
 ('DEMO-DA-2026-004',1,'2026011','REJETEE','Budget de formation indisponible.','2026-09-14'),('DEMO-DA-2026-004',2,'2026013','EN_ATTENTE',NULL,NULL),
 ('DEMO-DA-2026-005',1,'2026011','VALIDEE','Intervention nécessaire pour la continuité de service.','2026-08-17'),('DEMO-DA-2026-005',2,'2026013','VALIDEE','Offres comparées et budget disponible.','2026-08-18'),
 ('DEMO-DA-2026-006',1,'2026012','VALIDEE','Accord du responsable comptable.','2026-08-20'),('DEMO-DA-2026-006',2,'2026013','VALIDEE','Achat conforme au budget.','2026-08-21'),
 ('DEMO-DA-2026-007',1,'2026011','VALIDEE','Déploiement autorisé.','2026-09-03'),('DEMO-DA-2026-007',2,'2026013','VALIDEE','Offre retenue.','2026-09-04'),
 ('DEMO-DA-2026-008',1,'2026011','VALIDEE','Remplacement nécessaire.','2026-10-08'),('DEMO-DA-2026-008',2,'2026013','VALIDEE','Achat autorisé ; seuil budgétaire dépassé.','2026-10-08')
) v(reference,ordre,matricule,decision,commentaire,date_decision)
JOIN demande_achat d ON d.reference=v.reference
JOIN circuit_validation cv ON cv.nom='DEMO - Circuit standard' AND cv.service_id IS NULL
JOIN niveau_validation n ON n.circuit_id=cv.id AND n.ordre=v.ordre
JOIN utilisateurs u ON u.matricule=v.matricule
WHERE NOT EXISTS (SELECT 1 FROM validation x WHERE x.demande_id=d.id AND x.niveau_id=n.id);

INSERT INTO offre(demande_id,fournisseur_id,montant_total,delai_livraison_jours,conditions,statut,date_offre)
SELECT d.id,f.id,v.montant,v.delai,v.conditions,v.statut::statut_offre,v.date_offre
FROM (VALUES
 ('DEMO-DA-2026-003','DEMO Bureautique SARL',3600::numeric,7,'Livraison incluse','RETENUE','2026-09-22'::date),
 ('DEMO-DA-2026-003','DEMO Numérique SAS',3825,10,'Garantie 3 ans','REFUSEE','2026-09-22'),
 ('DEMO-DA-2026-005','DEMO Services & Maintenance',7200,10,'Intervention sous 10 jours','RETENUE','2026-08-18'),
 ('DEMO-DA-2026-005','DEMO Numérique SAS',7480,12,'Astreinte incluse','REFUSEE','2026-08-18'),
 ('DEMO-DA-2026-006','DEMO Bureautique SARL',950,5,'Livraison offerte','RETENUE','2026-08-21'),
 ('DEMO-DA-2026-007','DEMO Numérique SAS',3200,14,'Livraison en deux lots','RETENUE','2026-09-04')
) v(reference,fournisseur,montant,delai,conditions,statut,date_offre)
JOIN demande_achat d ON d.reference=v.reference
JOIN fournisseur f ON f.raison_sociale=v.fournisseur
WHERE NOT EXISTS (SELECT 1 FROM offre o WHERE o.demande_id=d.id AND o.fournisseur_id=f.id);

INSERT INTO bon_commande(numero,demande_id,offre_id,fournisseur_id,cree_par_id,montant_total,statut,date_emission,date_livraison_prevue)
SELECT v.numero,d.id,o.id,o.fournisseur_id,u.id,o.montant_total,v.statut::statut_bc,v.emission,v.livraison
FROM (VALUES
 ('DEMO-BC-2026-005','DEMO-DA-2026-005','2026013','GENERE','2026-08-20'::timestamp,'2026-09-15'::date),
 ('DEMO-BC-2026-006','DEMO-DA-2026-006','2026013','RECU','2026-08-22','2026-08-25'),
 ('DEMO-BC-2026-007','DEMO-DA-2026-007','2026013','PARTIELLEMENT_RECU','2026-09-06','2026-10-30')
) v(numero,reference,matricule,statut,emission,livraison)
JOIN demande_achat d ON d.reference=v.reference
JOIN utilisateurs u ON u.matricule=v.matricule
JOIN offre o ON o.demande_id=d.id AND o.statut='RETENUE'
WHERE NOT EXISTS (SELECT 1 FROM bon_commande b WHERE b.demande_id=d.id OR b.numero=v.numero);

INSERT INTO ligne_bon_commande(bon_commande_id,ligne_demande_id,designation,quantite,prix_unitaire)
SELECT b.id,l.id,l.designation,l.quantite,l.prix_unitaire_estime
FROM bon_commande b JOIN demande_achat d ON d.id=b.demande_id
JOIN ligne_demande l ON l.demande_id=d.id
WHERE b.numero LIKE 'DEMO-BC-%'
  AND NOT EXISTS (SELECT 1 FROM ligne_bon_commande x WHERE x.bon_commande_id=b.id AND x.ligne_demande_id=l.id);

INSERT INTO reception(bon_commande_id,receptionnaire_id,date_reception,type_reception,observation)
SELECT b.id,u.id,v.date_reception,v.type_reception,v.observation
FROM (VALUES
 ('DEMO-BC-2026-006','2026009','2026-08-25'::timestamp,'TOTALE','Livraison complète contrôlée.'),
 ('DEMO-BC-2026-007','2026008','2026-09-20','PARTIELLE','Premier lot reçu, solde attendu.')
) v(numero,matricule,date_reception,type_reception,observation)
JOIN bon_commande b ON b.numero=v.numero
JOIN utilisateurs u ON u.matricule=v.matricule
WHERE NOT EXISTS (SELECT 1 FROM reception r WHERE r.bon_commande_id=b.id AND r.observation=v.observation);

INSERT INTO ligne_reception(reception_id,ligne_bc_id,quantite_recue)
SELECT r.id,l.id,CASE WHEN b.numero='DEMO-BC-2026-007' THEN 4 ELSE l.quantite END
FROM reception r JOIN bon_commande b ON b.id=r.bon_commande_id
JOIN ligne_bon_commande l ON l.bon_commande_id=b.id
WHERE b.numero IN ('DEMO-BC-2026-006','DEMO-BC-2026-007')
  AND NOT EXISTS (SELECT 1 FROM ligne_reception lr WHERE lr.reception_id=r.id AND lr.ligne_bc_id=l.id);

-- Engagement agrégé des demandes approuvées ou commandées des centres DEMO.
UPDATE budget b SET montant_engage=x.engage
FROM (
 SELECT c.id centre_cout_id,COALESCE(sum(d.montant_estime),0) engage
 FROM centre_cout c LEFT JOIN demande_achat d ON d.centre_cout_id=c.id
   AND d.reference LIKE 'DEMO-DA-%' AND d.statut IN ('VALIDEE','COMMANDEE','RECEPTIONNEE')
 WHERE c.code LIKE 'DEMO-CC-%' GROUP BY c.id
) x
WHERE b.centre_cout_id=x.centre_cout_id AND b.annee=2026 AND b.montant_engage IS DISTINCT FROM x.engage;

-- L'enveloppe dédiée de 4 000 est dépassée par la demande validée de 5 000.
INSERT INTO alerte_budget(budget_id,type_alerte,message)
SELECT b.id,'SEUIL','Données de démonstration : le seuil budgétaire est atteint ou dépassé.'
FROM budget b JOIN centre_cout c ON c.id=b.centre_cout_id
WHERE c.code='DEMO-CC-ALERTE' AND b.annee=2026
  AND b.montant_alloue>0
  AND b.montant_engage*100/b.montant_alloue>=b.seuil_alerte_pct
  AND NOT EXISTS (SELECT 1 FROM alerte_budget a WHERE a.budget_id=b.id AND a.traitee=false);

COMMIT;
