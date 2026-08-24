import { create } from "zustand";
import type { DocumentUploadResponse, IngestionStatusResponse } from "@/lib/api";

export const MAX_DOCUMENTS = 5;

export interface TrackedDocument {
  documentId: string;
  filename: string;
  status: "PENDING" | "PROCESSING" | "INDEXED" | "FAILED" | "UPLOADED" | "EXTRACTING" | "CHUNKING" | "EMBEDDING";
  errorDetail: string | null;
  uploadedAt: string;
}

interface DocumentState {
  documents: Record<string, TrackedDocument>;

  addDocument: (response: DocumentUploadResponse) => void;
  setDocuments: (docs: DocumentUploadResponse[]) => void;
  updateStatus: (status: IngestionStatusResponse) => void;
  removeDocument: (documentId: string) => void;
  clearDocuments: () => void;
}

export const useDocumentStore = create<DocumentState>((set) => ({
  documents: {},

  addDocument: (response) =>
    set((state) => {
      const existingKeys = Object.keys(state.documents);
      if (existingKeys.length >= MAX_DOCUMENTS && !state.documents[response.documentId]) {
        return state; // Limit to MAX_DOCUMENTS
      }
      return {
        documents: {
          ...state.documents,
          [response.documentId]: {
            documentId: response.documentId,
            filename: response.filename,
            status: response.status as any,
            errorDetail: null,
            uploadedAt: new Date().toISOString(),
          },
        },
      };
    }),

  setDocuments: (docs) =>
    set(() => {
      const map: Record<string, TrackedDocument> = {};
      docs.slice(0, MAX_DOCUMENTS).forEach((doc) => {
        map[doc.documentId] = {
          documentId: doc.documentId,
          filename: doc.filename,
          status: doc.status as any,
          errorDetail: doc.message && doc.status === "FAILED" ? doc.message : null,
          uploadedAt: new Date().toISOString(),
        };
      });
      return { documents: map };
    }),

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

  clearDocuments: () => set({ documents: {} }),
}));
