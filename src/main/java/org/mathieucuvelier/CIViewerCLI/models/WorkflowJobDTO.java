package org.mathieucuvelier.CIViewerCLI.models;

import java.util.List;

import org.mathieucuvelier.CIViewerCLI.persistence.JobState;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowJobDTO(Long id, String name, String status, String conclusion,
                @JsonProperty("started_at") String startedAt, @JsonProperty("completed_at") String completedAt,
                List<StepDto> steps) {
}