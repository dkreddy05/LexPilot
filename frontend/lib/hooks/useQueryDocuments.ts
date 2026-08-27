import { useMutation } from "@tanstack/react-query";
import { queryDocuments } from "../api";

export function useQueryDocuments() {
  return useMutation({
    mutationFn: ({ query, sessionId }: { query: string; sessionId?: string | null }) =>
      queryDocuments(query, sessionId),
  });
}
