package org.mathieucuvelier.CIViewerCLI.models;

import java.time.LocalDateTime;

public record WorkflowRunDTO(Long id, String name, String headBranch, String headSha, String status, String conclusion, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
