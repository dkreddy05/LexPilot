import { useState, useRef } from "react";
import { useQueryStore } from "@/lib/stores/useQueryStore";
import { uploadDocument } from "@/lib/api";
import { useIngestionStatus } from "@/lib/hooks/useIngestionStatus";
import { Upload, FileText, Trash2, Info, Loader2, CheckCircle2, XCircle } from "lucide-react";
import { cn } from "@/lib/utils";

function FileStatus({ documentId, fileName }: { documentId: string; fileName: string }) {
  const { data } = useIngestionStatus(documentId);
  const status = data?.status || "PROCESSING";

  return (
    <div className="flex items-center gap-3 p-3 bg-surface-dark rounded-lg border border-gray-800 text-sm">
      <FileText className="w-4 h-4 text-brand-500" />
      <span className="flex-1 truncate">{fileName}</span>
      {status === "INDEXED" ? (
        <CheckCircle2 className="w-4 h-4 text-green-500" />
      ) : status === "FAILED" ? (
        <XCircle className="w-4 h-4 text-red-500" />
      ) : (
        <Loader2 className="w-4 h-4 text-brand-500 animate-spin" />
      )}
    </div>
  );
}

export function Sidebar() {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { clearMessages } = useQueryStore();
  
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploads, setUploads] = useState<{ id: string; name: string }[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState("");

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0]);
      setError("");
    }
  };

  const handleProcess = async () => {
    if (!selectedFile) return;
    setIsUploading(true);
    setError("");
    try {
      const res = await uploadDocument(selectedFile, selectedFile.name);
      setUploads((prev) => [...prev, { id: res.documentId, name: selectedFile.name }]);
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = "";
    } catch (err: any) {
      setError(err.message || "Failed to upload document");
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <aside className="w-80 h-full bg-[#16161a] border-r border-gray-800 flex flex-col p-4 shrink-0">
      <div className="mb-8 px-2">
        <h1 className="text-xl font-bold text-white flex items-center gap-2">
          <span className="text-2xl">⚖️</span> LexPilot
        </h1>
        <p className="text-sm text-gray-400 mt-1">Legal Rights Assistant</p>
      </div>

      <div className="flex-1 overflow-y-auto pr-2 space-y-6">
        <section>
          <h2 className="text-sm font-semibold text-gray-300 uppercase tracking-wider mb-3">
            📁 Your Documents
          </h2>
          
          <div className="space-y-3">
            <div 
              className="border-2 border-dashed border-gray-700 rounded-xl p-4 text-center cursor-pointer hover:border-brand-500 hover:bg-surface-dark transition-colors"
              onClick={() => fileInputRef.current?.click()}
            >
              <Upload className="w-6 h-6 text-gray-400 mx-auto mb-2" />
              <p className="text-sm text-gray-300">
                {selectedFile ? selectedFile.name : "Click to select PDF"}
              </p>
            </div>
            <input 
              type="file" 
              accept=".pdf" 
              className="hidden" 
              ref={fileInputRef}
              onChange={handleFileChange}
            />

            {error && <p className="text-xs text-red-400">{error}</p>}

            <button 
              onClick={handleProcess}
              disabled={!selectedFile || isUploading}
              className={cn(
                "w-full py-2.5 rounded-lg text-sm font-medium transition-all",
                (!selectedFile || isUploading) 
                  ? "bg-gray-800 text-gray-500 cursor-not-allowed" 
                  : "bg-brand-600 text-white hover:bg-brand-500 active:scale-95 shadow-[0_0_15px_rgba(59,85,230,0.4)]"
              )}
            >
              {isUploading ? "Uploading..." : "Process Document"}
            </button>
          </div>

          {uploads.length > 0 && (
            <div className="mt-4 space-y-2">
              {uploads.map((u) => (
                <FileStatus key={u.id} documentId={u.id} fileName={u.name} />
              ))}
            </div>
          )}
        </section>

        <section className="bg-surface-dark p-4 rounded-xl border border-gray-800">
          <div className="flex items-center gap-2 mb-2 text-brand-400 font-medium text-sm">
            <Info className="w-4 h-4" /> About this App
          </div>
          <p className="text-xs text-gray-400 leading-relaxed">
            LexPilot helps you navigate Indian consumer protection, RBI guidelines, and tenant disputes.
            Upload your case files and ask questions to receive AI-generated insights backed by specific citations.
          </p>
        </section>
      </div>

      <div className="pt-4 border-t border-gray-800 mt-auto">
        <button 
          onClick={clearMessages}
          className="w-full py-2.5 bg-surface-dark hover:bg-gray-800 text-gray-300 rounded-lg text-sm font-medium transition-colors flex items-center justify-center gap-2"
        >
          <Trash2 className="w-4 h-4" /> Clear Chat
        </button>
      </div>
    </aside>
  );
}
