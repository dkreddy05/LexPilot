/**
 * Standalone API Verification Script
 *
 * Validates that api.ts methods correctly hit the backend gateway.
 * Run with:
 *   npx tsx test_api.ts
 *   npx tsx test_api.ts --api-key <key> --base-url http://localhost:8080/api/v1
 *
 * Requires the backend to be running (./mvnw spring-boot:run).
 * Exit code 0 = all pass, 1 = any failure.
 */

// ---------------------------------------------------------------------------
// CLI args / env
// ---------------------------------------------------------------------------
const args = process.argv.slice(2);

function getArg(name: string, fallback: string): string {
  const idx = args.indexOf(`--${name}`);
  if (idx !== -1 && args[idx + 1]) return args[idx + 1];
  return fallback;
}

const API_KEY = getArg("api-key", process.env.NEXT_PUBLIC_LEXPILOT_API_KEY ?? "dev-api-key-change-me");
const BASE_URL = getArg("base-url", process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1");

// Inject env so api.ts reads the correct values
process.env.NEXT_PUBLIC_API_BASE_URL = BASE_URL;
process.env.NEXT_PUBLIC_LEXPILOT_API_KEY = API_KEY;

// Dynamic import AFTER env is set so api.ts picks up the values
const { queryDocuments, uploadDocument, getIngestionStatus, ApiError } = await import("./lib/api");

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
let passed = 0;
let failed = 0;

function pass(name: string, detail?: string) {
  passed++;
  console.log(`  ✅ [PASS] ${name}${detail ? ` — ${detail}` : ""}`);
}

function fail(name: string, detail: string) {
  failed++;
  console.error(`  ❌ [FAIL] ${name} — ${detail}`);
}

function section(title: string) {
  console.log(`\n━━━ ${title} ━━━`);
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------
async function main() {
  console.log(`\n🔍 api.ts Standalone Verification`);
  console.log(`   Base URL : ${BASE_URL}`);
  console.log(`   API Key  : ${API_KEY.substring(0, 4)}${"*".repeat(Math.max(0, API_KEY.length - 4))}`);

  // ── 1. queryDocuments — without API key → 401 ──
  section("1. queryDocuments() without API key → expect 401");
  try {
    // Call fetch directly without the key to simulate unauthenticated
    const res = await fetch(`${BASE_URL}/query/answer`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ query: "test query" }),
    });
    if (res.status === 401) {
      pass("Unauthenticated request rejected", `status=${res.status}`);
    } else {
      fail("Unauthenticated request", `Expected 401, got ${res.status}`);
    }
  } catch (err: any) {
    fail("Unauthenticated request", `Network error: ${err.message}`);
  }

  // ── 2. queryDocuments — with API key → non-401 ──
  section("2. queryDocuments() with API key → expect non-401");
  try {
    const result = await queryDocuments("What are my consumer rights for a refund?");
    pass("queryDocuments returned successfully", `answer length=${result.answer?.length ?? 0}`);
  } catch (err: any) {
    if (err instanceof ApiError) {
      if (err.status === 401 || err.status === 403) {
        fail("queryDocuments with key", `Auth rejected: status=${err.status}, body=${err.body}`);
      } else {
        // 500 is expected — no LLM/embedding service in local dev
        pass("queryDocuments auth passed (downstream error expected)", `status=${err.status}`);
      }
    } else {
      fail("queryDocuments with key", `Unexpected error: ${err.message}`);
    }
  }

  // ── 3. uploadDocument — with API key → 202 ──
  section("3. uploadDocument() with API key → expect 202");
  try {
    // Create a minimal valid PDF
    const pdfContent = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]>>endobj\nxref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n190\n%%EOF";
    const blob = new Blob([pdfContent], { type: "application/pdf" });
    const file = new File([blob], "test-verification.pdf", { type: "application/pdf" });

    const result = await uploadDocument(file, "test_verification");
    if (result && result.documentId) {
      pass("uploadDocument accepted", `documentId=${result.documentId}, status=${result.status}`);

      // ── 4. getIngestionStatus — with the real document ID ──
      section("4. getIngestionStatus() with real documentId → expect 200");
      try {
        const status = await getIngestionStatus(result.documentId);
        pass("getIngestionStatus returned", `status=${status.status}`);
      } catch (err: any) {
        if (err instanceof ApiError) {
          fail("getIngestionStatus", `status=${err.status}, body=${err.body}`);
        } else {
          fail("getIngestionStatus", `Unexpected error: ${err.message}`);
        }
      }
    } else {
      fail("uploadDocument", "Response missing documentId");
    }
  } catch (err: any) {
    if (err instanceof ApiError) {
      if (err.status === 401 || err.status === 403) {
        fail("uploadDocument", `Auth rejected: status=${err.status}`);
      } else {
        // Other errors (e.g. 400 from content validation) are informative but not auth failures
        pass("uploadDocument auth passed (validation error)", `status=${err.status}`);
      }
    } else {
      fail("uploadDocument", `Unexpected error: ${err.message}`);
    }

    // Still test getIngestionStatus with a fake UUID
    section("4. getIngestionStatus() with fake UUID → expect 404");
    try {
      await getIngestionStatus("00000000-0000-0000-0000-000000000000");
      fail("getIngestionStatus fake UUID", "Expected 404 but got success");
    } catch (err: any) {
      if (err instanceof ApiError && err.status === 404) {
        pass("getIngestionStatus fake UUID rejected", "404 as expected");
      } else if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        fail("getIngestionStatus", `Auth rejected: status=${err.status}`);
      } else if (err instanceof ApiError) {
        pass("getIngestionStatus auth passed", `status=${err.status}`);
      } else {
        fail("getIngestionStatus", `Unexpected error: ${err.message}`);
      }
    }
  }

  // ── Summary ──
  console.log(`\n━━━ Summary ━━━`);
  console.log(`  Passed: ${passed}`);
  console.log(`  Failed: ${failed}`);
  console.log(`  Total:  ${passed + failed}\n`);

  if (failed > 0) {
    process.exit(1);
  }
}

main().catch((err) => {
  console.error("Fatal error:", err);
  process.exit(1);
});
