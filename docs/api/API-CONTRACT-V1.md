# Contrat API REST V1

## Statut

**VALIDATED**

## 1. Objet et conventions générales

Ce document décrit le contrat conceptuel de l'API REST V1. Il ne constitue ni une implémentation Spring, ni une spécification OpenAPI.

Principes obligatoires :

- l'API est orientée cas d'utilisation et actions métier ;
- les demandes sont identifiées dans les routes par leur `reference` métier ;
- `reference`, `statut`, `createur`, auteur de l'historique et dates métier sont gérés par le serveur ;
- aucun client ne peut choisir librement le statut d'une demande ;
- les changements de statut résultent uniquement des actions métier explicites ;
- les identifiants techniques restent utilisables pour `Client` et `Utilisateur` ;
- les dates sont représentées comme des instants ISO-8601 cohérents ;
- les permissions d'un utilisateur multi-rôles sont l'union de ses rôles ;
- les contrôles contextuels s'ajoutent toujours au contrôle du rôle.

Tous les endpoints décrits sont protégés. Ils utilisent l'identité authentifiée fournie par la sécurité, dont le mécanisme sera défini dans ADR-005.

## 2. Base path

```text
/api/v1
```

## 3. Tableau des endpoints

Dans le tableau, `RT` désigne `RESPONSABLE_TECHNIQUE`, `AT` désigne `AGENT_TECHNIQUE` et `ADM` désigne `ADMINISTRATEUR`.

