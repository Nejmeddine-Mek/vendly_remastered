package org.example.vendly_remastered.security;

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

    private String language;
    private ResourceBundle languageBundle;
    private Locale currentLocale;

    private boolean trackStock = false;
    private boolean updateStockOnSale = false;
    private boolean allowNegativeStock = false;

    private boolean lowStockPOSAlert = false;
    private boolean batchExpiryAlerts = false;

    private boolean trackClientsDebts = false;
    private boolean trackSupplierDebts = false;

    private static Config instance;



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
        props.setProperty("trackStock", String.valueOf(this.trackStock));
        props.setProperty("updateStockOnSale", String.valueOf(this.updateStockOnSale));
        props.setProperty("allowNegativeStock", String.valueOf(this.allowNegativeStock));
        props.setProperty("lowStockPOSAlert", String.valueOf(this.lowStockPOSAlert));
        props.setProperty("batchExpiryAlerts", String.valueOf(this.batchExpiryAlerts));
        props.setProperty("trackClientsDebts", String.valueOf(this.trackClientsDebts));
        props.setProperty("trackSupplierDebts", String.valueOf(this.trackSupplierDebts));

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Vendly Remastered Configuration");
        } catch (IOException e) {
            System.err.println("[Config] Failed to save " + CONFIG_FILE + ": " + e.getMessage());
        }
    }

    public void saveConfigToFile(String langCode, boolean trackStock, boolean updateStockOnSale,
                                 boolean allowNegativeStock, boolean lowStockPOSAlert,
                                 boolean batchExpiryAlerts, boolean trackClientsDebts,
                                 boolean trackSupplierDebts) {
        Properties props = new Properties();
        props.setProperty("language", langCode);
        props.setProperty("trackStock", String.valueOf(trackStock));
        props.setProperty("updateStockOnSale", String.valueOf(updateStockOnSale));
        props.setProperty("allowNegativeStock", String.valueOf(allowNegativeStock));
        props.setProperty("lowStockPOSAlert", String.valueOf(lowStockPOSAlert));
        props.setProperty("batchExpiryAlerts", String.valueOf(batchExpiryAlerts));
        props.setProperty("trackClientsDebts", String.valueOf(trackClientsDebts));
        props.setProperty("trackSupplierDebts", String.valueOf(trackSupplierDebts));

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

    public boolean isTrackStock() {
        return trackStock;
    }

    public void setTrackStock(boolean trackStock) {
        this.trackStock = trackStock;
    }

    public boolean isUpdateStockOnSale() {
        return updateStockOnSale;
    }

    public void setUpdateStockOnSale(boolean updateStockOnSale) {
        this.updateStockOnSale = updateStockOnSale;
    }

    public boolean isAllowNegativeStock() {
        return allowNegativeStock;
    }

    public void setAllowNegativeStock(boolean allowNegativeStock) {
        this.allowNegativeStock = allowNegativeStock;
    }

    public boolean isLowStockPOSAlert() {
        return lowStockPOSAlert;
    }

    public void setLowStockPOSAlert(boolean lowStockPOSAlert) {
        this.lowStockPOSAlert = lowStockPOSAlert;
    }

    public boolean isBatchExpiryAlerts() {
        return batchExpiryAlerts;
    }

    public void setBatchExpiryAlerts(boolean batchExpiryAlerts) {
        this.batchExpiryAlerts = batchExpiryAlerts;
    }

    public boolean isTrackClientsDebts() {
        return trackClientsDebts;
    }

    public void setTrackClientsDebts(boolean trackClientsDebts) {
        this.trackClientsDebts = trackClientsDebts;
    }

    public boolean isTrackSupplierDebts() {
        return trackSupplierDebts;
    }

    public void setTrackSupplierDebts(boolean trackSupplierDebts) {
        this.trackSupplierDebts = trackSupplierDebts;
    }
}