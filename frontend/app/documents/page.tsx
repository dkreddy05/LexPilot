"use client";

import { useState, useEffect } from "react";
import { useUploadDocument } from "@/lib/hooks/useUploadDocument";
import { useDeleteDocument } from "@/lib/hooks/useDeleteDocument";
import { useIngestionStatus } from "@/lib/hooks/useIngestionStatus";
import { useDocumentStore, MAX_DOCUMENTS } from "@/lib/stores/useDocumentStore";
import { getDocuments } from "@/lib/api";
import { FileText, Trash2, Loader2, CheckCircle2, AlertCircle, ArrowLeft } from "lucide-react";
import Link from "next/link";

// A small component to handle individual document polling and display
function DocumentTracker({ documentId }: { documentId: string }) {
  const { data } = useIngestionStatus(documentId);
  const deleteDoc = useDeleteDocument();
  const doc = useDocumentStore((state) => state.documents[documentId]);

  if (!doc) return null;

  const status = data?.status || doc.status || "PROCESSING";
  const isPending = status !== "INDEXED" && status !== "FAILED" && status !== "READY";

  const handleDelete = () => {
    deleteDoc.mutate(documentId);
  };

  return (
    <div className="flex items-center justify-between p-4 bg-[var(--color-surface)] border border-[var(--color-border)] rounded-lg mb-2 group hover:border-gray-700 transition-all">
      <div className="flex items-center gap-3">
        <FileText className="w-5 h-5 text-brand-400" />
        <div>
          <p className="font-medium text-white text-sm">{doc.filename}</p>
          <p className="text-xs text-[var(--color-muted)]">
            Uploaded: {new Date(doc.uploadedAt).toLocaleString()}
          </p>
        </div>
      </div>
      <div className="flex items-center gap-4">
        {isPending ? (
          <span className="flex items-center gap-2 text-yellow-500 text-xs font-medium">
            <Loader2 className="w-4 h-4 animate-spin" />
            {status}
          </span>
        ) : status === "INDEXED" || status === "READY" ? (
          <span className="flex items-center gap-1.5 text-green-500 text-xs font-medium">
            <CheckCircle2 className="w-4 h-4" />
            INDEXED
          </span>
        ) : (
          <span className="flex items-center gap-1.5 text-red-500 text-xs font-medium">
            <AlertCircle className="w-4 h-4" />
            FAILED: {doc.errorDetail || "Unknown error"}
          </span>
        )}

        <button
          type="button"
          onClick={handleDelete}
          disabled={deleteDoc.isPending}
          className="p-1.5 rounded text-gray-500 hover:text-red-400 hover:bg-gray-800 transition-colors"
          title="Delete document"
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}

export default function DocumentsPage() {
  const [files, setFiles] = useState<File[]>([]);
  const { mutate, isPending, error } = useUploadDocument();
  const setDocuments = useDocumentStore((state) => state.setDocuments);

  // Convert documents map to array and sort by upload date descending
  const documents = useDocumentStore((state) =>
    Object.values(state.documents).sort(
      (a, b) => new Date(b.uploadedAt).getTime() - new Date(a.uploadedAt).getTime()
    )
  );

  const activeCount = documents.length;
  const isMaxReached = activeCount >= MAX_DOCUMENTS;

  useEffect(() => {
    getDocuments()
      .then((docs) => {
        if (Array.isArray(docs)) setDocuments(docs);
      })
      .catch((err) => console.debug("Failed to load documents:", err));
  }, [setDocuments]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const selected = Array.from(e.target.files).filter((f) =>
        f.type === "application/pdf" || f.name.toLowerCase().endsWith(".pdf")
      );
      const available = MAX_DOCUMENTS - activeCount;
      setFiles(selected.slice(0, available));
    }
  };

  const handleUpload = (e: React.FormEvent) => {
    e.preventDefault();
    if (files.length === 0) return;

    for (const file of files) {
      mutate(
        { file, sourceType: "manual_upload" },
        {
          onSuccess: () => {
            setFiles([]);
            const fileInput = document.getElementById("file-upload") as HTMLInputElement;
            if (fileInput) fileInput.value = "";
          },
        }
      );
    }
  };

  return (
    <div className="flex flex-col items-center min-h-screen px-4 py-12 w-full max-w-4xl mx-auto">
      <div className="w-full space-y-8">
        <div className="flex items-center justify-between">
          <div className="space-y-1">
            <Link
              href="/"
              className="inline-flex items-center gap-1.5 text-xs text-brand-400 hover:text-brand-300 mb-2 transition-colors"
            >
              <ArrowLeft className="w-3.5 h-3.5" /> Back to Chat
            </Link>
            <h1 className="text-3xl font-bold text-white">Document Library</h1>
            <p className="text-[var(--color-muted)] text-sm">
              Manage up to {MAX_DOCUMENTS} documents indexed in the LexPilot RAG knowledge base.
            </p>
          </div>
          <span className="text-sm font-semibold text-gray-300 bg-surface-dark border border-gray-800 px-3 py-1.5 rounded-lg">
            {activeCount}/{MAX_DOCUMENTS} Active Docs
          </span>
        </div>

        <form
          onSubmit={handleUpload}
          className="rounded-2xl border-2 border-dashed border-[var(--color-border)] bg-[var(--color-surface)] p-10 flex flex-col items-center justify-center gap-4"
        >
          <input
            id="file-upload"
            type="file"
            accept="application/pdf,.pdf"
            multiple
            disabled={isMaxReached}
            onChange={handleFileChange}
            className="text-[var(--color-muted)] file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-brand-50 file:text-brand-700 hover:file:bg-brand-100 disabled:opacity-50"
          />
          {isMaxReached ? (
            <p className="text-amber-400 text-xs font-medium">
              Maximum {MAX_DOCUMENTS} documents reached. Please delete a document to upload another.
            </p>
          ) : (
            <button
              type="submit"
              disabled={files.length === 0 || isPending}
              className="px-6 py-2 bg-brand-600 text-white font-medium rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-brand-500 transition-colors text-sm shadow-sm"
            >
              {isPending ? "Uploading..." : `Upload ${files.length > 0 ? `${files.length} ` : ""}Document${files.length > 1 ? "s" : ""}`}
            </button>
          )}

          {error && <p className="text-red-500 text-sm mt-2">{(error as Error).message}</p>}
        </form>

        <div className="space-y-3">
          <h2 className="text-xl font-semibold text-white">Indexed Documents</h2>
          <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
            {documents.length === 0 ? (
              <p className="text-[var(--color-muted)] text-sm text-center py-4">
                No documents indexed yet. Attach PDFs in the chat or upload above.
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
