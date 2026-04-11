# nanocode

A minimal, single-file "Claude Code" alternative implemented in Java. Powered by **LangChain4j** and **Google AI Gemini**.

This repository contains two variants of a terminal-based agentic coding assistant that can reason, interact with your local filesystem, execute shell commands, and search the web.

![screenshot](screenshot.png)

## Variants

### 1. Basic Agent (`nanocode_basic.java`)
A robust, monolithic implementation using LangChain4j `AiServices`.
- **Architecture**: Single agent with access to all tools.
- **Library**: Stable `langchain4j-google-ai-gemini`.
- **Ideal for**: Standard coding tasks and stable performance.

### 2. Multi-Agent Supervisor (`nanocode_agentic.java`)
An advanced implementation using the experimental **LangChain4j Agentic** module.
- **Architecture**: A **Supervisor Agent** that orchestrates specialized sub-agents:
    - **`file_specialist`**: Filesystem navigation and manipulation.
    - **`system_specialist`**: Shell command execution and system management.
    - **`web_searcher`**: Internet research via Gemini's built-in Google Search.
- **Library**: Experimental `langchain4j-agentic`.
- **Ideal for**: Complex, multi-step tasks requiring specialized reasoning.

## Core Features

- **Full Agentic Loop**: Autonomous reasoning and tool usage.
- **Modern Java**: Fully optimized for **Java 25** preview features (uses `java.lang.IO`).
- **Gemini 3 Ready**: Configured for `gemini-3-flash-preview` with "thinking" and "thought signatures" enabled.
- **Pretty Rendering**: ANSI-highlighted Markdown output (optimized for dark terminals).
- **Interactive UI**: Distinguishable **bright yellow** user input and clear tool execution logs.

## Requirements

- **Java 25+**: (Uses preview features like Implicitly Declared Classes and the new `IO` class).
- **[JBang](https://jbang.dev)**: For running the single-file scripts.
- **API Key**: A valid Google AI Gemini API key.

## Usage

```bash
export GOOGLE_AI_GEMINI_API_KEY="your-key"

# Run the basic variant
./nanocode_basic.java

# OR run the multi-agent variant
./nanocode_agentic.java
```

## Tools

| Tool | Description |
|------|-------------|
| `read` | Read file with line numbers, offset/limit |
| `write` | Create or overwrite files |
| `edit` | Replace strings in files (with uniqueness validation) |
| `glob` | Find files by pattern (sorted by modification time) |
| `grep` | Search file contents for regex patterns |
| `bash` | Run arbitrary shell commands (supports optional `dir`) |
| `websearch` | Perform live internet research using Google Search |

## Commands

- `/c` - Clear conversation history / reset agentic state.
- `/q` or `exit` - Quit the application.

## License

MIT
