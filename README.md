# API REST sécurisée avec JWT

TP : sécuriser une API REST Spring Boot à l'aide du mécanisme **JWT (JSON Web Token)**.
L'approche est **stateless** : aucune session n'est stockée côté serveur, chaque requête porte
un jeton signé qui prouve l'identité de l'utilisateur.

## Objectifs

- Créer un endpoint d'authentification REST qui génère un token JWT.
- Mettre en place un filtre d'autorisation personnalisé.
- Comprendre comment Spring Security intègre le modèle stateless.
- Sécuriser les endpoints selon les rôles et droits.

## Technologies

| Composant | Version |
|---|---|
| Spring Boot | 3.2.5 |
| Spring Security | 6 |
| Spring Web / Spring Data JPA | 3.2.5 |
| MySQL Connector/J | runtime |
| Lombok | 1.18.x |
| jjwt (api / impl / jackson) | 0.11.5 |
| Java | 17 |
| Maven | 3.9+ |

## Notions clés

| Concept | Description |
|---|---|
| Spring Security | Framework de sécurité gérant l'authentification et l'autorisation. |
| Stateless | Chaque requête est indépendante, sans session stockée côté serveur. |
| JWT | Jeton signé numériquement contenant l'identité et les autorisations. |
| Header – Payload – Signature | Les trois parties du JWT : algorithme, données, signature. |
| Bearer Token | Jeton transmis dans le header HTTP `Authorization`. |
| Filter Chain | Suite de filtres appliqués par Spring Security à chaque requête. |

## Structure du projet

```
ma.ens.security
 ├─ config/
 │   ├─ SecurityConfig.java       # configuration globale (chain, règles, stateless)
 │   └─ DataInitializer.java      # jeu de données de test au démarrage
 ├─ jwt/
 │   ├─ JwtUtil.java              # génération / extraction / validation du token
 │   └─ JwtAuthorizationFilter.java  # filtre appliqué à chaque requête
 ├─ entities/
 │   ├─ User.java
 │   └─ Role.java
 ├─ repositories/
 │   ├─ UserRepository.java
 │   └─ RoleRepository.java
 ├─ services/
 │   └─ CustomUserDetailsService.java  # chargement des utilisateurs et autorités
 ├─ web/
 │   ├─ AuthController.java       # endpoint /api/auth/login
 │   └─ TestController.java       # endpoints protégés de démonstration
 └─ SpringJwtApiApplication.java
```

## Prérequis

- Java 17 ou 21
- Maven 3.9+
- MySQL 8 (serveur démarré sur `localhost:3306`)

## Configuration

Le fichier `src/main/resources/application.properties` :

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/security_jwt?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

jwt.secret=MySuperSecretKeyForJwtAuthentication123456
jwt.expiration=3600000
```

- `jwt.secret` doit être long et complexe (≥ 256 bits pour HS256), idéalement fourni par
  une variable d'environnement.
- `jwt.expiration` est exprimée en millisecondes (ici 1 heure).
- `ddl-auto=update` laisse Hibernate générer les tables à partir des entités.

## Démarrage

```bash
mvn spring-boot:run
```

L'API est disponible sur `http://localhost:8080`.

Au premier démarrage, `DataInitializer` crée les rôles et deux comptes de test :

| Utilisateur | Mot de passe | Rôles |
|---|---|---|
| `admin` | `1234` | `ROLE_ADMIN`, `ROLE_USER` |
| `user` | `1234` | `ROLE_USER` |

## Endpoints

| Méthode | URL | Accès | Description |
|---|---|---|---|
| POST | `/api/auth/login` | public | Authentification, renvoie un token JWT |
| GET | `/api/user/profile` | `ROLE_USER`, `ROLE_ADMIN` | Espace utilisateur |
| GET | `/api/admin/dashboard` | `ROLE_ADMIN` | Espace administrateur |

## Tests

### 1. Authentification

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"1234"}'
```

Réponse :

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "type": "Bearer"
}
```

### 2. Accès protégé

```bash
curl http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer <TOKEN>"
```

### 3. Accès interdit

Sans token ou avec un token invalide, l'API répond :

```json
{ "error": "Unauthorized", "message": "Authentication required" }
```

Avec un token valide mais des droits insuffisants (ex. `user` sur `/api/admin/dashboard`) :

```json
{ "error": "Forbidden", "message": "Access Denied" }
```

## Captures d'écran

> **À compléter.** Les captures doivent être prises après avoir lancé l'application
> (Postman : login, accès autorisé, accès refusé) puis déposées dans `docs/screenshots/`.
> Les lignes ci-dessous sont prêtes à l'emploi : supprimez le bloc de code et
> dé-commentez celles que vous souhaitez afficher.

```markdown
![Authentification - POST /api/auth/login](docs/screenshots/01-login.png)
![Accès autorisé - GET /api/user/profile](docs/screenshots/02-acces-autorise.png)
![Accès refusé - 403 Forbidden](docs/screenshots/03-acces-refuse.png)
```

## Fonctionnement du mécanisme

1. **Authentification** : `POST /api/auth/login` délègue la vérification à
   `AuthenticationManager`. En cas de succès, `JwtUtil` génère un token signé en
   HMAC-SHA256 dont le `subject` est le nom d'utilisateur.
2. **Autorisation** : `JwtAuthorizationFilter` s'exécute avant
   `UsernamePasswordAuthenticationFilter`. Il lit le header `Authorization: Bearer <token>`,
   valide la signature et l'expiration, reconstruit le contexte de sécurité, puis laisse
   la requête continuer. Si le token est absent ou invalide, la requête reste anonyme.
3. **Contrôle d'accès** : `SecurityConfig` applique les règles par URL
   (`permitAll`, `hasRole`, `hasAnyRole`) avec `SessionCreationPolicy.STATELESS`.

## Notes d'implémentation

- `JwtAuthorizationFilter` intercepte les exceptions de parsing : un token invalide n'entraîne
  pas d'erreur 500 mais un simple refus (401 / 403).
- `CustomUserDetailsService` préfixe les rôles par `ROLE_` pour que `hasRole("ADMIN")`
  fonctionne correctement.
- Un `BCryptPasswordEncoder` est déclaré : les mots de passe sont stockés hachés.

## Pistes d'approfondissement

- Refresh Tokens et rotation de clés
- OAuth2 / OpenID Connect
- Microservices sécurisés (Spring Cloud Gateway + Keycloak)
