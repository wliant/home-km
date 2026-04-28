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

## PR checklist

- [ ] Backend tests pass: `cd backend && ./gradlew test`
- [ ] Frontend type-check + tests pass: `cd frontend && npm run typecheck && npm test`
- [ ] You ran the relevant feature in a browser (or `e2e/`)
- [ ] You wrote a Conventional Commit message
- [ ] Migrations are additive and named `Vxxx__description.sql`

## Local setup

See `README.md` and `CLAUDE.md`.
