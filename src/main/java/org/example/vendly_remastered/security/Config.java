package org.example.vendly_remastered.security;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
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

    private final BooleanProperty trackStock = new SimpleBooleanProperty(false);
    private final BooleanProperty updateStockOnSale = new SimpleBooleanProperty(false);
    private final BooleanProperty allowNegativeStock = new SimpleBooleanProperty(false);

    private final BooleanProperty lowStockPOSAlert = new SimpleBooleanProperty(false);
    private final BooleanProperty batchExpiryAlerts = new SimpleBooleanProperty(false);

    private final BooleanProperty trackClientsDebts = new SimpleBooleanProperty(false);
    private final BooleanProperty trackSupplierDebts = new SimpleBooleanProperty(false);

    private static Config instance;

    private Config() {
        // Load settings and language from config.txt or create default if missing
        loadConfigFromFile();
    }

    public static synchronized Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    public void loadConfigFromFile() {
        Properties props = new Properties();

        if (Files.exists(Paths.get(CONFIG_FILE))) {
            try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                props.load(in);

                // Load language
                String lang = props.getProperty("language");
                setLanguage(lang != null && !lang.isBlank() ? lang.trim() : DEFAULT_LANGUAGE);

                // Load boolean configuration flags
                trackStock.set(Boolean.parseBoolean(props.getProperty("trackStock", "false")));
                updateStockOnSale.set(Boolean.parseBoolean(props.getProperty("updateStockOnSale", "false")));
                allowNegativeStock.set(Boolean.parseBoolean(props.getProperty("allowNegativeStock", "false")));
                lowStockPOSAlert.set(Boolean.parseBoolean(props.getProperty("lowStockPOSAlert", "false")));
                batchExpiryAlerts.set(Boolean.parseBoolean(props.getProperty("batchExpiryAlerts", "false")));
                trackClientsDebts.set(Boolean.parseBoolean(props.getProperty("trackClientsDebts", "false")));
                trackSupplierDebts.set(Boolean.parseBoolean(props.getProperty("trackSupplierDebts", "false")));

                return;
            } catch (IOException e) {
                System.err.println("[Config] Error reading " + CONFIG_FILE + ". Recreating default file.");
            }
        }

        // File doesn't exist or failed to load: create default file
        setLanguage(DEFAULT_LANGUAGE);
        saveConfigToFile(DEFAULT_LANGUAGE, false, false, false, false, false, false, false);
    }
    public void saveConfigToFile(){        Properties props = new Properties();
        props.setProperty("language", this.language);
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
    public void saveLanguageToConfigFile(String langCode) {
        saveConfigToFile(
                langCode,
                trackStock.get(),
                updateStockOnSale.get(),
                allowNegativeStock.get(),
                lowStockPOSAlert.get(),
                batchExpiryAlerts.get(),
                trackClientsDebts.get(),
                trackSupplierDebts.get()
        );
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

        // Update live properties
        this.trackStock.set(trackStock);
        this.updateStockOnSale.set(updateStockOnSale);
        this.allowNegativeStock.set(allowNegativeStock);
        this.lowStockPOSAlert.set(lowStockPOSAlert);
        this.batchExpiryAlerts.set(batchExpiryAlerts);
        this.trackClientsDebts.set(trackClientsDebts);
        this.trackSupplierDebts.set(trackSupplierDebts);

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

    // --- Stock & Alert Getters, Setters, and Properties ---

    public boolean isTrackStock() {
        return trackStock.get();
    }

    public void setTrackStock(boolean trackStock) {
        this.trackStock.set(trackStock);
    }

    public BooleanProperty trackStockProperty() {
        return trackStock;
    }

    public boolean isUpdateStockOnSale() {
        return updateStockOnSale.get();
    }

    public void setUpdateStockOnSale(boolean updateStockOnSale) {
        this.updateStockOnSale.set(updateStockOnSale);
    }

    public BooleanProperty updateStockOnSaleProperty() {
        return updateStockOnSale;
    }

    public boolean isAllowNegativeStock() {
        return allowNegativeStock.get();
    }

    public void setAllowNegativeStock(boolean allowNegativeStock) {
        this.allowNegativeStock.set(allowNegativeStock);
    }

    public BooleanProperty allowNegativeStockProperty() {
        return allowNegativeStock;
    }

    public boolean isLowStockPOSAlert() {
        return lowStockPOSAlert.get();
    }

    public void setLowStockPOSAlert(boolean lowStockPOSAlert) {
        this.lowStockPOSAlert.set(lowStockPOSAlert);
    }

    public BooleanProperty lowStockPOSAlertProperty() {
        return lowStockPOSAlert;
    }

    public boolean isBatchExpiryAlerts() {
        return batchExpiryAlerts.get();
    }

    public void setBatchExpiryAlerts(boolean batchExpiryAlerts) {
        this.batchExpiryAlerts.set(batchExpiryAlerts);
    }

    public BooleanProperty batchExpiryAlertsProperty() {
        return batchExpiryAlerts;
    }

    // --- Debt Getters, Setters, and Properties ---

    public boolean isTrackClientsDebts() {
        return trackClientsDebts.get();
    }

    public void setTrackClientsDebts(boolean trackClientsDebts) {
        this.trackClientsDebts.set(trackClientsDebts);
    }

    public ObservableValue<Boolean> trackClientsDebtsProperty() {
        return trackClientsDebts;
    }

    public boolean isTrackSupplierDebts() {
        return trackSupplierDebts.get();
    }

    public void setTrackSupplierDebts(boolean trackSupplierDebts) {
        this.trackSupplierDebts.set(trackSupplierDebts);
    }

    public ObservableValue<Boolean> trackSupplierDebtsProperty() {
        return trackSupplierDebts;
    }
}