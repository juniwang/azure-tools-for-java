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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.util.MethodUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.azurecommons.preference.StorageAccPrefPageTableElement;
import com.microsoftopentechnologies.azurecommons.preference.StorageAccPrefPageTableElements;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccount;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageRegistryUtilMethods;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class StorageAccountPanel implements AzureAbstractConfigurablePanel {
    private static final String DISPLAY_NAME = "Storage Accounts";
    private JPanel contentPane;

    private JTable accountsTable;
    private JButton importButton;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;

    private Project myProject;

    public StorageAccountPanel(Project project) {
        this.myProject = project;
        init();
    }

    protected void init() {
        accountsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountsTable.setModel(new StorageAccountTableModel(getTableContent()));
        for (int i = 0; i < accountsTable.getColumnModel().getColumnCount(); i++) {
            TableColumn each = accountsTable.getColumnModel().getColumn(i);
            each.setPreferredWidth(StorageAccountTableModel.getColumnWidth(i, 450));
        }
        importButton.addActionListener(createImportSubscriptionAction());
        addButton.addActionListener(createAddButtonListener());
        editButton.addActionListener(createEditButtonListener());
        removeButton.addActionListener(createRemoveButtonListener());
        editButton.setEnabled(false);
        removeButton.setEnabled(false);
        accountsTable.getSelectionModel().addListSelectionListener(createAccountsTableListener());
        if (!AzureSettings.getSafeInstance(myProject).isSubscriptionLoaded()) {
            MethodUtils.loadSubInfoFirstTime(myProject);
            ((StorageAccountTableModel) accountsTable.getModel()).setAccounts(getTableContent());
            ((StorageAccountTableModel) accountsTable.getModel()).fireTableDataChanged();
        }
    }

    @Override
    public boolean doOKAction() {
        return true;
    }

    public ValidationInfo doValidate() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    public JComponent getPanel() {
        return contentPane;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    private ActionListener createImportSubscriptionAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final ImportSubscriptionDialog importSubscriptionDialog = new ImportSubscriptionDialog();
                importSubscriptionDialog.show();
                if (importSubscriptionDialog.isOK()) {
                    String fileName = importSubscriptionDialog.getPublishSettingsPath();
                    try {
                        AzureManager apiManager = AzureManagerImpl.getManager();
                        apiManager.clearAuthentication();
                        apiManager.importPublishSettingsFile(fileName);
                        MethodUtils.handleFile(fileName, myProject);
                    } catch (Exception ex) {
                        DefaultLoader.getUIHelper().showException("Error importing publish settings", ex, "Error", true, true);
                    }
                    ((StorageAccountTableModel) accountsTable.getModel()).setAccounts(getTableContent());
                    ((StorageAccountTableModel) accountsTable.getModel()).fireTableDataChanged();
                }
            }
        };
    }

    private ActionListener createAddButtonListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EditStorageAccountDialog editStorageAccountDialog = new EditStorageAccountDialog(null, myProject);
                editStorageAccountDialog.show();
                if (editStorageAccountDialog.isOK()) {
                    AzureSettings.getSafeInstance(myProject).saveStorage();
                    ((StorageAccountTableModel) accountsTable.getModel()).setAccounts(getTableContent());
                    ((StorageAccountTableModel) accountsTable.getModel()).fireTableDataChanged();
                }
            }
        };
    }

    private ActionListener createEditButtonListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = accountsTable.getSelectedRow();
                StorageAccount accToEdit = StorageAccountRegistry.getStrgList().get(index);
                EditStorageAccountDialog dlg = new EditStorageAccountDialog(accToEdit, myProject);
                dlg.show();
                if (dlg.isOK()) {
                    AzureSettings.getSafeInstance(myProject).saveStorage();
                }
            }
        };
    }

    private ActionListener createRemoveButtonListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int curSelIndex = accountsTable.getSelectedRow();
                if (curSelIndex > -1) {
                    int choice = Messages.showOkCancelDialog(message("accRmvMsg"), message("accRmvTtl"), Messages.getQuestionIcon());
                    if (choice == Messages.OK) {
                        StorageAccountRegistry.getStrgList().remove(curSelIndex);
                        AzureSettings.getSafeInstance(myProject).saveStorage();
                        ((StorageAccountTableModel) accountsTable.getModel()).setAccounts(getTableContent());
                        ((StorageAccountTableModel) accountsTable.getModel()).fireTableDataChanged();
                    }
                }
            }
        };
    }

    private ListSelectionListener createAccountsTableListener() {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                boolean buttonsEnabled = accountsTable.getSelectedRow() > -1;
                editButton.setEnabled(buttonsEnabled);
                removeButton.setEnabled(buttonsEnabled);
            }
        };
    }

    /**
     * Method prepares storage account list to show in table.
     */
    private List<StorageAccPrefPageTableElement> getTableContent() {
        // loads data from preference file.
        AzureSettings.getSafeInstance(myProject).loadStorage();
        List<StorageAccount> strgList = StorageAccountRegistry.getStrgList();
        List<StorageAccPrefPageTableElement> tableRowElements = new ArrayList<StorageAccPrefPageTableElement>();
        for (StorageAccount storageAcc : strgList) {
            if (storageAcc != null) {
                StorageAccPrefPageTableElement ele = new StorageAccPrefPageTableElement();
                ele.setStorageName(storageAcc.getStrgName());
                ele.setStorageUrl(storageAcc.getStrgUrl());
                tableRowElements.add(ele);
            }
        }
        StorageAccPrefPageTableElements elements = new StorageAccPrefPageTableElements();
        elements.setElements(tableRowElements);
        return elements.getElements();
    }

    public String getSelectedValue() {
        int selectedIndex = accountsTable.getSelectedRow();
        if (selectedIndex >= 0) {
            return ((StorageAccountTableModel) accountsTable.getModel()).getAccountNameAtIndex(accountsTable.getSelectedRow());
        }
        return null;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void reset() {
    }

    private static class StorageAccountTableModel extends AbstractTableModel {
        public static final String[] COLUMNS = new String[]{"Name", "Service Endpoint"};
        private java.util.List<StorageAccPrefPageTableElement> accounts;

        public StorageAccountTableModel(List<StorageAccPrefPageTableElement> accounts) {
            this.accounts = accounts;
        }

        public void setAccounts(List<StorageAccPrefPageTableElement> accounts) {
            this.accounts = accounts;
        }

        public String getAccountNameAtIndex(int index) {
            return accounts.get(index).getStorageName();
        }

        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        public static int getColumnWidth(int column, int totalWidth) {
            switch (column) {
                case 0:
                    return (int) (totalWidth * 0.4);
                default:
                    return (int) (totalWidth * 0.6);
            }
        }

        public int getRowCount() {
            return accounts.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            StorageAccPrefPageTableElement account = accounts.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return account.getStorageName();
                case 1:
                    return StorageRegistryUtilMethods.getServiceEndpoint(account.getStorageUrl());
            }
            return null;
        }
    }
}
