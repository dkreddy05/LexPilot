import { NextRequest, NextResponse } from "next/server";

const BACKEND_BASE_URL =
  process.env.INTERNAL_API_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080/api/v1";

const API_KEY =
  process.env.LEXPILOT_API_KEY ??
  process.env.NEXT_PUBLIC_LEXPILOT_API_KEY ??
  "";

async function proxyRequest(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> }
) {
  const { path } = await params;
  const targetPath = path.join("/");
  const searchParams = request.nextUrl.search;
  const targetUrl = `${BACKEND_BASE_URL.replace(/\/+$/, "")}/${targetPath}${searchParams}`;

  const headers: Record<string, string> = {};

  // Copy relevant request headers
  const contentType = request.headers.get("content-type");
  if (contentType) {
    headers["content-type"] = contentType;
  }

  // Inject backend master API key server-side
  if (API_KEY) {
    headers["X-Api-Key"] = API_KEY;
  }

  const method = request.method;
  let body: BodyInit | undefined = undefined;

  if (method !== "GET" && method !== "HEAD") {
    // Read raw body as ArrayBuffer to support both JSON and Multipart File uploads
    body = await request.arrayBuffer();
  }

  try {
    const upstreamResponse = await fetch(targetUrl, {
      method,
      headers,
      body,
    });

    const responseHeaders = new Headers();
    const upstreamContentType = upstreamResponse.headers.get("content-type");
    if (upstreamContentType) {
      responseHeaders.set("content-type", upstreamContentType);
    }

    const responseBody = await upstreamResponse.arrayBuffer();

    return new NextResponse(responseBody, {
      status: upstreamResponse.status,
      headers: responseHeaders,
    });
  } catch (error: any) {
    return NextResponse.json(
      {
        error: "Gateway Proxy Error",
        message: error.message || "Failed to reach upstream backend service",
      },
      { status: 502 }
    );
  }
}

export const GET = proxyRequest;
export const POST = proxyRequest;
export const PUT = proxyRequest;
export const DELETE = proxyRequest;
export const PATCH = proxyRequest;
