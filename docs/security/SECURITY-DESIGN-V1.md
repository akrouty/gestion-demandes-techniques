# Conception sécurité V1

## Statut

**DRAFT**

## 1. Portée

Ce document décrit la conception de sécurité proposée pour l'API REST V1, sans code ni configuration exécutable. Il applique les rôles et permissions du périmètre fonctionnel ainsi que le contrat API validé.

Les mécanismes restent `DRAFT` jusqu'à validation humaine. Aucune classe Spring Security, clé, valeur de secret ou durée de token n'est définie ici.

## 2. Flux de login

Opération proposée :

```text
POST /api/v1/auth/login
```

Entrée conceptuelle :

- `email` ;
- `password`.

Flux :

1. recevoir la demande de connexion via HTTPS dans un environnement réel ;
2. normaliser l'email selon ADR-003 ;
3. rechercher l'utilisateur correspondant ;
4. vérifier que le compte existe et que `actif = true` ;
5. vérifier le mot de passe avec Spring Security `PasswordEncoder` ;
6. en cas de succès, émettre un JWT d'accès de durée limitée ;
7. en cas d'échec, retourner une erreur générique sans distinguer utilisateur inconnu, compte inactif ou mot de passe incorrect.

Réponse conceptuelle en cas de succès : le token d'accès, son type `Bearer` et les informations temporelles nécessaires au client. Aucun mot de passe, rôle complet inutile ou détail interne n'est retourné.

## 3. Flux d'une requête protégée

1. Angular envoie `Authorization: Bearer <token>`.
2. Le backend extrait le token sans le journaliser.
3. Il vérifie la signature, l'algorithme attendu, l'expiration, `iss` et, si elle est retenue, `aud`.
4. Il lit `sub` pour identifier l'utilisateur.
5. Il relit l'utilisateur dans `identity / administration` et refuse la requête si le compte est absent ou inactif.
6. Il construit les autorités à partir des rôles actuels du compte.
7. Il applique le RBAC de l'endpoint.
8. La couche application applique ensuite le contrôle métier contextuel sur la ressource.
9. Le cas d'utilisation est exécuté uniquement si tous les contrôles réussissent.

L'API n'utilise pas de `HttpSession` comme mécanisme d'authentification. Chaque requête protégée présente son JWT.

## 4. Séparation des contrôles

| Niveau | Question | Exemple |
|---|---|---|
| Authentification | Qui est l'utilisateur ? | JWT valide, compte existant et actif. |
| RBAC | Possède-t-il le rôle requis ? | `AGENT_TECHNIQUE` pour démarrer un traitement. |
| Autorisation contextuelle | Peut-il agir sur cette ressource précise ? | L'utilisateur est l'Agent affecté à la demande ciblée. |

Le succès d'un niveau ne remplace jamais les suivants.

## 5. Règles par rôle

### `RESPONSABLE_TECHNIQUE`

Accès aux opérations de création, consultation globale, qualification, analyse IA, affectation, examen de résolution, annulation, recherche de clients et liste des Agents affectables prévues dans le contrat API.

### `AGENT_TECHNIQUE`

Accès uniquement à ses demandes affectées et aux opérations de démarrage, saisie du traitement, saisie de la solution et résolution sur ces demandes.

### `ADMINISTRATEUR`

Accès aux opérations d'administration des utilisateurs et des rôles métier. Ce rôle seul ne donne aucun accès métier aux demandes.

### Utilisateur multi-rôles

Les permissions sont l'union des rôles actuels. Les contrôles contextuels restent applicables pour chaque opération.

La gestion des rôles métier ne peut ni attribuer ni retirer `ADMINISTRATEUR` et conserve ce rôle s'il existe déjà.

## 6. Compte inactif et évolution des rôles

Un compte inactif est refusé au login et sur toute requête protégée, même si le JWT présenté n'est pas expiré.

Les rôles et l'état actif sont relus depuis Identity lors de l'authentification de chaque requête. Un rôle retiré après émission du JWT ne continue donc pas à produire de permission. Un éventuel claim de rôles dans le token ne remplace jamais cette source d'autorité.

Ce choix introduit un accès Identity ou base par requête authentifiée. Il est acceptable pour le volume attendu d'une application interne V1 et privilégie l'effet immédiat des changements administratifs.

## 7. Claims JWT proposés

| Claim | Usage |
|---|---|
| `sub` | Identifiant stable de l'utilisateur. |
| `iat` | Instant d'émission. |
| `exp` | Instant d'expiration. |
| `iss` | Émetteur attendu. |
| `aud` | Optionnel ; utilisé seulement si une audience explicite est décidée. |

Le JWT ne contient ni mot de passe, ni données sensibles inutiles, ni informations métier complètes. Les rôles ne sont pas nécessaires comme source d'autorité puisque leur valeur actuelle est relue depuis Identity.

## 8. Validation du JWT

La validation vérifie au minimum :

- une signature valide avec la clé configurée ;
- l'algorithme attendu configuré côté serveur ;
- une date d'expiration valide ;
- l'émetteur attendu ;
- l'audience attendue si `aud` est utilisée.

L'algorithme indiqué dans le token n'est jamais accepté comme choix dynamique. Un token invalide, altéré ou expiré produit `401` sans exposer la cause technique détaillée.

## 9. Mots de passe

- aucune conservation en clair ou sous chiffrement réversible ;
- aucun mot de passe dans les logs, réponses ou JWT ;
- hachage et vérification via Spring Security `PasswordEncoder` ;
- BCrypt recommandé pour la V1 ;
- facteur de coût déterminé par mesure sur l'environnement cible avant implémentation, sans valeur arbitraire dans la documentation.

