import { useState, useRef, useEffect } from "react";
import { Send, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

interface Props {
  onSend: (message: string) => void;
  disabled?: boolean;
}

export function ChatInput({ onSend, disabled }: Props) {
  const [text, setText] = useState("");
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const handleSubmit = () => {
    if (text.trim() && !disabled) {
      onSend(text.trim());
      setText("");
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.style.height = "auto";
      inputRef.current.style.height = `${Math.min(inputRef.current.scrollHeight, 120)}px`;
    }
  }, [text]);

  return (
    <div className="relative bg-surface-dark border border-gray-800 rounded-xl shadow-lg p-2 flex items-end transition-all focus-within:border-brand-500 focus-within:ring-1 focus-within:ring-brand-500">
      <textarea
        ref={inputRef}
        value={text}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Ask a question about your documents..."
        disabled={disabled}
        className="w-full bg-transparent text-white placeholder-gray-500 resize-none outline-none py-2 px-3 text-sm max-h-[120px] overflow-y-auto"
        rows={1}
      />
      <button
        onClick={handleSubmit}
        disabled={disabled || !text.trim()}
        className={cn(
          "shrink-0 ml-2 p-2.5 rounded-lg transition-colors flex items-center justify-center",
          text.trim() && !disabled 
            ? "bg-brand-600 text-white hover:bg-brand-500" 
            : "bg-gray-800 text-gray-500 cursor-not-allowed"
        )}
      >
        {disabled ? <Loader2 className="w-5 h-5 animate-spin" /> : <Send className="w-5 h-5" />}
      </button>
    </div>
  );
}
