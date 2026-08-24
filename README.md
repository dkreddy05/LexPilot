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
- ✅ API key auth & per-IP rate limiting (opt-in via `lexpilot.security.enabled=true`; enabled by default in Docker Compose)
- ✅ Frontend chat flow fully wired:
  - TanStack Query (`QueryProvider`) mounted in layout, `useQueryDocuments` mutation calls `POST /api/v1/query/answer`
  - Chat messages managed via Zustand (`useQueryStore`) — not dead stores, active source of truth
  - `ChatMessage` renders `CitationsExpander` with expandable source markers and low-confidence warnings
  - Document upload + ingestion polling (`useUploadDocument`, `useIngestionStatus`, `useDocumentStore`) wired in sidebar and `/documents` page

Remaining work:
- 🔲 BM25 / tsvector indexing and true hybrid fusion (RRF)
- 🔲 Reranking (cross-encoder) + reciprocal-rank fusion
- 🔲 LLM generation / RAG pipeline (prompting, citation formatting, guardrails)
- 🔲 Frontend UI polish (improve UX, show citations, session management)
- 🔲 Docker Compose hardening (health checks, externalized secrets, pinned versions)
- 🔲 Multi-tenancy

> Note: While vector search and the ingestion/embedding pipeline are functional, the end-to-end RAG generation and reranking stages are not yet implemented. API key authentication and per-IP rate limiting are fully implemented and tested but gated behind a feature flag (`lexpilot.security.enabled`) — disabled for local dev, enabled in Docker Compose. The frontend chat UI is fully wired to the backend query/answer endpoint with citation rendering.

---

## Tests

- Integration tests cover the vector search path (VectorSearchIntegrationTest).
- Security filter chain tests cover API key auth (401 for missing/wrong key), rate limiting (429), and public endpoint bypass (SecurityConfigTest).
- Testcontainers version updated to 1.20.4 to address compatibility with newer Docker Desktop versions; docker-java.properties is included to pin the Docker Engine API.

---

## Planned milestones
1. ✅ Structural scaffold
2. ✅ Document ingestion pipeline
3. 🔲 Hybrid search (vector implemented; BM25/RRF/reranker pending)
4. 🔲 RAG generation pipeline (LLM integration + citation)
5. ✅ API key auth & rate limiting (opt-in via feature flag)
6. ✅ Frontend chat flow wired (query → cited answer → expandable citations)
7. 🔲 Docker Compose hardening & deployment
8. 🔲 Multi-tenancy
