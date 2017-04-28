package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.intellij.util.AppInsightsEventHelper;
import org.jdesktop.swingx.JXHyperlink;

import java.net.URI;

/**
 * Created by vlashch on 6/10/16.
 */
public class AzureCodeSamples extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        AppInsightsEventHelper.createEvent(AppInsightsEventHelper.EventType.MainMenu, "AzureCodeSamples", "Click");
        JXHyperlink portalLing = new JXHyperlink();
        portalLing.setURI(URI.create("https://azure.microsoft.com/en-us/documentation/samples/?platform=java"));
        portalLing.doClick();
    }
}
