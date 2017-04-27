/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.intellij.ui.components;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.intellij.util.AppInsightsEventHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by juniwang on 4/19/2017.
 * Subclass of DialogWrapper. Do some common implementation here like the telemetry.
 */
public abstract class AzureDialogWrapper extends DialogWrapper {
    protected AzureDialogWrapper(@Nullable Project project, boolean canBeParent) {
        super(project, canBeParent);
    }

    protected AzureDialogWrapper(@Nullable Project project, boolean canBeParent, @NotNull IdeModalityType ideModalityType) {
        super(project, canBeParent, ideModalityType);
    }

    protected AzureDialogWrapper(@Nullable Project project, @Nullable Component parentComponent, boolean canBeParent, @NotNull IdeModalityType ideModalityType) {
        super(project, parentComponent, canBeParent, ideModalityType);
    }

    protected AzureDialogWrapper(@Nullable Project project) {
        super(project);
    }

    protected AzureDialogWrapper(boolean canBeParent) {
        super(canBeParent);
    }

    protected AzureDialogWrapper(Project project, boolean canBeParent, boolean applicationModalIfPossible) {
        super(project, canBeParent, applicationModalIfPossible);
    }

    protected AzureDialogWrapper(@NotNull Component parent, boolean canBeParent) {
        super(parent, canBeParent);
    }

    /*
    Add custom properties to telemetry while Cancel button is pressed.
     */
    protected void addCancelTelemetryProperties(final Map<String, String> properties) {
    }

    /*
    Add custom properties to telemetry while OK button is pressed.
     */
    protected void addOKTelemetryProperties(final Map<String, String> properties) {
        final JComponent centerPanel = this.createCenterPanel();
        for (final Component component : getAllComponents(this.getContentPane())) {
            if (!component.isEnabled() || !component.isVisible())
                continue;

            if (component instanceof JRadioButton) {
                JRadioButton jRadioButton = (JRadioButton) component;
                String name = jRadioButton.getName() == null ? jRadioButton.getText() : jRadioButton.getName();
                properties.put("JRadioButton." + name + ".Selected", String.valueOf(jRadioButton.isSelected()));
            } else if (component instanceof JCheckBox) {
                JCheckBox jCheckBox = (JCheckBox) component;
                String name = jCheckBox.getName() == null ? jCheckBox.getText() : jCheckBox.getName();
                properties.put("JCheckBox." + name + ".Selected", String.valueOf(jCheckBox.isSelected()));
            } else if (component instanceof JComboBox) {
                JComboBox comboBox = (JComboBox) component;
                StringBuilder stringBuilder = new StringBuilder();
                for (final Object object : comboBox.getSelectedObjects()) {
                    stringBuilder.append(object.toString());
                    stringBuilder.append(";");
                }
                String name = comboBox.getName() == null ? comboBox.getLocation().toString() : comboBox.getName();
                properties.put("JComboBox." + name + ".Selected", stringBuilder.toString());
            } else if (component instanceof JTextField && !(component instanceof JPasswordField)) {
                properties.put("JTextField." + component.getName() + ".Value", String.valueOf(((JTextField) component).getText()));
            }
        }
    }

    protected java.util.List<Component> getAllComponents(final Container c) {
        Component[] comps = c.getComponents();
        java.util.List<Component> compList = new ArrayList<Component>();
        for (Component comp : comps) {
            compList.add(comp);
            if (comp instanceof Container)
                compList.addAll(getAllComponents((Container) comp));
        }
        return compList;
    }

    protected void sendOKorCancelTelemetry(boolean isOK) {
        final Map<String, String> properties = new HashMap<>();
        String action = "OK";
        properties.put("Window", this.getClass().getSimpleName());
        properties.put("Title", this.getTitle());
        if (isOK) {
            addOKTelemetryProperties(properties);
        } else {
            addCancelTelemetryProperties(properties);
            action = "Cancel";
        }

        AppInsightsEventHelper.createEvent(AppInsightsEventHelper.EventType.Dialog, this.getClass().getSimpleName(), action, properties);
    }

    @Override
    protected void doOKAction() {
        // send telemetry when OK button pressed.
        // In case subclass overrides doOKAction(), it should call super.doOKAction() explicitly
        // Otherwise the telemetry is omitted.
        this.sendOKorCancelTelemetry(true);
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        // send telemetry when Cancel button pressed.
        // In case subclass overrides doCancelAction(), it should call super.doCancelAction() explicitly
        // Otherwise the telemetry is omitted.
        this.sendOKorCancelTelemetry(false);
        super.doCancelAction();
    }
}
