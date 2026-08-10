import axios from "axios";

// Generated Answer DTOs
export interface Citation {
  marker: number;
  chunkId: string;
  documentId: string;
  sourceLabel: string;
}

export interface GeneratedAnswer {
  answer: string;
  citations: Citation[];
  lowConfidence: boolean;
}

export interface QueryRequest {
  query: string;
}

// Ingestion DTOs (preserved stubs)
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

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

export class ApiError extends Error {
  constructor(public status: number, public body: string) {
    super(`API error ${status}: ${body}`);
  }
}

export async function queryDocuments(query: string): Promise<GeneratedAnswer> {
  // TODO: Add auth header wiring once ApiKeyAuthFilter is implemented
  const res = await fetch(`${API_BASE_URL}/query/answer`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query } satisfies QueryRequest),
  });

  if (!res.ok) {
    const body = await res.text();
    throw new ApiError(res.status, body);
  }

  return res.json();
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

export default axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 30_000,
});
