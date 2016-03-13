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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TitlePanel;
import com.intellij.openapi.ui.ValidationInfo;
import com.interopbridges.tools.windowsazure.OSFamilyType;
import com.interopbridges.tools.windowsazure.WindowsAzurePackageType;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.forms.ImportSubscriptionForm;
import com.microsoft.intellij.forms.ManageSubscriptionPanel;
import com.microsoft.intellij.runnable.LoadAccountWithProgressBar;
import com.microsoft.intellij.ui.components.DefaultDialogWrapper;
import com.microsoft.intellij.ui.components.WindowsAzurePage;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.ui.util.UIUtils.ElementWrapper;
import com.microsoft.intellij.util.MethodUtils;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.wizards.WizardCacheManager;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.CloudService;
import com.microsoftopentechnologies.azurecommons.deploy.util.PublishData;
import com.microsoftopentechnologies.azurecommons.deploy.wizard.ConfigurationEventArgs;
import com.microsoftopentechnologies.azuremanagementutil.model.KeyName;
import com.microsoftopentechnologies.azuremanagementutil.model.Subscription;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.microsoft.intellij.AzurePlugin.log;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;
import static com.microsoft.windowsazure.management.compute.models.HostedServiceListResponse.HostedService;

public class DeployWizardDialog extends WindowsAzurePage {
    private JPanel contentPane;
    private JButton importButton;
    private JComboBox subscriptionCombo;
    private JXHyperlink subLink;
    private JComboBox storageAccountCmb;
    private JButton newStorageAccountBtn;
    private JComboBox hostedServiceCombo;
    private JButton newHostedServiceBtn;
    private JComboBox targetOS;
    private JComboBox deployStateCmb;
    private JCheckBox unpublishChBox;
    private JTextField userName;
    private JPasswordField userPassword;
    private JPasswordField confirmPassword;
    private JCheckBox conToDplyChkBtn;
    private JLabel userPasswordLbl;
    private JLabel confirmPasswordLbl;
    private JXHyperlink encLink;

    private final Module myModule;
    private PublishData publishData;
    private CloudService currentHostedService;
    private StorageAccount currentStorageAccount;
    private WindowsAzurePackageType deployMode = WindowsAzurePackageType.CLOUD;
    private String defaultLocation;
    public ArrayList<String> newServices = new ArrayList<String>();
    private String deployFileName;
    private String deployConfigFileName;
    private WindowsAzureProjectManager waProjManager;

    public DeployWizardDialog(Module module) {
        super(module.getProject());
        this.myModule = module;
        loadProject();
        init();
    }

    @Override
    protected void init() {
        super.init();
        myOKAction.putValue(Action.NAME, "Publish");
        importButton.addActionListener(createImportSubscriptionAction());
        subscriptionCombo.addItemListener(createSubscriptionComboListener());
        subLink.setAction(createSubLinkAction());
        UIUtils.populateSubscriptionCombo(subscriptionCombo);
        AzureSettings azureSettings = AzureSettings.getSafeInstance(myModule.getProject());
        if (!azureSettings.isSubscriptionLoaded()) {
            doLoadPreferences();
            // reload information if its new session.
            AzureSettings.getSafeInstance(myModule.getProject()).loadStorage();
            MethodUtils.prepareListFromPublishData(myModule.getProject());
            azureSettings.setSubscriptionLoaded(true);
            UIUtils.populateSubscriptionCombo(subscriptionCombo);
            if ((subscriptionCombo.getSelectedItem() != null)) {
                loadDefaultWizardValues();
            }
        }
        storageAccountCmb.addItemListener(createStorageAccountListener());
        populateStorageAccounts();
        newStorageAccountBtn.addActionListener(createNewStorageAccountListener());

        hostedServiceCombo.addItemListener(createHostedServiceComboListener());
        populateHostedServices();
        newHostedServiceBtn.addActionListener(createNewHostedServiceListener());

        populateTargetOs();

        deployStateCmb.setModel(new DefaultComboBoxModel(new String[]{message("deplStaging"), message("deplProd")}));
        deployStateCmb.addItemListener(createDeployStateCmbListener());

        userName.getDocument().addDocumentListener(createUserNameListener());
        encLink.setAction(createEncLinkAction());

        boolean isSubPresent = subscriptionCombo.getSelectedItem() != null;
        setComponentState(isSubPresent);
        if (isSubPresent) {
            // load cached subscription, cloud service & storage account
            loadDefaultWizardValues();
        }
        loadDefaultRDPValues();
    }

