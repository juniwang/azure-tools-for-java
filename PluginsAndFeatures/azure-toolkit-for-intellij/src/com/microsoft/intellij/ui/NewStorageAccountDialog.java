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
import com.microsoft.intellij.runnable.NewStorageAccountWithProgressWindow;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.ui.util.UIUtils.ElementWrapper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.wizards.WizardCacheManager;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.Location;
import com.microsoft.windowsazure.management.storage.models.StorageAccountCreateParameters;
import com.microsoftopentechnologies.azurecommons.deploy.util.PublishData;
import com.microsoftopentechnologies.azuremanagementutil.model.Subscription;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class NewStorageAccountDialog extends DialogWrapper {
    private final static String STORAGE_ACCOUNT_NAME_PATTERN = "^[a-z0-9]+$";

    private JPanel contentPane;
    private JTextField storageAccountTxt;
    private JComboBox locationComb;
    private JTextField descriptionTxt;
    private JComboBox subscriptionCombo;

    private String defaultLocation;
    private String subscription;
    private StorageAccount storageService;

    private Project myProject;

    public NewStorageAccountDialog(String subscription, Project project) {
        super(true);
        setTitle(message("strgAcc"));
        this.subscription = subscription;
        this.myProject = project;
        init();
    }

    protected void init() {
        UIUtils.populateSubscriptionCombo(subscriptionCombo);
        /*
         * If subscription name is there,
		 * dialog invoked from publish wizard,
		 * hence disable subscription combo.
		 */
        if (subscription != null) {
            subscriptionCombo.setEnabled(false);
            UIUtils.selectByText(subscriptionCombo, subscription);
        }
        subscriptionCombo.addItemListener(createSubscriptionComboListener());
        populateLocations();
        super.init();
    }

    private ItemListener createSubscriptionComboListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                populateLocations();
            }
        };
    }

    private void populateLocations() {
        List<Location> items;
        ElementWrapper<PublishData> pd = (ElementWrapper<PublishData>) subscriptionCombo.getSelectedItem();
        if (pd != null) {
            Subscription sub = WizardCacheManager.findSubscriptionByName(pd.getKey());
            items = pd.getValue().getLocationsPerSubscription().get(sub.getId());
        } else {
            items = WizardCacheManager.getLocation();
        }
        locationComb.removeAllItems();

        for (Location location : items) {
            locationComb.addItem(location.getName());
        }
        /*
         * default location will exist if the user has
		 * created a storage account before creating the hosted service
		 */
        if (defaultLocation != null) {
            locationComb.setSelectedItem(defaultLocation);
        }
    }

    @Override
    protected void doOKAction() {
        String storageAccountNameToCreate = storageAccountTxt.getText();
        String storageAccountLocation = (String) locationComb.getSelectedItem();

        try {
            boolean isNameAvailable = WizardCacheManager.isStorageAccountNameAvailable(storageAccountNameToCreate, myProject);
            if (isNameAvailable) {
                /*
                 * case 1 : Invoked through publish wizard
				 * create mock and add account through publish process
				 */
                if (subscription != null) {
                    WizardCacheManager.createStorageServiceMock(storageAccountNameToCreate, storageAccountLocation, descriptionTxt.getText());
                } else {
					/*
                     * case 2 : Invoked through preference page
					 * Add account immediately.
					*/
                    PublishData pubData = UIUtils.changeCurrentSubAsPerCombo(subscriptionCombo);
                    PublishData publishData = WizardCacheManager.getCurrentPublishData();
                    Subscription curSub = publishData.getCurrentSubscription();
                    int maxStorageAccounts = curSub.getMaxStorageAccounts();

                    if (maxStorageAccounts > publishData.getStoragesPerSubscription().get(curSub.getId()).size()) {
                        NewStorageAccountWithProgressWindow task = new NewStorageAccountWithProgressWindow(pubData,
                                storageAccountTxt.getText(), storageAccountTxt.getText(), (String) locationComb.getSelectedItem(), descriptionTxt.getText(), myProject);
                        ProgressManager.getInstance().runProcessWithProgressSynchronously(task, "Progress Information", true, myProject);
                        storageService = task.getStorageService();
                    } else {
                        PluginUtil.displayErrorDialog(message("storageAccountsLimitTitle"), message("storageAccountsLimitErr"));
                        return;
                    }
                }
//                valid = true;
                super.doOKAction();
            } else {
                PluginUtil.displayErrorDialog(message("dnsCnf"), message("storageAccountConflictError"));
                storageAccountTxt.requestFocus();
                storageAccountTxt.selectAll();
            }
        } catch (final Exception e1) {
            PluginUtil.displayErrorDialogAndLog(message("error"), e1.getMessage(), e1);
        }
    }

    @Override
    protected boolean postponeValidation() {
        return false;
    }

    @Nullable
    protected ValidationInfo doValidate() {
        String host = storageAccountTxt.getText();
        String location = (String) locationComb.getSelectedItem();

        boolean legalName = validateStorageAccountName(host);
        if (host == null || host.isEmpty()) {
            return new ValidationInfo(message("storageAccountIsNullError"), storageAccountTxt);
        }
        if (!legalName) {
            return new ValidationInfo(message("wrongStorageName"), storageAccountTxt);
        }
        if (location == null || location.isEmpty()) {
            return new ValidationInfo(message("hostedLocNotSelectedError"), locationComb);
        }
//        setMessage(message("storageCreateNew"));
        return null;
    }

    private boolean validateStorageAccountName(String host) {
        if (host.length() < 3 || host.length() > 24) {
            return false;
        }
        return host.matches(STORAGE_ACCOUNT_NAME_PATTERN);
    }

    public void setDefaultLocation(String defaultLocation) {
        this.defaultLocation = defaultLocation;
    }

    public String getStorageAccountName() {
        return storageAccountTxt.getText();
    }

    public StorageAccount getStorageService() {
        return storageService;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("storageNew"), message("storageCreateNew"));
    }

    @Override
    public String getHelpId() {
        return "new_storage_account";
    }
}
