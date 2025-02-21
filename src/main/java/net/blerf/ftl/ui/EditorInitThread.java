package net.blerf.ftl.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.blerf.ftl.core.EditorConfig;
import net.blerf.ftl.net.TaggedString;
import net.blerf.ftl.net.TaggedStringResponseHandler;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

@Slf4j
public class EditorInitThread extends Thread {

    private static final String BASE_URL = "https://raw.github.com/reseto/ftl-profile-editor/master/";
    private static final String LATEST_VERSION_URL = BASE_URL + "latest-version.txt";
    private static final String VERSION_HISTORY_URL = BASE_URL + "release-notes.txt";

    private final FTLFrame frame;
    private final EditorConfig initConfig;
    private final int appVersion;


    public EditorInitThread(FTLFrame frame, EditorConfig initConfig, int appVersion) {
        super("init");
        this.frame = frame;
        this.initConfig = initConfig;
        this.appVersion = appVersion;
    }

    @Override
    public void run() {
        try {
            init();
        } catch (Exception e) {
            log.error("Error during EditorFrame init", e);
        }
    }

    private void init() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        int appUpdateInterval = initConfig.getPropertyAsInt(EditorConfig.UPDATE_APP, 0);
        boolean needAppUpdate = false;

        if (appUpdateInterval > 0) {
            String prevTimestampString = initConfig.getProperty(EditorConfig.APP_UPDATE_TIMESTAMP, "");

            if ("true".equals(initConfig.getProperty(EditorConfig.APP_UPDATE_AVAILABLE, "false"))) {
                // Previously saw an update was available. Keep nagging.

                log.debug("Overriding update check interval because a new version has been seen already");
                needAppUpdate = true;
            } else if (prevTimestampString.matches("^\\d+$")) {
                // Only check if it's been a while since last time.

                long prevTimestamp = Long.parseLong(prevTimestampString) * 1000L;
                Calendar prevCal = Calendar.getInstance();
                prevCal.setTimeInMillis(prevTimestamp);
                prevCal.getTimeInMillis();  // Re-calculate calendar fields.

                Calendar freshCal = Calendar.getInstance();
                freshCal.add(Calendar.DATE, appUpdateInterval * -1);
                freshCal.getTimeInMillis();  // Re-calculate calendar fields.

                needAppUpdate = (prevCal.compareTo(freshCal) < 0);

                if (needAppUpdate) {
                    log.debug(String.format("App update info is older than %d days: %s", appUpdateInterval, dateFormat.format(prevCal.getTime())));
                } else {
                    log.debug(String.format("App update info isn't stale yet: %s", dateFormat.format(prevCal.getTime())));
                }
            } else {
                needAppUpdate = true;
            }
        }

        if (needAppUpdate) {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(5000)
                    .setConnectTimeout(5000)
                    .setSocketTimeout(10000)
                    .setRedirectsEnabled(true)
                    .build();

            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                    .setDefaultRequestConfig(requestConfig)
                    //.setUserAgent( "" )
                    .disableAuthCaching()
                    .disableAutomaticRetries()
                    .disableConnectionState()
                    .disableCookieManagement();

            String eTagCached = null;  // TODO.
            HttpGet request = null;
            try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
                TaggedStringResponseHandler responseHandler = new TaggedStringResponseHandler();

                final long updateTimestamp = System.currentTimeMillis() / 1000L;
                final String updateETag;
                final boolean updateAvailable;
                final Map<Integer, List<String>> historyMap;

                log.debug("Checking for the latest version");

                request = new HttpGet(LATEST_VERSION_URL);

                if (eTagCached != null) request.addHeader("If-None-Match", eTagCached);

                TaggedString latestResult = httpClient.execute(request, responseHandler);

                // When an ETag is known and the file hasn't changed, latestResult will be null.

                int latestVersion = -1;
                if (latestResult != null) {
                    String latestVersionString = latestResult.text.trim();  // Trailing line break.
                    if (latestVersionString.matches("^\\d+$")) {
                        latestVersion = Integer.parseInt(latestVersionString);
                    }

                    updateETag = (latestResult.etag != null ? latestResult.etag : "");
                } else {
                    updateETag = "";
                }
                updateAvailable = (latestVersion > appVersion);

                if (updateAvailable) {
                    log.debug("A newer version is available, fetching version history...");

                    request = new HttpGet(VERSION_HISTORY_URL);
                    TaggedString historyResult = httpClient.execute(request, responseHandler);

                    historyMap = parseVersionHistory(historyResult.text);
                } else {
                    historyMap = null;
                }

                // Make changes from the GUI thread.
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if (historyMap != null) {
                            frame.setVersionHistory(historyMap);
                        }
                        EditorConfig frameConfig = frame.getConfig();
                        frameConfig.setProperty(EditorConfig.APP_UPDATE_TIMESTAMP, "" + updateTimestamp);
                        frameConfig.setProperty(EditorConfig.APP_UPDATE_ETAG, updateETag);
                        frameConfig.setProperty(EditorConfig.APP_UPDATE_AVAILABLE, (updateAvailable ? "true" : "false"));
                    }
                };
                SwingUtilities.invokeLater(r);
            } catch (ClientProtocolException e) {
                log.error("GET request failed for url: {}", request.getURI().toString(), e);
            } catch (Exception e) {
                log.error("Checking for latest version failed", e);
            }
        }
    }


    /**
     * Parses history text to a Map of release versions with itemized changes.
     */
    private Map<Integer, List<String>> parseVersionHistory(String historyText) {
        Map<Integer, List<String>> historyMap = new LinkedHashMap<>();

        Scanner historyScanner = new Scanner(historyText);
        while (historyScanner.hasNextLine()) {
            int releaseVersion = Integer.parseInt(historyScanner.nextLine());
            List<String> releaseChangeList = new ArrayList<>();
            historyMap.put(releaseVersion, releaseChangeList);

            while (historyScanner.hasNextLine()) {
                String line = historyScanner.nextLine();
                if (line.isEmpty()) break;

                releaseChangeList.add(line);
            }

            // Must've either hit a blank or done.
        }
        historyScanner.close();

        return historyMap;
    }
}
