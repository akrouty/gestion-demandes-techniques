# ADR-005 — Sécurité V1

## Statut

**VALIDATED**

## 1. Contexte

L'API REST V1 est définie et validée. Elle doit être protégée pour une application interne utilisant trois rôles applicatifs, des permissions cumulables et des contrôles contextuels sur les demandes. L'architecture retient Spring Security, JWT, RBAC et des contrôles métier côté backend, sans implémentation existante à ce stade.

Cet ADR formalise la conception de sécurité V1 validée. Le détail opérationnel est décrit dans `docs/security/SECURITY-DESIGN-V1.md`.

## 2. Menaces et besoins

La V1 doit notamment prévenir :

- l'accès à l'API sans identité valide ;
- l'usage d'un token falsifié, expiré ou émis avec des paramètres inattendus ;
- l'accès après désactivation d'un compte ou retrait d'un rôle ;
- le contournement du RBAC ou des contrôles métier contextuels ;
- l'exposition des mots de passe, tokens, secrets ou détails internes ;
- les appels provenant d'origines frontend non autorisées.

La sécurité doit rester proportionnée à une application interne et ne pas introduire de mécanismes sans besoin établi.

## 3. Authentification

La V1 retient l'opération suivante :

```text
POST /api/v1/auth/login
```

Elle reçoit `email` et `password`. Le backend normalise l'email conformément à ADR-003, recherche l'utilisateur, vérifie que le compte existe et est actif, puis vérifie le mot de passe au moyen de Spring Security `PasswordEncoder`. Une authentification réussie retourne le JWT d'accès (`accessToken`), son type (`tokenType` : `Bearer`), son instant d'expiration (`expiresAt`, instant ISO-8601 correspondant à `exp`) et un snapshot minimal `user` destiné uniquement à l'UX cliente (`id`, `nom`, `email`, `roles`), conformément à `API-CONTRACT-V1.md`.

Ce snapshot représente l'utilisateur authentifié au moment du login et n'est jamais utilisé comme source d'autorisation. L'existence du compte, son état actif et ses rôles actuels sont toujours relus depuis `identity / administration` lors de chaque requête protégée ; le backend applique le RBAC puis les contrôles métier contextuels. Les rôles du snapshot ne deviennent pas des claims JWT obligatoires.

Un échec retourne un message générique identique pour un utilisateur inconnu, un mot de passe incorrect ou un compte inactif. L'API ne révèle ni l'existence ni l'état d'un compte.

L'API est stateless : aucune `HttpSession` ne sert de mécanisme d'authentification et chaque requête protégée présente son JWT.

## 4. Mots de passe

Un mot de passe n'est jamais stocké en clair, chiffré de manière réversible, journalisé ou retourné par l'API. La vérification utilise `PasswordEncoder`.

BCrypt est retenu pour la V1, car il est directement pris en charge par Spring Security et répond au besoin sans dépendance ni mécanisme supplémentaire. Son facteur de coût n'est pas fixé dans la documentation : il devra être mesuré sur l'environnement cible avant l'implémentation afin d'obtenir un coût de vérification raisonnable.

Argon2 offre des propriétés de résistance mémoire intéressantes, mais ajoute des paramètres et une complexité qui ne répondent pas actuellement à un besoin identifié. Il reste une alternative future si les contraintes de sécurité évoluent.

## 5. JWT

Le JWT est un token d'accès de durée limitée. Sa durée exacte reste configurable et devra être fixée puis testée avant l'implémentation.

Claims minimaux proposés :

- `sub` : identifiant stable de l'utilisateur ;
- `iat` : instant d'émission ;
- `exp` : expiration ;
- `iss` : émetteur attendu ;
- `aud` uniquement si une audience explicite est retenue et justifiée.

Le token ne contient aucun mot de passe, donnée sensible inutile ou information métier complète. La validation contrôle au minimum la signature, l'algorithme attendu, l'expiration et l'émetteur, ainsi que l'audience lorsqu'elle est utilisée. L'algorithme déclaré par le token n'est jamais accepté dynamiquement sans comparaison avec l'algorithme configuré côté serveur.

Le JWT identifie l'utilisateur. La stratégie privilégiée pour la V1 consiste à relire depuis le module `identity / administration` l'état actif et les rôles courants lors de l'authentification de chaque requête. Une désactivation ou un retrait de rôle prend ainsi effet sans attendre l'expiration d'un token déjà émis. L'accès supplémentaire à Identity ou à la base est accepté pour cette application interne de taille limitée. Si des rôles figurent également dans le JWT, ils ne constituent jamais la seule source d'autorité.

## 6. RBAC et autorisation contextuelle

Trois contrôles restent distincts :

1. authentification : établir l'identité ;
2. RBAC : vérifier le rôle requis par l'opération ;
3. autorisation métier contextuelle : vérifier le droit d'agir sur la ressource ciblée.

Ainsi, traiter une demande exige à la fois le rôle `AGENT_TECHNIQUE` et l'identité de l'Agent réellement affecté. Le rôle `ADMINISTRATEUR` seul ne donne aucun droit métier sur les demandes. Un utilisateur multi-rôles bénéficie de l'union des permissions associées à ses rôles.

## 7. Gestion des comptes actifs et rôles

Un compte avec `actif = false` est refusé au login et lors des requêtes protégées. Les autorisations utilisent les rôles actuels relus depuis Identity.

L'opération de gestion des rôles métier reste limitée à `RESPONSABLE_TECHNIQUE` et `AGENT_TECHNIQUE`. Elle ne peut ni attribuer ni retirer `ADMINISTRATEUR` et conserve ce rôle s'il existe déjà, conformément au contrat API validé.