    private DocumentListener createUserNameListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setEnableRemAccess(e.getDocument().getLength() > 0);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setEnableRemAccess(e.getDocument().getLength() > 0);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setEnableRemAccess(e.getDocument().getLength() > 0);
            }
        };
    }

    private ActionListener createNewStorageAccountListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ElementWrapper<PublishData> subscription = (ElementWrapper<PublishData>) subscriptionCombo.getSelectedItem();
                if (subscription != null) {
                    PublishData publishData = ((ElementWrapper<PublishData>) subscriptionCombo.getSelectedItem()).getValue();
                    int maxStorageAccounts = publishData.getCurrentSubscription().getMaxStorageAccounts();

                    String currentSubscriptionId = publishData.getCurrentSubscription().getId();
                    if (maxStorageAccounts > publishData.getStoragesPerSubscription().get(currentSubscriptionId).size()) {
                        NewStorageAccountDialog storageAccountDialog = new NewStorageAccountDialog(subscription.getKey(), myModule.getProject());
                        if (defaultLocation != null) { // user has created a hosted service before a storage account
                            storageAccountDialog.setDefaultLocation(defaultLocation);
                        }
                        storageAccountDialog.show();
                        if (storageAccountDialog.isOK()) {
                            populateStorageAccounts();
                            UIUtils.selectByText(storageAccountCmb, storageAccountDialog.getStorageAccountName());
                            defaultLocation = WizardCacheManager.getStorageAccountFromCurrentPublishData(storageAccountDialog.getStorageAccountName()).getLocation();
                        }
                    } else {
                        PluginUtil.displayErrorDialog(message("storageAccountsLimitTitle"), message("storageAccountsLimitErr"));
                    }
                }
            }
        };
    }

    private Action createSubLinkAction() {
        return new AbstractAction(message("linkLblSub")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ManageSubscriptionPanel manageSubscriptionPanel = new ManageSubscriptionPanel(myModule.getProject(), true);
                final DefaultDialogWrapper subscriptionsDialog = new DefaultDialogWrapper((Project) myModule.getProject(),
                        manageSubscriptionPanel) {
                    @Nullable
                    @Override
                    protected JComponent createSouthPanel() {
                        return null;
                    }

                    @Override
                    protected JComponent createTitlePane() {
                        return null;
                    }
                };
                manageSubscriptionPanel.setDialog(subscriptionsDialog);
                subscriptionsDialog.show();

                /*
                 * Update data in every case.
				 * No need to check which button (OK/Cancel)
				 * has been pressed as change is permanent
				 * even though user presses cancel
				 * according to functionality.
				 */
//                doLoadPreferences();
                UIUtils.populateSubscriptionCombo(subscriptionCombo);
                // update cache of publish data object
                if (subscriptionCombo.getSelectedItem() != null) {
                    publishData = ((ElementWrapper<PublishData>) subscriptionCombo.getSelectedItem()).getValue();
                }
                // enable and disable components.
                setComponentState((subscriptionCombo.getSelectedItem() != null));
            }
        };
    }

    private Action createEncLinkAction() {
        return new AbstractAction(message("linkLblEnc")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // open remote access dialog
                DefaultDialogWrapper remoteAccess = new DefaultDialogWrapper(myModule.getProject(),
                        new WARemoteAccessPanel(myModule, true, userName.getText(), String.valueOf(userPassword.getPassword()),
                                String.valueOf(confirmPassword.getPassword())));
                remoteAccess.show();
                if (remoteAccess.isOK()) {
                    loadDefaultRDPValues();
                /*
                 * To handle the case, if you typed
			     * password on Publish wizard --> Encryption link
			     * Remote access --> OK --> Toggle password text boxes
			     */
//                    isPwdChanged = false;
                }
            }
        };
    }

    protected void doOKAction() {
        handlePageComplete();
        super.doOKAction();
    }

    /**
     * Initialize {@link WindowsAzureProjectManager} object
     * according to selected project.
     */
    private void loadProject() {
        try {
            String modulePath = PluginUtil.getModulePath(myModule);
            File projectDir = new File(modulePath);
            waProjManager = WindowsAzureProjectManager.load(projectDir);
        } catch (Exception e) {
            log(message("projLoadEr"), e);
        }
    }

    private void doLoadPreferences() {
        LoadAccountWithProgressBar task = new LoadAccountWithProgressBar(myModule.getProject());
        ProgressManager.getInstance().runProcessWithProgressSynchronously(task, "Loading Account Settings...", true, myModule.getProject());
    }

    private ActionListener createNewHostedServiceListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UIUtils.ElementWrapper<PublishData> subscriptionItem = (UIUtils.ElementWrapper<PublishData>) subscriptionCombo.getSelectedItem();
                if (subscriptionItem != null) {
                    PublishData publishData = subscriptionItem.getValue();
                    int maxHostedServices = publishData.getCurrentSubscription().getMaxHostedServices();
                    String currentSubscriptionId = publishData.getCurrentSubscription().getId();
                    if (maxHostedServices > publishData.getServicesPerSubscription().get(currentSubscriptionId).size()) {

                        NewHostedServiceDialog hostedServiceDialog = new NewHostedServiceDialog();
                        if (defaultLocation != null) { // user has created a storage account before creating the hosted service
                            hostedServiceDialog.setDefaultLocation(defaultLocation);
                        }
                        hostedServiceDialog.show();
                        if (hostedServiceDialog.isOK()) {
                            populateHostedServices();
                            newServices.add(hostedServiceDialog.getHostedServiceName());
                            UIUtils.selectByText(hostedServiceCombo, hostedServiceDialog.getHostedServiceName());
                            defaultLocation = WizardCacheManager.getHostedServiceFromCurrentPublishData(hostedServiceDialog.getHostedServiceName()).getLocation();
                        }
                    } else {
                        PluginUtil.displayErrorDialog(message("hostServLimitTitle"), message("hostServLimitErr"));
                    }
                }
            }
        };
    }

    private ItemListener createStorageAccountListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED && storageAccountCmb.getSelectedItem() != null) {
                    currentStorageAccount = ((ElementWrapper<StorageAccount>) storageAccountCmb.getSelectedItem()).getValue();
                }
