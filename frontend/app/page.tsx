"use client";

import { useEffect, useRef } from "react";
import { Sidebar } from "@/components/Sidebar";
import { ChatMessage } from "@/components/Chat/ChatMessage";
import { ChatInput } from "@/components/Chat/ChatInput";
import { useQueryStore } from "@/lib/stores/useQueryStore";
import { useQueryDocuments } from "@/lib/hooks/useQueryDocuments";

export default function Home() {
  const messages = useQueryStore((state) => state.messages);
  const appendMessage = useQueryStore((state) => state.appendMessage);
  const updateMessage = useQueryStore((state) => state.updateMessage);
  const { mutate, isPending } = useQueryDocuments();
  const scrollRef = useRef<HTMLDivElement>(null);

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

    mutate(text, {
      onSuccess: (data) => {
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
    });
  };

  return (
    <div className="flex h-full w-full">
      <Sidebar />
      
      <main className="flex-1 flex flex-col bg-[#121216] relative">
        <div 
          ref={scrollRef}
          className="flex-1 overflow-y-auto p-4 sm:p-8 space-y-6 pb-32 scroll-smooth"
        >
          {messages.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-center px-4 animate-fade-in-up">
              <div className="w-16 h-16 bg-brand-900 rounded-2xl flex items-center justify-center mb-6 shadow-lg shadow-brand-500/20">
                <span className="text-3xl">⚖️</span>
              </div>
              <h2 className="text-2xl font-bold text-white mb-2">Welcome to LexPilot</h2>
              <p className="text-gray-400 max-w-md">
                Upload your legal documents in the sidebar and start asking questions to get AI-powered, cited answers.
              </p>
            </div>
          ) : (
            messages.map((msg) => <ChatMessage key={msg.id} message={msg} />)
          )}
        </div>

        <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-[#121216] via-[#121216] to-transparent p-4 sm:p-8 pt-12">
          <div className="max-w-4xl mx-auto">
            <ChatInput onSend={handleSend} disabled={isPending} />
            <p className="text-center text-xs text-gray-500 mt-3">
              LexPilot may occasionally provide inaccurate information. Please verify important details.
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
