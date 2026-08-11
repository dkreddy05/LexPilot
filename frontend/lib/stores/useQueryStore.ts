import { create } from "zustand";
import type { QueryResponse } from "@/lib/api";

interface QueryState {
  queryText: string;
  latestResponse: QueryResponse | null;

  setQueryText: (text: string) => void;
  setLatestResponse: (response: QueryResponse) => void;
  reset: () => void;
}

export const useQueryStore = create<QueryState>((set) => ({
  queryText: "",
  latestResponse: null,

  setQueryText: (text) => set({ queryText: text }),
  setLatestResponse: (response) => set({ latestResponse: response }),
  reset: () => set({ queryText: "", latestResponse: null }),
}));
