"use client";

import { useState } from "react";
import { useUploadDocument } from "@/lib/hooks/useUploadDocument";
import { useIngestionStatus } from "@/lib/hooks/useIngestionStatus";
import { useDocumentStore } from "@/lib/stores/useDocumentStore";

// A small component to handle individual document polling and display
function DocumentTracker({ documentId }: { documentId: string }) {
  const { data, isLoading } = useIngestionStatus(documentId);
  const doc = useDocumentStore((state) => state.documents[documentId]);

  if (!doc) return null;

  return (
    <div className="flex items-center justify-between p-4 bg-[var(--color-surface)] border border-[var(--color-border)] rounded-lg mb-2">
      <div>
        <p className="font-medium text-white">{doc.filename}</p>
        <p className="text-sm text-[var(--color-muted)]">Uploaded: {new Date(doc.uploadedAt).toLocaleString()}</p>
      </div>
      <div className="flex items-center gap-3">
        {doc.status === "PENDING" || doc.status === "PROCESSING" ? (
          <span className="flex items-center gap-2 text-yellow-500">
            <div className="w-4 h-4 border-2 border-yellow-500 border-t-transparent rounded-full animate-spin"></div>
            {doc.status}
          </span>
        ) : doc.status === "INDEXED" ? (
          <span className="text-green-500 font-medium">INDEXED</span>
        ) : (
          <span className="text-red-500 font-medium">FAILED: {doc.errorDetail || "Unknown error"}</span>
        )}
      </div>
    </div>
  );
}

export default function DocumentsPage() {
  const [file, setFile] = useState<File | null>(null);
  const { mutate, isPending, error } = useUploadDocument();
  
  // Convert documents map to array and sort by upload date descending
  const documents = useDocumentStore((state) => 
    Object.values(state.documents).sort((a, b) => 
      new Date(b.uploadedAt).getTime() - new Date(a.uploadedAt).getTime()
    )
  );

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setFile(e.target.files[0]);
    }
  };

  const handleUpload = (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) return;
    
    mutate({ file, sourceType: "manual_upload" }, {
      onSuccess: () => {
        setFile(null); // Reset form on success
        // Also reset the file input visually
        const fileInput = document.getElementById("file-upload") as HTMLInputElement;
        if (fileInput) fileInput.value = "";
      }
    });
  };

  return (
    <div className="flex flex-col items-center min-h-screen px-4 py-16">
      <div className="max-w-3xl w-full space-y-8">
        <div className="space-y-2">
          <h1 className="text-3xl font-bold text-white">Document Library</h1>
          <p className="text-[var(--color-muted)]">
            Upload source documents to be indexed into the RAG knowledge base.
          </p>
        </div>

        <form onSubmit={handleUpload} className="rounded-2xl border-2 border-dashed border-[var(--color-border)] bg-[var(--color-surface)] p-12 flex flex-col items-center justify-center gap-4">
          <input 
            id="file-upload"
            type="file" 
            accept="application/pdf" 
            onChange={handleFileChange}
            className="text-[var(--color-muted)] file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-brand-50 file:text-brand-700 hover:file:bg-brand-100"
          />
          <button 
            type="submit" 
            disabled={!file || isPending}
            className="px-6 py-2 bg-brand-600 text-white font-medium rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-brand-500 transition-colors"
          >
            {isPending ? "Uploading..." : "Upload Document"}
          </button>
          
          {error && <p className="text-red-500 text-sm mt-2">{(error as Error).message}</p>}
        </form>

        <div className="space-y-3">
          <h2 className="text-xl font-semibold text-white">Indexed Documents</h2>
          <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
            {documents.length === 0 ? (
              <p className="text-[var(--color-muted)] text-sm text-center">
                No documents indexed yet.
              </p>
            ) : (
              <div className="flex flex-col gap-2">
                {documents.map((doc) => (
                  <DocumentTracker key={doc.documentId} documentId={doc.documentId} />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