| Méthode et route | Rôle requis | Contrôle contextuel et résultat métier | Entrée | Sortie | Succès |
|---|---|---|---|---|---|
| `POST /demandes` | RT | Le serveur prend l'utilisateur connecté comme créateur, génère la référence, impose `NOUVELLE`, conserve éventuellement le nouveau client et historise la création. | `CreationDemandeRequest` | `DemandeDetailResponse` et en-tête `Location: /api/v1/demandes/{reference}` | `201` |
| `GET /demandes` | RT ou AT | RT voit toutes les demandes. AT voit uniquement celles qui lui sont affectées. Les filtres ne peuvent pas élargir ce périmètre. ADM seul n'a aucun accès. | Paramètres de pagination, tri et filtres | Page de `DemandeSummaryResponse` | `200` |
| `GET /demandes/{reference}` | RT ou AT | RT peut consulter toute demande. AT peut consulter uniquement une demande qui lui est affectée. L'historique complet n'est pas inclus. | — | `DemandeDetailResponse` | `200` |
| `PUT /demandes/{reference}/qualification` | RT | La demande doit rester fonctionnellement modifiable. Aucun statut n'est accepté en entrée. | `QualificationDemandeRequest` | `DemandeDetailResponse` | `200` |
| `POST /demandes/analyse-ia` | RT | Ne crée ni ne modifie une demande. Retourne des propositions temporaires ; un échec n'empêche pas la qualification manuelle. | `AnalyseIaRequest` | `AnalyseIaResponse` | `200` |
| `PUT /demandes/{reference}/affectation` | RT | L'utilisateur choisi doit être actif et avoir le rôle AT. `NOUVELLE → ASSIGNEE`, `ASSIGNEE → ASSIGNEE`, `EN_COURS → ASSIGNEE`. | `AffectationDemandeRequest` | `DemandeDetailResponse` | `200` |
| `POST /demandes/{reference}/demarrage-traitement` | AT | L'utilisateur connecté doit être l'Agent affecté et la demande doit être `ASSIGNEE`. Transition vers `EN_COURS`. | — | `DemandeDetailResponse` | `200` |
| `PATCH /demandes/{reference}/traitement` | AT | L'utilisateur connecté doit être l'Agent affecté et la demande doit être `EN_COURS`. Cette opération ne change pas librement le statut. | `TraitementDemandeRequest` | `DemandeDetailResponse` | `200` |
| `POST /demandes/{reference}/resolution` | AT | L'utilisateur connecté doit être l'Agent affecté. La demande doit être `EN_COURS` et posséder une solution. Transition vers `RESOLUE`. | — | `DemandeDetailResponse` | `200` |
| `POST /demandes/{reference}/cloture` | RT | La demande doit être `RESOLUE`. Transition vers `CLOTUREE`. | — | `DemandeDetailResponse` | `200` |
| `POST /demandes/{reference}/refus-resolution` | RT | La demande doit être `RESOLUE`. Transition vers `EN_COURS`. Aucun motif obligatoire n'est ajouté. | — | `DemandeDetailResponse` | `200` |
| `POST /demandes/{reference}/annulation` | RT | Autorisée depuis `NOUVELLE`, `ASSIGNEE` ou `EN_COURS`. Interdite depuis `RESOLUE`, `CLOTUREE` ou `ANNULEE`. Transition vers `ANNULEE`. | `AnnulationDemandeRequest` | `DemandeDetailResponse` | `200` |
| `GET /clients` | RT | Recherche simple destinée à sélectionner un client pendant l'enregistrement. Aucun droit de création, modification ou suppression autonome. | Recherche et pagination éventuelle | Page de `ClientSummaryResponse` | `200` |
| `GET /agents` | RT | Retourne uniquement les utilisateurs actifs ayant le rôle AT et les données utiles à leur sélection. | — | Liste de `AgentAssignableResponse` | `200` |
| `GET /utilisateurs` | ADM | Consultation administrative paginée. | Pagination et tri | Page de `UtilisateurSummaryResponse` | `200` |
| `GET /utilisateurs/{id}` | ADM | L'utilisateur doit exister. | — | `UtilisateurDetailResponse` | `200` |
| `POST /utilisateurs` | ADM | Crée un utilisateur interne sans définir ici ses credentials. | `CreationUtilisateurRequest` | `UtilisateurDetailResponse` et en-tête `Location: /api/v1/utilisateurs/{id}` | `201` |
| `PUT /utilisateurs/{id}` | ADM | Modifie les informations générales ; l'activation et les rôles utilisent leurs opérations dédiées. | `ModificationUtilisateurRequest` | `UtilisateurDetailResponse` | `200` |
| `POST /utilisateurs/{id}/activation` | ADM | Active le compte ciblé. | — | `UtilisateurDetailResponse` | `200` |
| `POST /utilisateurs/{id}/desactivation` | ADM | Désactive le compte ciblé. | — | `UtilisateurDetailResponse` | `200` |
| `PUT /utilisateurs/{id}/roles-metier` | ADM | Remplace uniquement l'ensemble des rôles métier `RESPONSABLE_TECHNIQUE` et `AGENT_TECHNIQUE`. Un éventuel rôle `ADMINISTRATEUR` existant est conservé intact : cette opération ne peut ni l'attribuer ni le retirer. | `RolesMetierRequest` | `UtilisateurDetailResponse` | `200` |

Il n'existe en V1 aucun endpoint générique de modification de statut, aucun CRUD autonome de clients ou d'historique et aucun endpoint de suppression d'utilisateur.

## 4. DTO conceptuels

Les types d'identifiants techniques ne sont pas imposés par ce contrat. Les champs optionnels sont signalés explicitement.

### 4.1 Demandes

#### `CreationDemandeRequest`

| Champ | Règle |
|---|---|
| `titre` | Obligatoire, non vide. |
| `description` | Obligatoire, non vide. |
| `categorie` | Obligatoire, valeur de `Categorie`. |
| `priorite` | Obligatoire, valeur de `Priorite`. |
| `clientId` | Obligatoire uniquement lorsqu'un client existant est choisi. |
| `nouveauClient` | `NouveauClientRequest`, obligatoire uniquement lorsque le client n'est pas enregistré. |

Exactement un des champs `clientId` ou `nouveauClient` doit être fourni. Le DTO ne contient ni référence, ni statut, ni créateur, ni dates métier.

#### `QualificationDemandeRequest`

| Champ | Règle |
|---|---|
| `categorie` | Obligatoire, valeur de `Categorie`. |
| `priorite` | Obligatoire, valeur de `Priorite`. |

