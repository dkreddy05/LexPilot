/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  output: 'standalone',
  env: {
    NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1",
    NEXT_PUBLIC_LEXPILOT_API_KEY: process.env.NEXT_PUBLIC_LEXPILOT_API_KEY ?? "",
  },
};

module.exports = nextConfig;
