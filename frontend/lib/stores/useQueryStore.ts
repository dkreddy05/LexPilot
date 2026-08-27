import { create } from "zustand";
import type { QueryResponse } from "@/lib/api";

export type MessageRole = "user" | "assistant";

export interface Message {
  id: string;
  role: MessageRole;
  content: string;
  response?: QueryResponse; // For assistant messages containing citations
}

interface QueryState {
  queryText: string;
  messages: Message[];
  sessionId: string | null;

  setQueryText: (text: string) => void;
  appendMessage: (msg: Message) => void;
  updateMessage: (id: string, partial: Partial<Message>) => void;
  setSessionId: (sessionId: string | null) => void;
  clearMessages: () => void;
}

export const useQueryStore = create<QueryState>((set) => ({
  queryText: "",
  messages: [],
  sessionId: null,

  setQueryText: (text) => set({ queryText: text }),
  appendMessage: (msg) => set((state) => ({ messages: [...state.messages, msg] })),
  updateMessage: (id, partial) => set((state) => ({
    messages: state.messages.map((m) => (m.id === id ? { ...m, ...partial } : m))
  })),
  setSessionId: (sessionId) => set({ sessionId }),
  clearMessages: () => set({ messages: [], queryText: "", sessionId: null }),
}));
