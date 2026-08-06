# ---------------------------------------------------------------------------
# LexPilot — Embedding Service Dockerfile
# Base: python:3.11-slim
# ---------------------------------------------------------------------------

FROM python:3.11-slim

# Avoid interactive prompts during package install
ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

# Model name — override via docker-compose environment
ENV MODEL_EMBED=sentence-transformers/all-MiniLM-L6-v2

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

# Pre-download the model so the first startup doesn't timeout the healthcheck.
# This runs as root before switching to non-root user (needs write access to cache).
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('${MODEL_EMBED}')"

# Create non-root user
RUN useradd -m -s /bin/bash lexpilot
USER lexpilot

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
