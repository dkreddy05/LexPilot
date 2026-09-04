package com.lexpilot.graph.entity;

/**
 * Status constants for the repository analysis pipeline.
 * Matches the {@code analysis_status} column in the {@code graph_repositories} table.
 */
public final class AnalysisStatus {

    private AnalysisStatus() {
        // utility class — no instantiation
    }

    /** Repository registered but analysis has not started. */
    public static final String PENDING = "PENDING";

    /** Analysis engine is actively processing the repository. */
    public static final String ANALYZING = "ANALYZING";

    /** Analysis completed successfully; graph data is available. */
    public static final String COMPLETED = "COMPLETED";

    /** An error occurred during analysis; see {@code error_detail}. */
    public static final String FAILED = "FAILED";
}
