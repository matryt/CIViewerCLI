# GitHub Workflow Monitor CLI

A real-time command-line tool for monitoring GitHub Actions workflows, jobs, and steps with persistent state tracking across multiple repositories.

## Features

- **Real-time monitoring** - Polls GitHub Actions every 5 seconds to capture workflow state changes
- **Complete event tracking** - Reports workflows being queued, jobs starting/finishing, and individual step execution
- **Persistent state** - Resumes monitoring from the last checkpoint using SQLite storage
- **Multi-repository support** - Each repository maintains independent state
- **Graceful shutdown** - Handles Ctrl+C interruption cleanly
- **Detailed output** - Shows timestamps, branch names, commit SHAs, and completion status
- **Failure detection** - Clearly identifies failed steps and workflows

## Requirements

- Java 25 or later
- Maven 3.6+
- GitHub Personal Access Token with `repo` scope

## Building

```bash
mvn clean package
```

This creates an executable JAR: `target/CIViewerCLI-1.0-SNAPSHOT.jar`

## Usage

```bash
java -jar target/CIViewerCLI-1.0-SNAPSHOT.jar <owner> <repo> <github_token>
```

### Parameters

- `owner` - GitHub repository owner (user or organization)
- `repo` - Repository name
- `github_token` - GitHub Personal Access Token (create at https://github.com/settings/tokens)

### Example

```bash
java -jar target/CIViewerCLI-1.0-SNAPSHOT.jar microsoft vscode ghp_xxxxxxxxxxxxxxxxxxxx
```

## Output Format

Each event is displayed on a single line with the following format:

```
[timestamp] EVENT_TYPE - workflow / job / step - status (conclusion) - branch@commit
```

### Event Types

- `WORKFLOW_STARTED` - Workflow has been queued or started
- `WORKFLOW_COMPLETED` - Workflow finished (success/failure)
- `JOB_STARTED` - Job within a workflow started
- `JOB_COMPLETED` - Job finished
- `STEP_STARTED` - Individual step started
- `STEP_COMPLETED` - Step finished successfully
- `STEP_FAILED` - Step failed

### Example Output

```
[2025-12-18 21:33:23 CET] WORKFLOW_STARTED - Build React App - queued - main@5d57fc9
[2025-12-18 21:33:23 CET] JOB_STARTED - Build React App / build - in_progress - main@5d57fc9
[2025-12-18 21:33:23 CET] STEP_COMPLETED - Build React App / build / Set up job - completed (success) - main@5d57fc9
[2025-12-18 21:33:23 CET] STEP_STARTED - Build React App / build / Setup Node.js - in_progress - main@5d57fc9
Monitoring... (1 polls, no events)
Monitoring... (2 polls, no events)
[2025-12-18 21:33:39 CET] WORKFLOW_COMPLETED - Build React App - completed (failure) - main@5d57fc9
[2025-12-18 21:33:39 CET] JOB_COMPLETED - Build React App / build - completed (failure) - main@5d57fc9
[2025-12-18 21:33:39 CET] STEP_COMPLETED - Build React App / build / Setup Node.js - completed (success) - main@5d57fc9
[2025-12-18 21:33:39 CET] STEP_FAILED - Build React App / build / Build - completed (failure) - main@5d57fc9
```

## Behavior

### First Run

When monitoring a repository for the first time:
- Initializes state with current workflow status
- Only reports **new events** going forward
- Does not display historical workflows

### Subsequent Runs

When restarted for the same repository:
- Resumes from the last checkpoint timestamp
- Reports all workflow/job/step completion events that occurred since the last run
- Maintains separate state for each repository

### Interruption

Press `Ctrl+C` to stop monitoring gracefully. The tool saves its state before exiting.

## Architecture

- **Language**: Java 25
- **Build Tool**: Maven
- **Persistence**: SQLite (stores state in `.civiewer/state.db`)
- **HTTP Client**: Java 11+ HttpClient
- **JSON Processing**: Jackson

### Project Structure

```
src/main/java/org/mathieucuvelier/CIViewerCLI/
├── Main.java                      # Entry point
├── models/                        # DTOs and domain models
│   ├── Event.java                 # Event representation with factory methods
│   ├── EventType.java             # Event type enumeration
│   ├── WorkflowRunDTO.java        # GitHub workflow run data
│   ├── WorkflowJobDTO.java        # GitHub job data
│   └── StepDto.java               # GitHub step data
├── persistence/                   # State management
│   ├── DatabaseManager.java       # SQLite connection and schema
│   ├── StateManager.java          # Load/save operations
│   ├── MonitorState.java          # State snapshot
│   ├── RunState.java              # Workflow run state
│   ├── JobState.java              # Job state
│   └── StepState.java             # Step state
├── service/                       # Core logic
│   ├── WorkflowMonitor.java       # Main monitoring loop
│   ├── EventDetector.java         # State change detection
│   └── GithubClient.java          # GitHub API client
└── utils/                         # Utilities
    ├── AnsiColors.java            # Terminal colors
    └── ArgumentsParser.java       # CLI argument parsing
```

## Dependencies

- Jackson (JSON processing)
- SQLite JDBC driver
- Java 25 standard library

## License

This project is provided as-is for educational and demonstration purposes.