#### `AffectationDemandeRequest`

| Champ | Règle |
|---|---|
| `agentId` | Obligatoire ; cible un utilisateur actif ayant le rôle `AGENT_TECHNIQUE`. |

#### `TraitementDemandeRequest`

| Champ | Règle |
|---|---|
| `descriptionTraitement` | Optionnel ; champ absent : valeur existante inchangée ; champ fourni : nouvelle valeur obligatoire et non vide. |
| `solution` | Optionnel ; champ absent : valeur existante inchangée ; champ fourni : nouvelle valeur obligatoire et non vide. |

Au moins un des deux champs doit être fourni. Aucun champ de statut n'est accepté et aucune longueur maximale arbitraire n'est définie.

#### `AnnulationDemandeRequest`

| Champ | Règle |
|---|---|
| `motif` | Obligatoire, non vide. |

#### `AnalyseIaRequest`

| Champ | Règle |
|---|---|
| `titre` | Obligatoire, non vide. |
| `description` | Obligatoire, non vide. |

#### `AnalyseIaResponse`

| Champ | Règle |
|---|---|
| `categorieProposee` | Valeur de `Categorie`. |
| `prioriteProposee` | Valeur de `Priorite`. |

Ces valeurs restent des propositions temporaires. Elles ne constituent pas une décision métier validée.

#### `DemandeSummaryResponse`

- `reference` ;
- `titre` ;
- `categorie` ;
- `priorite` ;
- `statut` ;
- `client` : `ClientSummaryResponse` ;
- `agentAffecte` : `UtilisateurSummaryResponse` ou `null` ;
- `dateCreation` ;
- `dateModification`.

#### `DemandeDetailResponse`

- `reference` ;
- `titre` ;
- `description` ;
- `categorie` ;
- `priorite` ;
- `statut` ;
- `client` : `ClientSummaryResponse` ;
- `createur` : `UtilisateurSummaryResponse` ;
- `agentAffecte` : `UtilisateurSummaryResponse` ou `null` ;
- `descriptionTraitement` ou `null` ;
- `solution` ou `null` ;
- `motifAnnulation` ou `null` ;
- `dateCreation` ;
- `dateModification` ;
- `dateResolution` ou `null` ;
- `dateCloture` ou `null` ;
- `dateAnnulation` ou `null`.

L'historique complet n'est pas inclus automatiquement.

### 4.2 Clients

#### `NouveauClientRequest`

- `nom` : obligatoire, non vide ;
- `email` : obligatoire, syntaxiquement valide ;
- `telephone` : obligatoire, non vide.

Ces champs correspondent aux informations minimales du Client validées pour la V1. Aucune longueur maximale arbitraire ni aucun format téléphonique national particulier n'est imposé. Conformément à ADR-003, l'adresse email d'un Client n'est pas unique.

#### `ClientSummaryResponse`

- `id` ;
- `nom` ;
- `email` ;
- `telephone`.

### 4.3 Utilisateurs

#### `CreationUtilisateurRequest`

- `nom` : obligatoire, non vide ;
- `email` : obligatoire, syntaxiquement valide ;
- `actif` : état fonctionnel initial explicite ;
- `rolesMetier` : ensemble composé uniquement de `RESPONSABLE_TECHNIQUE` et `AGENT_TECHNIQUE`.

Le DTO ne contient aucun mot de passe ni paramètre de jeton. L'initialisation des credentials est différée à ADR-005.

#### `ModificationUtilisateurRequest`

- `nom` : obligatoire, non vide ;
- `email` : obligatoire, syntaxiquement valide.

L'état actif et les rôles sont modifiés par leurs opérations dédiées.

#### `RolesMetierRequest`

- `rolesMetier` : ensemble des rôles métier souhaités parmi `RESPONSABLE_TECHNIQUE` et `AGENT_TECHNIQUE`.

