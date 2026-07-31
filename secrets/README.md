# Local Secrets

This directory is reserved for local-only secrets.

Do not commit real credentials, tokens, private keys, database passwords, JWT
secrets, or production connection strings.

Only this README is intended to be tracked by git. Everything else under
`secrets/` is gitignored. Use `config/examples/` for safe templates that
contain variable names without secret values.
