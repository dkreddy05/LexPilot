/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  output: 'standalone',
  env: {
    NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1",
    // LEXPILOT_API_KEY is injected server-side only by the BFF proxy (route.ts).
    // Do NOT expose it with a NEXT_PUBLIC_ prefix — that leaks it into client JS bundles.
  },
};

module.exports = nextConfig;
