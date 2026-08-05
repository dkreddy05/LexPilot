package com.lexpilot.gateway.controller;

import com.lexpilot.common.dto.QueryRequest;
import com.lexpilot.common.dto.QueryResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class QueryController {

    public QueryController() {
    }

    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@Valid @RequestBody QueryRequest request) {
        // TODO: Wire RAG retrieval and generation pipeline
        return ResponseEntity.ok(new QueryResponse(
                "TODO: RAG answer stub",
                List.of(),
                false,
                null
        ));
    }
}
