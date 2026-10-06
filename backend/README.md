# Backend

- Java 21
- Spring Boot 4.1.1 (Maven Wrapper inclus)

## Base de données (PostgreSQL, variables d'environnement)

| Variable | Défaut local |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/gestion_demandes` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | aucune valeur par défaut (à définir) |

## Sécurité (variables d'environnement obligatoires, aucun secret versionné)

| Variable | Rôle |
|---|---|
| `JWT_SECRET` | Secret HMAC (≥ 32 octets pour HS256) ; jamais commité, jamais journalisé. |
| `JWT_ISSUER` | Valeur du claim `iss`. |
| `JWT_TTL` | Durée de vie du token, format Duration (ex. `PT15M`). |
| `JWT_ALGORITHM` | `HS256`, `HS384` ou `HS512` (épinglage serveur, `alg` du token jamais suivi). |
| `PASSWORD_MIN_LENGTH` | Longueur minimale du mot de passe initial (> 0). |
| `BCRYPT_STRENGTH` | Facteur de coût BCrypt (4–31), à mesurer sur l'environnement cible. |
| `CORS_ALLOWED_ORIGINS` | Origines autorisées, séparées par des virgules ; vide = aucune origine. |

Une valeur critique absente ou invalide provoque un échec explicite de
démarrage : aucun repli ni secret codé en dur n'existe.

## Commandes (depuis `backend/`)

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Les tests automatiques s'exécutent sur un H2 de test isolé (avec leurs
propres paramètres de sécurité TEST dans `src/test/resources`) ; ils ne
remplacent pas une validation PostgreSQL réelle.
