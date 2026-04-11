# LangChain4j Agentic Migration Plan

## Objective
Refactor `nanocode.java` to leverage the experimental `langchain4j-agentic` module. This will transition the application from a monolithic `AiServices` instance (where one agent has all tools) to a **Multi-Agent Supervisor Architecture**. The Supervisor will coordinate specialized sub-agents (File, System, Search) to accomplish complex tasks more autonomously and reliably.

## Key Files & Context
- `nanocode.java`: The core JBang script to be refactored.
- **Dependencies**: Need to add `dev.langchain4j:langchain4j-agentic:1.13.0`.

## Proposed Architecture

1. **Specialized Tool Classes**:
   Split the current massive `Tools` class into smaller, focused classes:
   - `FileTools`: Contains `read`, `write`, `edit`, `glob`, and `grep`.
   - `SystemTools`: Contains `bash`.
   - `SearchTools`: Contains the `websearch` logic (instantiating its own model to avoid global search conflicts).

2. **Sub-Agent Interfaces**:
   Define interfaces for the sub-agents so they can be orchestrated by the Supervisor:
   - `FileAgent`: Built with `FileTools`.
   - `SystemAgent`: Built with `SystemTools`.
   - `WebSearchAgent`: Built with `SearchTools`.

3. **The Supervisor Agent**:
   Replace the current `Assistant` interface with a `SupervisorAgent`.
   - The Supervisor will be built using `AgenticServices.supervisorBuilder()`.
   - It will use the primary `GoogleAiGeminiChatModel` (gemini-3-flash-preview).
   - It will be configured with `.responseStrategy(SupervisorResponseStrategy.LAST)` to ensure the user sees the final output of the last executing agent rather than a dry summary.

4. **Observability (Optional but Recommended)**:
   - Attach an `AgentMonitor` to the Supervisor to log the execution path (e.g., "Supervisor -> SystemAgent -> FileAgent -> WebSearchAgent"). This provides transparency into *how* the agent solves complex problems.

## Implementation Steps

1. **Update JBang Header**:
   - Add `//DEPS dev.langchain4j:langchain4j-agentic:1.13.0`.
   - Add necessary imports for the agentic module (`dev.langchain4j.agentic.agent.*`, `dev.langchain4j.agentic.builder.*`, etc.).

2. **Refactor Tools**:
   - Break apart the `Tools` class into `FileTools` and `SystemTools`.
   - Keep the `println` logging (with ANSI colors) inside the tool methods so the user still sees when a tool is invoked.

3. **Define and Build Sub-Agents**:
   - Create interfaces (`FileAgent`, `SystemAgent`, `SearchAgent`) extending `AgentInstance` or defining `@Agent` methods.
   - In `main()`, use `AgenticServices.agentBuilder(Interface.class)` to instantiate them, wiring them up with their respective tool classes and a base model.

4. **Build the Supervisor**:
   - Replace the `AiServices.builder()` block with `AgenticServices.supervisorBuilder()`.
   - Inject the sub-agents into the supervisor.

5. **Update the Main Loop**:
   - Change `assistant.chat(cwd, input)` to `supervisor.invoke(input)` (or the equivalent method signature defined for the Supervisor).
   - Ensure the `/c` (clear memory) command properly evicts or clears the `AgenticScope` for the current session.

## Verification & Testing
- Run `./nanocode.java` to ensure compilation succeeds.
- Test a simple query: `echo hello world` to verify `SystemAgent` usage.
- Test a complex query: `find the latest langchain4j release on the web, then write a pom.xml snippet to a new file called deps.txt` to verify the Supervisor correctly orchestrates `SearchAgent` followed by `FileAgent`.
- Ensure all markdown formatting and ANSI colors remain perfectly intact.
