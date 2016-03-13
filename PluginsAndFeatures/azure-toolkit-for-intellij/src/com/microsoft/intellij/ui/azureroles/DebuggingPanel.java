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
package com.microsoft.intellij.ui.azureroles;

import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.interopbridges.tools.windowsazure.*;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoftopentechnologies.azurecommons.model.RoleAndEndpoint;
import com.microsoftopentechnologies.azurecommons.roleoperations.WARDebuggingUtilMethods;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.intellij.AzurePlugin.log;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class DebuggingPanel extends BaseConfigurable implements SearchableConfigurable, Configurable.NoScroll {
    private JPanel contentPane;
    private JCheckBox debugCheck;
    private JLabel lblDebugEndPoint;
    private JComboBox comboEndPoint;
    private JCheckBox jvmCheck;
    private JButton createDebug;

    private Project project;
    private WindowsAzureProjectManager waProjManager;
    private WindowsAzureRole waRole;
    private WindowsAzureEndpoint dbgSelEndpoint;
    private boolean isDebugChecked;
    private Map<String, String> debugMap = new HashMap<String, String>();
    private boolean childOk;

    public DebuggingPanel(Project project, WindowsAzureProjectManager waProjManager, WindowsAzureRole waRole) {
        this.project = project;
        this.waProjManager = waProjManager;
        this.waRole = waRole;
        init();
    }

    private void init() {
        try {
            WindowsAzureEndpoint endPt = waRole.getDebuggingEndpoint();
            if (endPt == null) {
                debugCheck.setSelected(false);
                comboEndPoint.removeAllItems(); // todo: ???
                makeAllDisable();
            } else {
                populateEndPointList();
                comboEndPoint.setSelectedItem(String.format(message("dbgEndPtStr"), endPt.getName(), endPt.getPort(), endPt.getPrivatePort()));
            }
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("dlgDbgErr"), e);
        }
        isDebugChecked = false;
        try {
            isDebugChecked = waRole.getDebuggingEndpoint() != null;
        } catch (Exception ex) {
            //As getTitle() is also showing the error message if any exception
            //occurs in role.getDebuggingEndpoint(), so only logging
            //the exception. getTitle() gets called every time this page is
            //selected but createContents() is called only once while creating
            //the page.
            log(message("dlgDbgErr"), ex);
        }
        debugCheck.setSelected(isDebugChecked);
        debugCheck.addItemListener(createDebugCheckListener());
        try {
            populateEndPointList();
        } catch (WindowsAzureInvalidProjectOperationException e1) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("dlgDbgErr"), e1);
        }
        comboEndPoint.addItemListener(createComboEndPointListener());

        try {
            if (isDebugChecked) {
                jvmCheck.setSelected(waRole.getStartSuspended());
            } else {
                jvmCheck.setSelected(false);
            }

        } catch (WindowsAzureInvalidProjectOperationException e2) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("dlgDbgErr"), e2);
        }
        jvmCheck.addItemListener(createJvmCheckListener());
        createDebug.addActionListener(createCreateDebugListener());

        try {
            if (isDebugChecked) {
                WindowsAzureEndpoint endPt = waRole.getDebuggingEndpoint();
                comboEndPoint.setSelectedItem(String.format(message("dbgEndPtStr"), endPt.getName(), endPt.getPort(), endPt.getPrivatePort()));
            } else {
                makeAllDisable();
            }
        } catch (WindowsAzureInvalidProjectOperationException e1) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("dlgDbgErr"), e1);
        }
        if (debugCheck.isSelected() && comboEndPoint.getSelectedItem().equals("")) {
//            setValid(false);
        }
    }

    private ItemListener createDebugCheckListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                debugOptionStatus();
            }
        };
    }

    private ItemListener createComboEndPointListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                try {
                    if (debugCheck.isSelected()) {
                        dbgSelEndpoint = WARDebuggingUtilMethods.getDebugSelectedEndpoint(waRole, (String) comboEndPoint.getSelectedItem());
                        waRole.setDebuggingEndpoint(dbgSelEndpoint);
                        myModified = true;
                        waRole.setStartSuspended(jvmCheck.isSelected());
                    }
                } catch (WindowsAzureInvalidProjectOperationException ex) {
                    PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
                }
            }
        };
    }

    private ItemListener createJvmCheckListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                try {
                    waRole.setStartSuspended(jvmCheck.isSelected());
                    myModified = true;
                } catch (WindowsAzureInvalidProjectOperationException ex) {
                    PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("dlgDbgErr"), ex);
                }
            }
        };
    }

    private ActionListener createCreateDebugListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DebugConfigurationDialog dialog = null;
                try {
                    dialog = new DebugConfigurationDialog(project, waRole, WARDebuggingUtilMethods.getDebugSelectedEndpoint(waRole, (String) comboEndPoint.getSelectedItem()), debugMap);
                    dialog.show();
                    childOk = dialog.isOK();
                } catch (WindowsAzureInvalidProjectOperationException ex) {
                    PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("dlgDbgErr"), ex);
                }
            }
        };
    }


    /**
     * This method sets the status of debug option
     * based on the user input.If user checks the debug check box
     * first time then it will add a debugging end point otherwise it will
     * prompt the user for removal of associated end point for debugging if
     * user already has some debug end point associated and unchecked
     * the debug check box.
     */
    private void debugOptionStatus() {
        if (debugCheck.isSelected()) {
            makeDebugEnable();
            try {
                waRole.setDebuggingEndpoint(dbgSelEndpoint);
                waRole.setStartSuspended(jvmCheck.isSelected());
            } catch (WindowsAzureInvalidProjectOperationException e1) {
                PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("dlgDbgErr"), e1);
            }
        } else {
            if (isDebugChecked && !"".equals(comboEndPoint.getSelectedItem())) {
                String msg = String.format("%s%s", message("dlgDbgEdPtAscMsg"), comboEndPoint.getSelectedItem());
                int choice = Messages.showOkCancelDialog(msg, message("dlgDbgEndPtErrTtl"), Messages.getQuestionIcon());
                if (choice == Messages.OK) {
                    removeDebugAssociatedEndpoint();
                } else {
                    makeAllDisable();
                    try {
                        waRole.setDebuggingEndpoint(null);
                    } catch (WindowsAzureInvalidProjectOperationException e) {
                        PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("dlgDbgErr"), e);
                    }
                }
            } else {
                removeDebugAssociatedEndpoint();
            }
        }
    }

    /**
     * This method removed the associated debug end point
     * if debug check box get unchecked.
     */
    private void removeDebugAssociatedEndpoint() {
        List<WindowsAzureEndpoint> endpointsList;
        try {
            endpointsList = new ArrayList<WindowsAzureEndpoint>(waRole.getEndpoints());
            for (WindowsAzureEndpoint endpoint : endpointsList) {
                if (((String) comboEndPoint.getSelectedItem())
                        .equalsIgnoreCase(String.format(message("dbgEndPtStr"), endpoint.getName(), endpoint.getPort(), endpoint.getPrivatePort()))) {
                    endpoint.delete();
                }
            }
            comboEndPoint.removeAllItems();
//            comboEndPoint.setText("");
            makeAllDisable();
            waRole.setDebuggingEndpoint(null);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("dlgDbgErr"), e);
        }
    }

    /**
     * This method populates the endpoint list
     * every time we made any changes in the endpoint list.
     *
     * @throws com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException
     */
    private void populateEndPointList() throws WindowsAzureInvalidProjectOperationException {
        List<WindowsAzureEndpoint> endpointsList;
        endpointsList = new ArrayList<WindowsAzureEndpoint>(waRole.getEndpoints());
        comboEndPoint.removeAllItems();
        for (WindowsAzureEndpoint endpoint : endpointsList) {
            if (((endpoint.getEndPointType().equals(WindowsAzureEndpointType.Input) && endpoint.getPrivatePort() != null)
                    || endpoint.getEndPointType().equals(WindowsAzureEndpointType.InstanceInput))
                    && !endpoint.equals(waRole.getSessionAffinityInputEndpoint()) && !endpoint.equals(waRole.getSslOffloadingInputEndpoint())) {
                comboEndPoint.addItem(String.format(message("dbgEndPtStr"), endpoint.getName(), endpoint.getPort(), endpoint.getPrivatePort()));
            }
        }
    }

    /**
     * This method enables all the control on UI
     * if Debug is enabled.
     */
    private void makeDebugEnable() {
        try {
            createDebug.setEnabled(true);
            comboEndPoint.setEnabled(true);
            jvmCheck.setEnabled(true);
            lblDebugEndPoint.setEnabled(true);

            RoleAndEndpoint obj = WARDebuggingUtilMethods.getDebuggingEndpoint(waRole, waProjManager);
            waRole = obj.getRole();
            WindowsAzureEndpoint endpt = obj.getEndPt();

            populateEndPointList();
            comboEndPoint.setSelectedItem(String.format(message("dbgEndPtStr"), endpt.getName(), endpt.getPort(), endpt.getPrivatePort()));

            dbgSelEndpoint = endpt;
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("dlgDbgErr"), e);
        }
    }

    /**
     * This method disables all the control on UI
     * if Debug is disabled.
     */
    private void makeAllDisable() {
        createDebug.setEnabled(false);
        comboEndPoint.setEnabled(false);
        jvmCheck.setEnabled(false);
        lblDebugEndPoint.setEnabled(false);
    }

    @NotNull
    @Override
    public String getId() {
        return getDisplayName();
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return message("cmhLblDbg");
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "windows_azure_debug_page";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return contentPane;
    }

    @Override
    public void apply() throws ConfigurationException {
        //check for child window's OK button was pressed or not
        //if not then we do not need to create a debug configuration otherwise
        //we do need to create debug configuration based on the user
        //selected options.
        if (childOk) {
            String emuCheck = debugMap.get(message("dlgDbgEmuChkd"));
            String cloudCheck = debugMap.get(message("dlgDbgCldChkd"));
            if (emuCheck != null && emuCheck.equals("true")) {
                // todo!!!
//                createLaunchConfig(
//                        debugMap.get(Messages.dlgDbgEmuConf),
//                        debugMap.get(Messages.dlgDbgEmuProj),
//                        debugMap.get(Messages.dlgDbgEmuHost),
//                        debugMap.get(Messages.dlgDbgEmuPort));
            }
            if (cloudCheck != null && cloudCheck.equals("true")) {
//                createLaunchConfig(
//                        debugMap.get(Messages.dlgDbgCldConf),
//                        debugMap.get(Messages.dlgDbgCldProj),
//                        debugMap.get(Messages.dlgDbgCldHost),
//                        debugMap.get(Messages.dlgDbgCldPort));
            }
        }
        boolean okToProceed = true;
        try {
            waProjManager.save();
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
            throw new ConfigurationException(message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), message("adRolErrTitle"));
        }
        myModified = false;
    }

    @Override
    public void reset() {
        myModified = false;
    }

    @Override
    public void disposeUIResources() {

    }
}
