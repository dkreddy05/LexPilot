"use client";

import { useState, useEffect, useCallback, createContext, useContext } from "react";
import { CheckCircle2, AlertCircle, Info, X } from "lucide-react";
import { cn } from "@/lib/utils";

type ToastType = "success" | "error" | "info";

interface Toast {
  id: string;
  type: ToastType;
  message: string;
  dismissing?: boolean;
}

interface ToastContextValue {
  showToast: (type: ToastType, message: string) => void;
}

const ToastContext = createContext<ToastContextValue>({
  showToast: () => {},
});

export function useToast() {
  return useContext(ToastContext);
}

const ICONS: Record<ToastType, typeof CheckCircle2> = {
  success: CheckCircle2,
  error: AlertCircle,
  info: Info,
};

const STYLES: Record<ToastType, string> = {
  success: "bg-emerald-950/90 border-emerald-500/30 text-emerald-200",
  error: "bg-red-950/90 border-red-500/30 text-red-200",
  info: "bg-brand-950/90 border-brand-500/30 text-brand-200",
};

const ICON_STYLES: Record<ToastType, string> = {
  success: "text-emerald-400",
  error: "text-red-400",
  info: "text-brand-400",
};

function ToastItem({ toast, onDismiss }: { toast: Toast; onDismiss: (id: string) => void }) {
  const Icon = ICONS[toast.type];

  return (
    <div
      className={cn(
        "flex items-center gap-3 px-4 py-3 rounded-xl border backdrop-blur-md shadow-lg max-w-sm",
        STYLES[toast.type],
        toast.dismissing
          ? "animate-[toast-slide-out_0.25s_ease-in_forwards]"
          : "animate-[toast-slide-in_0.3s_ease-out_forwards]"
      )}
    >
      <Icon className={cn("w-4 h-4 shrink-0", ICON_STYLES[toast.type])} />
      <span className="text-sm font-medium flex-1">{toast.message}</span>
      <button
        onClick={() => onDismiss(toast.id)}
        className="shrink-0 p-0.5 rounded hover:bg-white/10 transition-colors"
      >
        <X className="w-3.5 h-3.5" />
      </button>
    </div>
  );
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismiss = useCallback((id: string) => {
    setToasts((prev) =>
      prev.map((t) => (t.id === id ? { ...t, dismissing: true } : t))
    );
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 250);
  }, []);

  const showToast = useCallback(
    (type: ToastType, message: string) => {
      const id = Date.now().toString() + Math.random().toString(36).slice(2);
      setToasts((prev) => [...prev, { id, type, message }]);
      setTimeout(() => dismiss(id), 4000);
    },
    [dismiss]
  );

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      {/* Toast container — fixed at top center */}
      {toasts.length > 0 && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[100] flex flex-col gap-2 pointer-events-auto">
          {toasts.map((toast) => (
            <ToastItem key={toast.id} toast={toast} onDismiss={dismiss} />
          ))}
        </div>
      )}
    </ToastContext.Provider>
  );
}
