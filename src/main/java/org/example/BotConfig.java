package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BotConfig {

    private String botToken;
    private String gptToken;

    public BotConfig() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }
            properties.load(input);
            this.botToken = properties.getProperty("bot.token");
            this.gptToken = properties.getProperty("gpt.token");
        } catch (IOException ignored) {
        }
    }

    public String getBotToken() {
        return botToken;
    }

    public String getGptToken() {
        return gptToken;
    }
}
