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
package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryProperties;

import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;

public abstract class NodeActionListener implements EventListener {
    protected static String name;

    public NodeActionListener() {
        // need a nullary constructor defined in order for
        // Class.newInstance to work on sub-classes
    }

    public NodeActionListener(Node node) {
    }

    protected void beforeActionPerformed(NodeActionEvent e) {
        // mark node as loading
//        e.getAction().getNode().setLoading(true);
        sendTelemetry(e);
    }

    protected void sendTelemetry(NodeActionEvent nodeActionEvent) {
        final Map<String, String> properties = new HashMap<>();
        Node node = nodeActionEvent.getAction().getNode();
        properties.put("Node", node.getName());
        if (node instanceof TelemetryProperties) {
            properties.putAll(((TelemetryProperties) node).toProperties());
        }
        if (node.getParent() != null) {
            properties.put("Parent", node.getParent().getName());
            properties.put("ParentType", node.getParent().getClass().getSimpleName());
        }

        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, node.getClass().getSimpleName(), nodeActionEvent.getAction().getName(), properties);
    }

    protected abstract void actionPerformed(NodeActionEvent e)
            throws AzureCmdException;

    public ListenableFuture<Void> actionPerformedAsync(NodeActionEvent e) {
        try {
            actionPerformed(e);
            return Futures.immediateFuture(null);
        } catch (AzureCmdException ex) {
            return Futures.immediateFailedFuture(ex);
        }
    }

    protected void afterActionPerformed(NodeActionEvent e) {
        // mark node as done loading
//        e.getAction().getNode().setLoading(false);
    }
}
