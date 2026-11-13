# ADR case intake

Reads reports about medicine side effects, extracts the fields listed in `catalog/field_catalog.json` with a language model, and lets a reviewer check every value against its source text.

- `backend/`: Java 21, Spring Boot 3. The extraction pipeline and the REST API.
- `frontend/`: React 19 and TypeScript. The review screen.
- `documents/`: the input reports (`.txt`).
- `catalog/`: the field catalog. Prompts, validation and the UI are all built from it.
- `fixtures/`: recorded model requests and responses.
- `out/`: the extraction result for each document, rebuilt from `fixtures/`.

Provider and model used for the committed outputs: Google Gemini, `gemini-3.5-flash-lite`.

## Requirements

- Java 21
- Node.js with npm
- Bash for `start.sh` and `replay.sh` (Git Bash or WSL on Windows)

No API key is needed for anything in this section.

## Start the backend and the UI (no key)

From the repository root:

```bash
./start.sh
```

Then open http://localhost:5173. The backend runs on http://localhost:8080 and serves the committed `out/` and `documents/`. Stop both with Ctrl+C.

Without Bash, use two terminals:

```
cd backend
mvnw.cmd spring-boot:run
```

```
cd frontend
npm ci
npm run dev
```

## Tests

Both suites run offline.

```
cd backend
./mvnw test
```

```
cd frontend
npm ci
npm test
npx tsc --noEmit
```

On Windows Command Prompt use `mvnw.cmd test` in `backend`.

The Gemini smoke test in the backend is skipped unless `GEMINI_API_KEY` is set and the system property `smoke=true` is passed, because it calls the real API.

## Rebuild out/ from the recordings (no key, no network)

```bash
./replay.sh
```

This runs the whole pipeline against `fixtures/`, `documents/` and the committed catalog, and rewrites `out/`. An optional first argument is a different catalog file:

```bash
./replay.sh path/to/other_catalog.json
```

Set `OUT_DIR=some/folder` to write somewhere other than `out/`.

## Run the pipeline with a key

1. Get a Gemini API key and put it in the environment variable `GEMINI_API_KEY`. The key is read only from the environment and is never written to `fixtures/`.
2. From `backend/`, run one document first:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.main.web-application-type=none --app.pipeline.enabled=true --app.llm.mode=record --app.pipeline.only=D01"
```

Leave out `--app.pipeline.only=D01` to process every document in `documents/`.

`app.llm.mode` takes three values:

| Mode | What it does |
| --- | --- |
| `replay` | Answers from `fixtures/`. No key, no network. This is the default. |
| `record` | Calls the model and saves each request and response to `fixtures/`. An existing fixture for the same document is overwritten. |
| `live` | Calls the model and saves nothing. |

Other settings are in `backend/src/main/resources/application.properties`: `app.llm.model`, `app.llm.requests-per-minute`, `app.llm.max-attempts`, `app.llm.first-backoff-seconds` and `app.llm.timeout-seconds`. The provider is `app.llm.provider`; only `gemini` is implemented.

Free tiers limit requests per minute and per day. Check the limit for your key before recording all documents.

## REST API

| Method and path | Returns |
| --- | --- |
| `GET /api/catalog` | The field catalog the UI renders from. |
| `GET /api/documents` | For each document: id, title, number of cases, flags. |
| `GET /api/documents/{id}` | The document text and its extraction output. |

## Notes

Decisions, the checker output and how AI tools were used are in `NOTES.md`.
