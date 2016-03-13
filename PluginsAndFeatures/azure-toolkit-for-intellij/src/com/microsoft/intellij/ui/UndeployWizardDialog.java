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
package com.microsoft.intellij.ui;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TitlePanel;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.runnable.FetchDeploymentsForHostedServiceWithProgressWindow;
import com.microsoft.intellij.runnable.LoadAccountWithProgressBar;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.wizards.WizardCacheManager;
import com.microsoft.tooling.msservices.model.vm.CloudService;
import com.microsoft.windowsazure.management.compute.models.DeploymentStatus;
import com.microsoft.windowsazure.management.compute.models.HostedServiceGetDetailedResponse;
import com.microsoft.windowsazure.management.compute.models.HostedServiceGetDetailedResponse.Deployment;
import com.microsoftopentechnologies.azurecommons.deploy.util.PublishData;
import com.microsoftopentechnologies.azuremanagementutil.model.Subscription;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;
import static com.microsoft.intellij.ui.util.UIUtils.ElementWrapper;

public class UndeployWizardDialog extends DialogWrapper {
    private JPanel contentPane;
    private JComboBox subscriptionCombo;
    private JComboBox hostedServiceCombo;
    private JComboBox deploymentCombo;

    private Project myProject;
    private PublishData currentPublishData;

    public UndeployWizardDialog(Project project) {
        super(project);
        this.myProject = project;
        init();
    }

    @Override
    protected void init() {
        setTitle(message("undeployWizTitle"));
        setOKButtonText(message("undeployWizTitle"));
        subscriptionCombo.addItemListener(createSubscriptionComboListener());
        hostedServiceCombo.addItemListener(createHostedServiceComboListener());
        deploymentCombo.addItemListener(createDeploymentComboListener());
        AzureSettings azureSettings = AzureSettings.getSafeInstance(myProject);
        if (!azureSettings.isSubscriptionLoaded()) {
            LoadAccountWithProgressBar task = new LoadAccountWithProgressBar(myProject);
            ProgressManager.getInstance().runProcessWithProgressSynchronously(task, "Loading Account Settings...", true, myProject);
            azureSettings.setSubscriptionLoaded(true);
        }
        populateSubscriptionCombo();

        super.init();
    }

    private ItemListener createSubscriptionComboListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                currentPublishData = UIUtils.changeCurrentSubAsPerCombo((JComboBox) e.getSource());

                hostedServiceCombo.setEnabled(false);
                deploymentCombo.setEnabled(false);
                deploymentCombo.removeAllItems();
                populateHostedServices();
//                setPageComplete(validatePageComplete());
            }
        };
    }

    private ItemListener createHostedServiceComboListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    deploymentCombo.removeAllItems();
                    deploymentCombo.setEnabled(false);

                    populateDeployment();

                    setComponentState();

//                setPageComplete(validatePageComplete());
                }
            }
        };
    }

    private ItemListener createDeploymentComboListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
//                setPageComplete(validatePageComplete());
            }
        };
    }

    private void populateSubscriptionCombo() {
        UIUtils.populateSubscriptionCombo(subscriptionCombo);
        setComponentState();
    }

    public void populateHostedServices() {
        if (currentPublishData != null) {
            Subscription currentSubscription = currentPublishData.getCurrentSubscription();
            List<CloudService> hostedServices = currentPublishData.getServicesPerSubscription().get(currentSubscription.getId());
            if (hostedServices != null) {
                hostedServiceCombo.removeAllItems();
                for (CloudService hsd : hostedServices) {
                    hostedServiceCombo.addItem(new ElementWrapper<CloudService>(hsd.getName(), hsd));
                }
                if (hostedServiceCombo.getItemCount() > 0) {
                    String defaultSelection = null;
                    CloudService currentHostedService = WizardCacheManager.getCurentHostedService();
                    if (currentHostedService != null) {
                        defaultSelection = currentHostedService.getName();
                    }
                    UIUtils.selectByText(hostedServiceCombo, defaultSelection);
                }
            }
        }
        setComponentState();
    }

    private void populateDeployment() {
        int sel = hostedServiceCombo.getSelectedIndex();
        if (sel > -1) {
            CloudService hostedServiceDetailed;
            FetchDeploymentsForHostedServiceWithProgressWindow progress =
                    new FetchDeploymentsForHostedServiceWithProgressWindow(null,
                            ((ElementWrapper<CloudService>) hostedServiceCombo.getSelectedItem()).getValue());
            ProgressManager.getInstance().runProcessWithProgressSynchronously(progress, "Progress Information", true, myProject);

            hostedServiceDetailed = progress.getHostedServiceDetailed();

            deploymentCombo.removeAllItems();

            addDeployment(hostedServiceDetailed.getProductionDeployment());
            addDeployment(hostedServiceDetailed.getStagingDeployment());
//            setComponentState();
        }
    }

    private void addDeployment(CloudService.Deployment deployment) {
        if (deployment.getName() == null || deployment.getName().isEmpty()) {
            return;
        }
        if (!DeploymentStatus.Deleting.equals(deployment.getStatus())) {
            String label = deployment.getLabel();
            String id = label + " - " + deployment.getSlot();
            deploymentCombo.addItem(new ElementWrapper<CloudService.Deployment>(id, deployment));
        }
    }

    private void setComponentState() {
        if (hostedServiceCombo == null) {
            return;
        }
        hostedServiceCombo.setEnabled(subscriptionCombo.getSelectedIndex() > -1 && hostedServiceCombo.getItemCount() > 0);
        if (deploymentCombo == null) {
            return;
        }
        deploymentCombo.setEnabled(hostedServiceCombo.getSelectedIndex() > -1 && deploymentCombo.getItemCount() > 0);
    }

    @Override
    protected ValidationInfo doValidate() {
        Object subscription = subscriptionCombo.getSelectedItem();
        if (subscription == null) {
            return new ValidationInfo("subscription can not be null or empty");
        }
        Object service = hostedServiceCombo.getSelectedItem();
        if (service == null) {
            return new ValidationInfo(message("hostedServiceIsNull"));
        }
        ElementWrapper<CloudService.Deployment> deploymentItem = (ElementWrapper<CloudService.Deployment>) deploymentCombo.getSelectedItem();
        if (deploymentItem == null) {
            return new ValidationInfo(message("deploymentIsNull"));
        }
        return null;
    }

    public String getServiceName() {
        return hostedServiceCombo.getSelectedItem().toString();
    }

    public CloudService.Deployment getDeployment() {
        return ((ElementWrapper<CloudService.Deployment>) deploymentCombo.getSelectedItem()).getValue();
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("unpubplishAzureProjPage"), "");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected String getHelpId() {
        return "unpublish_project_command";
    }
}