from __future__ import annotations

from typing import List
from fastapi import FastAPI
from pydantic import BaseModel, Field

app = FastAPI(
    title="LexPilot Embedding Service",
    description="Text embeddings and cross-encoder reranking.",
    version="0.1.0",
)

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

@app.get("/health", tags=["ops"])
def health() -> dict:
    return {"status": "ok"}

@app.post("/embed", response_model=EmbedResponse, tags=["embeddings"])
def embed(request: EmbedRequest) -> EmbedResponse:
    raise NotImplementedError("/embed endpoint is a stub")

@app.post("/rerank", response_model=RerankResponse, tags=["reranking"])
def rerank(request: RerankRequest) -> RerankResponse:
    raise NotImplementedError("/rerank endpoint is a stub")
