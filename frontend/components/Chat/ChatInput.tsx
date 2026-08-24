"use client";

import { useState, useRef, useEffect } from "react";
import { Send, Loader2, Paperclip, X, FileText, CheckCircle2, AlertCircle } from "lucide-react";
import { cn } from "@/lib/utils";
import { useDocumentStore, MAX_DOCUMENTS } from "@/lib/stores/useDocumentStore";
import { useUploadDocument } from "@/lib/hooks/useUploadDocument";
import { useDeleteDocument } from "@/lib/hooks/useDeleteDocument";
import { useIngestionStatus } from "@/lib/hooks/useIngestionStatus";

interface Props {
  onSend: (message: string) => void;
  disabled?: boolean;
}

function DocumentChip({ documentId, filename }: { documentId: string; filename: string }) {
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
    <div className="flex items-center gap-1.5 px-2.5 py-1 bg-[#1a1d27] border border-gray-700/60 hover:border-gray-600 rounded-lg text-xs text-gray-200 transition-all shadow-sm max-w-[200px]">
      <FileText className="w-3.5 h-3.5 text-brand-400 shrink-0" />
      <span className="truncate max-w-[110px]" title={filename}>
        {filename}
      </span>
      {isPending ? (
        <Loader2 className="w-3 h-3 text-brand-400 animate-spin shrink-0" />
      ) : status === "INDEXED" || status === "READY" ? (
        <CheckCircle2 className="w-3 h-3 text-green-400 shrink-0" />
      ) : (
        <AlertCircle className="w-3 h-3 text-red-400 shrink-0" />
      )}
      <button
        type="button"
        onClick={handleDelete}
        disabled={deleteDoc.isPending}
        className="ml-0.5 p-0.5 rounded text-gray-400 hover:text-white hover:bg-gray-700/60 transition-colors"
        title="Remove document"
      >
        <X className="w-3 h-3" />
      </button>
    </div>
  );
}

export function ChatInput({ onSend, disabled }: Props) {
  const [text, setText] = useState("");
  const [isDragging, setIsDragging] = useState(false);
  const [limitWarning, setLimitWarning] = useState<string | null>(null);

  const inputRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const documents = useDocumentStore((state) => Object.values(state.documents));
  const activeCount = documents.length;
  const isMaxReached = activeCount >= MAX_DOCUMENTS;

  const uploadDoc = useUploadDocument();

  const handleFiles = async (files: FileList | File[]) => {
    const fileArray = Array.from(files).filter(
      (f) => f.type === "application/pdf" || f.name.toLowerCase().endsWith(".pdf")
    );

    if (fileArray.length === 0) {
      setLimitWarning("Please select valid PDF documents.");
      setTimeout(() => setLimitWarning(null), 4000);
      return;
    }

    const availableSlots = MAX_DOCUMENTS - activeCount;
    if (availableSlots <= 0) {
      setLimitWarning(`Maximum ${MAX_DOCUMENTS} documents reached. Please remove a document to add another.`);
      setTimeout(() => setLimitWarning(null), 4000);
      return;
    }

    const filesToUpload = fileArray.slice(0, availableSlots);
    if (fileArray.length > availableSlots) {
      setLimitWarning(`Only ${availableSlots} more document(s) allowed (Max ${MAX_DOCUMENTS}). Uploading the first ${availableSlots}.`);
      setTimeout(() => setLimitWarning(null), 5000);
    } else {
      setLimitWarning(null);
    }

    for (const file of filesToUpload) {
      uploadDoc.mutate({ file, sourceType: "chat_upload" });
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      handleFiles(e.target.files);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    if (!isMaxReached) {
      setIsDragging(true);
    }
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFiles(e.dataTransfer.files);
    }
  };

  const handleSubmit = () => {
    if (text.trim() && !disabled) {
      onSend(text.trim());
      setText("");
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.style.height = "auto";
      inputRef.current.style.height = `${Math.min(inputRef.current.scrollHeight, 120)}px`;
    }
  }, [text]);

  return (
    <div className="flex flex-col gap-2 w-full">
      {/* Attached Documents Dock / Chips */}
      {activeCount > 0 && (
        <div className="flex items-center justify-between gap-2 px-1">
          <div className="flex flex-wrap items-center gap-2">
            {documents.map((doc) => (
              <DocumentChip
                key={doc.documentId}
                documentId={doc.documentId}
                filename={doc.filename}
              />
            ))}
          </div>
          <span className="text-[11px] text-gray-400 whitespace-nowrap">
            {activeCount}/{MAX_DOCUMENTS} docs
          </span>
        </div>
      )}

      {/* Warning Alert */}
      {limitWarning && (
        <div className="px-3 py-1.5 bg-amber-500/10 border border-amber-500/30 rounded-lg text-xs text-amber-300 flex items-center gap-2 animate-fade-in-up">
          <AlertCircle className="w-3.5 h-3.5 text-amber-400 shrink-0" />
          <span>{limitWarning}</span>
        </div>
      )}

      {/* Main Input Box */}
      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={cn(
          "relative bg-surface-dark border rounded-xl shadow-lg p-2 flex items-end transition-all",
          isDragging
            ? "border-brand-400 ring-2 ring-brand-500/40 bg-brand-950/20"
            : "border-gray-800 focus-within:border-brand-500 focus-within:ring-1 focus-within:ring-brand-500"
        )}
      >
        {/* Hidden File Input */}
        <input
          ref={fileInputRef}
          type="file"
          multiple
          accept=".pdf,application/pdf"
          className="hidden"
          onChange={handleFileInputChange}
        />

        {/* Attachment Button */}
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={disabled || isMaxReached || uploadDoc.isPending}
          className={cn(
            "shrink-0 mr-1 p-2 rounded-lg transition-colors flex items-center justify-center",
            isMaxReached
              ? "text-gray-600 cursor-not-allowed"
              : "text-gray-400 hover:text-white hover:bg-gray-800 active:scale-95"
          )}
          title={
            isMaxReached
              ? `Maximum ${MAX_DOCUMENTS} documents attached. Remove one to add more.`
              : `Attach PDF documents (max ${MAX_DOCUMENTS})`
          }
        >
          {uploadDoc.isPending ? (
            <Loader2 className="w-4 h-4 text-brand-400 animate-spin" />
          ) : (
            <Paperclip className="w-4 h-4" />
          )}
        </button>

        {/* Text Input */}
        <textarea
          ref={inputRef}
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={
            activeCount === 0
              ? "Attach PDFs (up to 5) or ask a question..."
              : `Ask a question about your ${activeCount} document${activeCount > 1 ? "s" : ""}...`
          }
          disabled={disabled}
          className="w-full bg-transparent text-white placeholder-gray-500 resize-none outline-none py-1.5 px-2 text-sm max-h-[120px] overflow-y-auto"
          rows={1}
        />

        {/* Send Button */}
        <button
          type="button"
          onClick={handleSubmit}
          disabled={disabled || !text.trim()}
          className={cn(
            "shrink-0 ml-2 p-2 rounded-lg transition-colors flex items-center justify-center",
            text.trim() && !disabled
              ? "bg-brand-600 text-white hover:bg-brand-500 active:scale-95 shadow-sm"
              : "bg-gray-800 text-gray-500 cursor-not-allowed"
          )}
          title="Send message"
        >
          {disabled ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
        </button>
      </div>
    </div>
  );
}
