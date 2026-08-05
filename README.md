# LexPilot 🏛️

> **RAG-based legal & grievance rights assistant for Indian consumers**  
> Covers: Consumer Protection Act, RBI / Banking grievances, Tenant disputes

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT (Browser)                         │
│                  Next.js 15 — TypeScript / Tailwind              │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTP (REST)
┌───────────────────────────▼─────────────────────────────────────┐
│                     lexpilot-app                                 │
│              Spring Boot 3.x Modular Monolith                    │
│                                                                  │
│  ┌──────────┐ ┌───────────┐ ┌───────────┐ ┌──────────────────┐  │
│  │ gateway/ │ │ingestion/ │ │retrieval/ │ │  generation/     │  │
│  │ API Key  │ │Tika +     │ │Vector +   │ │PromptBuilder +   │  │
│  │ Auth     │ │Chunking + │ │BM25 +     │ │LLM Client +      │  │
│  │ Bucket4j │ │Kafka prod │ │RRF fusion │ │Citation + Guard  │  │
│  └──────────┘ └─────┬─────┘ └─────▲─────┘ └──────────────────┘  │
└───────────────────────────────────┼────────────────────────────-─┘
                                    │ Kafka events
          ┌─────────────────────────┘
          │
┌─────────▼─────────┐   ┌──────────────────────┐
│   Kafka (KRaft)   │   │  embedding-service    │
│   ingestion topic │   │  FastAPI / Python     │
└───────────────────┘   │  POST /embed          │
                        │  POST /rerank         │
┌───────────────────┐   └──────────────────────┘
│  Postgres 16      │
│  + pgvector ext   │
└───────────────────┘
┌───────────────────┐
│  Redis 7          │
│  (rate-limit +    │
│   session cache)  │
└───────────────────┘
```

### Services

| Service | Port | Technology |
|---|---|---|
| `lexpilot-app` | 8080 | Spring Boot 3.x / Java 21 |
| `embedding-service` | 8000 | FastAPI / Python 3.11 |
| `frontend` | 3000 | Next.js 15 |
| `postgres` | 5432 | PostgreSQL 16 + pgvector |
| `kafka` | 9092 | Confluent Kafka (KRaft) |
| `redis` | 6379 | Redis 7 |

---

## Internal Packages — lexpilot-app

```
com.lexpilot
├── gateway/       REST controllers, API key auth filter, Bucket4j rate limiting
├── ingestion/     Document upload, Apache Tika extraction, chunking strategies, Kafka producer
├── retrieval/     Hybrid search (vector + BM25/tsvector), reciprocal rank fusion, reranker client
├── generation/    Prompt construction, LLM API client, citation formatting, low-confidence guardrail
└── common/        Shared DTOs (records), exceptions, config
```

---

## Quick Start

### Prerequisites
- Docker 24+ with Compose v2
- Java 21 (for local dev)
- Node.js 20+ (for frontend local dev)
- Python 3.11+ (for embedding-service local dev)

### Run everything with Docker Compose

```bash
# From repo root
docker-compose up --build
```

| URL | What |
|---|---|
| http://localhost:3000 | Frontend — query interface |
| http://localhost:3000/documents | Frontend — document upload |
| http://localhost:8080/api/v1 | Backend REST API |
| http://localhost:8000/docs | FastAPI /embed & /rerank Swagger |

### Run services individually (local dev)

**Backend**
```bash
cd lexpilot-app
./mvnw spring-boot:run
```

**Embedding service**
```bash
cd embedding-service
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

**Frontend**
```bash
cd frontend
npm install
npm run dev
```

---

## Environment Variables

Copy `.env.example` to `.env` and fill in values before running:

| Variable | Description |
|---|---|
| `LEXPILOT_API_KEY` | Static API key for gateway auth (dev) |
| `LLM_API_KEY` | LLM provider API key |
| `LLM_BASE_URL` | LLM provider base URL |
| `POSTGRES_PASSWORD` | Postgres password |

---

## Development Status

> ⚠️ **Scaffold only.** No business logic is implemented yet. All service classes contain TODO stubs describing intended responsibility.

Planned milestones:
1. ✅ Structural scaffold
2. 🔲 Document ingestion pipeline
3. 🔲 Hybrid search + reranking
4. 🔲 RAG generation pipeline
5. 🔲 Frontend UI polish
6. 🔲 Auth & multi-tenancy
