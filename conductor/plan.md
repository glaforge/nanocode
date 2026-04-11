# LangChain4j and Gemini Migration Plan

## Objective
Refactor `nanocode.java` to use the LangChain4j library for its core agentic loop, transitioning from raw HTTP API calls to the Anthropic/OpenRouter APIs to LangChain4j's high-level `AiServices` and `@Tool` annotations. Change the default AI model integration to Google AI Gemini (`gemini-3-flash-preview`).

## Key Files & Context
- `nanocode.java`: The single file that will be completely refactored.
- Dependencies will shift from `jackson-databind` to LangChain4j core and Gemini integrations (`dev.langchain4j:langchain4j:1.13.0` and `dev.langchain4j:langchain4j-google-ai-gemini:1.13.0`).

## Implementation Steps

1. **Update JBang Directives & Imports:**
   - Remove the Jackson `//DEPS`.
   - Add `//DEPS dev.langchain4j:langchain4j:1.13.0` and `//DEPS dev.langchain4j:langchain4j-google-ai-gemini:1.13.0`.
   - Update `import` statements to include necessary LangChain4j classes (`AiServices`, `ChatLanguageModel`, `Tool`, `SystemMessage`, `MessageWindowChatMemory`, etc.).

2. **Define Agent Interface & Configuration:**
   - Create an interface `Assistant` with a `chat` method.
   - Use `@SystemMessage` to define the system prompt ("Concise coding assistant. cwd: {{cwd}}").
   - Configure a `GoogleAiGeminiChatModel` using `GEMINI_API_KEY` (or fallback to an existing default if available, though Gemini requires its own key). Set the model name to `gemini-3-flash-preview`.

3. **Refactor Tools into a Class:**
   - Create a `Tools` class.
   - Migrate existing `toolRead`, `toolWrite`, `toolEdit`, `toolGlob`, `toolGrep`, and `toolBash` methods into this class.
   - Replace `JsonNode` parameters with strongly-typed Java parameters (e.g., `String path`, `Integer offset`).
   - Annotate each method with `@Tool(...)` containing the tool's description.
   - To maintain the existing terminal UI (showing tool execution logs), add `System.out.println(...)` statements *inside* these tool methods to print their invocation and the preview of their returned results before returning.

4. **Update the Main Loop:**
   - Replace the manual `callApi` and JSON message array management with an `AiServices` builder that wires up the model, memory (e.g., `MessageWindowChatMemory`), and `Tools`.
   - To support clearing the conversation history (`/c`), manage `memoryId` via an incrementing counter or recreate the `ChatMemory`.
   - Pass user input to the `Assistant.chat(...)` method and print the final assistant response.

5. **Cleanup:**
   - Remove manual JSON schema definition (`SCHEMA` string).
   - Remove raw HTTP connection and `ObjectMapper` code.
   - Ensure the UI separators and colored text utility functions continue to work correctly.

## Verification & Testing
- Run `jbang nanocode.java` with a valid `GEMINI_API_KEY`.
- Verify the interactive loop starts correctly.
- Test a basic query ("what files are here?") to ensure the `glob` or `bash` tool is invoked and logged appropriately.
- Check if clearing the conversation with `/c` successfully resets context.
- Verify file reading and writing tools work by issuing a command to create and read a temporary file.