//                setPageComplete(validatePageComplete());
            }
        };
    }

    private ItemListener createHostedServiceComboListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ElementWrapper<CloudService> selectedItem = (ElementWrapper<CloudService>) hostedServiceCombo.getSelectedItem();
                    currentHostedService = selectedItem == null ? null : selectedItem.getValue();
//                setPageComplete(validatePageComplete());
                }
            }
        };
    }

    private void populateTargetOs() {
        List<String> osNames = new ArrayList<String>();
        for (OSFamilyType osType : OSFamilyType.values()) {
            osNames.add(osType.getName());
        }
        targetOS.setModel(new DefaultComboBoxModel(osNames.toArray(new String[osNames.size()])));
    }

    private ItemListener createSubscriptionComboListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                publishData = UIUtils.changeCurrentSubAsPerCombo((JComboBox) e.getSource());
                if (storageAccountCmb != null && publishData != null) {
                    populateStorageAccounts();
                    populateHostedServices();
                    setComponentState((subscriptionCombo.getSelectedItem() != null));
                }
//                setPageComplete(validatePageComplete());
            }
        };
    }

    private ActionListener createImportSubscriptionAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImportSubscriptionForm isf = new ImportSubscriptionForm(myModule.getProject());
                isf.setOnSubscriptionLoaded(new Runnable() {
                    @Override
                    public void run() {
                        /*
             * logic to set un-pubilsh check box to true
			 * when ever importing publish settings
			 * file for the first time.
			 */
                        if (subscriptionCombo.getItemCount() == 0) {
                            unpublishChBox.setSelected(true);
                        }
                        UIUtils.populateSubscriptionCombo(subscriptionCombo);
//
//            int selection = 0;
//            selection = findSelectionIndex(publishDataToCache);
//
//            subscriptionCombo.select(selection);
//            WizardCacheManager.setCurrentPublishData(publishDataToCache);
//
                        setComponentState((subscriptionCombo.getSelectedItem() != null));
//            // Make centralized storage registry.
//            MethodUtils.prepareListFromPublishData();
                    }
                });
                isf.show();
            }
        };
