import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "LexPilot — Ask Your Legal Question",
  description: "Get instant, cited answers to consumer protection, RBI grievance, and tenant dispute questions.",
};

export default function QueryPage() {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen px-4 py-16">
      <div className="max-w-3xl w-full space-y-8">
        <div className="text-center space-y-3">
          <h1 className="text-4xl font-bold tracking-tight text-white">
            LexPilot
          </h1>
          <p className="text-lg text-[var(--color-muted)]">
            Ask about your rights — Consumer Protection, RBI Grievances, Tenant Disputes.
          </p>
        </div>

        <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
          <p className="text-[var(--color-muted)] text-sm text-center">
            Query input interface stub.
          </p>
        </div>

        <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6 min-h-32">
          <p className="text-[var(--color-muted)] text-sm text-center">
            Answer + citations stub.
          </p>
        </div>
      </div>
    </div>
  );
}
