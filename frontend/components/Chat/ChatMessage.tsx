"use client";

import { useMemo, useCallback, useState } from "react";
import { User, Bot, Loader2, AlertCircle } from "lucide-react";
import { cn } from "@/lib/utils";
import type { Message } from "@/lib/stores/useQueryStore";
import { CitationsExpander } from "./CitationsExpander";

/** Regex matching inline citation markers like [1], [2], [10] etc. */
const CITATION_MARKER_RE = /\[(\d+)\]/g;

/**
 * Splits answer text into alternating prose segments and citation markers.
 * E.g. "Based on [1] and [2]" → ["Based on ", 1, " and ", 2]
 */
function parseAnswerWithMarkers(text: string): (string | number)[] {
  const parts: (string | number)[] = [];
  let lastIndex = 0;

  const matches = text.matchAll(CITATION_MARKER_RE);
  for (const match of matches) {
    const idx = match.index!;
    if (idx > lastIndex) {
      parts.push(text.slice(lastIndex, idx));
    }
    parts.push(parseInt(match[1], 10));
    lastIndex = idx + match[0].length;
  }

  if (lastIndex < text.length) {
    parts.push(text.slice(lastIndex));
  }

  return parts;
}

/**
 * Formats prose text with basic Markdown features:
 * - Bold: **term**
 * - Inline code: `section 35`
 * - Bullets: - item or * item
 */
function FormattedProse({ text }: { text: string }) {
  // If text contains newlines, render structured blocks
  const lines = text.split("\n");

  return (
    <>
      {lines.map((line, lIdx) => {
        const trimmed = line.trim();
        const isBullet = trimmed.startsWith("- ") || trimmed.startsWith("* ");
        const content = isBullet ? trimmed.slice(2) : line;

        // Split on bold (**...**) and inline code (`...`)
        const segments = content.split(/(\*\*.*?\*\*|`.*?`)/g);

        return (
          <span
            key={lIdx}
            className={cn(
              lIdx > 0 ? "block mt-1" : "inline",
              isBullet && "block pl-4 relative my-1 text-gray-200"
            )}
          >
            {isBullet && <span className="absolute left-1 text-brand-400 font-bold">•</span>}
            {segments.map((seg, sIdx) => {
              if (seg.startsWith("**") && seg.endsWith("**") && seg.length >= 4) {
                return (
                  <strong key={sIdx} className="font-semibold text-white">
                    {seg.slice(2, -2)}
                  </strong>
                );
              }
              if (seg.startsWith("`") && seg.endsWith("`") && seg.length >= 2) {
                return (
                  <code
                    key={sIdx}
                    className="px-1 py-0.5 rounded bg-gray-800 text-brand-300 font-mono text-[11px]"
                  >
                    {seg.slice(1, -1)}
                  </code>
                );
              }
              return <span key={sIdx}>{seg}</span>;
            })}
          </span>
        );
      })}
    </>
  );
}

/** Animated thinking dots indicator shown while waiting for response */
function ThinkingIndicator() {
  return (
    <div className="flex items-center gap-1.5 py-1">
      <div className="flex items-center gap-1">
        <span
          className="w-2 h-2 rounded-full bg-brand-400 animate-bounce"
          style={{ animationDelay: "0ms", animationDuration: "1.2s" }}
        />
        <span
          className="w-2 h-2 rounded-full bg-brand-400 animate-bounce"
          style={{ animationDelay: "200ms", animationDuration: "1.2s" }}
        />
        <span
          className="w-2 h-2 rounded-full bg-brand-400 animate-bounce"
          style={{ animationDelay: "400ms", animationDuration: "1.2s" }}
        />
      </div>
      <span className="text-sm text-gray-400 ml-1.5">Thinking…</span>
    </div>
  );
}