//        return new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                final ImportSubscriptionDialog importSubscriptionDialog = new ImportSubscriptionDialog();
//                importSubscriptionDialog.show();
//                if (importSubscriptionDialog.isOK()) {
//                    importBtn(importSubscriptionDialog.getPublishSettingsPath());
//
//                }
//            }
//        };
    }

    private ItemListener createDeployStateCmbListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                String deployState = (String) ((JComboBox) e.getSource()).getSelectedItem();
                if (deployState.equalsIgnoreCase(message("deplProd"))) {
                    unpublishChBox.setSelected(false);
                }
//                setPageComplete(validatePageComplete());
            }
        };
    }

//    private void importBtn(String fileName) {
//        if (fileName != null && !fileName.isEmpty()) {
//            File file = new File(fileName);
//            PublishData publishDataToCache = MethodUtils.handlePublishSettings(file, myModule.getProject());
//            if (publishDataToCache == null) {
//                return;
//            }
//            /*
//             * logic to set un-pubilsh check box to true
//			 * when ever importing publish settings
//			 * file for the first time.
//			 */
//            if (subscriptionCombo.getItemCount() == 0) {
//                unpublishChBox.setSelected(true);
//            }
//            UIUtils.populateSubscriptionCombo(subscriptionCombo);
////
////            int selection = 0;
////            selection = findSelectionIndex(publishDataToCache);
////
////            subscriptionCombo.select(selection);
////            WizardCacheManager.setCurrentPublishData(publishDataToCache);
////
//            setComponentState((subscriptionCombo.getSelectedItem() != null));
////            // Make centralized storage registry.
////            MethodUtils.prepareListFromPublishData();
//        }
//    }

    /**
     * Method loads configured remote access values
     * on wizard page.
     */
    private void loadDefaultRDPValues() {
        try {
            // to update project manager object
            loadProject();
            String uname = waProjManager.getRemoteAccessUsername();
            if (uname != null && !uname.isEmpty()) {
                userName.setText(uname);
                try {
                    String pwd = waProjManager.getRemoteAccessEncryptedPassword();
                    /*
					 * If its dummy password,
					 * then do not show it on UI
					 */
                    if (pwd.equals(message("remAccDummyPwd")) || pwd.isEmpty()) {
                        userPassword.setText("");
                        confirmPassword.setText("");
                    } else {
                        userPassword.setText(pwd);
                        confirmPassword.setText(pwd);
                    }
                    setEnableRemAccess(true);
                } catch (Exception e) {
                    userPassword.setText("");
                    confirmPassword.setText("");
                }
            } else {
                userName.setText("");
                setEnableRemAccess(false);
            }
        } catch (Exception e) {
            userName.setText("");
            setEnableRemAccess(false);
        }
        /*
		 * Non windows OS then disable components,
		 * but keep values as it is
		 */
        if (!AzurePlugin.IS_WINDOWS) {
            userName.setEnabled(false);
            userPassword.setEnabled(false);
            confirmPassword.setEnabled(false);
            userPasswordLbl.setEnabled(false);
            confirmPasswordLbl.setEnabled(false);
            conToDplyChkBtn.setEnabled(false);
        }
    }

    private void loadDefaultWizardValues() {
        try {
            loadProject();
            // Get global properties from package.xml
            String subId = waProjManager.getPublishSubscriptionId();
            String cloudServiceName = waProjManager.getPublishCloudServiceName();
            String storageAccName = waProjManager.getPublishStorageAccountName();

            if (subId != null && !subId.isEmpty()) {
                String subName = WizardCacheManager.findSubscriptionNameBySubscriptionId(subId);
                if (subName != null && !subName.isEmpty()) {
                    UIUtils.selectByText(subscriptionCombo, subName);
                    publishData = UIUtils.changeCurrentSubAsPerCombo(subscriptionCombo);
                    if (publishData != null) {
                        populateStorageAccounts();
                        populateHostedServices();
                        setComponentState(subscriptionCombo.getSelectedItem() != null);
                        UIUtils.selectByText(hostedServiceCombo, cloudServiceName);
                        UIUtils.selectByText(storageAccountCmb, storageAccName);
                    }
                }
            }
            try {
                String deploymentSlot = waProjManager.getPublishDeploymentSlot().toString();
                if (deploymentSlot != null && !deploymentSlot.isEmpty()) {
                    UIUtils.selectByText(deployStateCmb, deploymentSlot);
                }
            } catch (Exception e) {
                // ignore.
                // Mostly it would be IllegalArgumentException if valid deployment string not specified
            }
            try {
                boolean overwriteDeployment = waProjManager.getPublishOverwritePreviousDeployment();
                unpublishChBox.setSelected(overwriteDeployment);
            } catch (Exception e) {
                // ignore
            }
        } catch (Exception e) {
            log(message("error"), e);
        }
    }

    /**
     * Enable or disable password fields.
     *
     * @param status
     */
    private void setEnableRemAccess(boolean status) {
        userPassword.setEnabled(status);
        confirmPassword.setEnabled(status);
        userPasswordLbl.setEnabled(status);
        confirmPasswordLbl.setEnabled(status);
        conToDplyChkBtn.setEnabled(status);
        if (!status) {
            userPassword.setText("");
            confirmPassword.setText("");
            conToDplyChkBtn.setSelected(false);
        }
    }

    /**
     * Enable or disable components related to
     * publish settings.
     *
     * @param enabled
     */
    private void setComponentState(boolean enabled) {
        subscriptionCombo.setEnabled(enabled);
        storageAccountCmb.setEnabled(enabled);
        newStorageAccountBtn.setEnabled(enabled);
        hostedServiceCombo.setEnabled(enabled);
        targetOS.setEnabled(enabled);
        if (!enabled) {
            hostedServiceCombo.removeAllItems();
            storageAccountCmb.removeAllItems();
        }
        deployStateCmb.setEnabled(enabled);
        newHostedServiceBtn.setEnabled(enabled);
        unpublishChBox.setEnabled(enabled);
    }

    protected void populateStorageAccounts() {
        if (publishData != null) {
            Object currentSelection = storageAccountCmb.getSelectedItem();
            Subscription currentSubscription = publishData.getCurrentSubscription();
            List<StorageAccount> storageServices = publishData.getStoragesPerSubscription().get(currentSubscription.getId());
            storageAccountCmb.removeAllItems();
            if (storageServices != null && !storageServices.isEmpty()) {
                for (StorageAccount storageService : storageServices) {
                    storageAccountCmb.addItem(new ElementWrapper<StorageAccount>(storageService.getName(), storageService));
                }
            }
            if (currentSelection != null) {
                storageAccountCmb.setSelectedItem(currentSelection);
            }
        }
    }

    public void populateHostedServices() {
        if (publishData != null) {
            Object currentSelection = hostedServiceCombo.getSelectedItem();
            Subscription currentSubscription = publishData.getCurrentSubscription();
            java.util.List<CloudService> cloudServices = publishData.getServicesPerSubscription().get(currentSubscription.getId());
            hostedServiceCombo.removeAllItems();
            if (cloudServices != null && !cloudServices.isEmpty()) {
                for (CloudService cs : cloudServices) {
                    hostedServiceCombo.addItem(new ElementWrapper<CloudService>(cs.getName(), cs));
                }
                if (currentSelection != null) {
                    hostedServiceCombo.setSelectedItem(currentSelection);
                }
            }
        }
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("deplWizTitle"), "");
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(600, 400);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (publishData == null) {
            return new ValidationInfo(message("deplFillSubsciptionId"), subscriptionCombo);
        }
        if (currentStorageAccount == null) {
            return new ValidationInfo(message("deplFillStorageAcc"), storageAccountCmb);
        }
        if (currentHostedService == null) {
            return new ValidationInfo(message("deplFillHostedServiceMsg"), hostedServiceCombo);
        }
		/*
		 * Validation for remote access settings.
		 */
        if (!userName.getText().isEmpty()) {
            char[] pwd = userPassword.getPassword();
            if (pwd == null || pwd.length == 0) {
                // password is empty
                return new ValidationInfo(message("rdpPasswordEmpty"), userPassword);
            } else {
                char[] confirm = confirmPassword.getPassword();
                if (confirm == null || confirm.length == 0) {
                    // confirm password is empty
                    return new ValidationInfo(message("rdpConfirmPasswordEmpty"), confirmPassword);
                } else {
                    if (!Arrays.equals(pwd, confirm)) {
                        // password and confirm password do not match.
                        return new ValidationInfo(message("rdpPasswordsDontMatch"), confirmPassword);
                    }
                }
            }
        }
        return null;
    }

    private void handlePageComplete() {
        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.DEPLOY_MODE, deployMode));

        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.SUBSCRIPTION, publishData));

        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.CONFIG_HTTPS_LINK, waProjManager.getSSLInfoIfUnique() != null ? "true" : "false"));


        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.STORAGE_ACCOUNT,
                currentStorageAccount));
        // Always set key to primary
        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.STORAGE_ACCESS_KEY,
                KeyName.Primary.toString()));

        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.DEPLOY_STATE,
                deployStateCmb.getSelectedItem()));

        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.UN_PUBLISH,
                unpublishChBox.isSelected()));

        deployFileName = constructDeployFilePath(message("cspckDefaultFileName"));

        deployConfigFileName = constructDeployFilePath(message("cscfgDefaultFileName"));

        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.DEPLOY_FILE,
                deployFileName));

        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.DEPLOY_CONFIG_FILE,
                deployConfigFileName));

        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.HOSTED_SERVICE,
                currentHostedService));
    }

    private String constructDeployFilePath(String fileName) {
        String moduleLocation = PluginUtil.getModulePath(myModule);
        return moduleLocation + File.separator + message("deployDir") + File.separator + fileName;
    }

    /**
     * Method returns new services names, if created by user.
     *
     * @return
     */
    public ArrayList<String> getNewServices() {
        return newServices;
    }

    public String getTargetOSName() {
        return (String) targetOS.getSelectedItem();
    }

    public String getRdpUname() {
        return userName.getText();
    }

    public String getRdpPwd() {
        return new String(userPassword.getPassword());
    }

    public boolean getConToDplyChkStatus() {
        return conToDplyChkBtn.isSelected();
    }

    @Override
    public String getHelpId() {
        return "publish_project_command";
    }
}

