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
  sessionId: string;
}

export interface QueryRequest {
  query: string;
  sessionId?: string | null;
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

// In browser context, route through the secure Next.js BFF proxy (/api/backend)
// to prevent exposing master API key in client bundles.
const isBrowser = typeof window !== "undefined";
const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  (isBrowser ? "/api/backend" : "http://localhost:8080/api/v1");

// Only used for standalone CLI testing or direct client calls if explicitly provided
const API_KEY = process.env.NEXT_PUBLIC_LEXPILOT_API_KEY ?? "";

/**
 * Returns common headers including the API key when available.
 * Callers that need extra headers (e.g. Content-Type) should spread this.
 */
function getAuthHeaders(): Record<string, string> {
  const headers: Record<string, string> = {};
  if (API_KEY) {
    headers["X-Api-Key"] = API_KEY;
  }
  return headers;
}

export class ApiError extends Error {
  constructor(public status: number, public body: string) {
    super(`API error ${status}: ${body}`);
  }
}

export type QueryResponse = GeneratedAnswer;

export async function queryDocuments(
  query: string,
  sessionId?: string | null
): Promise<GeneratedAnswer> {
  const requestBody: QueryRequest = { query };
  if (sessionId) {
    requestBody.sessionId = sessionId;
  }

  const res = await fetch(`${API_BASE_URL}/query/answer`, {
    method: "POST",
    headers: { ...getAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify(requestBody),
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
  const formData = new FormData();
  formData.append("file", file);
  if (sourceType) {
    formData.append("sourceType", sourceType);
  }

  const res = await fetch(`${API_BASE_URL}/documents`, {
    method: "POST",
    headers: getAuthHeaders(),
    // Note: Do not set Content-Type header manually when sending FormData, 
    // the browser will automatically set it with the correct boundary.
    body: formData,
  });

  if (!res.ok) {
    const body = await res.text();
    throw new ApiError(res.status, body);
  }

  return res.json();
}

export async function getIngestionStatus(
  documentId: string
): Promise<IngestionStatusResponse> {
  const res = await fetch(`${API_BASE_URL}/documents/${documentId}/status`, {
    method: "GET",
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const body = await res.text();
    throw new ApiError(res.status, body);
  }

  return res.json();
}

export async function getDocuments(): Promise<DocumentUploadResponse[]> {
  const res = await fetch(`${API_BASE_URL}/documents`, {
    method: "GET",
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const body = await res.text();
    throw new ApiError(res.status, body);
  }

  return res.json();
}

export async function deleteDocument(documentId: string): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/documents/${documentId}`, {
    method: "DELETE",
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const body = await res.text();
    throw new ApiError(res.status, body);
  }
}

export default axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
    ...(API_KEY ? { "X-Api-Key": API_KEY } : {}),
  },
  timeout: 30_000,
});
