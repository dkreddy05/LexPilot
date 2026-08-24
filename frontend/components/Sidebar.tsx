"use client";

import { useState, useRef } from "react";
import { useQueryStore } from "@/lib/stores/useQueryStore";
import { useDocumentStore, MAX_DOCUMENTS } from "@/lib/stores/useDocumentStore";
import { useUploadDocument } from "@/lib/hooks/useUploadDocument";
import { useDeleteDocument } from "@/lib/hooks/useDeleteDocument";
import { useIngestionStatus } from "@/lib/hooks/useIngestionStatus";
import { Upload, FileText, Trash2, Info, Loader2, CheckCircle2, XCircle, X } from "lucide-react";
import { cn } from "@/lib/utils";

function FileStatusItem({ documentId, fileName }: { documentId: string; fileName: string }) {
  const { data } = useIngestionStatus(documentId);
  const deleteDoc = useDeleteDocument();
  const doc = useDocumentStore((state) => state.documents[documentId]);

  const status = data?.status || doc?.status || "PROCESSING";
  const isPending = status !== "INDEXED" && status !== "FAILED" && status !== "READY";

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    deleteDoc.mutate(documentId);
  };

  return (
    <div className="flex items-center gap-2.5 p-2.5 bg-surface-dark rounded-lg border border-gray-800 text-sm group hover:border-gray-700 transition-all">
      <FileText className="w-4 h-4 text-brand-400 shrink-0" />
      <span className="flex-1 truncate text-xs text-gray-200" title={fileName}>
        {fileName}
      </span>
      {isPending ? (
        <Loader2 className="w-3.5 h-3.5 text-brand-400 animate-spin shrink-0" />
      ) : status === "INDEXED" || status === "READY" ? (
        <CheckCircle2 className="w-3.5 h-3.5 text-green-500 shrink-0" />
      ) : (
        <XCircle className="w-3.5 h-3.5 text-red-500 shrink-0" />
      )}
      <button
        type="button"
        onClick={handleDelete}
        disabled={deleteDoc.isPending}
        className="p-1 rounded text-gray-500 hover:text-red-400 hover:bg-gray-800 transition-colors"
        title="Remove document"
      >
        <X className="w-3.5 h-3.5" />
      </button>
    </div>
  );
}

export function Sidebar() {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const clearMessages = useQueryStore((state) => state.clearMessages);
  const messages = useQueryStore((state) => state.messages);

  const documents = useDocumentStore((state) => Object.values(state.documents));
  const activeCount = documents.length;
  const isMaxReached = activeCount >= MAX_DOCUMENTS;

  const uploadDoc = useUploadDocument();
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [error, setError] = useState("");

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const files = Array.from(e.target.files).filter((f) =>
        f.type === "application/pdf" || f.name.toLowerCase().endsWith(".pdf")
      );
      if (files.length === 0) {
        setError("Only PDF documents are supported.");
        return;
      }
      const available = MAX_DOCUMENTS - activeCount;
      if (available <= 0) {
        setError(`Max ${MAX_DOCUMENTS} documents allowed. Remove one to add more.`);
        return;
      }
      setSelectedFiles(files.slice(0, available));
      setError("");
    }
  };

  const handleProcess = async () => {
    if (selectedFiles.length === 0) return;
    setError("");
    for (const file of selectedFiles) {
      uploadDoc.mutate({ file, sourceType: "sidebar_upload" });
    }
    setSelectedFiles([]);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  return (
    <aside className="w-80 h-full bg-[#16161a] border-r border-gray-800 flex flex-col p-4 shrink-0">
      <div className="mb-6 px-2">
        <h1 className="text-xl font-bold text-white flex items-center gap-2">
          <span className="text-2xl">⚖️</span> LexPilot
        </h1>
        <p className="text-sm text-gray-400 mt-1">Legal Rights Assistant</p>
      </div>

      <div className="flex-1 overflow-y-auto pr-2 space-y-6">
        <section>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-gray-300 uppercase tracking-wider">
              📁 Documents
            </h2>
            <span className="text-xs text-gray-400 bg-surface-dark px-2 py-0.5 rounded border border-gray-800">
              {activeCount}/{MAX_DOCUMENTS}
            </span>
          </div>

          <div className="space-y-3">
            <div
              className={cn(
                "border-2 border-dashed rounded-xl p-4 text-center transition-colors",
                isMaxReached
                  ? "border-gray-800 bg-gray-900/40 opacity-60 cursor-not-allowed"
                  : "border-gray-700 cursor-pointer hover:border-brand-500 hover:bg-surface-dark"
              )}
              onClick={() => {
                if (!isMaxReached) fileInputRef.current?.click();
              }}
            >
              <Upload className="w-6 h-6 text-gray-400 mx-auto mb-2" />
              <p className="text-sm text-gray-300">
                {isMaxReached
                  ? "Max 5 documents reached"
                  : selectedFiles.length > 0
                  ? `${selectedFiles.length} file(s) selected`
                  : "Click to select PDF"}
              </p>
            </div>
            <input
              type="file"
              accept=".pdf,application/pdf"
              multiple
              className="hidden"
              ref={fileInputRef}
              onChange={handleFileChange}
              disabled={isMaxReached}
            />

            {error && <p className="text-xs text-red-400">{error}</p>}

            <button
              onClick={handleProcess}
              disabled={selectedFiles.length === 0 || uploadDoc.isPending}
              className={cn(
                "w-full py-2 rounded-lg text-sm font-medium transition-all flex items-center justify-center gap-2",
                selectedFiles.length === 0 || uploadDoc.isPending
                  ? "bg-gray-800 text-gray-500 cursor-not-allowed"
                  : "bg-brand-600 text-white hover:bg-brand-500 active:scale-95 shadow-[0_0_15px_rgba(59,85,230,0.4)]"
              )}
            >
              {uploadDoc.isPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" /> Uploading...
                </>
              ) : (
                `Upload ${selectedFiles.length > 0 ? selectedFiles.length : ""} Document${selectedFiles.length > 1 ? "s" : ""}`
              )}
            </button>
          </div>

          {documents.length > 0 && (
            <div className="mt-4 space-y-2">
              {documents.map((u) => (
                <FileStatusItem
                  key={u.documentId}
                  documentId={u.documentId}
                  fileName={u.filename}
                />
              ))}
            </div>
          )}
        </section>

        <section className="bg-surface-dark p-4 rounded-xl border border-gray-800">
          <div className="flex items-center gap-2 mb-2 text-brand-400 font-medium text-sm">
            <Info className="w-4 h-4" /> About LexPilot
          </div>
          <p className="text-xs text-gray-400 leading-relaxed">
            Upload up to 5 legal or grievance files to start asking questions. LexPilot generates cited answers grounded in your specific documents.
          </p>
        </section>
      </div>

      <div className="pt-4 border-t border-gray-800 mt-auto">
        <button
          type="button"
          onClick={clearMessages}
          disabled={messages.length === 0}
          className={cn(
            "w-full py-2.5 bg-surface-dark text-gray-300 rounded-lg text-sm font-medium transition-all flex items-center justify-center gap-2",
            messages.length === 0
              ? "opacity-50 cursor-not-allowed text-gray-500"
              : "hover:bg-gray-800 hover:text-red-400 active:scale-95 cursor-pointer"
          )}
        >
          <Trash2 className="w-4 h-4" /> Clear Chat
        </button>
      </div>
    </aside>
  );
}
