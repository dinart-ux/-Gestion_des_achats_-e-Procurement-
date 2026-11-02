package com.example.backend.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.math.BigDecimal;
import java.util.List;

public final class AchatRequests {
    private AchatRequests() {}

    public record LigneDemande(
            Integer categorieId,
            @NotBlank(message = "Le type de ligne est obligatoire.") @Pattern(regexp = "ARTICLE|SERVICE", message = "Le type doit être ARTICLE ou SERVICE.") String type,
            @NotBlank @Size(max = 200, message = "La désignation ne peut pas dépasser 200 caractères.") String designation,
            @NotNull(message = "La quantité est obligatoire.") @Positive(message = "La quantité doit être supérieure à zéro.") @Digits(integer = 10, fraction = 2, message = "La quantité doit avoir au plus 10 chiffres et 2 décimales.") BigDecimal quantite,
            @NotNull(message = "Le prix unitaire est obligatoire.") @PositiveOrZero(message = "Le prix unitaire doit être positif ou nul.") @Digits(integer = 13, fraction = 2, message = "Le prix unitaire doit avoir au plus 13 chiffres et 2 décimales.") BigDecimal prixUnitaireEstime) {}

    public record Demande(
            @NotNull(message = "Le centre de coût est obligatoire.") @Positive(message = "Sélectionnez un centre de coût valide.") Integer centreCoutId,
            @NotBlank(message = "L’objet de la demande est obligatoire.") @Size(max = 200, message = "L’objet ne peut pas dépasser 200 caractères.") String objet,
            String justification,
            @NotEmpty(message = "La demande doit contenir au moins une ligne.") List<@Valid LigneDemande> lignes) {}

    public record Decision(@NotBlank String decision, String commentaire) {}

    public record Fournisseur(
            @NotBlank(message = "La raison sociale est obligatoire.") @Size(max = 150, message = "La raison sociale ne peut pas dépasser 150 caractères.") String raisonSociale,
            @Size(max = 30, message = "Le NIF ne peut pas dépasser 30 caractères.") String nif,
            @Size(max = 30, message = "Le STAT ne peut pas dépasser 30 caractères.") String stat,
            @Size(max = 255, message = "L’adresse ne peut pas dépasser 255 caractères.") String adresse,
            @Size(max = 30, message = "Le téléphone ne peut pas dépasser 30 caractères.") String telephone,
            @Email(message = "L’adresse e-mail est invalide.") @Size(max = 150, message = "L’adresse e-mail ne peut pas dépasser 150 caractères.") String email,
            @Size(max = 100, message = "Le nom du contact ne peut pas dépasser 100 caractères.") String contactNom,
            @Size(max = 255, message = "Les conditions de paiement ne peuvent pas dépasser 255 caractères.") String conditionsPaiement,
            @PositiveOrZero(message = "Le délai doit être un nombre entier positif ou nul.") Integer delaiLivraisonJours,
            List<@NotNull @Positive(message = "Chaque catégorie doit être valide.") Integer> categories) {}

    public record Offre(
            @NotNull @Positive Integer fournisseurId,
            @NotNull @PositiveOrZero BigDecimal montantTotal,
            @PositiveOrZero Integer delaiLivraisonJours,
            @Size(max = 255) String conditions) {}

    public record LigneReception(@NotNull Integer ligneCommandeId, @NotNull @Positive BigDecimal quantiteRecue) {}

    public record Reception(@NotEmpty List<@Valid LigneReception> lignes, String observation) {}

    public record Budget(
            @NotNull @Positive Integer centreCoutId,
            @NotNull @Min(2000) @Max(2100) Integer annee,
            @NotNull @Positive(message = "Le montant alloué doit être strictement positif.") @Digits(integer = 13, fraction = 2) BigDecimal montantAlloue,
            @NotNull @PositiveOrZero @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal seuilAlertePct) {}
}
