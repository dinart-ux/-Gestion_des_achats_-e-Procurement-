package com.example.backend.Repository;

import com.example.backend.Persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;

// Ces dépôts suivent le schéma e_procurement de référence.
interface SchemaRoleRepository extends JpaRepository<RoleEntity, Integer> {}
interface SchemaServiceRepository extends JpaRepository<ServiceEntity, Integer> {}
interface SchemaCentreCoutRepository extends JpaRepository<CentreCoutEntity, Integer> {}
interface SchemaBudgetRepository extends JpaRepository<BudgetEntity, Integer> {}
interface SchemaAlerteBudgetRepository extends JpaRepository<AlerteBudgetEntity, Integer> {}
interface SchemaDemandeAchatRepository extends JpaRepository<DemandeAchatEntity, Integer> {}
interface SchemaLigneDemandeRepository extends JpaRepository<LigneDemandeEntity, Integer> {}
interface SchemaHistoriqueStatutRepository extends JpaRepository<HistoriqueStatutEntity, Integer> {}
interface SchemaCircuitValidationRepository extends JpaRepository<CircuitValidationEntity, Integer> {}
interface SchemaNiveauValidationRepository extends JpaRepository<NiveauValidationEntity, Integer> {}
interface SchemaValidationRepository extends JpaRepository<ValidationEntity, Integer> {}
interface SchemaOffreRepository extends JpaRepository<OffreEntity, Integer> {}
interface SchemaBonCommandeRepository extends JpaRepository<BonCommandeEntity, Integer> {}
interface SchemaLigneBonCommandeRepository extends JpaRepository<LigneBonCommandeEntity, Integer> {}
interface SchemaReceptionRepository extends JpaRepository<ReceptionEntity, Integer> {}
interface SchemaLigneReceptionRepository extends JpaRepository<LigneReceptionEntity, Integer> {}
