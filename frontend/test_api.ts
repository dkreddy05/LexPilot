/**
 * Standalone API Test Script
 * Run this with: npx tsx test_api.ts
 * Requires the backend to be running on http://localhost:8080
 */
import { queryDocuments, uploadDocument, getIngestionStatus, ApiError } from "./lib/api";

async function runTests() {
  try {
    console.log("Testing queryDocuments()...");
    const result = await queryDocuments("How do I get a refund?");
    console.log("Unexpected success:", result);
  } catch (error) {
    if (error instanceof ApiError) {
      console.log(`[PASS] Caught expected ApiError on queryDocuments (Status: ${error.status})`);
      console.log(`Error body: ${error.body}`);
    } else {
      console.log(`[FAIL] Caught unexpected error type:`, error);
    }
  }

  console.log("\n-----------------------\n");

  // 2. Test uploadDocument
  let testDocumentId = "dummy-uuid";
  try {
    console.log("Testing uploadDocument()...");
    // We pass a fake File object to satisfy TS
    const fakeFile = new File(["dummy content"], "test.pdf", { type: "application/pdf" });
    const result = await uploadDocument(fakeFile, "manual_upload");
    console.log("[PASS] uploadDocument returned:", result);
    if (result && result.documentId) {
       testDocumentId = result.documentId;
    }
  } catch (error) {
    if (error instanceof ApiError) {
      console.log(`[PASS? or FAIL?] Caught expected ApiError on uploadDocument (Status: ${error.status}) - body: ${error.body}`);
    } else {
       console.log(`[FAIL] Caught unexpected error:`, error);
    }
  }

  console.log("\n-----------------------\n");

  // 3. Test getIngestionStatus
  try {
    console.log(`Testing getIngestionStatus() for ID: ${testDocumentId}...`);
    const status = await getIngestionStatus(testDocumentId);
    console.log("[PASS] getIngestionStatus returned:", status);
  } catch (error) {
    if (error instanceof ApiError) {
       console.log(`[PASS? or FAIL?] Caught expected ApiError on getIngestionStatus (Status: ${error.status}) - body: ${error.body}`);
    } else {
       console.log(`[FAIL] Caught unexpected error:`, error);
    }
  }

  console.log("\n-----------------------\n");

  // 4. Test CORS
  try {
    console.log("Testing CORS with an OPTIONS preflight to /api/v1/query/answer...");
    const optionsRes = await fetch("http://localhost:8080/api/v1/query/answer", {
      method: "OPTIONS",
      headers: {
        "Origin": "http://localhost:3000",
        "Access-Control-Request-Method": "POST",
      },
    });

    if (optionsRes.ok && optionsRes.headers.get("access-control-allow-origin") === "http://localhost:3000") {
      console.log("[PASS] CORS preflight successful and origin is allowed.");
    } else {
      console.log(`[FAIL] CORS preflight failed or missing headers. Status: ${optionsRes.status}`);
      console.log(`Access-Control-Allow-Origin: ${optionsRes.headers.get("access-control-allow-origin")}`);
    }
  } catch (error) {
    console.log(`[FAIL] Network error during CORS test:`, error);
  }

}

runTests();
