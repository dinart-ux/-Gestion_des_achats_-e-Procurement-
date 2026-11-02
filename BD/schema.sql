CREATE TABLE IF NOT EXISTS utilisateur (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS produit (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(150) NOT NULL,
    description TEXT,
    prix NUMERIC(10,2) NOT NULL,
    stock INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS commande (
    id SERIAL PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    statut VARCHAR(50) DEFAULT 'EN_ATTENTE',
    total NUMERIC(10,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_commande_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id)
);

CREATE TABLE IF NOT EXISTS commande_detail (
    id SERIAL PRIMARY KEY,
    commande_id INT NOT NULL,
    produit_id INT NOT NULL,
    quantite INT NOT NULL,
    prix_unitaire NUMERIC(10,2) NOT NULL,
    CONSTRAINT fk_detail_commande
        FOREIGN KEY (commande_id) REFERENCES commande(id),
    CONSTRAINT fk_detail_produit
        FOREIGN KEY (produit_id) REFERENCES produit(id)
);

CREATE INDEX IF NOT EXISTS idx_utilisateur_email ON utilisateur(email);
CREATE INDEX IF NOT EXISTS idx_commande_utilisateur ON commande(utilisateur_id);
CREATE INDEX IF NOT EXISTS idx_detail_commande ON commande_detail(commande_id);
