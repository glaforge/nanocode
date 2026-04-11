///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//COMPILE_OPTIONS --enable-preview -source 25
//RUNTIME_OPTIONS --enable-preview
//DEPS dev.langchain4j:langchain4j:1.13.0
//DEPS dev.langchain4j:langchain4j-core:1.13.0
//DEPS dev.langchain4j:langchain4j-google-ai-gemini:1.13.0
//DEPS org.slf4j:slf4j-simple:2.0.17

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.io.*;

import static java.lang.System.getenv;
import static java.nio.file.Files.*;
import static java.lang.IO.*;

import dev.langchain4j.agent.tool.*;
import dev.langchain4j.memory.chat.*;
import dev.langchain4j.model.chat.*;
import dev.langchain4j.model.googleai.*;
import dev.langchain4j.service.*;

/**
 * nanocode - minimal CLI coding agent, powered by LangChain4j.
 * Original: https://github.com/1rgs/nanocode and https://github.com/maxandersen/nanocode
 */

static final String GEMINI_KEY = Optional.ofNullable(getenv("GOOGLE_AI_GEMINI_API_KEY"))
        .orElse(getenv("GEMINI_API_KEY"));
static final String MODEL_NAME = Optional.ofNullable(getenv("MODEL"))
        .orElse("gemini-3-flash-preview");

static final String RESET = "\033[0m", BOLD = "\033[1m", DIM = "\033[2m", ITALIC = "\033[3m";
static final String BLUE = "\033[34m", CYAN = "\033[36m", GREEN = "\033[32m", RED = "\033[31m", YELLOW = "\033[93m";
static final String MAGENTA = "\033[35m", UNDERLINE = "\033[4m", STRIKE = "\033[9m", CODE_BG = "\033[37;40m";

// --- Tools ---

class Tools {
    @Tool("Read file with line numbers (file path, not directory)")
    public String read(@P("Path to the file") String path, 
                       @P("Start line (optional)") Integer offset, 
                       @P("Number of lines to read (optional)") Integer limit) throws IOException {
        println("\n" + GREEN + "⏺ Read" + RESET + "(" + DIM + path + RESET + ")");
        var lines = readAllLines(Path.of(path));
        int off = offset != null ? offset : 0;
        int lim = limit != null ? limit : lines.size();
        var sb = new StringBuilder();
        for (int i = off; i < Math.min(off + lim, lines.size()); i++)
            sb.append("%4d| %s%n".formatted(i + 1, lines.get(i)));
        var result = sb.toString();
        println("  " + DIM + "⎿  " + preview(result, 60) + RESET);
        return result;
    }

    @Tool("Write content to file")
    public String write(@P("Path to the file") String path, 
                        @P("Content to write") String content) throws IOException {
        println("\n" + GREEN + "⏺ Write" + RESET + "(" + DIM + path + RESET + ")");
        writeString(Path.of(path), content);
        println("  " + DIM + "⎿  ok" + RESET);
        return "ok";
    }

    @Tool("Replace old with new in file (old must be unique unless all=true)")
    public String edit(@P("Path to the file") String path, 
                       @P("Original string to replace") String old, 
                       @P("New string") String repl, 
                       @P("Replace all occurrences") Boolean all) throws IOException {
        println("\n" + GREEN + "⏺ Edit" + RESET + "(" + DIM + path + RESET + ")");
        var filePath = Path.of(path);
        var text = readString(filePath);
        if (!text.contains(old)) return "error: old_string not found";
        
        long count = (text.length() - text.replace(old, "").length()) / old.length();
        boolean replaceAll = all != null && all;
        if (!replaceAll && count > 1)
            return "error: old_string appears " + count + " times, must be unique (use all=true)";

        writeString(filePath, replaceAll
                ? text.replace(old, repl)
                : text.replaceFirst(Pattern.quote(old), Matcher.quoteReplacement(repl)));
        println("  " + DIM + "⎿  ok" + RESET);
        return "ok";
    }

