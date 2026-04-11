# nanocode

Minimal Claude Code alternative. Single Java file, runnable with [jbang](https://jbang.dev), zero..eh..minimal dependencies via LangChain4j, ~200 lines.

Refactored to use [LangChain4j](https://github.com/langchain4j/langchain4j) and [Google AI Gemini](https://ai.google.dev/).

![screenshot](screenshot.png)

## Features

- Full agentic loop with tool use (powered by LangChain4j `AiServices`)
- Tools: `read`, `write`, `edit`, `glob`, `grep`, `bash`
- Conversation history (windowed)
- Colored terminal output

## Usage

```bash
export GOOGLE_AI_GEMINI_API_KEY="your-key"
jbang nanocode.java
```

### Configuration

To use a different model:

```bash
export MODEL="gemini-1.5-pro"
jbang nanocode.java
```

## Commands

- `/c` - Clear conversation history
- `/q` or `exit` - Quit

## Tools

| Tool | Description |
|------|-------------|
| `read` | Read file with line numbers, offset/limit |
| `write` | Write content to file |
| `edit` | Replace string in file (must be unique) |
| `glob` | Find files by pattern, sorted by mtime |
| `grep` | Search files for regex |
| `bash` | Run shell command |

## License

MIT
