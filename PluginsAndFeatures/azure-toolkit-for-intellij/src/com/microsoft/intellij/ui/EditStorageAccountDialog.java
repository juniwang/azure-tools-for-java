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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TitlePanel;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.wizards.WizardCacheManager;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccount;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageRegistryUtilMethods;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.azurecommons.wacommonutil.PreferenceSetUtil;
import com.microsoftopentechnologies.azuremanagementutil.model.StorageService;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

import static com.microsoft.intellij.AzurePlugin.log;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

// todo: add focus listener to fields
public class EditStorageAccountDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField txtName;
    private JTextField txtKey;
    private JTextField txtUrl;
    private JButton newStrAccBtn;
    private boolean isManualUpdate = true; // need this to avoid circular updates by listeners

    private StorageAccount account;
    private boolean isEdit;
    private Project myProject;

    public EditStorageAccountDialog(StorageAccount accToEdit, Project project) {
        super(true);
        this.myProject = project;
        this.account = accToEdit;
        isEdit = (account != null);
        setTitle(isEdit ? message("addStrTtl") : message("edtStrTtl"));
        init();
    }

    protected void init() {
        DocumentListener emptyFieldListener = createEmptyFieldListener();
        txtName.getDocument().addDocumentListener(emptyFieldListener);
        txtKey.getDocument().addDocumentListener(emptyFieldListener);
        txtUrl.getDocument().addDocumentListener(emptyFieldListener);
        // edit
        if (!isEdit) {
            txtName.getDocument().addDocumentListener(createTxtNameListener());
            txtUrl.getDocument().addDocumentListener(createTxtUrlListener());
        }
        if (isEdit) {
            txtName.setEditable(false);
            txtUrl.setEditable(false);
            txtName.setText(account.getStrgName());
            txtKey.setText(account.getStrgKey());
            txtUrl.setText(account.getStrgUrl());
        } else {
            txtUrl.setText(constructURL(""));
        }
        /*
         * If any subscription is present then
		 * only enable New... button.
		 */
        newStrAccBtn.setEnabled(WizardCacheManager.getCurrentPublishData() != null);
        newStrAccBtn.addActionListener(createNewStrAccBtnListener());
        enableOkBtn();
        super.init();
    }

    private DocumentListener createTxtNameListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateUrl();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateUrl();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateUrl();
            }

            private void updateUrl() {
                if (isManualUpdate) {
                    String name = txtName.getText().trim();
                    String url = txtUrl.getText().trim();
                    isManualUpdate = false;
                    if (name.isEmpty() && url.isEmpty()) {
                        txtUrl.setText(constructURL(""));
                    } else {
                        syncUpAccNameAndNameInUrl(name, url);
                    }
                    isManualUpdate = true;
                }
            }
        };
    }

    private DocumentListener createTxtUrlListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateName();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateName();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateName();
            }

            private void updateName() {
                if (isManualUpdate) {
                    isManualUpdate = false;
                    String url = txtUrl.getText();
                    String nameInUrl = StorageRegistryUtilMethods.getAccNameFromUrl(url);
                    if (nameInUrl != null && !nameInUrl.equalsIgnoreCase(txtName.getText().trim())) {
                        txtName.setText(nameInUrl);
                    }
                    isManualUpdate = true;
                }
            }
        };
    }

    private DocumentListener createEmptyFieldListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                enableOkBtn();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                enableOkBtn();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                enableOkBtn();
            }
        };
    }

    private ActionListener createNewStrAccBtnListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewStorageAccountDialog newStorageAccountDialog = new NewStorageAccountDialog(null, myProject);
                newStorageAccountDialog.show();
                // populate data in storage registry dialog
                if (newStorageAccountDialog.isOK()) {
                    com.microsoft.tooling.msservices.model.storage.StorageAccount service = newStorageAccountDialog.getStorageService();
                    if (service != null) {
                        txtName.setText(service.getName());
                        txtKey.setText(service.getPrimaryKey());
                        txtUrl.setText(service.getBlobsUri());
                    }
                }
            }
        };
    }

    /**
     * Method is to update blob URL text
     * in order to keep storage account name and
     * storage account name's substring from URL
     * in sync.
     *
     * @param name - account name
     * @param url  - url
     */
    private void syncUpAccNameAndNameInUrl(String name, String url) {
        String nameInUrl = StorageRegistryUtilMethods.getAccNameFromUrl(url);
        if (nameInUrl != null && !name.equalsIgnoreCase(nameInUrl)) {
            String rplcNameInUrl = "//" + nameInUrl;
            String rplcName = "//" + name;
            txtUrl.setText(url.replaceFirst(rplcNameInUrl, rplcName));
        }
    }

    /**
     * Method enables or disables OK button.
     * Disable OK button if PFX path or password are empty.
     */
    private void enableOkBtn() {
        myOKAction.setEnabled(!(txtName.getText().trim().isEmpty()
                || txtKey.getText().trim().isEmpty()
                || txtUrl.getText().trim().isEmpty()));
    }

    @Nullable
    protected ValidationInfo doValidate() {
        // edit scenario.
        if (isEdit) {
            // check access key is changed, then edit else not.
            String newKey = txtKey.getText().trim();
            if (!account.getStrgKey().equals(newKey)) {
                if (newKey.contains(" ")) {
                    return new ValidationInfo(message("keyErrMsg"), txtKey);
                } else {
                    StorageAccountRegistry.editAccountAccessKey(account, newKey);
                }
            }
        } else {
            // add scenario.
            // validate account name
            ValidationInfo validationInfo = validateName();
            if (validationInfo != null) {
                return validationInfo;
            }
            // validate URL
            String name = txtName.getText().trim();
            try {
                String url = txtUrl.getText().trim();
                // append '/' if not present.
                if (!url.endsWith("/")) {
                    url = url + "/";
                }
                if (url.equalsIgnoreCase(name + message("blobEnPt"))) {
                    url = String.format("%s%s%s", message("http"), name, message("blobEnPt"));
                }
                new URL(url);
                if (url.startsWith(message("http") + name + '.') || url.startsWith(message("https") + name + '.')) {
                    // validate access key
                    String key = txtKey.getText().trim();
                    if (key.contains(" ")) {
                        return new ValidationInfo(message("keyErrMsg"), txtKey);
                    } else {
                        // check account did not exist previously
                        account = new StorageAccount(txtName.getText().trim(), key, url);
                        if (StorageAccountRegistry.getStrgList().contains(account)) {
                            return new ValidationInfo(message("urlPreErrMsg"));
                        }
                    }
                } else {
                    return new ValidationInfo(message("urlErMsg"), txtUrl);
                }
            } catch (MalformedURLException e) {
                return new ValidationInfo(message("urlErMsg"), txtUrl);
            }
        }
        return null;
    }

    /**
     * Method validates storage account name.
     */
    private ValidationInfo validateName() {
        String name = txtName.getText().trim();
        if (WAEclipseHelperMethods.isLowerCaseAndInteger(name)) {
            if (!(name.length() >= 3 && name.length() <= 24)) {
                return new ValidationInfo(message("namelnErMsg"), txtName);
            }
        } else {
            return new ValidationInfo(message("nameRxErMsg"), txtName);
        }
        return null;
    }

    /**
     * Method constructs URL as per preference sets file.
     *
     * @param storageName - storage account name
     * @return url
     */
    private String constructURL(String storageName) {
        String url = "";
        try {
            url = PreferenceSetUtil.getSelectedBlobServiceURL(storageName, AzurePlugin.prefFilePath);
        } catch (Exception e) {
            log(message("errTtl"), e);
        }
        return url;
    }

    @Override
    protected void doOKAction() {
        StorageAccountRegistry.addAccount(account);
        super.doOKAction();
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("strTxt"), message("strNmMsg"));
    }

    @Override
    protected String getHelpId() {
        return "storage_account_dialog";
    }
}
