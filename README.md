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
│  │ Auth     │ │Chunking + │ │BM25* +    │ │LLM Client +      │  │
│  │ Bucket4j │ │Kafka prod │ │pgvector   │ │Citation + Guard  │  │
│  └──────────┘ └─────┬─────┘ └─────▲─────┘ └──────────────────┘  │
└───────────────────────────────────┼─────────────────────────────┘
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

Notes: the retrieval service currently performs vector search using pgvector (cosine similarity). BM25/tsvector and RRF/reciprocal-rank fusion are planned but not yet active.

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
├── ingestion/     Document upload, Apache Tika extraction, chunking strategies, Kafka producer, embedding pipeline
├── retrieval/     Vector search (pgvector cosine similarity), ScoredChunk DTO, VectorSearchRepository, SearchResultsResponse, QueryController
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

Progress updates from recent commits:

- ✅ Initial scaffold
- ✅ Document ingestion, extraction, and embedding pipeline implemented
- ✅ Vector search implemented using pgvector (cosine similarity) with:
  - ScoredChunk DTO and SearchResultsResponse
  - VectorSearchRepository (JPA + native pgvector query)
  - HybridSearchService wired to embedding client for query embeddings
  - QueryController returning ranked scored chunks on POST /query
  - VectorSearchIntegrationTest with precomputed sparse vectors
  - Testcontainers bumped to 1.20.4 and docker-java.properties added for Docker Desktop 29 compatibility

Remaining work:
- 🔲 BM25 / tsvector indexing and true hybrid fusion (RRF)
- 🔲 Reranking (cross-encoder) + reciprocal-rank fusion
- 🔲 LLM generation / RAG pipeline (prompting, citation formatting, guardrails)
- 🔲 Frontend UI polish (improve UX, show citations, session management)
- 🔲 Auth & multi-tenancy

> Note: While vector search and the ingestion/embedding pipeline are functional, the end-to-end RAG generation and reranking stages are not yet implemented. The project is no longer a pure scaffold — core retrieval and ingestion are working.

---

## Tests

- Integration tests cover the vector search path (VectorSearchIntegrationTest).
- Testcontainers version updated to 1.20.4 to address compatibility with newer Docker Desktop versions; docker-java.properties is included to pin the Docker Engine API.

---

## Planned milestones
1. ✅ Structural scaffold
2. ✅ Document ingestion pipeline
3. 🔲 Hybrid search (vector implemented; BM25/RRF/reranker pending)
4. 🔲 RAG generation pipeline (LLM integration + citation)
5. 🔲 Frontend UI polish
6. 🔲 Auth & multi-tenancy
