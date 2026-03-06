# ChatGPT Conversation Parser

This is a Java 17 CLI tool that walks a directory tree of saved ChatGPT conversations (HTML exports) and produces nicely formatted text files that summarize what was said plus the attachment list per conversation.

## Features

- Scans the provided root directory and every child folder for `.html`/`.htm` exports generated via **Save Page As** from ChatGPT.
- Extracts the conversation title plus each user/assistant message using Jsoup, and renders them as:

  ```
  user said : <text>

  chatgpt said : <text>
  ```

  Each question/answer pair is separated by an empty line, and the first user message mentions image attachments when present.
- Outputs one `.txt` file per HTML export (same base name) and logs the generated file paths.
- Every invocation writes under an output folder (default `chatgpt-conversations-parser-OUTPUT` next to the input) inside a timestamped subfolder (`yyyyMMdd-HHmmss-SSS`), or a custom location when a second argument is provided.

## Requirements

- Java 17 (JDK 17+)
- Maven
- Network access only required for the first build to download Jsoup from Maven Central.

## Build

```sh
mvn -DskipTests package
```

This produces:

- `target/chatgpt-conversation-parser-0.1.0.jar`
- `target/chatgpt-conversation-parser-0.1.0-jar-with-dependencies.jar`

The assembly jar includes Jsoup and can be executed directly.

## Usage

```
java -jar target/chatgpt-conversation-parser-0.1.0-jar-with-dependencies.jar <root-directory> [output-directory]
```

- `<root-directory>` — required. The directory holding conversation subfolders or HTML exports. The tool searches the root itself plus every immediate subdirectory.
- `[output-directory]` — optional. When omitted, the tool creates `chatgpt-conversations-parser-OUTPUT` next to the root directory; otherwise it uses the provided path. Each execution adds a timestamped folder inside that base location and writes individual `.txt` files there.

### Example

```sh
cd target
java -jar chatgpt-conversation-parser-0.1.0-jar-with-dependencies.jar "C:/path/to/ChatGPT/exports"
```

Outputs will look like:

- `.../chatgpt-conversations-parser-OUTPUT/20260306-170012-123/CV - SCA1.txt`
- `.../chatgpt-conversations-parser-OUTPUT/20260306-170012-123/CV - SCA2.txt`
- etc.

Each `.txt` mirrors the conversation and notes attachments so it can be archived, searched, or shared easily.

## Troubleshooting

- If you receive `Provided path is not a directory`, double-check the root argument is the folder with the exports, not a single HTML file.
- If “No ChatGPT conversations were detected” appears, ensure there are `.html` files in the root or its subdirectories, and that they were saved via the browser’s “Save Page As” so the markup is parsable.
- For parsing tweaks, inspect `src/main/java/com/chatgpt/conversation/parser/ConversationScanner.java` and adjust the selectors (currently `div[data-author]`, `div[data-role=message]`, `article`, and `div[class*=message]`).

## Extending

- Add unit tests under `src/test/java` when you formalize selectors.
- Improve attachment-to-message correlation by expanding `ConversationScanner`’s TODOs.
- If you need JSON/CSV output, replace the string formatting logic in `ConversationParserApp.formatConversation`.

## Support

File issues or ideas on the repository issue tracker or reach out via the appropriate project channel.
