# SecretSharingApp

A secure, API-key-authenticated REST service for sharing time-limited or view-limited secrets. Secrets are encrypted at rest using **AES-256-GCM** and can only be retrieved via a single-use access token. Built with raw Jakarta Servlets, Hibernate ORM, and PostgreSQL — no Spring, no frameworks beyond the essentials.

---

## Features

- **AES-256-GCM encryption** — secrets are encrypted before storage using a server-side master key; the plaintext never touches the database
- **Access tokens** — each secret is retrieved via a unique token; the token hash is stored, never the raw token
- **Burn-after-reading** — secrets can be configured to self-destruct after a set number of views (1–5)
- **Expiration time** — secrets can alternatively expire after a set datetime
- **API key authentication** — all write operations require an `X-API-Key` header
- **Tiered rate limiting** — FREE tier: 20 requests/min, PREMIUM tier: 300 requests/min, ADMIN tier: unlimited
- **Full audit log** — every action (create, view, burn, expire, auth failure, rate limit hit) is recorded
- **Admin panel** — dedicated endpoints to manage API keys, view audit logs, and see platform stats
- **Scheduled cleanup** — expired secrets are automatically purged every 10 minutes

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 25, Jakarta Servlet 6.1 |
| ORM | Hibernate 7 / Jakarta Persistence 3.2 |
| Validation | Hibernate Validator 9 |
| Database | PostgreSQL |
| JSON | Jackson 2.20 |
| Build | Maven (WAR packaging) |
| Utilities | Lombok |

---

## API Reference

### Public

#### `POST /api/keys/register`
Register a new FREE-tier API key.

**Request:**
```json
{ "ownerName": "john" }
```
**Response:**
```json
{ "apiKey": "abc123...", "tier": "FREE", "rateLimit": 20 }
```

---

### Secrets (requires `X-API-Key` header)

#### `POST /api/secrets`
Create a new secret. At least one of `expirationTime` or `viewCount` must be provided.

**Request:**
```json
{
  "content": "my secret password",
  "expirationTime": "2026-12-31T23:59:59",
  "viewCount": 3
}
```
**Response:**
```json
{ "accessToken": "f3a1b2c3..." }
```

#### `GET /api/secrets?accessToken=<token>`
Retrieve and decrypt a secret. Decrements `viewCount` if set; deletes the secret if it was the last view (burn).

**Response:**
```json
{
  "content": "my secret password",
  "expirationTime": "2026-12-31T23:59:59",
  "viewCount": 2
}
```

#### `GET /api/secrets/meta?accessToken=<token>`
Retrieve secret metadata only (no decryption). Useful for checking if a secret is still valid.

**Response:**
```json
{ "expiresAt": "2026-12-31T23:59:59", "viewsRemaining": 2 }
```

#### `DELETE /api/secrets?accessToken=<token>`
Delete a secret. Only the API key that created it (or an ADMIN) can delete it.

---

### Admin (requires ADMIN-tier `X-API-Key`)

#### `GET /api/admin/audit`
Returns the full audit log.

#### `GET /api/admin/keys`
Lists all API keys (id, owner, tier, active status).

#### `POST /api/admin/keys`
Creates an API key with a specific tier.

**Request:**
```json
{ "ownerName": "alice", "tier": "PREMIUM", "isActive": true }
```

#### `PUT /api/admin/keys?id=<uuid>`
Updates a key's tier or active status.

#### `DELETE /api/admin/keys?id=<uuid>`
Deletes an API key.

#### `GET /api/admin/stats`
Returns platform-wide stats.

**Response:**
```json
{ "totalSecrets": 42, "activeKeys": 10, "nonActiveKeys": 2 }
```

---

## Getting Started

### Prerequisites

- Java 25+
- PostgreSQL running on `localhost:5432`
- A servlet container (e.g. Apache Tomcat 11)
- Maven

### Database Setup

Create a PostgreSQL database:
```sql
CREATE DATABASE secret_sharing;
```

Hibernate will auto-create the schema on first startup (`schema-generation.database.action=create`).

### Configuration

Set the master encryption key as an environment variable (must be a Base64-encoded 256-bit AES key):
```bash
export SECRET_SHARING_APP_MASTER_KEY="<your-base64-encoded-32-byte-key>"
```

To generate a key:
```bash
openssl rand -base64 32
```

Update `src/main/resources/META-INF/persistence.xml` if your PostgreSQL credentials differ from the defaults (`postgres`/`postgres`).

### Build & Deploy

```bash
mvn clean package
# Deploy the generated WAR to your servlet container
cp target/SecretSharingApp-1.0-SNAPSHOT.war $TOMCAT_HOME/webapps/
```

The app will be available at `https://localhost:8443/SecretSharingApp-1.0-SNAPSHOT/api/` (HTTPS enforced via `web.xml` CONFIDENTIAL transport guarantee).

---

## Security Notes

- All traffic requires HTTPS (enforced at the servlet container level via `web.xml`)
- Secrets are encrypted with AES-256-GCM before storage; the master key never touches the database
- API keys and access tokens are stored as SHA-256 hashes only
- Auth failures and rate limit hits are recorded in the audit log

---

## Project Structure

```
src/main/java/org/pavl/secretsharingapp/
├── db/                  # JPA entities (Secret, ApiKey, AuditLog)
├── domain/              # Enums (Tier, ActionType)
├── repository/          # Data access layer (GenericRepository + specific repos)
├── util/                # CryptoUtils (AES-GCM, SHA-256, token generation), ServletUtils
├── validation/          # Custom @CombinedNotNull constraint
└── web/
    ├── Filter/          # SecurityFilter (API key auth), RateLimitFilter
    ├── admin/           # AdminAuditServlet, AdminKeysServlet, AdminStatsServlet
    ├── RegisterServlet.java
    ├── SecretServlet.java
    ├── SecretMetadataServlet.java
    └── ServletContextInitializer.java
```

---

## Author

**Giorgi Pavliashvili**  
Backend Java Developer  
[LinkedIn](https://www.linkedin.com/in/giorgi-pavliashvili-6718861b6/) · [GitHub](https://github.com/Xorxe7421)
