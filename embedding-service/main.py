from __future__ import annotations

import os
from typing import List

from fastapi import FastAPI
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer

# ---------------------------------------------------------------------------
# Model loading — done once at module level so the first request isn't slow.
# Override via MODEL_EMBED env var if needed.
# ---------------------------------------------------------------------------
MODEL_NAME = os.getenv("MODEL_EMBED", "sentence-transformers/all-MiniLM-L6-v2")
print(f"[embedding-service] Loading model: {MODEL_NAME}")
model = SentenceTransformer(MODEL_NAME)
print(f"[embedding-service] Model loaded. Embedding dimension: {model.get_sentence_embedding_dimension()}")

app = FastAPI(
    title="LexPilot Embedding Service",
    description="Text embeddings and cross-encoder reranking.",
    version="0.1.0",
)


# ---------------------------------------------------------------------------
# DTOs
# ---------------------------------------------------------------------------
class EmbedRequest(BaseModel):
    texts: List[str] = Field(..., min_length=1, max_length=256)


class EmbedResponse(BaseModel):
    embeddings: List[List[float]]
    model: str


class RerankRequest(BaseModel):
    query: str
    candidates: List[str] = Field(..., min_length=1)


class RerankResponse(BaseModel):
    ranked_indices: List[int]
    scores: List[float]
    model: str


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------
@app.get("/health", tags=["ops"])
def health() -> dict:
    return {"status": "ok", "model": MODEL_NAME}


@app.post("/embed", response_model=EmbedResponse, tags=["embeddings"])
def embed(request: EmbedRequest) -> EmbedResponse:
    """
    Encode a batch of texts into normalised embeddings.
    Returns one 384-dim vector per input text.
    """
    vectors = model.encode(request.texts, normalize_embeddings=True).tolist()
    return EmbedResponse(embeddings=vectors, model=MODEL_NAME)


@app.post("/rerank", response_model=RerankResponse, tags=["reranking"])
def rerank(request: RerankRequest) -> RerankResponse:
    # Out of scope for this slice — will be implemented with retrieval pipeline
    raise NotImplementedError("/rerank endpoint is a stub — not yet implemented")
