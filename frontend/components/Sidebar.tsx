"use client";

import { useState, useRef } from "react";
import { useQueryStore } from "@/lib/stores/useQueryStore";
import { useDocumentStore, MAX_DOCUMENTS } from "@/lib/stores/useDocumentStore";
import { useUploadDocument } from "@/lib/hooks/useUploadDocument";
import { useDeleteDocument } from "@/lib/hooks/useDeleteDocument";
import { useIngestionStatus } from "@/lib/hooks/useIngestionStatus";
import {
  Upload,
  FileText,
  Trash2,
  Info,
  Loader2,
  CheckCircle2,
  XCircle,
  X,
  MessageSquarePlus,
  MessageSquare,
} from "lucide-react";
import { cn } from "@/lib/utils";

function FileStatusItem({ documentId, fileName }: { documentId: string; fileName: string }) {
  const { data } = useIngestionStatus(documentId);
  const deleteDoc = useDeleteDocument();
  const doc = useDocumentStore((state) => state.documents[documentId]);

  const status = data?.status || doc?.status || "PROCESSING";
  const isPending = status !== "INDEXED" && status !== "FAILED";

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    deleteDoc.mutate(documentId);
  };

  return (
    <div className="flex items-center gap-2.5 p-2 bg-surface-dark rounded-lg border border-gray-800 text-sm group hover:border-gray-700 transition-all">
      <FileText className="w-4 h-4 text-brand-400 shrink-0" />
      <span className="flex-1 truncate text-xs text-gray-200" title={fileName}>
        {fileName}
      </span>
      {isPending ? (
        <Loader2 className="w-3.5 h-3.5 text-brand-400 animate-spin shrink-0" />
      ) : status === "INDEXED" ? (
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

  const messages = useQueryStore((state) => state.messages);
  const clearMessages = useQueryStore((state) => state.clearMessages);
  const sessions = useQueryStore((state) => state.sessions);
  const currentSessionId = useQueryStore((state) => state.sessionId);
  const startNewSession = useQueryStore((state) => state.startNewSession);
  const switchSession = useQueryStore((state) => state.switchSession);
  const deleteSession = useQueryStore((state) => state.deleteSession);

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
      {/* App Header */}
      <div className="mb-4 px-2">
        <h1 className="text-xl font-bold text-white flex items-center gap-2">
          <span className="text-2xl">⚖️</span> LexPilot
        </h1>
        <p className="text-xs text-gray-400 mt-0.5">Legal Rights & Grievance Assistant</p>
      </div>

      {/* New Chat Action Button */}
      <button
        type="button"
        onClick={startNewSession}
        className="w-full mb-4 py-2.5 px-3 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-medium transition-all flex items-center justify-center gap-2 shadow-sm shadow-brand-500/20 active:scale-98 cursor-pointer"
      >
        <MessageSquarePlus className="w-4 h-4" /> New Conversation
      </button>

      <div className="flex-1 overflow-y-auto pr-1 space-y-6">
        {/* Recent Chat Sessions Section */}
        {sessions.length > 0 && (
          <section>
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
                Recent Chats
              </h2>
              <span className="text-[11px] text-gray-500 font-mono">
                {sessions.length}
              </span>
            </div>
            <div className="space-y-1">
              {sessions.map((s) => {
                const isActive = s.id === currentSessionId;
                return (
                  <div
                    key={s.id}
                    onClick={() => switchSession(s.id)}
                    className={cn(
                      "flex items-center justify-between gap-2 px-2.5 py-2 rounded-lg text-xs cursor-pointer transition-colors group",
                      isActive
                        ? "bg-brand-950/60 border border-brand-500/30 text-white font-medium"
                        : "text-gray-300 hover:bg-gray-800/60 hover:text-white"
                    )}
                  >
                    <div className="flex items-center gap-2 truncate min-w-0">
                      <MessageSquare
                        className={cn(
                          "w-3.5 h-3.5 shrink-0",
                          isActive ? "text-brand-400" : "text-gray-500"
                        )}
                      />
                      <span className="truncate" title={s.title}>
                        {s.title || "Conversation"}
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        deleteSession(s.id);
                      }}
                      className="opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-gray-700/60 text-gray-400 hover:text-red-400 transition-all"
                      title="Delete conversation"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </div>
                );
              })}
            </div>
          </section>
        )}

        {/* Documents Management Section */}
        <section>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
              📁 Attached Documents
            </h2>
            <span className="text-[11px] text-gray-400 bg-surface-dark px-2 py-0.5 rounded border border-gray-800">
              {activeCount}/{MAX_DOCUMENTS}
            </span>
          </div>

          <div className="space-y-3">
            <div
              className={cn(
                "border-2 border-dashed rounded-xl p-3.5 text-center transition-colors",
                isMaxReached
                  ? "border-gray-800 bg-gray-900/40 opacity-60 cursor-not-allowed"
                  : "border-gray-700 cursor-pointer hover:border-brand-500 hover:bg-surface-dark"
              )}
              onClick={() => {
                if (!isMaxReached) fileInputRef.current?.click();
              }}
            >
              <Upload className="w-5 h-5 text-gray-400 mx-auto mb-1.5" />
              <p className="text-xs text-gray-300">
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

            {selectedFiles.length > 0 && (
              <button
                onClick={handleProcess}
                disabled={uploadDoc.isPending}
                className="w-full py-2 rounded-lg text-xs font-medium bg-brand-600 text-white hover:bg-brand-500 transition-all flex items-center justify-center gap-2"
              >
                {uploadDoc.isPending ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" /> Uploading...
                  </>
                ) : (
                  `Upload ${selectedFiles.length} Document(s)`
                )}
              </button>
            )}
          </div>

          {documents.length > 0 && (
            <div className="mt-3 space-y-1.5">
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

        {/* About Section */}
        <section className="bg-surface-dark p-3.5 rounded-xl border border-gray-800 text-xs">
          <div className="flex items-center gap-2 mb-1.5 text-brand-400 font-medium">
            <Info className="w-3.5 h-3.5" /> About LexPilot
          </div>
          <p className="text-gray-400 leading-relaxed text-[11px]">
            Grounded legal rights assistant for Indian consumer, banking, and tenancy grievances with verifiable source citations.
          </p>
        </section>
      </div>

      {/* Clear Chat Footer */}
      <div className="pt-3 border-t border-gray-800 mt-auto">
        <button
          type="button"
          onClick={clearMessages}
          disabled={messages.length === 0}
          className={cn(
            "w-full py-2 bg-surface-dark text-gray-300 rounded-lg text-xs font-medium transition-all flex items-center justify-center gap-2",
            messages.length === 0
              ? "opacity-50 cursor-not-allowed text-gray-500"
              : "hover:bg-gray-800 hover:text-red-400 active:scale-95 cursor-pointer"
          )}
        >
          <Trash2 className="w-3.5 h-3.5" /> Clear Current Chat
        </button>
      </div>
    </aside>
  );
}
