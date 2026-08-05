import { create } from "zustand";
import type { QueryResponse } from "@/lib/api";

interface QueryState {
  queryText: string;
  domain: "consumer_protection" | "rbi" | "tenant" | null;
  latestResponse: QueryResponse | null;

  setQueryText: (text: string) => void;
  setDomain: (domain: QueryState["domain"]) => void;
  setLatestResponse: (response: QueryResponse) => void;
  reset: () => void;
}

export const useQueryStore = create<QueryState>((set) => ({
  queryText: "",
  domain: null,
  latestResponse: null,

  setQueryText: (text) => set({ queryText: text }),
  setDomain: (domain) => set({ domain }),
  setLatestResponse: (response) => set({ latestResponse: response }),
  reset: () => set({ queryText: "", domain: null, latestResponse: null }),
}));
