# Contributing

WatchNest keeps module history clean so modules can be separated later if needed.

## Commit Boundaries

Each commit must affect exactly one module.

- `root` is a module for top-level project infrastructure.
- A module commit may include that module's code, tests, resources, version file, changelog, and local docs.
- Cross-module work must be split into separate commits.
- Product release changes belong in a dedicated commit named
  `release(X.Y.Z): …` (see Commit Messages). Files: `productVersion`,
  `RELEASES.md`, top-level `README.md`.

## Commit Messages

Use Conventional Commits with the module name as scope:

```text
type(module): message
```

If a commit introduces or changes a module version, include the new version in the scope:

```text
type(module X.Y.Z): message
```

Versioned scopes are required for module version changes. Commits without a version in the scope are allowed only when the commit does not change any version value.

Product releases use a custom form (not `chore(root)`):

```text
release(X.Y.Z): message
```

Examples:

```text
chore(root 0.1.0): initialize project structure
chore(root 0.2.0): bump root version
docs(root): add development conventions
feat(auth 0.1.0): initialize auth module
fix(auth 0.1.1): reject expired refresh tokens
release(0.2.0): add browser auth and per-user libraries
```

Allowed types: `feat`, `fix`, `docs`, `test`, `refactor`, `build`, `ci`, `chore`,
and `release` only as `release(X.Y.Z): …` for product cuts.

A commit body is welcome whenever the subject alone does not explain the purpose or context.
It is required for foundational, release, versioning, convention, or other decision-heavy changes.

```text
type(module X.Y.Z): concise subject

Optionally explain the purpose of the change in 1-3 short sentences.
Mention the main project/module decisions, not every changed file.
```

Initial root commit example:

```text
chore(root 0.1.0): initialize project structure

Set up the root module for a future multi-module project.
Add Gradle wrapper/configuration, project documentation, versioning and release
conventions, Cursor project rules, and Apache-2.0 licensing.
```

Do not use multiple module scopes in one commit. Split this:

```text
feat(auth, frontend): add login flow
```

into:

```text
feat(auth): add login endpoint
feat(frontend): add login form
```

## Versioning

- Product releases use SemVer and are tracked in `RELEASES.md`.
- Each module owns its SemVer version and changelog.
- Version bumps must be committed with the matching module changelog entry.
- Top-level `README.md` is the project/product overview. Update it only when
  cutting a product release (`productVersion` + `RELEASES.md`), not in ordinary
  module or `root` infra commits.

## Module docs

- Each feature module keeps a `README.md` for that module only: purpose,
  responsibilities, public surface, build/run/test, scope.
- Cross-module wiring and dependency topology belong in `ROOT_README.md`
  (developer/root docs). The product overview `README.md` is updated with the
  product release, not with every module change.
- Prefer a technical tone: short declarative sentences, tables, lists, code
  identifiers. Avoid literary or essay-style prose.
- See `.cursor/rules/module-readme.mdc`.

## Documentation language

Committed project text is English (READMEs, changelogs, `RELEASES.md`,
`BACKLOG.md`, comments, commit messages, Cursor rules).

**Exception:** `### Цель` in the top-level product `README.md` — that heading
and the Russian paragraphs under it until the next heading. Do not copy this
exception into module docs or other README sections.

See `.cursor/rules/doc-language.mdc`.

## Secrets

- Do not commit real secrets, credentials, tokens, private keys, or production connection strings.
- Local secrets belong in ignored files such as `.env`, `.env.*`, or files under `secrets/`.
- Safe templates belong in `config/examples/` and must contain placeholders only.
- Module-specific config examples should be added only after the corresponding module exists.