    @Tool("Find files by pattern, sorted by mtime")
    public String glob(@P("Glob pattern (e.g. **/*.java)") String pat, 
                       @P("Base path (optional)") String path) throws IOException {
        println("\n" + GREEN + "⏺ Glob" + RESET + "(" + DIM + pat + RESET + ")");
        var base = Path.of(path != null ? path : ".");
        var matcher = FileSystems.getDefault().getPathMatcher("glob:" + base + "/" + pat);
        if (!exists(base)) return "none";
        try (var walk = walk(base)) {
            var files = walk.filter(Files::isRegularFile).filter(matcher::matches)
                    .sorted((a, b) -> {
                        try { return getLastModifiedTime(b).compareTo(getLastModifiedTime(a)); } 
                        catch (IOException e) { return 0; }
                    })
                    .map(Path::toString).toList();
            var result = files.isEmpty() ? "none" : String.join("\n", files);
            println("  " + DIM + "⎿  " + preview(result, 60) + RESET);
            return result;
        }
    }

    @Tool("Search files for regex pattern")
    public String grep(@P("Regex pattern") String pat, 
                       @P("Base path (optional)") String path) throws IOException {
        println("\n" + GREEN + "⏺ Grep" + RESET + "(" + DIM + pat + RESET + ")");
        var pattern = Pattern.compile(pat);
        var base = Path.of(path != null ? path : ".");
        var hits = new ArrayList<String>();
        try (var walk = walk(base)) {
            walk.filter(Files::isRegularFile).takeWhile(_ -> hits.size() < 50).forEach(file -> {
                try {
                    var lines = readAllLines(file);
                    for (int i = 0; i < lines.size() && hits.size() < 50; i++)
                        if (pattern.matcher(lines.get(i)).find())
                            hits.add(file + ":" + (i + 1) + ":" + lines.get(i));
                } catch (Exception e) { /* skip */ }
            });
        }
        var result = hits.isEmpty() ? "none" : String.join("\n", hits);
        println("  " + DIM + "⎿  " + preview(result, 60) + RESET);
        return result;
    }

    @Tool("Searches the web for up-to-date information on a given topic using Google Search")
    public String websearch(@P("The search query") String query) {
        println("\n" + GREEN + "⏺ WebSearch" + RESET + "(" + DIM + query + RESET + ")");
        var searchModel = GoogleAiGeminiChatModel.builder()
                .apiKey(GEMINI_KEY)
                .modelName(MODEL_NAME)
                .allowGoogleSearch(true)
                .build();
        return searchModel.chat(query);
    }

    @Tool("Run shell command")
    public String bash(@P("Shell command to run") String cmd,
                       @P("Working directory (optional)") String dir) throws Exception {
        println("\n" + GREEN + "⏺ Bash" + RESET + "(" + DIM + preview(cmd, 50) + RESET + ")");
        var pb = new ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true);
        if (dir != null && !dir.isBlank()) pb.directory(new File(dir));
        var proc = pb.start();
        var out = new ArrayList<String>();
        try (var r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                println("  " + DIM + "│ " + line + RESET);
                out.add(line);
            }
        }
        if (!proc.waitFor(30, TimeUnit.SECONDS)) {
            proc.destroyForcibly();
            out.add("(timed out after 30s)");
        }
        return out.isEmpty() ? "(empty)" : String.join("\n", out);
    }
}

// --- Assistant ---

interface Assistant {
    @SystemMessage("Concise coding assistant. cwd: {{cwd}}")
    String chat(@V("cwd") String cwd, @UserMessage String userMessage);
}

// --- UI Utils ---

static String sep() {
    try {
        var p = new ProcessBuilder("tput", "cols").redirectErrorStream(true).start();
        return DIM + "─".repeat(Math.min(Integer.parseInt(new String(p.getInputStream().readAllBytes()).trim()), 80)) + RESET;
    } catch (Exception e) {
        return DIM + "─".repeat(80) + RESET;
    }
}

static String preview(String s, int max) {
    if (s == null || s.isEmpty()) return "";
    var lines = s.split("\n");
    var p = lines[0].substring(0, Math.min(lines[0].length(), max));
    return lines.length > 1 ? p + " ... +" + (lines.length - 1) + " lines" : (lines[0].length() > max ? p + "..." : p);
}

