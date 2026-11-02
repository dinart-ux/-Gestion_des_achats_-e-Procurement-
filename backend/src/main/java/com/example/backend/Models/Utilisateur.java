/**package com.example.backend.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;*/

/** Maps the existing legacy table without changing its column names. */
/*@Entity
@Table(name = "\"UTILISATEURS\"")
public class Utilisateur {

    @Id
    @Column(name = "\"Matricule\"", nullable = false, length = 7)
    private String matricule;

    @Column(name = "\"Nom\"", nullable = false, length = 50)
    private String nom;

    @Column(name = "\"Prenom\"", nullable = false, length = 75)
    private String prenom;

    @Column(name = "\"role_id\"", nullable = false, length = 50)
    private String role;

    @Column(name = "\"Email\"", nullable = false, length = 75)
    private String email;

    @Column(name = "\"service_id\"", nullable = false, length = 50)
    private String service;

    @Column(name = "\"Mot_de_Passe\"", nullable = false, length = 255)
    private String password;

    public Utilisateur() {}

    // Getters and Setters
    public String getId() { return matricule; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    
    public boolean isActif() { return true; }
}*/
package com.example.backend.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;

/** Maps the existing legacy table without changing its column names. */
@Entity
@Table(name = "\"utilisateurs\"")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Integer id;

    @Column(name = "\"matricule\"", nullable = false, unique = true, length = 20)
    private String matricule;

    @Column(name = "\"nom\"", nullable = false, length = 100)
    private String nom;

    @Column(name = "\"prenom\"", nullable = false, length = 100)
    private String prenom;

    @Column(name = "\"role_id\"", nullable = false, length = 50)
    private Integer role;

    @Column(name = "\"email\"", nullable = false, length = 150)
    private String email;

    @Column(name = "\"telephone\"", length = 30)
    private String telephone;

    @Column(name = "\"service_id\"")
    private Integer service;

    @Column(name = "\"manager_id\"")
    private Integer managerId;

    @Column(name = "\"mot_de_passe\"", nullable = false, length = 255)
    private String password;

    @Column(name = "\"actif\"", nullable = false)
    private boolean actif = true;

    @Column(name = "\"date_creation\"", nullable = false)
    private LocalDateTime dateCreation;

    public Utilisateur() {}

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }
    
    public Integer getService() { return service; }
    public void setService(Integer service) { this.service = service; }

    public Integer getManagerId() { return managerId; }
    public void setManagerId(Integer managerId) { this.managerId = managerId; }
    
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    @PrePersist
    void initialiserDateCreation() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
