# Contributing

Thanks for your interest in improving this project.

## Development workflow

- Create a branch from `module_one_sprint_three_branch`.
- Keep commits small and focused.
- Use clear commit messages in imperative style (e.g. `Fix ...`, `Add ...`).
- Open a PR to `main` when changes are ready.

## Local checks before PR

```bash
mvn -B verify
docker compose up --build
```

Then verify:

- API health endpoint (`/api/health`)
- core post/comment flows (create/update/delete)
- image upload flow

## Coding conventions

- Keep Spring application layered: `controller -> service -> repository`.
- Prefer small targeted patches over broad rewrites.
- Keep compatibility with Practicum frontend contract.
- Do not commit secrets (`.env`, credentials, local dumps).

## Pull request checklist

- [ ] Feature/bug scope is clear
- [ ] Tests pass locally
- [ ] README updated (if behavior/setup changed)
- [ ] No accidental debug/temp files committed
