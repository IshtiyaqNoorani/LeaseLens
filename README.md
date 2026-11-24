# LeaseLens

Rental portfolio cash-flow and payment-behaviour analytics, built on a hand-written **Segment Tree** and **Randomized Quickselect** in Java.
CIA 3 Advanced Algorithm project: Raakin Majeed (2404537), Ishtiyaq Noorani (2408912).

> Status: **Phase 1 (project foundation).** The backend serves a health check and the frontend shell connects to it. The algorithms, data and analytics are built in Phases 2–7. See [PROGRESS.md](PROGRESS.md).

## Prerequisites
| Tool | Version | Check |
|---|---|---|
| JDK | 21 or newer | `java -version` |
| Node.js | 20.19+ or 22.12+ (developed on 24) | `node -v` |
| Maven | not needed; the Maven wrapper `./mvnw` downloads it | n/a |

## Build and test (backend)
```bash
./mvnw verify
```
Windows: `mvnw.cmd verify`

## Run
Terminal 1: Java API on http://localhost:8090
```bash
./mvnw -DskipTests package && java -jar leaselens-api/target/leaselens-api.jar
```
Terminal 2: frontend on http://localhost:5173 (forwards `/api` to the backend)
```bash
cd frontend && npm ci && npm run dev
```

### Environment variables (optional, no secrets)
| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8090` | Backend port |
| `LEASELENS_API_URL` | `http://localhost:8090` | Backend URL used by the Vite dev proxy |

## API example
```bash
curl http://localhost:8090/api/health
```
Full contract: [docs/api/API_CONTRACT.md](docs/api/API_CONTRACT.md)

## Repository layout
| Path | Contents |
|---|---|
| `leaselens-algorithms/` | Assessed algorithms. JDK only, no third-party dependencies |
| `leaselens-generator/` | Seeded synthetic data generator (Phase 3) |
| `leaselens-api/` | Spring Boot REST API |
| `leaselens-bench/` | Benchmark harness (Phase 6) |
| `frontend/` | React + Vite + Tailwind + Recharts |
| `analytics/` | Python EDA notebook (Phase 7) |
| `data/` | Sample, invalid-fixture and golden datasets |
| `docs/` | Proposal, report, API contract, demo and viva material |

## Project documents
[PROJECT_PLAN.md](PROJECT_PLAN.md) · [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md) · [DECISIONS.md](DECISIONS.md) · [PROGRESS.md](PROGRESS.md) · [CONTRIBUTIONS.md](CONTRIBUTIONS.md) · [docs/PHASE0_DISCOVERY.md](docs/PHASE0_DISCOVERY.md)

## Data
All data is synthetic and generated with a fixed seed. It contains no real tenants, properties or financial records.
