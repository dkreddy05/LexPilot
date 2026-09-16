"use client";

import { signIn } from "next-auth/react";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { Scale, Loader2 } from "lucide-react";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const router = useRouter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const res = await signIn("credentials", {
        redirect: false,
        email,
        password,
      });

      if (res?.error) {
        setError("Invalid email or password");
      } else {
        router.push("/");
        router.refresh();
      }
    } catch (err) {
      setError("An unexpected error occurred");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full bg-slate-950 flex flex-col items-center justify-center p-4 relative overflow-hidden">
      {/* Decorative background gradients */}
      <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] rounded-full bg-emerald-500/10 blur-[120px] pointer-events-none" />
      <div className="absolute bottom-[-20%] right-[-10%] w-[50%] h-[50%] rounded-full bg-teal-500/10 blur-[120px] pointer-events-none" />

      <div className="w-full max-w-md bg-slate-900/80 backdrop-blur-xl border border-slate-800 p-8 rounded-2xl shadow-2xl z-10">
        <div className="flex flex-col items-center mb-8">
          <div className="h-12 w-12 bg-gradient-to-br from-emerald-400 to-teal-600 rounded-xl flex items-center justify-center mb-4 shadow-lg shadow-emerald-900/20">
            <Scale className="text-white h-6 w-6" />
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Welcome to LexPilot</h1>
          <p className="text-slate-400 mt-2 text-sm text-center">
            Your AI-powered legal & grievance rights assistant.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">
              Email Address
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full bg-slate-950/50 border border-slate-700 text-white rounded-lg px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500 transition-colors placeholder:text-slate-600"
              placeholder="you@example.com"
            />
          </div>

          <div>
            <div className="flex justify-between items-center mb-1.5">
              <label className="block text-sm font-medium text-slate-300">
                Password
              </label>
              <a href="#" className="text-xs text-emerald-400 hover:text-emerald-300 transition-colors">
                Forgot password?
              </a>
            </div>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full bg-slate-950/50 border border-slate-700 text-white rounded-lg px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500 transition-colors"
              placeholder="••••••••"
            />
          </div>

          {error && (
            <div className="bg-red-500/10 border border-red-500/20 text-red-400 text-sm p-3 rounded-lg flex items-start">
              <span className="block sm:inline">{error}</span>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-white font-medium py-2.5 px-4 rounded-lg shadow-lg shadow-emerald-500/20 transition-all focus:outline-none focus:ring-2 focus:ring-emerald-500/50 disabled:opacity-70 disabled:cursor-not-allowed flex items-center justify-center h-[46px]"
          >
            {loading ? (
              <Loader2 className="h-5 w-5 animate-spin text-white" />
            ) : (
              "Sign In"
            )}
          </button>
        </form>

        <div className="mt-8 text-center text-sm text-slate-400">
          Don't have an account?{" "}
          <a href="#" className="text-emerald-400 hover:text-emerald-300 font-medium transition-colors">
            Sign up
          </a>
        </div>
      </div>
    </div>
  );
}
