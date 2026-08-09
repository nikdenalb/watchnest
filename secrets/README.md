# Local Secrets

This directory is reserved for local-only notes and credentials.

Do not commit real credentials, tokens, private keys, database passwords, JWT
secrets, or production connection strings.

Only this README is tracked by git. Everything else under `secrets/` is
gitignored.

## Where things live

| What | Where | Git |
| --- | --- | --- |
| Runtime env for `planner-app` (`SPRING_DATASOURCE_*`, …) | `.env.planner-app` at repo root | ignored (`.env.*`) |
| Optional local notes (install paths, playbooks, decisions) | any file under `secrets/` except this README | ignored |
| Safe templates (placeholders only) | `config/examples/` | tracked |

The API (`planner-app`) owns the application database connection (JPA /
Liquibase). Domain libraries (`identity`, `planner`) do not open a DataSource.

Real passwords may live in an external password manager instead of project
files. If a real secret is written into any project path, follow the dual-check
in `.cursor/rules/secret-handling.mdc`.