## 8. Gestion des tokens côté SPA

La V1 conserve le JWT d'accès uniquement en mémoire dans la SPA Angular et l'envoie dans l'en-tête :

```text
Authorization: Bearer <token>
```

Le token n'est pas conservé dans `localStorage` ni `sessionStorage`. Un rechargement complet de la SPA peut donc nécessiter une nouvelle authentification. Ce compromis est accepté provisoirement afin d'éviter un stockage navigateur durablement accessible à JavaScript.

Aucun refresh token, mécanisme de rotation, blacklist, Redis ou OAuth2/OIDC n'est introduit en V1 sans besoin supplémentaire. Dans ce modèle stateless, la déconnexion consiste principalement à supprimer le token en mémoire côté Angular. Aucun endpoint backend de logout sans effet réel n'est ajouté.

## 9. Secrets

Le secret ou la clé de signature JWT n'est jamais codé en dur, commité dans Git ou placé dans un fichier `application.properties` versionné. Il provient d'une configuration externe, d'une variable d'environnement ou d'un mécanisme de secret adapté à l'environnement.

Deux approches sont considérées :

- HMAC avec un secret cryptographiquement fort et correctement protégé ;
- signature asymétrique avec séparation des clés de signature et de vérification.

Pour un monolithe V1 qui émet et valide lui-même ses tokens, HMAC est la recommandation simple. L'algorithme exact ne sera fixé qu'après justification et devra être imposé explicitement lors de la validation.

## 10. CORS et CSRF

CORS est configuré explicitement pour les seules origines frontend autorisées, avec uniquement les méthodes et en-têtes nécessaires. Aucun wildcard incontrôlé n'est retenu. La configuration peut différer selon l'environnement ; l'origine exacte de production reste une décision de déploiement.

Le JWT retenu est envoyé explicitement dans l'en-tête `Authorization` et aucun cookie d'authentification n'est envoyé automatiquement par le navigateur. Le risque CSRF classique fondé sur l'envoi automatique de credentials est donc différent. La configuration Spring Security peut ne pas appliquer une protection CSRF conçue pour une authentification par cookie, à condition de rester strictement cohérente avec cette architecture stateless par en-tête. Si l'authentification migre vers des cookies, la protection CSRF devra être réévaluée avant ce changement.

Les credentials et JWT sont transmis uniquement via HTTPS dans tout environnement réel. Cet ADR ne décide aucune infrastructure de certificat, proxy ou déploiement.

## 11. Erreurs

Les erreurs respectent le contrat API validé :

- `401 Unauthorized` pour une authentification absente ou invalide ;
- `403 Forbidden` pour un utilisateur authentifié mais non autorisé.

Les réponses ne révèlent ni l'existence d'un utilisateur, ni le détail d'échec de validation du JWT, ni une stack trace, un secret ou une exception interne.

## 12. Alternatives et compromis

- Relire l'état actif et les rôles à chaque requête ajoute un accès à Identity ou à la base, mais rend les désactivations et retraits de rôle immédiatement effectifs.
- Mettre les rôles uniquement dans le JWT éviterait cet accès, mais conserverait des permissions obsolètes jusqu'à l'expiration du token ; cette option n'est pas privilégiée.
- Conserver le JWT uniquement en mémoire réduit sa persistance côté navigateur, mais une actualisation complète peut imposer une nouvelle connexion.
- Un refresh token améliorerait la continuité de session, mais introduirait stockage, rotation et révocation sans besoin validé ; il n'est pas retenu.
- La signature asymétrique facilite la séparation entre émetteur et vérificateurs, mais cette séparation n'est pas nécessaire dans le monolithe V1 ; HMAC est recommandé sous réserve d'une gestion robuste du secret.

## 13. Risques

- Un secret faible ou exposé compromettrait tous les tokens signés avec ce secret.
- Un facteur BCrypt trop faible réduirait la protection des mots de passe ; un facteur trop élevé pourrait dégrader fortement le login.
- Un mauvais paramétrage CORS pourrait autoriser une origine indésirable ou bloquer le frontend légitime.
- Une évolution vers des cookies sans réévaluation CSRF créerait une incohérence de sécurité.
- La relecture de l'utilisateur à chaque requête augmente la charge de la persistance et doit être mesurée.
- La conservation en mémoire du token réduit la continuité de session après rechargement de la SPA.

## 14. Décisions différées

Restent à valider ou définir :

- la durée exacte du JWT ;
- l'algorithme de signature exact et la forme précise du secret ou des clés ;
- l'utilisation éventuelle de `aud` et sa valeur ;
- les noms exacts des propriétés de configuration ;
- les origines CORS propres à chaque environnement ;
- les classes et la configuration Spring Security exactes.

## 15. Initialisation des credentials

La V1 retient le mot de passe initial fourni par l'Administrateur lors de `POST /api/v1/utilisateurs`.

Le backend :

- reçoit ce mot de passe uniquement comme donnée d'entrée lors de la création ;
- vérifie sa présence et sa validité ;
- le hache immédiatement au moyen de `PasswordEncoder` ;
- persiste uniquement son hash ;
- ne le stocke jamais en clair ;
- ne le journalise jamais ;
- ne le retourne jamais dans une réponse.

Le mot de passe haché est une donnée technique de sécurité et ne devient pas un attribut du modèle métier UML. La politique exacte de longueur et de complexité devra être définie avant l'implémentation sans valeur arbitraire dans cet ADR.
