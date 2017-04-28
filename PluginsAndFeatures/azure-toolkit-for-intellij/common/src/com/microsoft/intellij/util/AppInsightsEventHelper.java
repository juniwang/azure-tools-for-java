/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.util;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class AppInsightsEventHelper {
    private static final String EVENT_NAME_PREFIX = "AzurePlugin.Intellij.";

    public enum EventType {
        MainMenu,
        AzureExplorer,
        Dialog,
        WizardStep,
        Telemetry,
        DockerContainer,
        DockerHost,
        WebApp,
        Application,
        Subscription
    }

    public static void createEvent(final EventType eventType, final String objectName, final String action) {
        createEvent(eventType, objectName, action, null);
    }

    public static void createEvent(final EventType eventType, final String objectName, final String action, final Map<String, String> properties) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(EVENT_NAME_PREFIX).append(eventType.name());
        if (!StringUtils.isEmpty(objectName)) stringBuilder.append(".").append(objectName);
        if (!StringUtils.isEmpty(action)) stringBuilder.append(".").append(action);
        AppInsightsCustomEvent.create(stringBuilder.toString(), null, properties);
    }
}
