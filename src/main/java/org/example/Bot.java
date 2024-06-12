package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramBot;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Bot extends TelegramLongPollingBot {
    private static final Logger LOGGER = Logger.getLogger(TelegramBot.class.getName());
    private static final String BOT_USER_NAME = "TinderTelegramAiBot";
    private static ChatGPTService chatGPTService;
    private final ArrayList<String> stringArrayList = new ArrayList<>();
    private DialogMode dialogMode;
    private UserInfo userInfo;
    private UserInfo targetInfo;
    private int questionCount;

    public Bot(String botToken) {
        super(botToken);
    }

    public static void main(String[] args) throws TelegramApiException {
        BotConfig botConfig = new BotConfig();

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new Bot(botConfig.getBotToken()));

        chatGPTService = new ChatGPTService(botConfig.getGptToken());
    }

    @Override
    public String getBotUsername() {
        return BOT_USER_NAME;
    }

    @Override
    public void onUpdateReceived(Update update) {
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        boolean isCommand = update.getMessage().isCommand();

        if (messageText.equals("/start")) {
            dialogMode = DialogMode.MAIN;
            sendPhotoMessage(String.valueOf(chatId), "main.jpg");
            sendTextMessage(String.valueOf(chatId), "main.txt");
            sendTextButtonsMessage(chatId, List.of("/start", "/profile", "/opener", "/message",
                    "/date", "/gpt"));
            return;
        }

        if (messageText.equals("/gpt")) {
            dialogMode = DialogMode.GPT;
            sendPhotoMessage(String.valueOf(chatId), "gpt.jpg");
            sendTextMessage(String.valueOf(chatId), "gpt.txt");
            return;
        }

        if (dialogMode == DialogMode.GPT && !isCommand) {
            String gpt = loadPrompt("gpt.txt");
            Message message = sendTextMessage(String.valueOf(chatId), "thinking.txt");
            String answerGPT = chatGPTService.sendMessage(gpt, messageText);
            Integer messageId = message.getMessageId();
            updateTextMessage(answerGPT, chatId, messageId);
            return;
        }

        if (messageText.equals("/message")) {
            dialogMode = DialogMode.MESSAGE;
            sendPhotoMessage(String.valueOf(chatId), "message.jpg");
            sendTextMessage(String.valueOf(chatId), "message.txt");
            sendTextButtonsMessage(chatId, List.of("Ask out on a date!", "/start"));
            return;
        }

        if (dialogMode == DialogMode.MESSAGE && !isCommand) {
            if (messageText.equals("Ask out on a date!")) {
                String date = loadPrompt("message_date.txt");
                String userChatHistory = String.join("\n\n", stringArrayList);
                Message message = sendTextMessage(String.valueOf(chatId), "thinking.txt");
                String answerGPT = chatGPTService.sendMessage(date, userChatHistory);
                Integer messageId = message.getMessageId();
                updateTextMessage(answerGPT, chatId, messageId);
                sendTextButtonsMessage(chatId, List.of("/start", "/profile", "/opener", "/message",
                        "/date", "/gpt"));
            } else {
                stringArrayList.add(messageText);
            }
            return;
        }

        if (messageText.equals("/date")) {
            dialogMode = DialogMode.DATE;
            sendPhotoMessage(String.valueOf(chatId), "date.jpg");
            sendTextMessage(String.valueOf(chatId), "date.txt");
            sendTextButtonsMessage(chatId, List.of("Ariana_Grande", "Margot_Robbie", "Zendaya", "Ryan_Gosling",
                    "Tom_Hardy", "/start"));
            return;
        }

        if (dialogMode == DialogMode.DATE && !isCommand) {
            switch (messageText) {
                case "Ariana_Grande", "Margot_Robbie", "Zendaya", "Ryan_Gosling", "Tom_Hardy" -> {
                    sendPhotoMessage(String.valueOf(chatId), messageText + ".jpg");
                    String date = loadPrompt(messageText + ".txt");
                    chatGPTService.setPrompt(date);
                    sendTextMessage(String.valueOf(chatId), "choice.txt");
                }
            }
            Message message = sendTextMessage(String.valueOf(chatId), "thinking.txt");
            Integer messageId = message.getMessageId();
            String answerGPT = chatGPTService.addMessage(messageText);
            updateTextMessage(answerGPT, chatId, messageId);
            sendTextButtonsMessage(chatId, List.of("/start", "/profile", "/opener", "/message",
                    "/date", "/gpt"));
            return;
        }

        if (messageText.equals("/profile")) {
            dialogMode = DialogMode.PROFILE;
            sendPhotoMessage(String.valueOf(chatId), "profile.jpg");
            sendTextMessage(String.valueOf(chatId), "profile.txt");
            userInfo = new UserInfo();
            questionCount = 0;
            sendTextMessage(chatId, "Provide information about yourself:");
            sendTextMessage(chatId, "What is your name?");
            return;
        }

        if (dialogMode == DialogMode.PROFILE && !isCommand) {
            switch (questionCount) {
                case 0 -> {
                    userInfo.name = messageText;
                    questionCount++;
                    sendTextMessage(chatId, "What is your gender?");
                }
                case 1 -> {
                    userInfo.sex = messageText;
                    questionCount++;
                    sendTextMessage(chatId, "How old are you?");
                }
                case 2 -> {
                    userInfo.age = messageText;
                    questionCount++;
                    sendTextMessage(chatId, "What city do you live in?");
                }
                case 3 -> {
                    userInfo.city = messageText;
                    questionCount++;
                    sendTextMessage(chatId, "What do you do?");
                }
                case 4 -> {
                    userInfo.occupation = messageText;

                    String aboutMyself = userInfo.toString();
                    String profile = loadPrompt("profile.txt");
                    Message message = sendTextMessage(String.valueOf(chatId), "thinking.txt");
                    Integer messageId = message.getMessageId();
                    String answerGPT = chatGPTService.sendMessage(profile, aboutMyself);
                    updateTextMessage(answerGPT, chatId, messageId);
                    sendTextButtonsMessage(chatId, List.of("/start", "/profile", "/opener", "/message",
                            "/date", "/gpt"));
                }
            }
            return;
        }

        if (messageText.equals("/opener")) {
            dialogMode = DialogMode.OPENER;
            sendPhotoMessage(String.valueOf(chatId), "opener.jpg");
            sendTextMessage(String.valueOf(chatId), "opener.txt");
            targetInfo = new UserInfo();
            questionCount = 0;
            sendTextMessage(chatId, "Let's figure out who you want to meet!");
            sendTextMessage(chatId, "Purpose of dating?");
            return;
        }

        if (dialogMode == DialogMode.OPENER && !isCommand) {
            switch (questionCount) {
                case 0 -> {
                    targetInfo.goals = messageText;
                    questionCount++;
                    sendTextMessage(chatId, "Gender?");
                }
                case 1 -> {
                    targetInfo.sex = messageText;
                    questionCount++;
                    sendTextMessage(chatId, "Age?");
                }
                case 2 -> {
                    targetInfo.age = messageText;
                    questionCount++;
                    sendTextMessage(chatId, "What city do you live in?");
                }
                case 3 -> {
                    targetInfo.city = messageText;
                    questionCount++;
                    sendTextMessage(chatId, "Hobby?");
                }
                case 4 -> {
                    targetInfo.hobby = messageText;

                    String aboutMyself = targetInfo.toString();
                    String profile = loadPrompt("opener.txt");
                    Message message = sendTextMessage(String.valueOf(chatId), "thinking.txt");
                    Integer messageId = message.getMessageId();
                    String answerGPT = chatGPTService.sendMessage(profile, aboutMyself);
                    updateTextMessage(answerGPT, chatId, messageId);
                    sendTextButtonsMessage(chatId, List.of("/start", "/profile", "/opener", "/message",
                            "/date", "/gpt"));
                }
            }
        }
    }

    private void sendPhotoMessage(String chatId, String photoFileName) {

        try {

            ClassLoader classLoader = getClass().getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream("images/" + photoFileName);
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found! in images/" + photoFileName);
            }
            Path tempFile = Files.createTempFile(null, null);
            Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            inputStream.close();

            InputFile inputFile = new InputFile(tempFile.toFile());

            SendPhoto sendPhotoRequest = new SendPhoto();
            sendPhotoRequest.setChatId(chatId);
            sendPhotoRequest.setPhoto(inputFile);

            execute(sendPhotoRequest);

            Files.delete(tempFile);

        } catch (TelegramApiException | IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to send image: " + e.getMessage(), e);
        }
    }

    private void sendTextMessage(long chatId, String textMessage) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setParseMode("Markdown");
        message.setText(textMessage);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            LOGGER.log(Level.SEVERE, "Failed to send message: " + e.getMessage(), e);
        }
    }

    private Message sendTextMessage(String chatId, String fileName) {

        String text;
        Message execute = null;

        try {

            ClassLoader classLoader = getClass().getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream("messages/" + fileName);
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found! in messages/" + fileName);
            }
            text = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setParseMode("Markdown");
            message.setText(text);

            execute = execute(message);

        } catch (TelegramApiException e) {
            LOGGER.log(Level.SEVERE, "Failed to send message: " + e.getMessage(), e);
        }

        return execute;
    }

    private void updateTextMessage(String answerGPT, long chatId, Integer messageId) {

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(String.valueOf(chatId));
        editMessageText.setMessageId(messageId);
        editMessageText.setText(answerGPT);

        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            LOGGER.log(Level.SEVERE, "Failed to edit message: " + e.getMessage(), e);
        }
    }

    private String loadPrompt(String fileName) {

        try {

            ClassLoader classLoader = getClass().getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream("prompts/" + fileName);
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found! in prompts/" + fileName);
            }
            return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load prompt: " + e.getMessage(), e);
        }
        return null;
    }

    private void sendTextButtonsMessage(long chatId, List<String> buttonLabels) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(">>>");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow currentRow = new KeyboardRow();

        for (int i = 0; i < buttonLabels.size(); i++) {
            if (i % 3 == 0 && !currentRow.isEmpty()) {
                keyboard.add(currentRow);
                currentRow = new KeyboardRow();
            }
            currentRow.add(buttonLabels.get(i));
        }

        if (!currentRow.isEmpty()) {
            keyboard.add(currentRow);
        }

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            LOGGER.log(Level.SEVERE, "Failed to add buttons: " + e.getMessage(), e);
        }
    }
}
