package com.microsoft.intellij.ui.components;

import com.intellij.ui.wizard.WizardModel;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.intellij.util.AppInsightsCustomEvent;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Customized WizardStep specifically for Azure Intellij Plugin.
 * So that we can perform common actions in a base-class level for example telemetry.
 * Literally all concrete WizardStep implementations should inherit from this class rather than WizardStep.
 */
public abstract class AzureWizardStep<T extends WizardModel> extends WizardStep<T> {
    protected AzureWizardStep() {
    }

    public AzureWizardStep(String title) {
        super(title);
    }

    public AzureWizardStep(String title, String explanation) {
        super(title, explanation);
    }

    public AzureWizardStep(String title, String explanation, Icon icon) {
        super(title, explanation, icon);
    }

    public AzureWizardStep(String title, String explanation, Icon icon, String helpId) {
        super(title, explanation, icon, helpId);
    }

    /*
    Override this method if more additional properties to be sent.
     */
    protected void addExtraTelemetryProperties(final Map<String, String> properties) {

    }

    protected void sendTelemetryOnNext(final String action) {
        sendTelemetryOnNext(action, null);
    }

    protected void sendTelemetryOnNext(final String action, final T model) {
        final String eventName = "AzurePlugin.Intellij.WizardStep." + this.getClass().getSimpleName() + "." + action;
        final Map<String, String> properties = new HashMap<>();
        properties.put("wizardStep", this.getClass().getSimpleName());
        properties.put("action", action);
        properties.put("title", this.getTitle());

        if (model != null && model instanceof TelemetryProperties) {
            properties.putAll(((TelemetryProperties) model).toProperties());
        }

        addExtraTelemetryProperties(properties);
        AppInsightsCustomEvent.create(eventName, "", properties);
    }

    @Override
    public WizardStep onNext(T model) {
        sendTelemetryOnNext("next", model);
        return super.onNext(model);
    }

    @Override
    public boolean onFinish() {
        sendTelemetryOnNext("finish");
        return super.onFinish();
    }

    @Override
    public boolean onCancel() {
        sendTelemetryOnNext("cancel");
        return super.onCancel();
    }
}
