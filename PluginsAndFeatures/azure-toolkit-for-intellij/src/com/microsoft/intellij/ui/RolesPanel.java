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
import com.intellij.openapi.options.ConfigurableGroup;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoft.intellij.ui.azureroles.RoleConfigurablesGroup;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.util.WAHelper;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class RolesPanel implements AzureAbstractPanel {
    private JPanel contentPane;

    private JTable tblRoles;
    private JButton btnAddRole;
    private JButton btnEditRole;
    private JButton btnRemoveRole;

    private Module myModule;
    private WindowsAzureProjectManager waProjManager;
    private List<WindowsAzureRole> listRoles;

    public RolesPanel(Module myModule) {
        this.myModule = myModule;
        loadProject();
        init();
    }

    protected void init() {
        tblRoles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblRoles.setModel(new RolesTableModel());
        for (int i = 0; i < tblRoles.getColumnModel().getColumnCount(); i++) {
            TableColumn each = tblRoles.getColumnModel().getColumn(i);
            each.setPreferredWidth(((RolesTableModel) tblRoles.getModel()).getColumnWidth(i, tblRoles.getPreferredScrollableViewportSize().width));
        }
        btnAddRole.addActionListener(createBtnAddListener());
        btnEditRole.addActionListener(createBtnEditListener());
        btnRemoveRole.addActionListener(createBtnRemoveListener());
        tblRoles.getSelectionModel().addListSelectionListener(createRolesTableListener());
    }

    private ActionListener createBtnAddListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    loadProject();
                    WindowsAzureRole windowsAzureRole = WAHelper.prepareRoleToAdd(waProjManager);
                    /*
                     * Check whether user has pressed OK or Cancel button.
                     * If OK : Refresh roles table so that newly added role is visible
                     * else CANCEL : remove added role from list of roles.
                     */
                    if (windowsAzureRole != null) {
                        RoleConfigurablesGroup group = new RoleConfigurablesGroup(myModule, waProjManager, windowsAzureRole, true);
                        ShowSettingsUtil.getInstance().showSettingsDialog(myModule.getProject(),
                                new ConfigurableGroup[]{group});
                        if (group.isModified()) { // Cancel was clicked, so changes should be reverted
                            listRoles.remove(windowsAzureRole);
                        }
                    }
                    ((RolesTableModel) tblRoles.getModel()).fireTableDataChanged();
                    LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(myModule)).refresh(true, true);
                } catch (Exception ex) {
                    PluginUtil.displayErrorDialogAndLog(message("rolsDlgErr"), message("rolsDlgErrMsg"), ex);
                }
            }
        };
    }

    private ActionListener createBtnEditListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selIndex = tblRoles.getSelectedRow();
                if (selIndex > -1) {
                    try {
                        loadProject();
                        WindowsAzureRole windowsAzureRole = listRoles.get(tblRoles.getSelectedRow());
                        ShowSettingsUtil.getInstance().showSettingsDialog(myModule.getProject(),
                                new ConfigurableGroup[]{new RoleConfigurablesGroup(myModule, waProjManager, windowsAzureRole, false)});
                        ((RolesTableModel) tblRoles.getModel()).fireTableDataChanged();
                        LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(myModule)).refresh(true, true);
                    } catch (Exception ex) {
                        PluginUtil.displayErrorDialogAndLog(message("rolsDlgErr"), message("rolsDlgErrMsg"), ex);
                    }
                }
            }
        };
    }

    /**
     * This method loads the project data.
     */
    private void loadProject() {
        File projDirPath = new File(PluginUtil.getModulePath(myModule));
        try {
            waProjManager = WindowsAzureProjectManager.load(projDirPath);
            listRoles = waProjManager.getRoles();
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("rolsErr"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
        }
    }

    private ActionListener createBtnRemoveListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int selIndex = tblRoles.getSelectedRow();
                    if (selIndex > -1) {
                        int choice = Messages.showYesNoDialog(myModule.getProject(), message("rolsDelMsg"), message("rolsDelTitle"), Messages.getQuestionIcon());
                        if (choice == Messages.YES) {
                            /*
                             * If the role selected for deletion is the last role,
                	         * then do not delete it and give error message.
                	        */
                            if (listRoles.size() == 1) {
                                PluginUtil.displayErrorDialog(message("rolsDelTitle"), message("lastRolDelMsg"));
                            } else {
                                WindowsAzureRole windowsAzureRole = listRoles.get(selIndex);
                                windowsAzureRole.delete();
                                waProjManager.save();
                                ((RolesTableModel) tblRoles.getModel()).fireTableDataChanged();
                            }
                        }
                    }
                } catch (WindowsAzureInvalidProjectOperationException ex) {
                    PluginUtil.displayErrorDialogAndLog(message("rolsErr"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
                }
            }
        };
    }

    private ListSelectionListener createRolesTableListener() {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                boolean buttonsEnabled = tblRoles.getSelectedRow() > -1;
                btnEditRole.setEnabled(buttonsEnabled);
                btnRemoveRole.setEnabled(buttonsEnabled);
            }
        };
    }

    @Override
    public JComponent getPanel() {
        return contentPane;
    }

    @Override
    public String getDisplayName() {
        return message("cmhLblRoles");
    }

    @Override
    public boolean doOKAction() {
        return true;
    }

    @Override
    public String getSelectedValue() {
        return null;
    }

    @Override
    public ValidationInfo doValidate() {
        return null;
    }

    public String getHelpTopic() {
        return "windows_azure_project_roles";
    }

    private class RolesTableModel extends AbstractTableModel {
        public final String[] COLUMNS = new String[]{message("rolsName"), message("rolsVMSize"), message("rolsInstances")};

        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        public int getColumnWidth(int column, int totalWidth) {
            switch (column) {
                case 0:
                    return (int) (totalWidth * 0.5);
                default:
                    return (int) (totalWidth * 0.25);
            }
        }

        public int getRowCount() {
            return listRoles.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            WindowsAzureRole winAzureRole = listRoles.get(rowIndex);
            String result = "";
            try {
                switch (columnIndex) {
                    case 0:
                        result = winAzureRole.getName();
                        break;
                    case 1:
                        result = winAzureRole.getVMSize();
                        break;
                    case 2:
                        result = winAzureRole.getInstances();
                        break;
                    default:
                        break;
                }
            } catch (WindowsAzureInvalidProjectOperationException e) {
                //display error message if any exception occurs while
                //reading role data
                PluginUtil.displayErrorDialogAndLog(message("rolsErr"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
            }
            return result;
        }
    }
}
