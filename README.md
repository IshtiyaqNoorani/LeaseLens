# LeaseLens

Rental portfolio cash-flow and payment-behaviour analytics, built on a hand-written **Segment Tree** and **Randomized Quickselect** in Java.
CIA 3 Advanced Algorithm project: Raakin Majeed (2404537), Ishtiyaq Noorani (2408912).

> Status: **Phase 6 complete.** The full application works end to end (http://localhost:5173 after starting both servers), with measured benchmarks in the Algorithm Lab. The Python EDA (Phase 7) comes next. See [PROGRESS.md](PROGRESS.md).

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

## Data
| Folder | Contents |
|---|---|
| `data/sample/` | Default synthetic portfolio (seed 20261124): 50 properties, 248 leases, 3,980 obligations, 1,096 days |
| `data/golden/` | Tiny hand-checkable dataset; expected values in `EXPECTED.md` |
| `data/invalid/` | 23 datasets, each with one deliberate defect, for the error-handling demo |
| `data/GENERATOR_SPEC.md` | Generator assumptions, planted patterns and limitations |

Regenerate (produces identical files):
```bash
./mvnw -q -DskipTests package
java -jar leaselens-generator/target/leaselens-generator.jar sample
java -jar leaselens-generator/target/leaselens-generator.jar fixtures
```
Other seeds or sizes: `sample --seed 7 --properties 200 --months 60 --out /tmp/big`.

## Benchmarks
```bash
./mvnw -q -DskipTests package
java -Xms1g -Xmx1g -jar leaselens-bench/target/leaselens-bench-cli.jar --suite all --sizes 1000,10000,100000 --reps 20 --warmup 10 --seed 42 --seeds 1000
```
Writes `benchmarks/results/*.csv` in about 30 s. Method, results and findings: [benchmarks/BENCHMARKS.md](benchmarks/BENCHMARKS.md).

## API examples
The sample dataset is loaded automatically when the backend starts.
```bash
curl http://localhost:8090/api/portfolio/overview
curl "http://localhost:8090/api/properties/performance?sort=collectionEfficiencyPct&order=asc"
curl "http://localhost:8090/api/cashflow/range?scope=portfolio&from=2025-04-01&to=2026-03-31"
curl "http://localhost:8090/api/payments/percentiles?p=50,75,90&propertyType=RETAIL&seed=1"
curl -X POST http://localhost:8090/api/cashflow/adjustments -H "Content-Type: application/json" -d '{"propertyId":"P012","date":"2025-07-14","amountPaise":150000,"reason":"Reversed duplicate utility charge"}'
curl -X POST "http://localhost:8090/api/datasets/sample?name=golden"
```
Upload your own six CSV files:
```bash
curl -F files=@properties.csv -F files=@tenants.csv -F files=@leases.csv -F files=@rent_obligations.csv -F files=@rent_transactions.csv -F files=@expenses.csv http://localhost:8090/api/datasets/upload
```
Full contract: [docs/api/API_CONTRACT.md](docs/api/API_CONTRACT.md)

## Repository layout
| Path | Contents |
|---|---|
| `leaselens-algorithms/` | Assessed algorithms. JDK only, no third-party dependencies |
| `leaselens-data/` | Domain model, CSV parser, validation rules, dataset loader |
| `leaselens-generator/` | Seeded synthetic data generator and invalid-fixture builder |
| `leaselens-api/` | Spring Boot REST API |
| `leaselens-bench/` | Benchmark harness, baselines and suites |
| `benchmarks/` | Committed benchmark results (CSV) and BENCHMARKS.md |
| `frontend/` | React + Vite + Tailwind + Recharts |
| `analytics/` | Python EDA notebook (Phase 7) |
| `data/` | Sample, invalid-fixture and golden datasets |
| `docs/` | Proposal, report, API contract, demo and viva material |

## Project documents
[PROJECT_PLAN.md](PROJECT_PLAN.md) · [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md) · [DECISIONS.md](DECISIONS.md) · [PROGRESS.md](PROGRESS.md) · [CONTRIBUTIONS.md](CONTRIBUTIONS.md) · [docs/PHASE0_DISCOVERY.md](docs/PHASE0_DISCOVERY.md)

## Data
All data is synthetic and generated with a fixed seed. It contains no real tenants, properties or financial records.
