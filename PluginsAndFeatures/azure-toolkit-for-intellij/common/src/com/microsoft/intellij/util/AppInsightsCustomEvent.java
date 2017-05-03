package com.microsoft.intellij.util;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.intellij.ui.messages.AzureBundle;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;


public class AppInsightsCustomEvent {
    static String key = "9ee9694d-128e-4c2b-903f-dbe694548bf0";
    static String dataFile = PluginHelper.getTemplateFile(AzureBundle.message("dataFileName"));
    static String sessionId = UUID.randomUUID().toString();

    /**
     * @return resource filename in plugin's directory
     */
    private static String getTemplateFile(String fileName) {
        return String.format("%s%s%s", PluginUtil.getPluginRootDirectory(), File.separator, fileName);
    }

    public static void create(String eventName, String version, @Nullable Map<String, String> myProperties) {
        create(eventName, version, myProperties, false);
    }

    public static void create(String eventName, String version, @Nullable Map<String, String> myProperties, boolean force) {
        if (new File(dataFile).exists()) {
            String prefValue = DataOperations.getProperty(dataFile, AzureBundle.message("prefVal"));
            if (prefValue == null || prefValue.isEmpty() || prefValue.equalsIgnoreCase("true") || force) {
                TelemetryClient telemetry = new TelemetryClient();
                telemetry.getContext().setInstrumentationKey(key);
                Map<String, String> properties = myProperties == null ? new HashMap<String, String>() : new HashMap<String, String>(myProperties);
                properties.put("SessionId", sessionId);
                // Telemetry client doesn't accept null value
                for (Iterator<Map.Entry<String, String>> iter = properties.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry<String, String> entry = iter.next();
                    if (StringUtils.isNullOrEmpty(entry.getKey())) {
                        iter.remove();
                    }
                }
                if (version != null && !version.isEmpty()) {
                    properties.put("Library Version", version);
                }
                String pluginVersion = DataOperations.getProperty(dataFile, AzureBundle.message("pluginVersion"));
                if (pluginVersion != null && !pluginVersion.isEmpty()) {
                    properties.put("Plugin Version", pluginVersion);
                }

                String instID = DataOperations.getProperty(dataFile, AzureBundle.message("instID"));
                if (instID != null && !instID.isEmpty()) {
                    properties.put("Installation ID", instID);
                }

                telemetry.trackEvent(eventName, properties, null);
                telemetry.flush();
            }
        }
    }

    public static void create(String eventName, String version) {
        create(eventName, version, null);
    }

    public static void createFTPEvent(String eventName, String uri, String appName, String subId) {
        TelemetryClient telemetry = new TelemetryClient();
        telemetry.getContext().setInstrumentationKey(key);

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("SessionId", sessionId);
        if (uri != null && !uri.isEmpty()) {
            properties.put("WebApp URI", uri);
        }
        if (appName != null && !appName.isEmpty()) {
            properties.put("Java app name", appName);
        }
        if (subId != null && !subId.isEmpty()) {
            properties.put("Subscription ID", subId);
        }
        if (new File(dataFile).exists()) {
            String pluginVersion = DataOperations.getProperty(dataFile, AzureBundle.message("pluginVersion"));
            if (pluginVersion != null && !pluginVersion.isEmpty()) {
                properties.put("Plugin Version", pluginVersion);
            }

            String instID = DataOperations.getProperty(dataFile, AzureBundle.message("instID"));
            if (instID != null && !instID.isEmpty()) {
                properties.put("Installation ID", instID);
            }
        }
        telemetry.trackEvent(eventName, properties, null);
        telemetry.flush();
    }
}
