package org.mathieucuvelier.CIViewerCLI.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StepDto(String name, String status, String conclusion, Integer number, @JsonProperty("started_at") String startedAt,
        @JsonProperty("completed_at") String completedAt) {
}
