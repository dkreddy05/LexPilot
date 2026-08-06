# Graph Report - lexpilot  (2026-08-06)

## Corpus Check
- 58 files · ~7,518 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 202 nodes · 193 edges · 22 communities detected
- Extraction: 83% EXTRACTED · 17% INFERRED · 0% AMBIGUOUS · INFERRED: 32 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 27|Community 27]]

## God Nodes (most connected - your core abstractions)
1. `DocumentEntity` - 12 edges
2. `GlobalExceptionHandler` - 8 edges
3. `LexPilotIntegrationTest` - 8 edges
4. `DocumentChunkEntity` - 7 edges
5. `DocumentUploadService` - 7 edges
6. `RateLimitingFilter` - 5 edges
7. `ApiKeyAuthFilter` - 5 edges
8. `LexPilotException` - 4 edges
9. `DocumentController` - 4 edges
10. `OpenAiLlmClient` - 4 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities

### Community 0 - "Community 0"
Cohesion: 0.09
Nodes (5): StructureAwareChunker, DocumentChunkEntity, IngestionKafkaProducer, DocumentUploadService, TikaExtractionService

### Community 1 - "Community 1"
Cohesion: 0.12
Nodes (5): RerankerClient, OpenAiLlmClient, LlmApiClient, PromptBuilder, SecurityConfig

### Community 2 - "Community 2"
Cohesion: 0.13
Nodes (2): DocumentController, DocumentEntity

### Community 3 - "Community 3"
Cohesion: 0.19
Nodes (3): GlobalExceptionHandler, LexPilotException, RuntimeException

### Community 4 - "Community 4"
Cohesion: 0.15
Nodes (5): DocumentNotFoundException, ExtractionException, InvalidDocumentException, UpstreamServiceException, LexPilotException

### Community 5 - "Community 5"
Cohesion: 0.19
Nodes (2): CitationFormatter, LexPilotIntegrationTest

### Community 6 - "Community 6"
Cohesion: 0.18
Nodes (3): ChunkEmbeddingConsumer, DocumentChunkRepository, EmbeddingServiceClient

### Community 7 - "Community 7"
Cohesion: 0.22
Nodes (3): OncePerRequestFilter, RateLimitingFilter, ApiKeyAuthFilter

### Community 8 - "Community 8"
Cohesion: 0.29
Nodes (7): BaseModel, embed(), EmbedRequest, EmbedResponse, Encode a batch of texts into normalised embeddings.     Returns one 384-dim vect, RerankRequest, RerankResponse

### Community 10 - "Community 10"
Cohesion: 0.5
Nodes (1): QueryController

### Community 11 - "Community 11"
Cohesion: 0.5
Nodes (1): BM25SearchRepository

### Community 12 - "Community 12"
Cohesion: 0.5
Nodes (1): VectorSearchRepository

### Community 13 - "Community 13"
Cohesion: 0.5
Nodes (1): HybridSearchService

### Community 14 - "Community 14"
Cohesion: 0.67
Nodes (1): LexPilotApplication

### Community 15 - "Community 15"
Cohesion: 0.67
Nodes (1): LowConfidenceGuardrail

### Community 16 - "Community 16"
Cohesion: 0.67
Nodes (1): LlmApiClient

### Community 17 - "Community 17"
Cohesion: 0.67
Nodes (1): ChunkingStrategy

### Community 18 - "Community 18"
Cohesion: 0.67
Nodes (1): FixedSizeChunker

### Community 19 - "Community 19"
Cohesion: 0.67
Nodes (1): DocumentStatus

### Community 20 - "Community 20"
Cohesion: 0.67
Nodes (1): ReciprocalRankFusion

### Community 25 - "Community 25"
Cohesion: 1.0
Nodes (1): AppConfigRegistration

### Community 27 - "Community 27"
Cohesion: 1.0
Nodes (1): DocumentRepository

## Knowledge Gaps
- **3 isolated node(s):** `Encode a batch of texts into normalised embeddings.     Returns one 384-dim vect`, `AppConfigRegistration`, `DocumentRepository`
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Community 2`** (16 nodes): `DocumentController`, `.DocumentController()`, `.getIngestionStatus()`, `.uploadDocument()`, `DocumentEntity`, `.DocumentEntity()`, `.getErrorMessage()`, `.getFilename()`, `.getId()`, `.getStatus()`, `.getUploadedAt()`, `.setFilename()`, `.setStatus()`, `DocumentController.java`, `DocumentEntity.java`, `.getStatus()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 5`** (13 nodes): `CitationFormatter`, `.format()`, `.setContentType()`, `CitationFormatter.java`, `LexPilotIntegrationTest.java`, `LexPilotIntegrationTest`, `.configureProperties()`, `.contextLoads()`, `.createMinimalPdf()`, `.escPdf()`, `.getStatusForNonExistentDoc_shouldReturn404()`, `.uploadInvalidContentType_shouldReturn400()`, `.uploadPdf_shouldExtractAndChunk()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 10`** (4 nodes): `QueryController`, `.query()`, `.QueryController()`, `QueryController.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 11`** (4 nodes): `BM25SearchRepository.java`, `BM25SearchRepository`, `.BM25SearchRepository()`, `.findTopKByBM25()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 12`** (4 nodes): `VectorSearchRepository.java`, `VectorSearchRepository`, `.findTopKByVector()`, `.VectorSearchRepository()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 13`** (4 nodes): `HybridSearchService.java`, `HybridSearchService`, `.HybridSearchService()`, `.search()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 14`** (3 nodes): `LexPilotApplication.java`, `LexPilotApplication`, `.main()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 15`** (3 nodes): `LowConfidenceGuardrail`, `.evaluate()`, `LowConfidenceGuardrail.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 16`** (3 nodes): `LlmApiClient.java`, `LlmApiClient`, `.complete()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 17`** (3 nodes): `ChunkingStrategy`, `.chunk()`, `ChunkingStrategy.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 18`** (3 nodes): `FixedSizeChunker`, `.chunk()`, `FixedSizeChunker.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 19`** (3 nodes): `DocumentStatus`, `.DocumentStatus()`, `DocumentStatus.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 20`** (3 nodes): `ReciprocalRankFusion`, `.fuse()`, `ReciprocalRankFusion.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 25`** (2 nodes): `AppConfigRegistration`, `AppConfigRegistration.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 27`** (2 nodes): `DocumentRepository.java`, `DocumentRepository`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DocumentEntity` connect `Community 2` to `Community 0`, `Community 5`, `Community 6`?**
  _High betweenness centrality (0.073) - this node is a cross-community bridge._
- **Why does `EmbeddingServiceClient` connect `Community 6` to `Community 1`?**
  _High betweenness centrality (0.061) - this node is a cross-community bridge._
- **What connects `Encode a batch of texts into normalised embeddings.     Returns one 384-dim vect`, `AppConfigRegistration`, `DocumentRepository` to the rest of the system?**
  _3 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.09 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.12 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.13 - nodes in this community are weakly interconnected._