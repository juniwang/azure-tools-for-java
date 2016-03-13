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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.AnActionButtonUpdater;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.table.ComboBoxTableCellEditor;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureLocalStorage;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoft.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class LocalStoragePanel extends BaseConfigurable implements SearchableConfigurable, Configurable.NoScroll {
    private enum RecycleType {
        CLEAN(message("lclStgClean")), PRESERVE(message("lclStgPsv"));

        private String name;

        RecycleType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private JPanel contentPane;
    private JPanel tablePanel;
    private TableView<WindowsAzureLocalStorage> tblResources;

    private Module module;
    private WindowsAzureProjectManager waProjManager;
    private WindowsAzureRole waRole;
    private Map<String, WindowsAzureLocalStorage> mapLclStg;

    public LocalStoragePanel(Module module, WindowsAzureProjectManager waProjManager, WindowsAzureRole waRole) {
        this.module = module;
        this.waProjManager = waProjManager;
        this.waRole = waRole;
        try {
            mapLclStg = waRole.getLocalStorage();
            LocalStorageTableModel myModel = new LocalStorageTableModel(new ArrayList<WindowsAzureLocalStorage>(mapLclStg.values()));
            tblResources.setModelAndUpdateColumns(myModel);
            tblResources.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tblResources.setRowHeight(ComboBoxTableCellEditor.INSTANCE.getComponent().getPreferredSize().height);
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("lclStgSetErrTtl"), message("lclStgSetErrMsg"), e);
        }
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
        return message("cmhLblLclStg");
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "windows_azure_localstorage_page";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return contentPane;
    }

    @Override
    public void apply() throws ConfigurationException {
        boolean okToProceed = true;
        try {
            waProjManager.save();
            LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(module)).refresh(true, true);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
            okToProceed = false;
        }
        if (okToProceed) {
            setModified(false);
        }
    }

    @Override
    public void reset() {
        setModified(false);
    }

    @Override
    public void disposeUIResources() {
    }

    private final ColumnInfo<WindowsAzureLocalStorage, String> NAME = new ColumnInfo<WindowsAzureLocalStorage, String>(message("lclStgRname")) {
        public String valueOf(WindowsAzureLocalStorage object) {
            return object.getName();
        }

        @Override
        public void setValue(WindowsAzureLocalStorage entry, String modifiedVal) {
            try {
                String name = modifiedVal.trim();
                entry.setName(name);
                setModified(true);
            } catch (Exception e) {
                PluginUtil.displayErrorDialogAndLog(message("certErrTtl"), message("certErrMsg"), e);
            }
        }
    };

    private final ColumnInfo<WindowsAzureLocalStorage, String> SIZE = new ColumnInfo<WindowsAzureLocalStorage, String>(message("lclStgSize")) {
        public String valueOf(WindowsAzureLocalStorage object) {
            return String.valueOf(object.getSize());
        }

        @Override
        public void setValue(WindowsAzureLocalStorage entry, String modifiedVal) {
            try {
                entry.setSize(Integer.valueOf(modifiedVal));
                setModified(true);
            } catch (Exception e) {
                PluginUtil.displayErrorDialogAndLog(message("certErrTtl"), message("certErrMsg"), e);
            }
        }
    };

    private final ColumnInfo<WindowsAzureLocalStorage, RecycleType> RECYCLE = new ColumnInfo<WindowsAzureLocalStorage, RecycleType>(message("lclStgRcl")) {
        public RecycleType valueOf(WindowsAzureLocalStorage object) {
            if (object.getCleanOnRecycle()) {
                return RecycleType.CLEAN;
            } else {
                return RecycleType.PRESERVE;
            }
        }

        @Override
        public TableCellEditor getEditor(final WindowsAzureLocalStorage localStorage) {
            return ComboBoxTableCellEditor.INSTANCE;
        }

        @Override
        public void setValue(WindowsAzureLocalStorage entry, RecycleType modifiedVal) {
            try {
                entry.setCleanOnRecycle(modifiedVal == RecycleType.CLEAN);
                setModified(true);
            } catch (Exception e) {
                PluginUtil.displayErrorDialogAndLog(message("certErrTtl"), message("certErrMsg"), e);
            }
        }
    };

    private final ColumnInfo<WindowsAzureLocalStorage, String> PATH = new ColumnInfo<WindowsAzureLocalStorage, String>(message("lclStgPath")) {
        public String valueOf(WindowsAzureLocalStorage object) {
            String result = null;
            try {
                result = object.getPathEnv();
            } catch (WindowsAzureInvalidProjectOperationException e) {
                PluginUtil.displayErrorDialogAndLog(message("lclStgSetErrTtl"), message("lclStgSetErrMsg"), e);
            }
            return result;
        }

        @Override
        public void setValue(WindowsAzureLocalStorage entry, String modifiedVal) {
            try {
                String name = modifiedVal.trim();
                entry.setName(name);
                setModified(true);
            } catch (Exception e) {
                PluginUtil.displayErrorDialogAndLog(message("certErrTtl"), message("certErrMsg"), e);
            }
        }
    };

    /**
     * Listener method for add button which opens a dialog
     * to add a local storage resource.
     */
    protected void addBtnListener() {
        LocalStorageResourceDialog dialog = new LocalStorageResourceDialog(waRole, mapLclStg);
        dialog.show();
        if (dialog.isOK()) {
            setModified(true);
            String name = dialog.getResName();
            tblResources.getListTableModel().addRow(mapLclStg.get(name));
            List<WindowsAzureLocalStorage> items = tblResources.getItems();
            for (int i = 0; i < items.size(); i++) {
                WindowsAzureLocalStorage localStorage = items.get(i);
                if (localStorage.getName().equalsIgnoreCase(name)) {
                    tblResources.addSelection(localStorage);
                    break;
                }
            }
        }
    }

    /**
     * Listener for edit button, which launches a dialog to edit
     * a local storage resource.
     */
    @SuppressWarnings("unchecked")
    protected void editBtnListener() {
        try {
            WindowsAzureLocalStorage localStorage = tblResources.getSelectedObject();
            /*
             * Check local storage selected for modification
    		 * is associated with caching then give error
    		 * and does not allow to edit.
    		 */
            if (localStorage.isCachingLocalStorage()) {
                PluginUtil.displayErrorDialog(message("cachDsblErTtl"), message("lclStrEdtErMsg"));
            } else {
                LocalStorageResourceDialog dialog = new LocalStorageResourceDialog(waRole, mapLclStg, localStorage.getName());
                dialog.show();
                if (dialog.isOK()) {
                    ((LocalStorageTableModel) tblResources.getModel()).fireTableDataChanged();
                    setModified(true);
                }
            }
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("lclStgSetErrTtl"), message("lclStgSetErrMsg"), e);
        }
    }

    /**
     * Listener method for remove button which
     * deletes the selected local storage resource.
     */
    @SuppressWarnings("unchecked")
    protected void removeBtnListener() {
        try {
            WindowsAzureLocalStorage delRes = tblResources.getSelectedObject();
            /*
             * Check local storage selected for removal
    		 * is associated with caching then give error
    		 * and does not allow to remove.
    		 */
            if (delRes.isCachingLocalStorage()) {
                PluginUtil.displayErrorDialog(message("cachDsblErTtl"), message("lclStrRmvErMsg"));
            } else {
                int choice = Messages.showYesNoDialog(message("lclStgRmvMsg"), message("lclStgRmvTtl"), Messages.getQuestionIcon());
                if (choice == Messages.YES) {
                    delRes.delete();
                    setModified(true);
                    tblResources.getListTableModel().removeRow(tblResources.getSelectedRow());
                }
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("lclStgSetErrTtl"), message("lclStgSetErrMsg"), e);
        }
    }

    private void createUIComponents() {
        tblResources = new TableView<WindowsAzureLocalStorage>();
        tablePanel = ToolbarDecorator.createDecorator(tblResources, null)
                .setAddAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        addBtnListener();
                    }
                }).setEditAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton anActionButton) {
                        editBtnListener();
                    }
                }).setRemoveAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        removeBtnListener();
                    }
                }).setEditActionUpdater(new AnActionButtonUpdater() {
                    @Override
                    public boolean isEnabled(AnActionEvent e) {
                        return tblResources.getSelectedObject() != null;
                    }
                }).setRemoveActionUpdater(new AnActionButtonUpdater() {
                    @Override
                    public boolean isEnabled(AnActionEvent e) {
                        return tblResources.getSelectedObject() != null;
                    }
                }).disableUpDownActions().createPanel();
    }

    private class LocalStorageTableModel extends ListTableModel<WindowsAzureLocalStorage> {
        private LocalStorageTableModel(List<WindowsAzureLocalStorage> localStorages) {
            super(new ColumnInfo[]{NAME, SIZE, RECYCLE, PATH}, localStorages);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }
    }
}
