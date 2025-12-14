package org.mathieucuvelier.CIViewerCLI.persistence;

import java.util.HashMap;
import java.util.Map;

public record MonitorState(
    long lastCheckTimestamp,
    Map<Long, RunState> knownRuns
) {
    public static MonitorState empty() {
        return new MonitorState(0, new HashMap<>());
    }
}
