import { User, Bot } from "lucide-react";
import { cn } from "@/lib/utils";
import type { Message } from "@/lib/stores/useQueryStore";
import { CitationsExpander } from "./CitationsExpander";

export function ChatMessage({ message }: { message: Message }) {
  const isUser = message.role === "user";

  return (
    <div className={cn(
      "flex w-full animate-fade-in-up",
      isUser ? "justify-end" : "justify-start"
    )}>
      <div className={cn(
        "flex max-w-[80%] gap-4 p-5 rounded-2xl shadow-sm",
        isUser 
          ? "bg-brand-600 text-white rounded-br-sm" 
          : "bg-surface-light text-gray-800 dark:bg-surface-dark dark:text-gray-100 rounded-bl-sm border border-gray-100 dark:border-gray-800"
      )}>
        {!isUser && (
          <div className="shrink-0 w-8 h-8 rounded-full bg-brand-100 dark:bg-brand-900 flex items-center justify-center">
            <Bot className="w-5 h-5 text-brand-600 dark:text-brand-400" />
          </div>
        )}
        
        <div className="flex-1 space-y-2 overflow-hidden">
          <div className="whitespace-pre-wrap text-sm leading-relaxed">
            {message.content}
          </div>
          
          {message.response && (
            <CitationsExpander 
              citations={message.response.citations} 
              lowConfidence={message.response.lowConfidence} 
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
