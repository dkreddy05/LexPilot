import { create } from "zustand";
import type { DocumentUploadResponse, IngestionStatusResponse } from "@/lib/api";

export interface TrackedDocument {
  documentId: string;
  filename: string;
  status: "PENDING" | "PROCESSING" | "INDEXED" | "FAILED";
  errorDetail: string | null;
  uploadedAt: string;
}

interface DocumentState {
  documents: Record<string, TrackedDocument>;

  addDocument: (response: DocumentUploadResponse) => void;
  updateStatus: (status: IngestionStatusResponse) => void;
  removeDocument: (documentId: string) => void;
}

export const useDocumentStore = create<DocumentState>((set) => ({
  documents: {},

  addDocument: (response) =>
    set((state) => ({
      documents: {
        ...state.documents,
        [response.documentId]: {
          documentId: response.documentId,
          filename: response.filename,
          status: response.status,
          errorDetail: null,
          uploadedAt: new Date().toISOString(),
        },
      },
    })),

  updateStatus: (status) =>
    set((state) => ({
      documents: {
        ...state.documents,
        [status.documentId]: {
          ...(state.documents[status.documentId] ?? {
            documentId: status.documentId,
            filename: "unknown",
            uploadedAt: new Date().toISOString(),
          }),
          status: status.status,
          errorDetail: status.errorDetail,
        },
      },
    })),

  removeDocument: (documentId) =>
    set((state) => {
      const { [documentId]: _, ...rest } = state.documents;
      return { documents: rest };
    }),
}));
