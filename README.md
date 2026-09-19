# LexPilot 🏛️

> **A retrieval-augmented legal and grievance-rights assistant for Indian consumers.**
>
> LexPilot helps users find relevant information about consumer protection, RBI and banking grievances, and tenant disputes—with source citations and confidence-aware responses.

> **Disclaimer:** LexPilot provides informational assistance, not legal advice. Users should consult a qualified professional for advice about their specific circumstances.

## What it does

- Ingests PDFs and other supported documents with Apache Tika.
- Splits documents into searchable chunks and generates embeddings.
- Combines vector similarity, BM25/`tsvector` search, and reciprocal-rank fusion (RRF).
- Uses a cross-encoder reranker to improve result relevance.
- Generates cited answers through an LLM integration with low-confidence guardrails.
- Provides API-key authentication, per-IP rate limiting, and tenant isolation through PostgreSQL row-level security.
- Offers a Next.js interface for document upload, ingestion status, and conversational queries.

## Architecture

```text
Browser (Next.js 15 / TypeScript)
                │ REST + BFF proxy
                ▼
      lexpilot-app (Spring Boot / Java 21)
       ├── gateway       API keys, CORS, rate limiting
       ├── ingestion     Tika extraction, chunking, Kafka events
       ├── retrieval     pgvector, BM25, hybrid RRF, reranking
       ├── generation    prompts, LLM client, citations, guardrails
       └── common        shared DTOs, configuration, exceptions
                │
       ┌────────┼─────────┬───────────────┐
       ▼        ▼         ▼               ▼
   Postgres   Kafka      Redis       embedding-service
   +pgvector  (KRaft)    cache        FastAPI / Python
```

### Services

| Service | Port | Technology | Purpose |
|---|---:|---|---|
| `frontend` | `3000` | Next.js 15 / TypeScript | Query and document-upload UI |
| `lexpilot-app` | `8080` | Spring Boot 3.x / Java 21 | Main REST API and application modules |
| `embedding-service` | `8000` | FastAPI / Python 3.11 | Embedding and cross-encoder reranking |
| `postgres` | `5432` | PostgreSQL 16 + pgvector | Application data and vector search |
| `kafka` | `9092` | Confluent Kafka 7.6 / KRaft | Asynchronous ingestion events |
| `redis` | `6379` | Redis 7 | Rate-limit and session caching |

## Repository layout

```text
.
├── lexpilot-app/       Spring Boot backend
│   └── src/.../com/lexpilot/
│       ├── gateway/     API authentication and rate limiting
│       ├── ingestion/   Upload, extraction, chunking, and indexing
│       ├── retrieval/   Vector and hybrid search
│       ├── generation/  LLM prompts, citations, and guardrails
│       └── common/      Shared DTOs and configuration
├── embedding-service/  FastAPI embedding and reranking service
├── frontend/            Next.js web application
├── scratch/             Non-deployable prototype and learning scripts
├── docker-compose.yml   Local multi-service environment
└── .env.example         Environment variable template
```

## Quick start

### Prerequisites

- Docker 24+ with Compose v2
- Java 21 and Maven (for backend development outside Docker)
- Node.js 20+ and npm (for frontend development outside Docker)
- Python 3.11+ (for embedding-service development outside Docker)

### Run the complete stack

```bash
cp .env.example .env
# Edit .env and replace development credentials as needed
docker compose up --build
```

The first embedding-service startup may take longer while the Hugging Face models are downloaded.

| URL | Description |
|---|---|
| http://localhost:3000 | Web application |
| http://localhost:3000/documents | Document upload page |
| http://localhost:8080/api/v1 | Backend REST API |
| http://localhost:8000/docs | Embedding-service Swagger UI |
| http://localhost:8080/actuator/health | Backend health check |

To stop the stack while keeping volumes:

```bash
docker compose down
```

To remove persisted database, Kafka, Redis, and upload data as well:

```bash
docker compose down -v
```

### Run services locally

**Backend**

```bash
cd lexpilot-app
./mvnw spring-boot:run
```

**Embedding service**

```bash
cd embedding-service
python -m venv .venv
source .venv/bin/activate       # Windows: .venv\\Scripts\\activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

**Frontend**

```bash
cd frontend
npm install
npm run dev
```

## Configuration

Copy `.env.example` to `.env` before using Docker Compose. The most important variables are:

| Variable | Description |
|---|---|
| `LEXPILOT_API_KEY` | API key used by the backend gateway and frontend proxy |
| `LEXPILOT_SECURITY_ENABLED` | Enables API-key authentication and rate limiting |
| `LLM_API_KEY` | API key for the configured LLM provider |
| `LLM_BASE_URL` | OpenAI-compatible LLM base URL |
| `POSTGRES_PASSWORD` | PostgreSQL password |
| `LEXPILOT_CORS_ALLOWED_ORIGINS` | Comma-separated allowed browser origins |
| `INTERNAL_API_URL` | Backend URL used by the frontend BFF proxy |

Do not commit real credentials. The values in `.env.example` are development placeholders only.

## Testing

Backend tests can be run with:

```bash
cd lexpilot-app
./mvnw test
```

The test suite includes vector-search integration coverage, API-key authentication and rate-limit tests, and Testcontainers-based infrastructure tests. Docker must be available for tests that start containers.

Frontend checks can be run from `frontend/` using the scripts defined in `package.json`.

## Project status

The core retrieval and application platform is implemented:

- ✅ Document ingestion, extraction, chunking, and embedding pipeline
- ✅ pgvector search with BM25/`tsvector` indexing and hybrid RRF fusion
- ✅ Cross-encoder reranking
- ✅ LLM-backed RAG generation with citations and low-confidence guardrails
- ✅ API-key security, rate limiting, and tenant isolation with PostgreSQL RLS
- ✅ Frontend chat, document upload, ingestion polling, citations, and responsive UX
- ✅ Docker Compose health checks, resource limits, and hardened container settings

Planned improvements include broader legal-source coverage, production observability, stronger secret-management guidance, and additional end-to-end test coverage.

## License

No license has been specified yet. See the repository settings for the current licensing status.
