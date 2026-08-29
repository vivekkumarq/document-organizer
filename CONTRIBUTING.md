# Contributing

Thanks for taking an interest in the project. This file describes how to get a
working checkout, what the review bar is, and how to get a change merged.

## Getting set up

See [Getting started](README.md#getting-started) in the README for prerequisites
and the two ways to run the stack. The short version:

```bash
git clone https://github.com/vivekkumarq/document-organizer.git
cd document-organizer
cp .env.example .env          # then set JWT_SECRET

cd docorganizer && ./mvnw -B clean verify     # backend build + tests
cd ../docorganizer-ui && npm install && npm run lint && npm run build
```

The backend tests use an in-memory H2 database through the `test` profile, so
you do not need PostgreSQL running to work on the backend.

## Before you open a pull request

Both of these must pass. CI runs exactly the same commands, so a green local run
is a good predictor.

```bash
cd docorganizer     && ./mvnw -B clean verify
cd docorganizer-ui  && npm run lint && npm run build
```

Do not skip tests to get a green build. If a test is wrong, fix or delete it
deliberately and say so in the pull request.

## What a good change looks like

- **Tests.** Anything touching the service or controller layer needs a test. New
  endpoints need one for the happy path and one for the owner-scoping check.
- **Owner scoping is not optional.** Every read and write of a document must be
  constrained by the user id resolved from the verified JWT, never by an id
  taken from a path variable, query parameter or request body. If you add a
  repository method that loads documents, it should take a `userId`.
- **No secrets or user data in the repository.** Uploaded files belong in
  `storage/`, which is ignored. Configuration belongs in environment variables
  with a documented default, not in a literal.
- **Keep the README honest.** If you add an endpoint, add it to the API table.
  If you change a default, update the configuration table. Do not document
  anything that does not work.

## Style

Match the surrounding code rather than introducing a new convention.

- **Java** — four-space indent, constructor injection, no field injection.
  Package-private test classes. Domain exceptions from
  `com.vivek.docorganizer.exception` rather than bare `RuntimeException`, so the
  global handler can map them to the right status.
- **React** — function components with hooks, two-space indent, double quotes.
  Plain CSS in `src/styles.css` using the existing custom properties; no CSS
  framework and no styling libraries.
- **Commits** — conventional-commit prefixes (`feat:`, `fix:`, `chore:`,
  `docs:`, `test:`, `build:`) and a body that explains why, not just what.

## Reporting a security issue

Please do not open a public issue for a vulnerability. Open a GitHub security
advisory on the repository instead, with steps to reproduce.
