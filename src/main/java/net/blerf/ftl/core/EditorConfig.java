package net.blerf.ftl.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EditorConfig {


    public static final String CONFIG_FILE_NAME = "ftl-editor.cfg";
    public static final String FTL_DATS_PATH = "ftl_dats_path";
    public static final String UPDATE_APP = "update_app";
    public static final String USE_DEFAULT_UI = "use_default_ui";
    public static final String APP_UPDATE_TIMESTAMP = "app_update_timestamp";
    public static final String APP_UPDATE_ETAG = "app_update_etag";
    public static final String APP_UPDATE_AVAILABLE = "app_update_available";
    public static final String LAUNCHES = "number_of_launches";
    public static final String FRAME_WIDTH = "editor_width";
    public static final String FRAME_HEIGHT = "editor_height";
    public static final int DEFAULT_WIDTH = 800;
    public static final int DEFAULT_HEIGHT = 700;

    private final Properties properties;
    private final File configFile;

    public EditorConfig() {
        configFile = new File(CONFIG_FILE_NAME);
        properties = new Properties();
        properties.setProperty(FTL_DATS_PATH, "");  // Prompt.
        properties.setProperty(UPDATE_APP, "");     // Prompt.
        properties.setProperty(USE_DEFAULT_UI, "false");
        properties.setProperty(LAUNCHES, "0");
        properties.setProperty(FRAME_WIDTH, "" + DEFAULT_WIDTH);
        properties.setProperty(FRAME_HEIGHT, "" + DEFAULT_HEIGHT);
        // "app_update_timestamp" doesn't have a default.
        // "app_update_etag" doesn't have a default.
        // "app_update_available" doesn't have a default.
    }

    /**
     * Copy constructor.
     */
    public EditorConfig(EditorConfig srcConfig) {
        configFile = srcConfig.getConfigFile();
        properties = new Properties();
        properties.putAll(srcConfig.getProperties());
    }


    public Properties getProperties() {
        return properties;
    }

    public File getConfigFile() {
        return configFile;
    }


    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public int getPropertyAsInt(String key, int defaultValue) {
        String s = properties.getProperty(key);
        if (s != null && s.matches("^\\d+$"))
            return Integer.parseInt(s);
        else
            return defaultValue;
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public boolean isFirstLaunch() {
        return 0 == getPropertyAsInt(EditorConfig.LAUNCHES, 0);
    }

    public int getWidth() {
        return getPropertyAsInt(FRAME_WIDTH, DEFAULT_WIDTH);
    }

    public int getHeight() {
        return getPropertyAsInt(FRAME_HEIGHT, DEFAULT_HEIGHT);
    }

    public void readConfigFile() {
        if (configFile.exists()) {
            log.trace("Loading properties from config file {}", configFile.getAbsolutePath());
            try (InputStreamReader in = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                properties.load(in);
            } catch (IOException e) {
                log.error("Error loading config {}", configFile.getAbsolutePath(), e);
            }
        }
    }


    public void writeConfigFile() throws IOException {

        try (OutputStream out = new FileOutputStream(configFile)) {

            Map<String, String> userFieldsMap = new LinkedHashMap<>();
            Map<String, String> appFieldsMap = new LinkedHashMap<>();

            userFieldsMap.put(FTL_DATS_PATH, "The path to FTL's resources folder. If invalid, you'll be prompted.");
            userFieldsMap.put(USE_DEFAULT_UI, "If true, no attempt will be made to resemble a native GUI. Default: false.");
            userFieldsMap.put(UPDATE_APP, "If a number greater than 0, check for newer app versions every N days.");

            appFieldsMap.put(APP_UPDATE_TIMESTAMP, "Last update check's timestamp.");
            appFieldsMap.put(APP_UPDATE_ETAG, "Last update check's ETag.");
            appFieldsMap.put(APP_UPDATE_AVAILABLE, "Last update check's result.");

            List<String> allFieldsList = new ArrayList<>();
            allFieldsList.addAll(userFieldsMap.keySet());
            allFieldsList.addAll(appFieldsMap.keySet());
            int fieldWidth = 0;
            for (String fieldName : allFieldsList) {
                fieldWidth = Math.max(fieldName.length(), fieldWidth);
            }

            StringBuilder commentsBuf = new StringBuilder("\n");
            for (Map.Entry<String, String> entry : userFieldsMap.entrySet()) {
                commentsBuf.append(String.format(" %-" + fieldWidth + "s - %s%n", entry.getKey(), entry.getValue()));
            }
            commentsBuf.append("\n");
            for (Map.Entry<String, String> entry : appFieldsMap.entrySet()) {
                commentsBuf.append(String.format(" %-" + fieldWidth + "s - %s%n", entry.getKey(), entry.getValue()));
            }

            int launches = getPropertyAsInt(LAUNCHES, 0) + 1;
            properties.setProperty(LAUNCHES, "" + launches);
            OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            properties.store(writer, commentsBuf.toString());
            writer.flush();
        }
    }
}
