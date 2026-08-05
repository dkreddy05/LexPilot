import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "LexPilot — Upload Documents",
  description: "Upload legal source documents for RAG indexing.",
};

export default function DocumentsPage() {
  return (
    <div className="flex flex-col items-center min-h-screen px-4 py-16">
      <div className="max-w-3xl w-full space-y-8">
        <div className="space-y-2">
          <h1 className="text-3xl font-bold text-white">Document Library</h1>
          <p className="text-[var(--color-muted)]">
            Upload source documents to be indexed into the RAG knowledge base.
          </p>
        </div>

        <div className="rounded-2xl border-2 border-dashed border-[var(--color-border)] bg-[var(--color-surface)] p-12 flex items-center justify-center">
          <p className="text-[var(--color-muted)] text-sm text-center">
            Upload form stub.
          </p>
        </div>

        <div className="space-y-3">
          <h2 className="text-xl font-semibold text-white">Indexed Documents</h2>
          <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
            <p className="text-[var(--color-muted)] text-sm text-center">
              No documents indexed yet.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
