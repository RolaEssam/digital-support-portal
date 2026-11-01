# Database setup (SQL Server)

The database itself is not stored in Git. The scripts in this folder build it.

## 1. Create the database

Open each file in SQL Server Management Studio (SSMS) and run them **in order**:

| # | File | What it does |
|---|------|--------------|
| 1 | `01_create_database.sql` | Creates the `TicketingSystem` database and the `portal_app` login/user. Change the password inside the file first. |
| 2 | `02_schema.sql` | Creates the `users` and `audit_event` tables, indexes and permissions. |
| 3 | `03_seed_dev_data.sql` | Adds test users. **Dev only.** |

All scripts can be run again without breaking anything.

## 2. Set the environment variables

In IntelliJ: **Run > Edit Configurations > Environment variables**, and paste this one line
(replace the two `<...>` values):

```
DB_HOST=localhost;DB_PORT=1433;DB_NAME=TicketingSystem;DB_USERNAME=portal_app;DB_PASSWORD=<your portal_app password>;DSP_AES_KEY=<your AES key>
```

| Variable | Meaning |
|----------|---------|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | SQL Server location. These are the defaults, so they can be left out for a local setup. |
| `DB_USERNAME`, `DB_PASSWORD` | The `portal_app` login from step 1. |
| `DSP_AES_KEY` | AES key used by the app. Generate your own, see below. |
| `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY` | Optional locally. If empty, the app creates a temporary key at startup, so tokens stop working after every restart. |

**Never commit real passwords or keys to Git.**

### Generate your own AES key (PowerShell)

```powershell
$b = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
[Convert]::ToBase64String($b)
```

Copy the output into `DSP_AES_KEY`.

## 3. Other requirements

- **Redis** must be running on `localhost:6379`, for example:
  ```
  docker run -d --name redis -p 6379:6379 redis
  ```
  If the container already exists, use `docker start redis`.
- Start the app with the **`local`** Spring profile.

## 4. Check that audit logging works

A request that is rejected with 403 writes a row to `audit_event`. After a wrong request in Postman, run:

```sql
SELECT TOP 20 * FROM dbo.audit_event ORDER BY id DESC;
```

## 5. Changing the database later

The app uses `ddl-auto: validate`, so it will not start if a table does not match its entity.
When you change an entity, add a **new** script (`04_...sql`, `05_...sql`) instead of editing the old ones,
so people who already have the database can just run the new file.