static String markdown(String md) {
    if (md == null) return "";
    var blocks = new ArrayList<String>();
    var m = Pattern.compile("(?s)```(\\w+)?\\n(.*?)\\n```").matcher(md);
    var sb = new StringBuilder();
    while (m.find()) {
        var lang = m.group(1) == null ? "" : m.group(1);
        var content = m.group(2);
        var formatted = (lang.isEmpty() ? "" : ITALIC + BOLD + lang + RESET + "\n") +
                        content.replaceAll("(?m)^", CODE_BG) + RESET + "\n";
        m.appendReplacement(sb, Matcher.quoteReplacement("%%BLOCK_CODE_" + blocks.size() + "%%"));
        blocks.add(formatted);
    }
    m.appendTail(sb);
    var res = sb.toString()
        .replaceAll("\\*\\*(.*?)\\*\\*", BOLD + "$1" + RESET) // Bold
        .replaceAll("\\*(.*?)\\*", ITALIC + "$1" + RESET) // Italic
        .replaceAll("__(.*?)__", UNDERLINE + "$1" + RESET) // Underline
        .replaceAll("~~(.*?)~~", STRIKE + "$1" + RESET) // Strikethrough
        .replaceAll("(?m)^> ?(.*)", ITALIC + BLUE + BOLD + "> $1" + RESET) // Blockquote
        .replaceAll("(?m)^([\\d]+\\.|-|\\*) (.*)", MAGENTA + BOLD + "$1" + RESET + " $2") // Lists
        .replaceAll("(?m)^(#{1,6}) (.*)", CYAN + BOLD + "$1 $2" + RESET) // Headers
        .replaceAll("(?m)^(.*?\n={2,}\n)", CYAN + BOLD + "$1" + RESET) // Headers (===)
        .replaceAll("(?m)^(.*?\n-{2,}\n)", CYAN + BOLD + "$1" + RESET) // Headers (---)
        .replaceAll("!?\\[(.*?)]\\((.*?)\\)", BLUE + "$1" + RESET + " (" + BLUE + UNDERLINE + "$2" + RESET + ")"); // Links/Images
    for (int i = 0; i < blocks.size(); i++)
        res = res.replace("%%BLOCK_CODE_" + i + "%%", blocks.get(i));
    return res;
}

// --- Main ---

void main(String[] args) throws Exception {
    if (GEMINI_KEY == null || GEMINI_KEY.isBlank()) {
        println(RED + "Error: GOOGLE_AI_GEMINI_API_KEY or GEMINI_API_KEY environment variable is not set." + RESET);
        return;
    }

    var cwd = System.getProperty("user.dir");
    println(BOLD + "nanocode" + RESET + " | " + DIM + MODEL_NAME + " (Google AI Gemini) | " + cwd + RESET + "\n");

    var model = GoogleAiGeminiChatModel.builder()
            .apiKey(GEMINI_KEY)
            .modelName(MODEL_NAME)
            .sendThinking(true)
            .returnThinking(true)
            .build();

    var memory = MessageWindowChatMemory.withMaxMessages(20);
    var tools = new Tools();
    
    var assistant = AiServices.builder(Assistant.class)
            .chatModel(model)
            .chatMemory(memory)
            .tools(tools)
            .build();

    while (true) {
        try {
            println(sep());
            var input = readln(BOLD + BLUE + "❯ " + RESET + YELLOW);
            print(RESET);
            if (input == null) break;
            input = input.strip();
            println(sep());
            
            if (input.isEmpty()) continue;
            if (input.equals("/q") || input.equals("exit")) break;
            if (input.equals("/c")) {
                memory.clear();
                println(GREEN + "⏺ Cleared" + RESET);
                continue;
            }

            var response = assistant.chat(cwd, input);
            println("\n" + CYAN + "⏺" + RESET + " " + markdown(response));
            println();
        } catch (Exception e) {
            println(RED + "⏺ Error: " + e.getMessage() + RESET);
        }
    }
}