export function ChatMessage({ message }: { message: Message }) {
  const isUser = message.role === "user";
  const isThinking = message.content === "⟳ Thinking...";
  const isError = message.content.startsWith("Error:");

  // Track which citation marker is currently highlighted (clicked)
  const [highlightedMarker, setHighlightedMarker] = useState<number | null>(null);

  // Parse the answer text to split prose from citation markers
  const parsedContent = useMemo(() => {
    if (isUser || isThinking || isError) return null;
    return parseAnswerWithMarkers(message.content);
  }, [message.content, isUser, isThinking, isError]);

  // Available marker numbers for validation
  const availableMarkers = useMemo(() => {
    if (!message.response?.citations) return new Set<number>();
    return new Set(message.response.citations.map((c) => c.marker));
  }, [message.response]);

  const handleMarkerClick = useCallback((marker: number) => {
    setHighlightedMarker((prev) => (prev === marker ? null : marker));
  }, []);

  return (
    <div
      className={cn(
        "flex w-full animate-fade-in-up",
        isUser ? "justify-end" : "justify-start"
      )}
    >
      <div
        className={cn(
          "flex max-w-[80%] gap-4 p-5 rounded-2xl shadow-sm",
          isUser
            ? "bg-brand-600 text-white rounded-br-sm"
            : isError
            ? "bg-red-950/40 text-red-200 rounded-bl-sm border border-red-800/60"
            : "bg-surface-light text-gray-800 dark:bg-surface-dark dark:text-gray-100 rounded-bl-sm border border-gray-100 dark:border-gray-800"
        )}
      >
        {!isUser && (
          <div
            className={cn(
              "shrink-0 w-8 h-8 rounded-full flex items-center justify-center",
              isError
                ? "bg-red-900/60"
                : "bg-brand-100 dark:bg-brand-900"
            )}
          >
            {isError ? (
              <AlertCircle className="w-5 h-5 text-red-400" />
            ) : isThinking ? (
              <Loader2 className="w-5 h-5 text-brand-400 animate-spin" />
            ) : (
              <Bot className="w-5 h-5 text-brand-600 dark:text-brand-400" />
            )}
          </div>
        )}

        <div className="flex-1 space-y-2 overflow-hidden">
          {/* Message body */}
          <div className="text-sm leading-relaxed">
            {isThinking ? (
              <ThinkingIndicator />
            ) : parsedContent ? (
              /* Render parsed content with inline citation badges */
              parsedContent.map((part, i) => {
                if (typeof part === "string") {
                  return <FormattedProse key={i} text={part} />;
                }

                // Citation marker badge
                const isValid = availableMarkers.has(part);
                return (
                  <button
                    key={i}
                    type="button"
                    onClick={() => isValid && handleMarkerClick(part)}
                    disabled={!isValid}
                    className={cn(
                      "inline-flex items-center justify-center",
                      "min-w-[1.25rem] h-5 px-1 mx-0.5",
                      "text-[10px] font-bold rounded-md",
                      "transition-all duration-200 ease-out",
                      "align-super -translate-y-0.5",
                      isValid
                        ? "bg-brand-600/90 text-white cursor-pointer hover:bg-brand-500 hover:scale-110 hover:shadow-md hover:shadow-brand-500/30 active:scale-95"
                        : "bg-gray-600/50 text-gray-400 cursor-default line-through"
                    )}
                    title={
                      isValid
                        ? `Jump to source [${part}]`
                        : `Citation [${part}] not found`
                    }
                  >
                    {part}
                  </button>
                );
              })
            ) : (
              <div className="whitespace-pre-wrap">{message.content}</div>
            )}
          </div>

          {/* Citations expander — only for assistant messages with response data */}
          {message.response && (
            <CitationsExpander
              citations={message.response.citations}
              lowConfidence={message.response.lowConfidence}
              highlightedMarker={highlightedMarker}
              onMarkerClick={handleMarkerClick}
            />
          )}
        </div>

        {isUser && (
          <div className="shrink-0 w-8 h-8 rounded-full bg-white/20 flex items-center justify-center">
            <User className="w-5 h-5 text-white" />
          </div>
        )}
      </div>
    </div>
  );
}
