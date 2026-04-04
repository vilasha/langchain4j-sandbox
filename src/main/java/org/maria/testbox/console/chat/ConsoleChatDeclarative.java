package org.maria.testbox.console.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageJsonCodec;
import dev.langchain4j.data.message.JacksonChatMessageJsonCodec;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ConsoleChatDeclarative {
    private static final int MEMORY_WINDOW = 20;
    private static final String SYSTEM_PROMPT = "You are a helpful assistant";
    private static final String HISTORY_FILE = "src/main/resources/chat-history-declarative.json";

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(dotenv.get("OPENAI_API_KEY"))
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .build();
        FileChatMemoryStore store = new FileChatMemoryStore(HISTORY_FILE);
        ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(MEMORY_WINDOW)
                .chatMemoryStore(store)
                .build();
        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(model)
                .chatMemory(memory)
                .build();
        SummaryService summaryService = AiServices.builder(SummaryService.class)
                .chatModel(model)
                .build();

        System.out.println("Chat started. Type 'exit' or press Enter on an empty line to quit.");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print(">> ");
                String input = scanner.nextLine();
                if (input.isBlank() || input.equalsIgnoreCase("exit")) {
                    break;
                }
                try {
                    System.out.println(service.chat(input));
                } catch (RateLimitException e) {
                    System.err.println("Rate limit reached, try again later: " + e.getMessage());
                } catch (TimeoutException e) {
                    System.err.println("Request timed out: " + e.getMessage());
                } catch (HttpException e) {
                    System.err.println("HTTP error (" + e.statusCode() + "): " + e.getMessage());
                }
            }
        }

        summarizeAndSave(memory, store, summaryService);
    }

    private static void summarizeAndSave(ChatMemory memory, FileChatMemoryStore store, SummaryService summaryService) {
        List<ChatMessage> messages = memory.messages();
        boolean hasConversation = messages.stream().anyMatch(m -> m instanceof dev.langchain4j.data.message.UserMessage);
        if (!hasConversation) {
            return;
        }
        System.out.println("Summarizing conversation...");
        String transcript = buildTranscript(messages);
        String summary = summaryService.summarize(transcript);
        store.updateMessages(memory.id(), List.of(
                dev.langchain4j.data.message.UserMessage.from("[Previous session summary]\n" + summary),
                AiMessage.from("Understood. I have the context from our previous session and am ready to continue.")
        ));
        System.out.println("Summary saved.");
    }

    private static String buildTranscript(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            switch (msg) {
                case dev.langchain4j.data.message.UserMessage m -> sb.append("User: ").append(m.singleText()).append("\n");
                case AiMessage m -> sb.append("Assistant: ").append(m.text()).append("\n");
                default -> {}
            }
        }
        return sb.toString();
    }

    interface ChatService {
        @SystemMessage(SYSTEM_PROMPT)
        String chat(@UserMessage String content);
    }

    interface SummaryService {
        @SystemMessage("Summarize the following conversation. Preserve: key topics and results, " +
                "the exact final state of any ongoing task or calculation (so the user can continue " +
                "with follow-up questions like 'and add 5?' without losing context), and any other " +
                "information needed to seamlessly continue in a new session. Be concise.")
        String summarize(@UserMessage String transcript);
    }

    static class FileChatMemoryStore implements ChatMemoryStore {
        private final Path filePath;
        private final ChatMessageJsonCodec codec = new JacksonChatMessageJsonCodec();

        FileChatMemoryStore(String filePath) {
            this.filePath = Path.of(filePath);
        }

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            if (!Files.exists(filePath)) {
                return new ArrayList<>();
            }
            try {
                return codec.messagesFromJson(Files.readString(filePath));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read chat history from " + filePath, e);
            }
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            try {
                Files.writeString(filePath, codec.messagesToJson(messages));
            } catch (IOException e) {
                throw new RuntimeException("Failed to write chat history to " + filePath, e);
            }
        }

        @Override
        public void deleteMessages(Object memoryId) {
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete chat history at " + filePath, e);
            }
        }
    }
}
