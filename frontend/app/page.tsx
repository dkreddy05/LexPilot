"use client";

import { useQueryStore } from "@/lib/stores/useQueryStore";
import { useQueryDocuments } from "@/lib/hooks/useQueryDocuments";
import type { GeneratedAnswer } from "@/lib/api";

export default function QueryPage() {
  const { queryText, setQueryText, setLatestResponse } = useQueryStore();
  
  const { mutate, data, isPending, isError, error } = useQueryDocuments();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!queryText.trim()) return;
    mutate(queryText, {
      onSuccess: (data: GeneratedAnswer) => {
        setLatestResponse(data);
      }
    });
  };

  return (
    <main>
      <form onSubmit={handleSubmit}>
        <input
          value={queryText}
          onChange={(e) => setQueryText(e.target.value)}
          placeholder="Ask about your consumer rights..."
        />
        <button type="submit" disabled={isPending}>
          {isPending ? "Searching..." : "Ask"}
        </button>
      </form>

      {isError && <p role="alert">{(error as Error).message}</p>}

      {data && (
        <section>
          {data.lowConfidence && (
            <p role="alert">This answer may be incomplete — verify independently.</p>
          )}
          <p>{data.answer}</p>
          <ol>
            {data.citations.map((c) => (
              <li key={c.chunkId}>[{c.marker}] {c.sourceLabel}</li>
            ))}
          </ol>
        </section>
      )}
    </main>
  );
}
