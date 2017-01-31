/*
 * *
 *  * Copyright (c) Microsoft Corporation
 *  * <p/>
 *  * All rights reserved.
 *  * <p/>
 *  * MIT License
 *  * <p/>
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  * <p/>
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  * <p/>
 *  * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.WebAppUtils;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebAppDeployDialog extends DialogWrapper {
    private static final Logger LOGGER = Logger.getInstance(WebAppDeployDialog.class);

    private JPanel contentPane;
    private JTable table;
    private JButton createButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private JCheckBox deployToRootCheckBox;
    private JEditorPane editorPaneAppServiceDetails;
    private JButton editButton;

    private final Module module;
    private final Artifact artifact;

    static class WebAppDetails {
        public SubscriptionDetail subscriptionDetail;
        public ResourceGroup resourceGroup;
        public AppServicePlan appServicePlan;
        public WebApp webApp;
    }

    private Map<String, WebAppDetails> webAppWebAppDetailsMap = new HashMap<>();

    public static WebAppDeployDialog go(Module module, Artifact artifact) {
        WebAppDeployDialog d = new WebAppDeployDialog(module, artifact);
        d.show();
        if (d.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            return d;
        }

        return null;
    }

    @Override
    public void show() {
        fillTable();
        super.show();
    }

    protected WebAppDeployDialog(Module module, Artifact artifact) {
        super(module.getProject(), true, IdeModalityType.PROJECT);
        this.module = module;
        this.artifact = artifact;

        setModal(true);
        setTitle("Deploy Web App");
        setOKButtonText("Deploy");

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createAppService();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteAppService();
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                cleanTable();
                editorPaneAppServiceDetails.setText("");
                AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                fillTable();
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                editAppService();
            }
        });

        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (event.getValueIsAdjusting()) return;
                //System.out.println("row : " + table.getValueAt(table.getSelectedRow(), 0).toString());
                fillAppServiceDetails();
            }
        });

//        class DisabledItemSelectionModel extends DefaultListSelectionModel {
//            @Override
//            public void setSelectionInterval(int index0, int index1) {
//                super.setSelectionInterval(-1, -1);
//            }
//        }

//        listWebAppDetails.setSelectionModel(new DisabledItemSelectionModel());
//        listWebAppDetails.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        editorPaneAppServiceDetails.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    // Do something with e.getURL() here
                    //e.getURL().toString()
                    JXHyperlink link = new JXHyperlink();
                    link.setURI(URI.create(e.getURL().toString()));
                    link.doClick();
                }
            }
        });

        init();
    }

    private void cleanTable() {
        DefaultTableModel dm = (DefaultTableModel) table.getModel();
        dm.getDataVector().removeAllElements();
        dm.fireTableDataChanged();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "WebAppDeployDialog";
    }

    private void fillTable() {
        if (AzureModel.getInstance().getResourceGroupToWebAppMap() == null) {
            updateAndFillTable();
        } else {
            doFillTable();
        }
    }

    private void updateAndFillTable() {
        ProgressManager.getInstance().run(new Task.Modal(module.getProject(), "Getting App Services...", true) {
            @Override
            public void run(ProgressIndicator progressIndicator) {

                progressIndicator.setIndeterminate(true);
                try {
                    if (progressIndicator.isCanceled()) {
                        AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                doCancelAction();
                            }
                        }, ModalityState.any());
                    }

                    AzureModelController.updateResourceGroupMaps(new UpdateProgressIndicator(progressIndicator));

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            doFillTable();
                        }
                    }, ModalityState.any());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    LOGGER.debug("updateAndFillTable", ex);
                }
            }
        });
    }

    private void doFillTable() {
        Map<SubscriptionDetail, List<ResourceGroup>> srgMap = AzureModel.getInstance().getSubscriptionToResourceGroupMap();
        Map<ResourceGroup, List<WebApp>> rgwaMap = AzureModel.getInstance().getResourceGroupToWebAppMap();
        Map<ResourceGroup, List<AppServicePlan>> rgaspMap = AzureModel.getInstance().getResourceGroupToAppServicePlanMap();

        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.addColumn("Name");
        tableModel.addColumn("JDK");
        tableModel.addColumn("Web Container");
        tableModel.addColumn("Resource Group");
        webAppWebAppDetailsMap.clear();
        for (SubscriptionDetail sd : srgMap.keySet()) {
            if (!sd.isSelected()) continue;

            Map<String, AppServicePlan> aspMap = new HashMap<>();
            for (ResourceGroup rg : srgMap.get(sd)) {
                for (AppServicePlan asp : rgaspMap.get(rg)) {
                    aspMap.put(asp.id(), asp);
                }
            }

            for (ResourceGroup rg : srgMap.get(sd)) {
                for (WebApp wa : rgwaMap.get(rg)) {
                    if (wa.javaVersion() != JavaVersion.OFF) {
                        tableModel.addRow(new String[]{
                                wa.name(),
                                wa.javaVersion().toString(),
                                wa.javaContainer() + " " + wa.javaContainerVersion(),
                                wa.resourceGroupName()
                        });
                    } else {
                        tableModel.addRow(new String[]{
                                wa.name(),
                                "Off",
                                "N/A",
                                wa.resourceGroupName()
                        });
                    }
                    WebAppDetails webAppDetails = new WebAppDetails();
                    webAppDetails.webApp = wa;
                    webAppDetails.subscriptionDetail = sd;
                    webAppDetails.resourceGroup = rg;
                    webAppDetails.appServicePlan = aspMap.get(wa.appServicePlanId());
                    webAppWebAppDetailsMap.put(wa.name(), webAppDetails);
                }
            }
        }

        table.setModel(tableModel);
        if (tableModel.getRowCount() > 0)
            tableModel.fireTableDataChanged();
    }

    private void createAppService() {
        AppServiceCreateDialog d = AppServiceCreateDialog.go(this.module);
        if (d == null) {
            // something went wrong - report an error!
            return;
        }
        WebApp wa = d.getWebApp();
        doFillTable();
        selectTableRowWithWebAppName(wa.name());
        //fillAppServiceDetails();
    }

    private void editAppService() {

        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
            String appServiceName = (String) tableModel.getValueAt(selectedRow, 0);
            WebAppDetails wad = webAppWebAppDetailsMap.get(appServiceName);

            AppServiceChangeSettingsDialog d = AppServiceChangeSettingsDialog.go(wad, this.module);
            if (d == null) {
                // something went wrong - report an error!
                return;
            }
            WebApp wa = d.getWebApp();
            doFillTable();
            selectTableRowWithWebAppName(wa.name());
        }
        //fillAppServiceDetails();
    }

    private void selectTableRowWithWebAppName(String webAppName) {
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        for (int ri = 0; ri < tableModel.getRowCount(); ++ri) {
            String waName = (String) tableModel.getValueAt(ri, 0);
            if (waName.equals(webAppName)) {
                table.setRowSelectionInterval(ri, ri);
                break;
            }
        }
    }

    private void deleteAppService() {
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            String appServiceName = (String)tableModel.getValueAt(selectedRow, 0);
            WebAppDetails wad = webAppWebAppDetailsMap.get(appServiceName);

            int choice = JOptionPane.showOptionDialog(WebAppDeployDialog.this.getContentPane(),
                    "Do you really want to delete the App Service '" + appServiceName + "'?",
                    "Detete App Service",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null, null, null);

            if (choice == JOptionPane.NO_OPTION) {
                return;
            }

            try{
                AzureManager manager = AuthMethodManager.getInstance().getAzureManager();
                if (manager == null) {
                    return;
                }
                ProgressManager.getInstance().run(new Task.Modal(module.getProject(), "Deleting App Service...", true) {
                    @Override
                    public void run(ProgressIndicator progressIndicator) {
                        try {
                            progressIndicator.setIndeterminate(true);
                            manager.getAzure(wad.subscriptionDetail.getSubscriptionId()).webApps().deleteById(wad.webApp.id());
                            ApplicationManager.getApplication().invokeAndWait( new Runnable() {
                                @Override
                                public void run() {
                                    tableModel.removeRow(selectedRow);
                                    tableModel.fireTableDataChanged();
                                    // update cache
                                    AzureModelController.removeWebAppFromExistingResourceGroup(wad.resourceGroup, wad.webApp);
                                    editorPaneAppServiceDetails.setText("");
                                }
                            }, ModalityState.any());


                        } catch (Exception ex) {
                            ex.printStackTrace();
                            LOGGER.error("deleteAppService : Task.Modal ", ex);
                            ErrorWindow.show(ex.getMessage(), "Delete App Service Error", WebAppDeployDialog.this.contentPane);

                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("deleteAppService", e);
                ErrorWindow.show(e.getMessage(), "Delete App Service Error", this.contentPane);
            }
        }
    }

    private void fillAppServiceDetails() {
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            String appServiceName = (String)tableModel.getValueAt(selectedRow, 0);
            WebAppDetails wad = webAppWebAppDetailsMap.get(appServiceName);
            SubscriptionDetail sd = wad.subscriptionDetail;
            AppServicePlan asp = wad.appServicePlan;

            StringBuilder sb = new StringBuilder();
            sb.append("<div style=\"margin: 7px 7px 7px 7px;\">");
            sb.append(String.format("<b>App Service Name:</b>&nbsp;%s<br/>", appServiceName));
            sb.append(String.format("<b>Subscription Name:</b>&nbsp;%s;&nbsp;<b>ID:</b>&nbsp;%s<br/>", sd.getSubscriptionName(), sd.getSubscriptionId()));
            String aspName = asp == null ? "N/A" : asp.name();
            String aspPricingTier = asp == null ? "N/A" : asp.pricingTier().toString();
            sb.append(String.format("<b>App Service Plan Name:</b>&nbsp;%s;&nbsp;<b>Pricing Tier:</b>&nbsp;%s<br/>", aspName, aspPricingTier));

            String link = buildSiteLink(wad.webApp, null);
            sb.append(String.format("<b>Link:</b>&nbsp;<a href=\"%s\">%s</a>", link, link));
            sb.append("</div>");
            editorPaneAppServiceDetails.setText(sb.toString());
        }
//        listWebAppDetails.setModel(listModel);
    }

    private static String buildSiteLink(WebApp webApp, String artifactName) {
        String appServiceLink = "https://" + webApp.defaultHostName();
        if (artifactName != null && !artifactName.isEmpty())
            return appServiceLink + "/" + artifactName;
        else
            return appServiceLink;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return new ValidationInfo("Please select an App Service to deploy to", table);
        }
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        WebAppDetails wad = webAppWebAppDetailsMap.get(tableModel.getValueAt(selectedRow, 0));
        if (wad.webApp.javaVersion()  == JavaVersion.OFF ) {
            return new ValidationInfo("Please select JAVA bases App Service", table);
        }

        return super.doValidate();
    }

    @Override
    protected void doOKAction() {
        deploy();
        super.doOKAction();
    }

    private void deploy() {
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        int selectedRow = table.getSelectedRow();
        WebAppDetails wad = webAppWebAppDetailsMap.get(tableModel.getValueAt(selectedRow, 0));
        WebApp webApp = wad.webApp;
        boolean isDeployToRoot = deployToRootCheckBox.isSelected();
        ProgressManager.getInstance().run(new Task.Modal(module.getProject(), "Deploying Web App...", true) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {
                    progressIndicator.setIndeterminate(true);
                    PublishingProfile pp = webApp.getPublishingProfile();
                    WebAppUtils.deployArtifact(artifact.getName(), artifact.getOutputFilePath(),
                            pp, isDeployToRoot, new UpdateProgressIndicator(progressIndicator));
                    String sitePath = buildSiteLink(wad.webApp, artifact.getName());
                    progressIndicator.setText("Checking the web app is available...");
                    progressIndicator.setText2("Link: " + sitePath);

                    // to make warn up cancelable
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int stepLimit = 5;
                                int sleepMs = 2000;
                                for (int step = 0; step < stepLimit; ++step) {

                                    if (WebAppUtils.isUrlAccessabel(sitePath))  { // warm up
                                        break;
                                    }
                                    Thread.sleep(sleepMs);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                LOGGER.error("deploy::warmup", e);
                            }
                        }
                    });
                    thread.run();
                    while (thread.isAlive()) {
                        if (progressIndicator.isCanceled()) return;
                        else Thread.sleep(2000);
                    }
                    showLink(sitePath);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    LOGGER.error("deploy", ex);
                    ErrorWindow.show(ex.getMessage(), "Deploy Application Error", WebAppDeployDialog.this.contentPane);
                }
            }
        });
    }

    private void showLink(String link) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                int choice = JOptionPane.showOptionDialog(WebAppDeployDialog.this.getContentPane(),
                        "Web App has been uploaded successfully.\nLink: " + link + "\nOpen in browser?",
                        "Web App Upload Status",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null, null, null);

                if (choice == JOptionPane.YES_OPTION)
                {
                    JXHyperlink hl = new JXHyperlink();
                    hl.setURI(URI.create(link));
                    hl.doClick();
                }
            }
        }, ModalityState.any());
    }
}