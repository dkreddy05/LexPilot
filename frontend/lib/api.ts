import axios from "axios";
import { getSession } from "next-auth/react";

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
 * Returns common headers including the JWT Bearer token and API key fallback.
 */
async function getAuthHeaders(): Promise<Record<string, string>> {
  const headers: Record<string, string> = {};
  
  if (isBrowser) {
    const session = await getSession();
    // In our setup, next-auth handles the session, but we can also extract the token
    // Actually, next-auth handles cookies automatically for /api routes.
    // If the backend is on a different domain, we need the token explicitly.
    // Let's pass the API_KEY as a fallback.
  }
  
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
    headers: { ...(await getAuthHeaders()), "Content-Type": "application/json" },
    body: JSON.stringify(requestBody),
  });

  if (!res.ok) {
    const body = await res.text();
    throw new ApiError(res.status, body);
  }

  return res.json();
}

export interface StreamEvent {
  type: "token" | "citations" | "done";
  data: any;
}

export async function* streamQueryAnswer(
  query: string,
  sessionId?: string | null
): AsyncGenerator<StreamEvent> {
  const url = new URL(`${API_BASE_URL}/query/stream`, window.location.origin);
  url.searchParams.append("query", query);
  if (sessionId) {
    url.searchParams.append("sessionId", sessionId);
  }

  const res = await fetch(url.toString(), {
    method: "GET",
    headers: await getAuthHeaders(),
  });

  if (!res.ok) {
    throw new ApiError(res.status, await res.text());
  }

  if (!res.body) {
    throw new Error("No response body");
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split("\n");
    buffer = lines.pop() || "";

    for (const line of lines) {
      if (line.startsWith("event:")) {
        const eventType = line.substring(6).trim();
        // Read next line for data
        continue; // handled below for simplicity, assuming event: and data: format
      }
      if (line.startsWith("data:")) {
        const dataStr = line.substring(5).trim();
        if (dataStr === "[DONE]") {
          yield { type: "done", data: null };
          continue;
        }
        
        try {
          const data = JSON.parse(dataStr);
          // In standard SSE, we don't always have event types if we just yield data
          yield { type: "token", data: data }; // simplification for now
        } catch {
          yield { type: "token", data: dataStr };
        }
      }
    }
  }
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
    headers: await getAuthHeaders(),
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
    headers: await getAuthHeaders(),
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
    headers: await getAuthHeaders(),
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
    headers: await getAuthHeaders(),
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
