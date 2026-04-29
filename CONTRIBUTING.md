# Contributing

## Commit messages — Conventional Commits

We use [Conventional Commits](https://www.conventionalcommits.org/) so [release-please](https://github.com/googleapis/release-please) can generate `CHANGELOG.md` and tag versions automatically. Commit messages drive the next release notes.

Format:
```
<type>(<scope>): <subject>

<body>

<footer>
```

| Type | Semver bump | When |
|------|-------------|------|
| `feat` | minor | a new user-visible feature |
| `fix` | patch | a bug fix |
| `perf` | patch | performance improvement |
| `refactor` | (none) | code change without external effect |
| `docs` | (none) | documentation only |
| `test` | (none) | tests only |
| `chore` | (none) | tooling, deps, infra |
| `BREAKING CHANGE:` in footer | major | any incompatible change |

Examples:
```
feat(notes): inline image attachments via paste
fix(auth): refresh token TTL was using days regardless of remember-me
docs: add restore drill procedure
```

## Architecture Decision Records

Material architectural changes go alongside an ADR under `specs/adr/` — see `specs/adr/README.md` for the "when" matrix and the template. The PR that lands the change should land the ADR (status `Accepted`); the bar is "would a future contributor benefit from knowing why we did this".

## Pre-commit hooks

`lefthook` runs lightweight checks on staged files before each commit. Install once:

```bash
cd frontend && npm install     # pulls in lefthook
npx lefthook install           # registers .git/hooks/pre-commit + commit-msg
```

What it enforces:
- ESLint + auto-fix on staged frontend `.ts/.tsx/.js/.jsx`
- No trailing whitespace or CRLF in staged Java files
- No staged file larger than 1 MB (intentional baselines bypass with `git commit --no-verify`)
- Commit subject follows Conventional Commits

CI re-runs these checks, so a `--no-verify` bypass is local-only — the PR still needs to pass.

## PR checklist

- [ ] Backend tests pass: `cd backend && ./gradlew test`
- [ ] Frontend type-check + tests pass: `cd frontend && npm run typecheck && npm test`
- [ ] You ran the relevant feature in a browser (or `e2e/`)
- [ ] You wrote a Conventional Commit message
- [ ] Migrations are additive and named `Vxxx__description.sql`

## Local setup

See `README.md` and `CLAUDE.md`.