Cette requête remplace uniquement l'ensemble des rôles métier de l'utilisateur. Si l'utilisateur possède déjà `ADMINISTRATEUR`, ce rôle est conservé intact. L'opération ne peut ni attribuer ni retirer `ADMINISTRATEUR`, et toute présence de cette valeur dans `rolesMetier` rend la requête invalide.

#### `UtilisateurSummaryResponse`

- `id` ;
- `nom` ;
- `email` ;
- `actif` ;
- `roles`.

#### `UtilisateurDetailResponse`

- `id` ;
- `nom` ;
- `email` ;
- `actif` ;
- `roles`.

Aucun credential ni détail de sécurité n'est exposé.

#### `AgentAssignableResponse`

- `id` ;
- `nom` ;
- `email` ;
- `actif`.

Seuls les utilisateurs actifs ayant le rôle `AGENT_TECHNIQUE` figurent dans cette réponse.

## 5. Valeurs métier fermées

### `StatutDemande`

`NOUVELLE`, `ASSIGNEE`, `EN_COURS`, `RESOLUE`, `CLOTUREE`, `ANNULEE`.

### `Priorite`

`BASSE`, `MOYENNE`, `HAUTE`, `CRITIQUE`.

### `Categorie`

Les codes persistants et échangés sont :

- `ETUDE_DANGERS` ;
- `ANALYSE_RISQUES_INDUSTRIELS` ;
- `PROTECTION_INCENDIE` ;
- `NOTE_CALCUL` ;
- `DOSSIER_TECHNIQUE` ;
- `ASSISTANCE_REGLEMENTAIRE` ;
- `AUTRE`.

### Rôles

`RESPONSABLE_TECHNIQUE`, `AGENT_TECHNIQUE`, `ADMINISTRATEUR`.

L'opération `/roles-metier` accepte uniquement les deux rôles métier. Elle préserve tout rôle `ADMINISTRATEUR` déjà présent sans permettre de l'attribuer ou de le retirer.

## 6. Validation

La validation s'effectue à deux niveaux :

- validation de forme à la frontière HTTP : JSON, champs obligatoires, valeurs enum, email et paramètres ;
- validation métier côté backend : droits contextuels, état courant, transitions autorisées, demande terminale non modifiable, Agent affecté et solution requise avant résolution.

Règles transverses :

- les chaînes déclarées non vides ne peuvent pas contenir uniquement des espaces ;
- `CreationDemandeRequest` fournit exactement `clientId` ou `nouveauClient` ;
- `NouveauClientRequest` fournit un nom non vide, un email syntaxiquement valide et un téléphone non vide, sans exiger l'unicité de l'email Client ni un format téléphonique national particulier ;
- `TraitementDemandeRequest` fournit au moins un champ ; chaque champ absent reste inchangé et chaque champ fourni doit être non vide ;
- `AffectationDemandeRequest.agentId` désigne un compte actif ayant le rôle AT ;
- l'email utilisateur est syntaxiquement valide, unique et normalisé conformément à ADR-003 ;
- le motif d'annulation est obligatoire et non vide ;
- aucune longueur maximale arbitraire n'est fixée dans ce document.

## 7. Codes HTTP

| Code | Utilisation |
|---|---|
| `200 OK` | Lecture, modification ou action métier réussie retournant l'état obtenu. |
| `201 Created` | Création d'une demande ou d'un utilisateur. |
| `400 Bad Request` | JSON invalide, validation de forme, enum ou paramètre invalide. |
| `401 Unauthorized` | Authentification absente ou invalide. |
| `403 Forbidden` | Utilisateur authentifié sans rôle requis ou sans droit contextuel sur la ressource. |
| `404 Not Found` | Ressource ciblée inexistante. |
| `409 Conflict` | Transition interdite, état métier incompatible ou unicité déjà utilisée. |
| `503 Service Unavailable` | Analyse IA temporairement indisponible, uniquement sur l'opération d'analyse IA lorsque cette cause s'applique. |

## 8. Format d'erreur

Format conceptuel commun :