Argon2 reste une alternative si un besoin concret justifie ses paramètres et sa complexité supplémentaires.

## 10. Stockage du token côté Angular

Le JWT d'accès est conservé uniquement en mémoire et envoyé dans l'en-tête `Authorization`. Il n'est pas conservé dans `localStorage` ni `sessionStorage`.

Conséquence acceptée provisoirement : un rechargement complet de la SPA peut exiger une nouvelle authentification. Aucun refresh token n'est ajouté automatiquement pour masquer cette contrainte.

La déconnexion supprime le JWT en mémoire côté Angular. Aucun endpoint backend de logout n'est proposé tant qu'aucun état serveur de session ou de révocation ne lui donne un effet réel.

## 11. Gestion des secrets

Le secret ou la clé JWT :

- n'est jamais codé en dur ;
- n'est jamais commité dans Git ;
- n'est jamais placé dans un fichier de propriétés versionné ;
- provient d'une configuration externe, d'une variable d'environnement ou d'un mécanisme de secret adapté.

HMAC avec un secret cryptographiquement fort est recommandé pour le monolithe V1. La signature asymétrique reste une option lorsque la séparation entre émetteur et vérificateurs devient utile. L'algorithme exact et le mode de fourniture du secret restent à valider.

Une configuration absente ou manifestement invalide doit empêcher un démarrage sécurisé plutôt que produire des tokens avec une valeur de repli.

## 12. CORS et CSRF

### CORS

- autoriser uniquement les origines frontend explicitement configurées ;
- limiter les méthodes et en-têtes aux besoins de l'API ;
- ne pas utiliser de wildcard incontrôlé ;
- permettre une configuration différente selon l'environnement ;
- différer l'origine de production à la conception du déploiement.

### CSRF

Dans la proposition V1, le navigateur n'envoie pas automatiquement de cookie d'authentification : Angular ajoute explicitement un token conservé en mémoire dans l'en-tête `Authorization`. Une attaque CSRF classique ne bénéficie donc pas de credentials automatiquement joints par le navigateur.

La configuration Spring Security doit refléter exactement ce mode stateless. La protection CSRF ne doit pas être écartée par la seule formule « JWT = CSRF désactivé ». Toute évolution vers un cookie d'authentification impose une nouvelle analyse et une protection CSRF adaptée avant sa mise en service.

## 13. HTTPS

Les credentials et JWT sont transmis uniquement via HTTPS dans un environnement réel. Le proxy, les certificats et l'architecture de terminaison TLS ne sont pas décidés dans ce document.

## 14. Erreurs de sécurité

Le format reste celui d'`API-CONTRACT-V1.md` : `code`, `message` et éventuellement `fieldErrors` pour la validation de champs.

- `401 Unauthorized` : authentification absente ou invalide, notamment JWT manquant, altéré ou expiré, ou compte devenu inactif ;
- `403 Forbidden` : utilisateur authentifié mais rôle insuffisant ou contrôle contextuel refusé.

La réponse ne révèle pas l'existence d'un utilisateur, la raison cryptographique précise d'un rejet, une stack trace, un secret ou une exception interne.

## 15. Scénarios de tests prévus

| Scénario | Résultat attendu |
|---|---|
| Accès protégé sans JWT | `401`. |
| JWT invalide | `401` générique. |
| JWT altéré | `401` générique. |
| JWT expiré | `401` générique. |
| Compte désactivé | Login refusé ; requête avec ancien JWT refusée en `401`. |
| Rôle insuffisant | `403`. |
| `ADMINISTRATEUR` seul sur une route métier | `403`. |
| `AGENT_TECHNIQUE` sur une demande non affectée | `403`. |
| Agent affecté sur sa propre demande, état compatible | Autorisation accordée ; le cas d'utilisation applique ensuite ses invariants métier. |
| Utilisateur multi-rôles | Union des permissions, avec contrôles contextuels maintenus. |
| Rôle retiré après émission du JWT | Permission retirée dès la requête suivante. |
| Email ou mot de passe incorrect | Même échec d'authentification générique. |
| Tentative d'attribuer `ADMINISTRATEUR` via `/roles-metier` | Requête refusée conformément au contrat API. |
| Secret JWT absent ou mal configuré | Aucun démarrage sécurisé avec valeur de repli ; échec explicite de configuration sans révéler de secret. |
| Requête CORS depuis une origine non autorisée | Origine refusée par la politique CORS. |

Ces tests sont à concevoir et exécuter pendant l'implémentation ; aucun n'est exécuté à cette étape documentaire.

## 16. Point ouvert : initialisation des credentials

La création d'un utilisateur ne définit pas encore comment son premier credential est établi. Les options étudiées sont :

- mot de passe initial fourni par l'Administrateur : simple, mais connu de l'Administrateur et nécessitant un canal de transmission maîtrisé ;
- mot de passe temporaire généré : nécessite remise sécurisée, expiration et changement obligatoire ;
- invitation ou activation : meilleure séparation, mais ajoute un flux, un token temporaire et une capacité d'envoi.

**Recommandation DRAFT :** privilégier pour la V1 le mot de passe initial fourni par l'Administrateur, sous réserve de validation humaine et de la définition ultérieure du contrat d'entrée, des règles de validation et du canal de transmission. `API-CONTRACT-V1.md` et ADR-003 ne sont pas modifiés par cette recommandation.

## 17. Décisions différées

- option définitive d'initialisation des credentials ;
- durée exacte du JWT ;
- algorithme de signature exact ;
- utilisation et valeur éventuelle de `aud` ;
- valeurs CORS propres aux environnements ;
- classes, filtres et configuration Spring Security ;
- détails d'infrastructure HTTPS et de gestion des secrets.
