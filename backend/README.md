# Backend

- Java 21
- Spring Boot 4.1.1 (Maven Wrapper inclus)

## Base de données (PostgreSQL, variables d'environnement)

| Variable | Défaut local |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/gestion_demandes` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | aucune valeur par défaut (à définir) |

## Commandes (depuis `backend/`)

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Les tests automatiques s'exécutent sur un H2 de test isolé ; ils ne remplacent pas une validation PostgreSQL réelle.