```json
{
  "code": "CODE_STABLE",
  "message": "Message compréhensible",
  "fieldErrors": [
    {
      "field": "nomDuChamp",
      "code": "CODE_VALIDATION",
      "message": "Description de l'erreur"
    }
  ]
}
```

`fieldErrors` est optionnel et réservé aux erreurs de validation de champs. Les codes sont stables et exploitables par Angular. Les réponses ne contiennent jamais de stack trace, exception Java, SQL ou détail interne de sécurité.

## 9. Pagination, tri et filtres

### 9.1 Contrat de pagination

Paramètres conceptuels :

- `page` ;
- `size` ;
- `sort`, lorsque pertinent.

Réponse :

- `items` ;
- `page` ;
- `size` ;
- `totalElements` ;
- `totalPages`.

Aucune taille maximale arbitraire n'est décidée ici.

### 9.2 Demandes

`GET /demandes` accepte les filtres :

- `statut` ;
- `priorite` ;
- `categorie` ;
- `clientId` ;
- `agentId` ;
- `recherche`, pour une recherche textuelle simple.

Le tri par défaut est `dateCreation` décroissante. Les filtres sont appliqués à l'intérieur du périmètre déjà autorisé : un Agent ne peut jamais utiliser un filtre pour consulter les demandes d'un autre Agent.

### 9.3 Utilisateurs et clients

`GET /utilisateurs` est paginé. `GET /clients` accepte une recherche simple et peut être paginé. Aucun moteur de recherche complexe n'est introduit.

## 10. Matrice d'autorisation

| Groupe d'opérations | RT | AT | ADM | Contrôle contextuel principal |
|---|---:|---:|---:|---|
| Créer une demande | Oui | Non | Non | Le RT connecté devient le créateur. |
| Lister ou consulter les demandes | Toutes | Affectées uniquement | Non | L'AT connecté doit être l'Agent affecté. |
| Qualifier une demande | Oui | Non | Non | Demande fonctionnellement modifiable. |
| Demander une analyse IA | Oui | Non | Non | Aucune demande incomplète n'est persistée par cette opération. |
| Affecter ou réaffecter | Oui | Non | Non | Agent cible actif avec rôle AT ; état compatible. |
| Démarrer, renseigner ou résoudre le traitement | Non | Oui | Non | L'AT connecté est l'Agent affecté ; état compatible ; solution présente pour résoudre. |
| Clôturer ou refuser une résolution | Oui | Non | Non | Demande `RESOLUE`. |
| Annuler une demande | Oui | Non | Non | État autorisé et motif présent. |
| Rechercher un client | Oui | Non | Non | Sélection pendant l'enregistrement. |
| Lister les Agents affectables | Oui | Non | Non | Résultat limité aux comptes actifs avec rôle AT. |
| Gérer les utilisateurs et rôles métier | Non | Non | Oui | `/roles-metier` remplace uniquement RT et AT ; un rôle ADM existant reste intact et ne peut être ni attribué ni retiré par cette opération. |

Un utilisateur cumulant plusieurs rôles cumule leurs permissions. Le rôle `ADMINISTRATEUR` seul ne donne aucun accès métier aux demandes.

## 11. Historisation

Les changements importants sont historisés côté backend dans la même transaction que l'action métier concernée, conformément à ADR-002 et ADR-003. L'auteur est déduit de l'identité authentifiée. Aucun auteur d'historique n'est accepté dans un DTO d'entrée.

Il n'existe aucun endpoint CRUD autonome d'historique en V1.

## 12. Décisions différées

Ne sont pas définis par ce contrat :

- endpoint exact de connexion, déconnexion ou renouvellement ;
- durée, rotation, révocation, blacklist ou stockage des JWT ;
- stockage du mot de passe et initialisation des credentials ;
- format exact de la référence de demande ;
- longueurs maximales des champs et limites maximales de pagination ;
- fournisseur, modèle, SDK ou protocole IA ;
- classes Java, contrôleurs, services, repositories, mappers et annotations ;
- spécification OpenAPI et modèle physique SQL.
