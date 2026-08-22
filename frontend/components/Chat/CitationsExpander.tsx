"use client";

import { useState } from "react";
import { ChevronDown, ChevronUp, AlertTriangle } from "lucide-react";
import type { Citation } from "@/lib/api";

interface Props {
  citations: Citation[];
  lowConfidence: boolean;
}

export function CitationsExpander({ citations, lowConfidence }: Props) {
  const [isOpen, setIsOpen] = useState(false);

  if (citations.length === 0 && !lowConfidence) return null;

  return (
    <div className="mt-3 bg-surface-darker rounded-lg overflow-hidden border border-gray-800">
      <button 
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex items-center justify-between p-3 text-sm font-medium text-gray-300 hover:bg-gray-800 transition-colors"
      >
        <div className="flex items-center gap-2">
          {lowConfidence && <AlertTriangle className="w-4 h-4 text-amber-500" />}
          <span>{lowConfidence ? "Low Confidence & Sources" : "Source Documents"}</span>
        </div>
        {isOpen ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
      </button>

      {isOpen && (
        <div className="p-3 border-t border-gray-800 space-y-4">
          {lowConfidence && (
            <div className="bg-amber-500/10 text-amber-400 p-3 rounded-md text-xs">
              <strong>Warning:</strong> This answer may be incomplete or lack sufficient context in the provided documents. Please verify independently.
            </div>
          )}
          
          {citations.map((c) => (
            <div key={c.chunkId} className="text-sm">
              <div className="font-semibold text-brand-400 flex items-center gap-2">
                <span className="bg-brand-900 text-brand-100 px-1.5 py-0.5 rounded text-xs">
                  {c.marker}
                </span>
                {c.sourceLabel}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
