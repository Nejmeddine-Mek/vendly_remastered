package org.example.vendly_remastered.utils;

import javafx.geometry.NodeOrientation;
import javafx.scene.Node;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class Config {
    private static final String CONFIG_FILE = "config.txt";
    private static final String DEFAULT_LANGUAGE = "ar";
    private static Config instance;

    private String language;
    private ResourceBundle languageBundle;
    private Locale currentLocale;

    private Config() {
        // Load language from config.txt or create default if missing
        setLanguage(loadLanguageFromConfigFile());
    }

    public static synchronized Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    private String loadLanguageFromConfigFile() {
        Properties props = new Properties();

        if (Files.exists(Paths.get(CONFIG_FILE))) {
            try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                props.load(in);
                String lang = props.getProperty("language");
                if (lang != null && !lang.isBlank()) {
                    return lang.trim();
                }
            } catch (IOException e) {
                System.err.println("[Config] Error reading " + CONFIG_FILE + ". Recreating default file.");
            }
        }

        // File doesn't exist or failed to load: create default file with language=fr
        saveLanguageToConfigFile(DEFAULT_LANGUAGE);
        return DEFAULT_LANGUAGE;
    }

    public void saveLanguageToConfigFile(String langCode) {
        Properties props = new Properties();
        props.setProperty("language", langCode);

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Vendly Remastered Configuration");
        } catch (IOException e) {
            System.err.println("[Config] Failed to save " + CONFIG_FILE + ": " + e.getMessage());
        }
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String langCode) {
        this.language = langCode;
        this.currentLocale = new Locale(langCode);

        // Load properties file explicitly as UTF-8 to prevent '????' encoding glitches
        String bundlePath = "/i18n/messages_" + langCode + ".properties";
        try (InputStream input = getClass().getResourceAsStream(bundlePath)) {
            if (input != null) {
                this.languageBundle = new PropertyResourceBundle(new InputStreamReader(input, StandardCharsets.UTF_8));
            } else {
                // Fallback to standard bundle loading if stream is null
                this.languageBundle = ResourceBundle.getBundle("i18n.messages", currentLocale);
            }
        } catch (Exception e) {
            System.err.println("[Config] Failed to load bundle for language: " + langCode);
            this.languageBundle = ResourceBundle.getBundle("i18n.messages", currentLocale);
        }
    }

    public ResourceBundle getLanguageBundle() {
        return languageBundle;
    }

    public Locale getCurrentLocale() {
        return currentLocale;
    }

    public String getString(String key) {
        if (languageBundle != null && languageBundle.containsKey(key)) {
            return languageBundle.getString(key);
        }
        return "!" + key + "!";
    }

    public void applyOrientation(Node rootNode) {
        if ("ar".equalsIgnoreCase(language)) {
            rootNode.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        } else {
            rootNode.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        }
    }
}