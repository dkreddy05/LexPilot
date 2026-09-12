"use client";

import { useEffect, useRef, useState } from "react";
import { Menu } from "lucide-react";
import { Sidebar } from "@/components/Sidebar";
import { ChatMessage } from "@/components/Chat/ChatMessage";
import { ChatInput } from "@/components/Chat/ChatInput";
import { useQueryStore } from "@/lib/stores/useQueryStore";
import { useDocumentStore } from "@/lib/stores/useDocumentStore";
import { useQueryDocuments } from "@/lib/hooks/useQueryDocuments";
import { getDocuments } from "@/lib/api";
import { cn } from "@/lib/utils";

const STARTER_QUESTIONS = [
  "What are my rights under the Consumer Protection Act, 2019?",
  "How do I file a complaint with the RBI Banking Ombudsman?",
  "What is the process for resolving a tenant-landlord dispute?",
  "What compensation can I claim for defective products?",
];

export default function Home() {
  const messages = useQueryStore((state) => state.messages);
  const appendMessage = useQueryStore((state) => state.appendMessage);
  const updateMessage = useQueryStore((state) => state.updateMessage);
  const sessionId = useQueryStore((state) => state.sessionId);
  const setSessionId = useQueryStore((state) => state.setSessionId);
  const setDocuments = useDocumentStore((state) => state.setDocuments);
  const { mutate, isPending } = useQueryDocuments();
  const scrollRef = useRef<HTMLDivElement>(null);

  // Sidebar state — open by default on desktop, closed on mobile
  const [sidebarOpen, setSidebarOpen] = useState(true);

  // Hydrate documents on mount
  useEffect(() => {
    getDocuments()
      .then((docs) => {
        if (Array.isArray(docs)) {
          setDocuments(docs);
        }
      })
      .catch((err) => {
        console.debug("Failed to load initial documents:", err);
      });
  }, [setDocuments]);

  // Auto-close sidebar on mobile on mount
  useEffect(() => {
    if (typeof window !== "undefined" && window.innerWidth < 768) {
      setSidebarOpen(false);
    }
  }, []);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const handleSend = (text: string) => {
    const userMsgId = Date.now().toString();
    appendMessage({ id: userMsgId, role: "user", content: text });

    const aiMsgId = (Date.now() + 1).toString();
    appendMessage({ id: aiMsgId, role: "assistant", content: "⟳ Thinking..." });

    // On mobile, close sidebar when sending
    if (typeof window !== "undefined" && window.innerWidth < 768) {
      setSidebarOpen(false);
    }

    mutate(
      { query: text, sessionId },
      {
        onSuccess: (data) => {
          // Persist the session ID returned by the backend
          if (data.sessionId) {
            setSessionId(data.sessionId);
          }
          updateMessage(aiMsgId, {
            content: data.answer,
            response: data,
          });
        },
        onError: (err: any) => {
          updateMessage(aiMsgId, {
            content: `Error: ${err.message || "Something went wrong."}`,
          });
        },
      }
    );
  };

  return (
    <div className="flex h-full w-full">
      <Sidebar
        isOpen={sidebarOpen}
        onToggle={() => setSidebarOpen(!sidebarOpen)}
      />

      <main className="flex-1 flex flex-col bg-[#121216] relative min-w-0">
        {/* Mobile header with hamburger */}
        <div
          className={cn(
            "flex items-center gap-3 px-4 py-3 border-b border-gray-800/50 bg-[#121216]/80 backdrop-blur-sm",
            "md:hidden"
          )}
        >
          <button
            type="button"
            onClick={() => setSidebarOpen(true)}
            className="p-1.5 rounded-lg text-gray-400 hover:text-white hover:bg-gray-800 transition-colors"
            title="Open sidebar"
          >
            <Menu className="w-5 h-5" />
          </button>
          <h2 className="text-sm font-semibold text-white flex items-center gap-2">
            <span>⚖️</span> LexPilot
          </h2>
        </div>

        {/* Desktop sidebar toggle when collapsed */}
        {!sidebarOpen && (
          <button
            type="button"
            onClick={() => setSidebarOpen(true)}
            className="hidden md:flex absolute top-4 left-4 z-10 p-2 rounded-lg bg-surface-dark border border-gray-800 text-gray-400 hover:text-white hover:bg-gray-800 transition-colors items-center gap-2 text-xs"
            title="Open sidebar"
          >
            <Menu className="w-4 h-4" />
          </button>
        )}

        <div
          ref={scrollRef}
          className="flex-1 overflow-y-auto p-4 sm:p-8 space-y-6 pb-36 scroll-smooth"
        >
          {messages.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-center px-4 animate-fade-in-up">
              <div className="w-16 h-16 bg-brand-900 rounded-2xl flex items-center justify-center mb-6 shadow-lg shadow-brand-500/20">
                <span className="text-3xl">⚖️</span>
              </div>
              <h2 className="text-2xl font-bold text-white mb-2">Welcome to LexPilot</h2>
              <p className="text-gray-400 max-w-md mb-8">
                Attach up to 5 PDF documents directly in the chat or sidebar, then ask questions to receive AI-grounded, cited legal answers.
              </p>

              {/* Starter questions */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-w-2xl w-full">
                {STARTER_QUESTIONS.map((q, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => handleSend(q)}
                    disabled={isPending}
                    className="text-left px-4 py-3 bg-surface-dark border border-gray-800 rounded-xl text-sm text-gray-300 hover:text-white hover:border-brand-500/50 hover:bg-brand-950/20 transition-all group"
                  >
                    <span className="text-brand-400 mr-2 text-xs opacity-60 group-hover:opacity-100">→</span>
                    {q}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            messages.map((msg) => <ChatMessage key={msg.id} message={msg} />)
          )}
        </div>

        <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-[#121216] via-[#121216] to-transparent p-4 sm:p-8 pt-10">
          <div className="max-w-4xl mx-auto">
            <ChatInput onSend={handleSend} disabled={isPending} />
            <p className="text-center text-xs text-gray-500 mt-2">
              LexPilot may occasionally provide inaccurate information. Please verify important details.
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
