package org.example.vendly_remastered.models;

import javafx.geometry.NodeOrientation;
import javafx.scene.Node;

import java.util.Locale;
import java.util.ResourceBundle;

public class Config {
    private static Config instance;
    private String language;
    private ResourceBundle languageBundle;
    private Locale currentLocale;

    private Config() {
        // Default startup language (Fallback)
        setLanguage("en");
    }

    // Global access point to the configuration instance
    public static synchronized Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String langCode) {
        this.language = langCode;
        this.currentLocale = new Locale(langCode);

        // Looks inside src/main/resources/i18n/messages_XX.properties
        this.languageBundle = ResourceBundle.getBundle("i18n.messages", currentLocale);
    }

    public ResourceBundle getLanguageBundle() {
        return languageBundle;
    }

    /**
     * Helper method to get a translated string instantly by its key
     */
    public String getString(String key) {
        return languageBundle.getString(key);
    }

    /**
     * Defensive UI helper to automatically apply Right-To-Left or Left-To-Right
     * orientations to views depending on the active language.
     */
    public void applyOrientation(Node rootNode) {
        if ("ar".equalsIgnoreCase(language)) {
            rootNode.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        } else {
            rootNode.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        }
    }
}
