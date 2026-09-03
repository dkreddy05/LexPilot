from __future__ import annotations

import os
from typing import List, Optional

from fastapi import FastAPI
from pydantic import BaseModel, Field
from sentence_transformers import CrossEncoder, SentenceTransformer

# ---------------------------------------------------------------------------
# Model loading — embedding model loaded once at module level.
# Reranker model loaded lazily or via get_reranker() with configurable model.
# ---------------------------------------------------------------------------
MODEL_NAME = os.getenv("MODEL_EMBED", "sentence-transformers/all-MiniLM-L6-v2")
MODEL_RERANK_NAME = os.getenv("MODEL_RERANK", "BAAI/bge-reranker-base")

print(f"[embedding-service] Loading embedding model: {MODEL_NAME}")
model = SentenceTransformer(MODEL_NAME)
print(f"[embedding-service] Model loaded. Embedding dimension: {model.get_sentence_embedding_dimension()}")

reranker: Optional[CrossEncoder] = None


def get_reranker() -> CrossEncoder:
    global reranker
    if reranker is None:
        print(f"[embedding-service] Loading reranker model: {MODEL_RERANK_NAME}")
        reranker = CrossEncoder(MODEL_RERANK_NAME)
        print("[embedding-service] Reranker model loaded successfully.")
    return reranker


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
    return {
        "status": "ok",
        "model_embed": MODEL_NAME,
        "model_rerank": MODEL_RERANK_NAME,
    }


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
    """
    Rerank candidate passages against a natural language query using a Cross-Encoder.
    Returns ranked indices (0-based) and scores sorted in descending order.
    """
    if not request.candidates:
        return RerankResponse(ranked_indices=[], scores=[], model=MODEL_RERANK_NAME)

    cross_encoder = get_reranker()
    pairs = [[request.query, candidate] for candidate in request.candidates]
    raw_scores = cross_encoder.predict(pairs)

    if hasattr(raw_scores, "tolist"):
        scores_list = raw_scores.tolist()
    else:
        scores_list = list(raw_scores)

    if not isinstance(scores_list, list):
        scores_list = [float(scores_list)]
    else:
        scores_list = [float(s) for s in scores_list]

    indexed_scores = sorted(
        enumerate(scores_list),
        key=lambda item: item[1],
        reverse=True,
    )

    ranked_indices = [item[0] for item in indexed_scores]
    sorted_scores = [item[1] for item in indexed_scores]

    return RerankResponse(
        ranked_indices=ranked_indices,
        scores=sorted_scores,
        model=MODEL_RERANK_NAME,
    )
