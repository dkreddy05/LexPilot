package com.lexpilot.generation.citation;

import com.lexpilot.common.dto.QueryResponse.Citation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CitationFormatter {

    public List<Citation> format(String llmAnswer, List<String> contextChunks) {
        // TODO: Parse inline citation markers and map to chunk metadata
        throw new UnsupportedOperationException("CitationFormatter.format() not yet implemented");
    }
}
