import axios from "axios";

export interface QueryRequest {
  query: string;
  domain?: "consumer_protection" | "rbi" | "tenant" | null;
  sessionId?: string | null;
}

export interface Citation {
  chunkId: string;
  documentTitle: string;
  excerpt: string;
  pageNumber: number | null;
}

export interface QueryResponse {
  answer: string;
  citations: Citation[];
  lowConfidence: boolean;
  refusalReason: string | null;
}

export interface DocumentUploadResponse {
  documentId: string;
  filename: string;
  status: "PENDING" | "PROCESSING" | "INDEXED" | "FAILED";
  message: string;
}

export interface IngestionStatusResponse {
  documentId: string;
  status: "PENDING" | "PROCESSING" | "INDEXED" | "FAILED";
  errorDetail: string | null;
}

export interface ApiError {
  errorCode: string;
  message: string;
  timestamp: string;
}

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1",
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 30_000,
});

export async function query(request: QueryRequest): Promise<QueryResponse> {
  throw new Error("query() not yet implemented");
}

export async function uploadDocument(
  file: File,
  sourceType?: string
): Promise<DocumentUploadResponse> {
  throw new Error("uploadDocument() not yet implemented");
}

export async function getIngestionStatus(
  documentId: string
): Promise<IngestionStatusResponse> {
  throw new Error("getIngestionStatus() not yet implemented");
}

export default apiClient;
