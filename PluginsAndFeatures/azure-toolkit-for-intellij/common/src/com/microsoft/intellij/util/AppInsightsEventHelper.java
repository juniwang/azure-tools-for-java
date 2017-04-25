package com.microsoft.intellij.util;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by juniwang on 4/25/2017.
 */
public class AppInsightsEventHelper {
    private static final String EVENT_NAME_PREFIX = "AzurePlugin.Intellij.";

    public enum EventType {
        MainMenu,
        AzureExplorer,
        Dialog,
        WizardStep,
        Telemetry,
        DockerContainer,
        Application
    }

    public static void createEvent(final EventType eventType, final String objectName, final String action, final Map<String, String> properties) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(EVENT_NAME_PREFIX).append(eventType.name());
        if (!StringUtils.isEmpty(objectName)) stringBuilder.append(".").append(objectName);
        if (!StringUtils.isEmpty(action)) stringBuilder.append(".").append(action);

        final Map<String, String> props = properties == null ? new HashMap<>() : properties;
        props.put("EventType", eventType.name());
        AppInsightsCustomEvent.create(stringBuilder.toString(), null, props);
    }
}
