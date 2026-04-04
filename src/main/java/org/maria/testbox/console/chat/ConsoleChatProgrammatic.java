package org.maria.testbox.console.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ConsoleChatProgrammatic {
    private static final String SYSTEM_PROMPT = "You are a helpful assistant";
    private static final String HISTORY_FILE = "src/main/resources/chat-history-programmatic.json";
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        List<ChatMessage> history = loadHistory();
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(dotenv.get("OPENAI_API_KEY"))
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .build();

        System.out.println("Chat started. Type 'exit' or press Enter on an empty line to quit.");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print(">> ");
                String input = scanner.nextLine();
                if (input.isBlank() || input.equalsIgnoreCase("exit")) {
                    break;
                }
                history.add(UserMessage.from(input));
                try {
                    AiMessage response = model.chat(history.toArray(ChatMessage[]::new)).aiMessage();
                    System.out.println(response.text());
                    history.add(response);
                    saveHistory(history);
                } catch (RateLimitException e) {
                    System.err.println("Rate limit reached, try again later: " + e.getMessage());
                    history.remove(history.size() - 1);
                } catch (TimeoutException e) {
                    System.err.println("Request timed out: " + e.getMessage());
                    history.remove(history.size() - 1);
                } catch (HttpException e) {
                    System.err.println("HTTP error (" + e.statusCode() + "): " + e.getMessage());
                    history.remove(history.size() - 1);
                }
            }
        }

        summarizeAndSave(history, model);
    }

    private static void summarizeAndSave(List<ChatMessage> history, ChatModel model) {
        boolean hasConversation = history.stream().anyMatch(m -> m instanceof UserMessage);
        if (!hasConversation) {
            return;
        }
        System.out.println("Summarizing conversation...");
        String transcript = buildTranscript(history);
        List<ChatMessage> summaryRequest = List.of(
                SystemMessage.from("Summarize the following conversation. Preserve: key topics and results, " +
                        "the exact final state of any ongoing task or calculation (so the user can continue " +
                        "with follow-up questions like 'and add 5?' without losing context), and any other " +
                        "information needed to seamlessly continue in a new session. Be concise."),
                UserMessage.from(transcript)
        );
        AiMessage summaryResponse = model.chat(summaryRequest.toArray(ChatMessage[]::new)).aiMessage();
        String summary = summaryResponse.text();

        List<ChatMessage> summaryHistory = new ArrayList<>();
        summaryHistory.add(UserMessage.from("[Previous session summary]\n" + summary));
        summaryHistory.add(AiMessage.from("Understood. I have the context from our previous session and am ready to continue."));
        saveHistory(summaryHistory);
        System.out.println("Summary saved.");
    }

    private static String buildTranscript(List<ChatMessage> history) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : history) {
            switch (msg) {
                case UserMessage m  -> sb.append("User: ").append(m.singleText()).append("\n");
                case AiMessage m    -> sb.append("Assistant: ").append(m.text()).append("\n");
                default             -> {}
            }
        }
        return sb.toString();
    }

    private static List<ChatMessage> loadHistory() {
        Path path = Path.of(HISTORY_FILE);
        if (!Files.exists(path)) {
            List<ChatMessage> history = new ArrayList<>();
            history.add(SystemMessage.from(SYSTEM_PROMPT));
            return history;
        }
        try {
            MessageRecord[] records = MAPPER.readValue(path.toFile(), MessageRecord[].class);
            return new ArrayList<>(Arrays.stream(records).map(MessageRecord::toChatMessage).toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load chat history from " + path, e);
        }
    }

    private static void saveHistory(List<ChatMessage> history) {
        Path path = Path.of(HISTORY_FILE);
        List<MessageRecord> records = history.stream().map(MessageRecord::fromChatMessage).toList();
        try {
            Files.writeString(path, MAPPER.writeValueAsString(records));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save chat history to " + path, e);
        }
    }

    record MessageRecord(String type, String text) {
        @JsonCreator
        MessageRecord(@JsonProperty("type") String type, @JsonProperty("text") String text) {
            this.type = type;
            this.text = text;
        }

        static MessageRecord fromChatMessage(ChatMessage message) {
            return switch (message) {
                case SystemMessage m -> new MessageRecord("SYSTEM", m.text());
                case UserMessage m   -> new MessageRecord("USER", m.singleText());
                case AiMessage m     -> new MessageRecord("AI", m.text());
                default -> throw new IllegalArgumentException("Unsupported message type: " + message.getClass());
            };
        }

        ChatMessage toChatMessage() {
            return switch (type) {
                case "SYSTEM" -> SystemMessage.from(text);
                case "USER"   -> UserMessage.from(text);
                case "AI"     -> AiMessage.from(text);
                default -> throw new IllegalArgumentException("Unknown message type in history: " + type);
            };
        }
    }
}
