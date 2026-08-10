"use client";

import { useState } from "react";
import { useQueryDocuments } from "@/lib/hooks/useQueryDocuments";

export default function QueryPage() {
  const [query, setQuery] = useState("");
  const { mutate, data, isPending, isError, error } = useQueryDocuments();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;
    mutate(query);
  };

  return (
    <main>
      <form onSubmit={handleSubmit}>
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
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
