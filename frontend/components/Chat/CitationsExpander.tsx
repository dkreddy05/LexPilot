"use client";

import { useState, useEffect, useRef } from "react";
import {
  ChevronDown,
  AlertTriangle,
  FileText,
  BookOpen,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { Citation } from "@/lib/api";

interface Props {
  citations: Citation[];
  lowConfidence: boolean;
  /** Which marker is currently highlighted from the answer text (null = none) */
  highlightedMarker?: number | null;
  /** Callback when a marker in the expander is clicked */
  onMarkerClick?: (marker: number) => void;
}

export function CitationsExpander({
  citations,
  lowConfidence,
  highlightedMarker,
  onMarkerClick,
}: Props) {
  const [isOpen, setIsOpen] = useState(false);
  const contentRef = useRef<HTMLDivElement>(null);
  const highlightRef = useRef<HTMLDivElement>(null);

  // Auto-expand when a marker is highlighted from the answer text
  useEffect(() => {
    if (highlightedMarker !== null) {
      setIsOpen(true);
    }
  }, [highlightedMarker]);

  // Scroll to highlighted citation card when it changes
  useEffect(() => {
    if (highlightedMarker !== null && highlightRef.current) {
      highlightRef.current.scrollIntoView({
        behavior: "smooth",
        block: "nearest",
      });
    }
  }, [highlightedMarker]);

  if (citations.length === 0 && !lowConfidence) return null;

  const sourceCount = citations.length;

  return (
    <div className="mt-3 bg-[#13141a] rounded-xl overflow-hidden border border-gray-800/80 transition-all duration-300">
      {/* Trigger button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={cn(
          "w-full flex items-center justify-between px-4 py-3 text-sm font-medium transition-colors group",
          isOpen
            ? "text-gray-200 bg-gray-800/40"
            : "text-gray-400 hover:text-gray-200 hover:bg-gray-800/30"
        )}
      >
        <div className="flex items-center gap-2.5">
          {lowConfidence ? (
            <AlertTriangle className="w-4 h-4 text-amber-500" />
          ) : (
            <BookOpen className="w-4 h-4 text-brand-400 opacity-80" />
          )}
          <span>
            {lowConfidence ? "Low Confidence" : "Sources"}
          </span>
          {sourceCount > 0 && (
            <span className="inline-flex items-center justify-center min-w-[1.5rem] h-5 px-1.5 text-[11px] font-semibold rounded-full bg-brand-600/80 text-white">
              {sourceCount}
            </span>
          )}
        </div>
        <ChevronDown
          className={cn(
            "w-4 h-4 transition-transform duration-300",
            isOpen && "rotate-180"
          )}
        />
      </button>

      {/* Expandable content */}
      <div
        ref={contentRef}
        className={cn(
          "overflow-hidden transition-all duration-300 ease-in-out",
          isOpen ? "max-h-[600px] opacity-100" : "max-h-0 opacity-0"
        )}
      >
        <div className="px-4 pb-4 pt-1 space-y-3">
          {/* Low confidence warning */}
          {lowConfidence && (
            <div className="flex items-start gap-2.5 bg-amber-500/10 border border-amber-500/20 text-amber-300 px-3.5 py-2.5 rounded-lg text-xs leading-relaxed">
              <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
              <p>
                <strong>Low confidence:</strong> This answer may be incomplete or
                lack sufficient grounding in the provided documents. Please verify
                critical details independently.
              </p>
            </div>
          )}

          {/* Citation cards */}
          {citations.map((c) => {
            const isHighlighted = highlightedMarker === c.marker;
            return (
              <div
                key={c.chunkId}
                ref={isHighlighted ? highlightRef : undefined}
                onClick={() => onMarkerClick?.(c.marker)}
                className={cn(
                  "flex items-start gap-3 px-3.5 py-3 rounded-lg cursor-pointer transition-all duration-200",
                  isHighlighted
                    ? "bg-brand-600/15 border border-brand-500/40 shadow-[0_0_12px_rgba(79,110,247,0.15)]"
                    : "bg-gray-800/40 border border-transparent hover:bg-gray-800/70 hover:border-gray-700/60"
                )}
              >
                {/* Marker badge */}
                <span
                  className={cn(
                    "shrink-0 inline-flex items-center justify-center w-6 h-6 rounded-md text-xs font-bold transition-colors",
                    isHighlighted
                      ? "bg-brand-500 text-white shadow-sm"
                      : "bg-brand-900/80 text-brand-200"
                  )}
                >
                  {c.marker}
                </span>

                {/* Source info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <FileText
                      className={cn(
                        "w-3.5 h-3.5 shrink-0",
                        isHighlighted ? "text-brand-400" : "text-gray-500"
                      )}
                    />
                    <span
                      className={cn(
                        "text-sm font-medium truncate",
                        isHighlighted ? "text-brand-300" : "text-gray-200"
                      )}
                      title={c.sourceLabel}
                    >
                      {c.sourceLabel}
                    </span>
                  </div>
                  <p className="text-[11px] text-gray-500 mt-1 font-mono truncate">
                    chunk: {c.chunkId.slice(0, 8)}…
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
