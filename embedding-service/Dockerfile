# ---------------------------------------------------------------------------
# LexPilot — Embedding Service Dockerfile
# Base: python:3.11-slim
# ---------------------------------------------------------------------------

FROM python:3.11-slim

# Avoid interactive prompts during package install
ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

# Model names — override via docker-compose environment
ENV MODEL_EMBED=BAAI/bge-small-en-v1.5
ENV MODEL_RERANK=BAAI/bge-reranker-base

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies first (layer cache)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application source
COPY main.py .

# Create non-root user
RUN useradd -m -s /bin/bash lexpilot
USER lexpilot

EXPOSE 8000

# TODO (embedding-service): add model pre-download step here so the first
#   startup doesn't time out the Docker Compose health check.
#   Example: RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('${MODEL_EMBED}')"

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
