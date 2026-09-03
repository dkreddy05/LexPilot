import { create } from "zustand";
import type { QueryResponse } from "@/lib/api";

export type MessageRole = "user" | "assistant";

export interface Message {
  id: string;
  role: MessageRole;
  content: string;
  response?: QueryResponse; // For assistant messages containing citations
}

export interface ChatSession {
  id: string;
  title: string;
  messages: Message[];
  updatedAt: number;
}

interface QueryState {
  queryText: string;
  messages: Message[];
  sessionId: string | null;
  sessions: ChatSession[];

  setQueryText: (text: string) => void;
  appendMessage: (msg: Message) => void;
  updateMessage: (id: string, partial: Partial<Message>) => void;
  setSessionId: (sessionId: string | null) => void;
  clearMessages: () => void;

  startNewSession: () => void;
  switchSession: (sessionId: string) => void;
  deleteSession: (sessionId: string) => void;
}

const STORAGE_KEY = "lexpilot_chat_sessions_v1";

function loadSavedSessions(): ChatSession[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    return JSON.parse(raw);
  } catch {
    return [];
  }
}

function saveSessions(sessions: ChatSession[]) {
  if (typeof window === "undefined") return;
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
  } catch {
    // Ignore storage quota errors
  }
}

export const useQueryStore = create<QueryState>((set, get) => ({
  queryText: "",
  messages: [],
  sessionId: null,
  sessions: typeof window !== "undefined" ? loadSavedSessions() : [],

  setQueryText: (text) => set({ queryText: text }),

  appendMessage: (msg) => {
    set((state) => {
      const updatedMessages = [...state.messages, msg];
      const activeSessionId = state.sessionId ?? (msg.id || Date.now().toString());

      // Auto-title from the first user message
      let title = "New Conversation";
      if (msg.role === "user" && state.messages.length === 0) {
        title = msg.content.trim().slice(0, 32) + (msg.content.trim().length > 32 ? "…" : "");
      }

      // Upsert session
      const existingSessionIdx = state.sessions.findIndex((s) => s.id === activeSessionId);
      let updatedSessions: ChatSession[];

      if (existingSessionIdx >= 0) {
        updatedSessions = state.sessions.map((s, idx) =>
          idx === existingSessionIdx
            ? {
                ...s,
                title: s.title === "New Conversation" && msg.role === "user" ? title : s.title,
                messages: updatedMessages,
                updatedAt: Date.now(),
              }
            : s
        );
      } else {
        updatedSessions = [
          {
            id: activeSessionId,
            title,
            messages: updatedMessages,
            updatedAt: Date.now(),
          },
          ...state.sessions,
        ];
      }

      saveSessions(updatedSessions);

      return {
        messages: updatedMessages,
        sessionId: state.sessionId ?? activeSessionId,
        sessions: updatedSessions,
      };
    });
  },

  updateMessage: (id, partial) => {
    set((state) => {
      const updatedMessages = state.messages.map((m) =>
        m.id === id ? { ...m, ...partial } : m
      );

      let updatedSessions = state.sessions;
      if (state.sessionId) {
        updatedSessions = state.sessions.map((s) =>
          s.id === state.sessionId
            ? { ...s, messages: updatedMessages, updatedAt: Date.now() }
            : s
        );
        saveSessions(updatedSessions);
      }

      return {
        messages: updatedMessages,
        sessions: updatedSessions,
      };
    });
  },

  setSessionId: (sessionId) => {
    set((state) => {
      if (!sessionId) return { sessionId: null };

      // If switching to this session ID, update session list as well
      const updatedSessions = state.sessions.map((s) =>
        s.id === state.sessionId ? { ...s, id: sessionId } : s
      );
      saveSessions(updatedSessions);

      return {
        sessionId,
        sessions: updatedSessions,
      };
    });
  },

  clearMessages: () => {
    set({ messages: [], queryText: "", sessionId: null });
  },

  startNewSession: () => {
    set({ messages: [], queryText: "", sessionId: null });
  },

  switchSession: (sessionId: string) => {
    const session = get().sessions.find((s) => s.id === sessionId);
    if (session) {
      set({
        sessionId: session.id,
        messages: session.messages,
        queryText: "",
      });
    }
  },

  deleteSession: (sessionId: string) => {
    set((state) => {
      const remaining = state.sessions.filter((s) => s.id !== sessionId);
      saveSessions(remaining);

      if (state.sessionId === sessionId) {
        return {
          sessions: remaining,
          sessionId: null,
          messages: [],
          queryText: "",
        };
      }

      return { sessions: remaining };
    });
  },
}));
