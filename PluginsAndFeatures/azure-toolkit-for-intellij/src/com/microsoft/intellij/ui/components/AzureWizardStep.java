package com.microsoft.intellij.ui.components;

import com.intellij.ui.wizard.WizardModel;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.intellij.util.AppInsightsEventHelper;

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
        final Map<String, String> properties = new HashMap<>();
        properties.put("WizardStep", this.getClass().getSimpleName());
        properties.put("Action", action);
        properties.put("Title", this.getTitle());

        if (model != null && model instanceof TelemetryProperties) {
            properties.putAll(((TelemetryProperties) model).toProperties());
        }

        addExtraTelemetryProperties(properties);
        AppInsightsEventHelper.createEvent(AppInsightsEventHelper.EventType.WizardStep, this.getClass().getSimpleName(), action, properties);
    }

    @Override
    public WizardStep onNext(T model) {
        sendTelemetryOnNext("Next", model);
        return super.onNext(model);
    }

    @Override
    public boolean onFinish() {
        sendTelemetryOnNext("Finish");
        return super.onFinish();
    }

    @Override
    public boolean onCancel() {
        sendTelemetryOnNext("Cancel");
        return super.onCancel();
    }
}
