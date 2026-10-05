-- V1 : schéma initial (Bloc 1 — fondation backend + persistance V1).
-- Cohérent avec les mappings JPA : mêmes tables, séquences, colonnes et contraintes.
-- Enums persistés comme codes textuels, instants en timestamptz, aucune
-- suppression cascade SQL, aucun soft-delete, aucun CHECK de workflow.

CREATE SEQUENCE utilisateur_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE client_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE demande_technique_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE historique_demande_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE utilisateur (
    id BIGINT NOT NULL,
    nom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    actif BOOLEAN NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    CONSTRAINT pk_utilisateur PRIMARY KEY (id),
    CONSTRAINT uk_utilisateur_email UNIQUE (email)
);

CREATE TABLE utilisateur_role (
    utilisateur_id BIGINT NOT NULL,
    role VARCHAR(255) NOT NULL,
    CONSTRAINT pk_utilisateur_role PRIMARY KEY (utilisateur_id, role),
    CONSTRAINT fk_utilisateur_role_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id)
);

CREATE TABLE client (
    id BIGINT NOT NULL,
    nom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    telephone VARCHAR(255) NOT NULL,
    CONSTRAINT pk_client PRIMARY KEY (id)
);

CREATE TABLE demande_technique (
    id BIGINT NOT NULL,
    reference VARCHAR(255) NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    categorie VARCHAR(255) NOT NULL,
    priorite VARCHAR(255) NOT NULL,
    statut VARCHAR(255) NOT NULL,
    description_traitement VARCHAR(4000),
    solution VARCHAR(4000),
    motif_annulation VARCHAR(4000),
    date_creation TIMESTAMP WITH TIME ZONE NOT NULL,
    date_modification TIMESTAMP WITH TIME ZONE NOT NULL,
    date_resolution TIMESTAMP WITH TIME ZONE,
    date_cloture TIMESTAMP WITH TIME ZONE,
    date_annulation TIMESTAMP WITH TIME ZONE,
    client_id BIGINT NOT NULL,
    createur_id BIGINT NOT NULL,
    agent_affecte_id BIGINT,
    CONSTRAINT pk_demande_technique PRIMARY KEY (id),
    CONSTRAINT uk_demande_technique_reference UNIQUE (reference),
    CONSTRAINT fk_demande_technique_client FOREIGN KEY (client_id) REFERENCES client (id),
    CONSTRAINT fk_demande_technique_createur FOREIGN KEY (createur_id) REFERENCES utilisateur (id),
    CONSTRAINT fk_demande_technique_agent_affecte FOREIGN KEY (agent_affecte_id) REFERENCES utilisateur (id)
);

CREATE TABLE historique_demande (
    id BIGINT NOT NULL,
    date_evenement TIMESTAMP WITH TIME ZONE NOT NULL,
    type_evenement VARCHAR(255) NOT NULL,
    ancienne_valeur VARCHAR(255),
    nouvelle_valeur VARCHAR(255),
    demande_id BIGINT NOT NULL,
    auteur_id BIGINT NOT NULL,
    CONSTRAINT pk_historique_demande PRIMARY KEY (id),
    CONSTRAINT fk_historique_demande_demande FOREIGN KEY (demande_id) REFERENCES demande_technique (id),
    CONSTRAINT fk_historique_demande_auteur FOREIGN KEY (auteur_id) REFERENCES utilisateur (id)
);
