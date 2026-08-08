# Graph Report - lexpilot  (2026-08-08)

## Corpus Check
- 71 files · ~11,559 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 258 nodes · 287 edges · 22 communities detected
- Extraction: 76% EXTRACTED · 24% INFERRED · 0% AMBIGUOUS · INFERRED: 68 edges (avg confidence: 0.8)
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
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
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
1. `VectorSearchIntegrationTest` - 14 edges
2. `DocumentEntity` - 12 edges
3. `CitationFormatterTest` - 9 edges
4. `GlobalExceptionHandler` - 8 edges
5. `LexPilotIntegrationTest` - 8 edges
6. `DocumentChunkEntity` - 7 edges
7. `DocumentUploadService` - 7 edges
8. `RateLimitingFilter` - 5 edges
9. `ApiKeyAuthFilter` - 5 edges
10. `VectorSearchRepository` - 5 edges

## Surprising Connections (you probably didn't know these)
- `NoOpLowConfidenceGuardrail` --implements--> `LowConfidenceGuardrail`  [EXTRACTED]
  lexpilot-app\src\main\java\com\lexpilot\generation\guardrail\NoOpLowConfidenceGuardrail.java →   _Bridges community 2 → community 8_

## Communities

### Community 0 - "Community 0"
Cohesion: 0.09
Nodes (5): StructureAwareChunker, DocumentChunkEntity, IngestionKafkaProducer, DocumentUploadService, TikaExtractionService

### Community 1 - "Community 1"
Cohesion: 0.09
Nodes (4): DocumentController, DocumentEntity, ChunkEmbeddingConsumer, DocumentChunkRepository

### Community 2 - "Community 2"
Cohesion: 0.13
Nodes (6): RerankerClient, NoOpLowConfidenceGuardrail, OpenAiLlmClient, PromptBuilder, SecurityConfig, GenerationServiceTest

### Community 3 - "Community 3"
Cohesion: 0.15
Nodes (2): VectorSearchRepository, VectorSearchIntegrationTest

### Community 4 - "Community 4"
Cohesion: 0.19
Nodes (3): GlobalExceptionHandler, LexPilotException, RuntimeException

### Community 5 - "Community 5"
Cohesion: 0.18
Nodes (3): QueryController, EmbeddingServiceClient, HybridSearchService

### Community 6 - "Community 6"
Cohesion: 0.14
Nodes (5): DocumentNotFoundException, ExtractionException, InvalidDocumentException, UpstreamServiceException, LexPilotException

### Community 7 - "Community 7"
Cohesion: 0.19
Nodes (2): CitationFormatter, LexPilotIntegrationTest

### Community 8 - "Community 8"
Cohesion: 0.19
Nodes (7): RegexCitationFormatter, CitationFormatter, LlmApiClient, LowConfidenceGuardrail, LegalPromptBuilder, PromptBuilder, GenerationService

### Community 9 - "Community 9"
Cohesion: 0.22
Nodes (3): OncePerRequestFilter, RateLimitingFilter, ApiKeyAuthFilter

### Community 10 - "Community 10"
Cohesion: 0.31
Nodes (1): CitationFormatterTest

### Community 11 - "Community 11"
Cohesion: 0.29
Nodes (7): BaseModel, embed(), EmbedRequest, EmbedResponse, Encode a batch of texts into normalised embeddings.     Returns one 384-dim vect, RerankRequest, RerankResponse

### Community 13 - "Community 13"
Cohesion: 0.5
Nodes (1): BM25SearchRepository

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
- **Thin community `Community 3`** (21 nodes): `VectorSearchRepository.java`, `VectorSearchIntegrationTest.java`, `VectorSearchRepository`, `.findNearest()`, `.findNearestRaw()`, `.floatArrayToPgvector()`, `.mapRow()`, `VectorSearchIntegrationTest`, `.configureProperties()`, `.findNearest_noOverlapChunkHasLowScore()`, `.findNearest_partialOverlapRanksAboveNoOverlap()`, `.findNearest_returnsContentText()`, `.findNearest_returnsCorrectDocumentId()`, `.findNearest_returnsSourceLabel()`, `.findNearest_scoresAreDescending()`, `.findNearest_shouldReturnExactMatchAsTopResult()`, `.findNearest_topKLimitsResults()`, `.insertChunk()`, `.seedData()`, `.sparseVector()`, `.toPgvector()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 7`** (13 nodes): `CitationFormatter`, `.format()`, `.setContentType()`, `CitationFormatter.java`, `LexPilotIntegrationTest.java`, `LexPilotIntegrationTest`, `.configureProperties()`, `.contextLoads()`, `.createMinimalPdf()`, `.escPdf()`, `.getStatusForNonExistentDoc_shouldReturn404()`, `.uploadInvalidContentType_shouldReturn400()`, `.uploadPdf_shouldExtractAndChunk()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 10`** (11 nodes): `CitationFormatterTest`, `.allThreeMarkers_shouldMapCorrectly()`, `.duplicateMarkers_shouldBeDeduplicated()`, `.emptyChunkList_allMarkersShouldBeDropped()`, `.hallucinatedMarker_shouldBeDropped()`, `.lowConfidence_shouldBeFalseByDefault()`, `.noMarkers_shouldProduceEmptyCitations()`, `.validMarkers_shouldProduceCorrectCitations()`, `.zeroMarker_shouldBeDropped()`, `.format()`, `CitationFormatterTest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 13`** (4 nodes): `BM25SearchRepository.java`, `BM25SearchRepository`, `.BM25SearchRepository()`, `.findTopKByBM25()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 14`** (3 nodes): `LexPilotApplication.java`, `LexPilotApplication`, `.main()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 15`** (3 nodes): `LowConfidenceGuardrail`, `.isLowConfidence()`, `LowConfidenceGuardrail.java`
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

- **Why does `DocumentEntity` connect `Community 1` to `Community 0`, `Community 7`?**
  _High betweenness centrality (0.079) - this node is a cross-community bridge._
- **Why does `OpenAiLlmClient` connect `Community 2` to `Community 8`, `Community 6`?**
  _High betweenness centrality (0.065) - this node is a cross-community bridge._
- **What connects `Encode a batch of texts into normalised embeddings.     Returns one 384-dim vect`, `AppConfigRegistration`, `DocumentRepository` to the rest of the system?**
  _3 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.09 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.09 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.13 - nodes in this community are weakly interconnected._
- **Should `Community 6` be split into smaller, more focused modules?**
  _Cohesion score 0.14 - nodes in this community are weakly interconnected._