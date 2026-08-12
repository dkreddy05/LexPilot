import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { QueryProvider } from "@/lib/providers/QueryProvider";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "LexPilot — Legal Rights Assistant",
  description: "AI-powered RAG assistant for Indian consumer protection, banking/RBI grievances, and tenant dispute rights.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="dark">
      <body className={`${inter.className} bg-surface-darker text-white h-screen overflow-hidden flex`}>
        <QueryProvider>
          {children}
        </QueryProvider>
      </body>
    </html>
  );
}
