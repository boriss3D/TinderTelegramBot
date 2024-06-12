package org.example;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatGPTService {
    private final OpenAiService openAiService;
    private List<ChatMessage> chatGptMessageHistory = new ArrayList<>();

    public ChatGPTService(String token) {
        this.openAiService = new OpenAiService(token);
    }

    /**
     * Single request to ChatGPT in the format "request" -> "response".
     * The request consists of two parts:
     * prompt - the context of the question is the actual query
     */
    public String sendMessage(String prompt, String question) {
        ChatMessage systemMessage = new ChatMessage("system", prompt);
        ChatMessage userMessage = new ChatMessage("user", question);
        chatGptMessageHistory = new ArrayList<>(Arrays.asList(systemMessage, userMessage));

        return sendMessagesToChatGPT();
    }

    /**
     * Requests to ChatGPT with message history preservation.
     * The setPrompt() method sets the context of the request.
     */
    public void setPrompt(String prompt) {
        ChatMessage systemMessage = new ChatMessage("system", prompt);
        chatGptMessageHistory = new ArrayList<>(List.of(systemMessage));
    }

    /**
     * Requests to ChatGPT with message history preservation.
     * The addMessage() method adds a new question (message) to the chat.
     */
    public String addMessage(String question) {
        ChatMessage userMessage = new ChatMessage("user", question);
        chatGptMessageHistory.add(userMessage);

        return sendMessagesToChatGPT();
    }

    /**
     * Send a series of messages to ChatGPT: prompt, message1, answer1, message2, answer2, ..., messageN.
     * The ChatGPT response is appended to the end of messageHistory for future use.
     */
    private String sendMessagesToChatGPT() {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o") /* set version here */
                .messages(chatGptMessageHistory)
                .maxTokens(3000)
                .temperature(0.9)
                .build();

        ChatCompletionResult response = openAiService.createChatCompletion(request);
        ChatMessage chatMessage = response.getChoices().get(0).getMessage();
        chatGptMessageHistory.add(chatMessage);

        return chatMessage.getContent();
    }
}
