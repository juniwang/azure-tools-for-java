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
package com.microsoft.intellij.docker.forms;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.intellij.docker.dialogs.AzureSelectKeyVault;
import com.microsoft.intellij.docker.utils.AzureDockerValidationUtils;
import com.microsoft.intellij.ui.util.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AzureDockerHostUpdateLoginPanel {
  private JPanel contentPane;
  private JPanel mainPanel;
  private JButton copyFromAzureKeyButton;
  public JTextField dockerHostUsernameTextField;
  public JLabel dockerHostUsernameLabel;
  public JPasswordField dockerHostFirstPwdField;
  public JLabel dockerHostFirstPwdLabel;
  public JPasswordField dockerHostSecondPwdField;
  public JRadioButton dockerHostKeepSshRadioButton;
  public JRadioButton dockerHostAutoSshRadioButton;
  public JRadioButton dockerHostImportSshRadioButton;
  public TextFieldWithBrowseButton dockerHostImportSSHBrowseTextField;
  public JLabel dockerHostImportSSHBrowseLabel;
  private ButtonGroup authSelectionGroup;


  private Project project;
  private EditableDockerHost editableHost;
  private AzureDockerHostsManager dockerManager;

  public AzureDockerHostUpdateLoginPanel(Project project, EditableDockerHost editableHost, AzureDockerHostsManager dockerUIManager) {
    this.project = project;
    this.editableHost = editableHost;
    this.dockerManager = dockerUIManager;

    authSelectionGroup = new ButtonGroup();
    authSelectionGroup.add(dockerHostKeepSshRadioButton);
    authSelectionGroup.add(dockerHostAutoSshRadioButton);
    authSelectionGroup.add(dockerHostImportSshRadioButton);

    initDefaultUI();

    copyFromAzureKeyButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        AzureSelectKeyVault selectKeyvaultDialog = new AzureSelectKeyVault(project, dockerUIManager);
        selectKeyvaultDialog.show();

        if (selectKeyvaultDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
          updateUIWithKeyvault(selectKeyvaultDialog.getSelectedKeyvault());
        }
      }
    });

    dockerHostUsernameLabel.setVisible(editableHost.originalDockerHost.certVault == null || editableHost.originalDockerHost.certVault.vmUsername == null);
    dockerHostUsernameTextField.setText((editableHost.originalDockerHost.certVault != null && editableHost.originalDockerHost.certVault.vmUsername != null) ?
        editableHost.originalDockerHost.certVault.vmUsername : "");
    dockerHostUsernameTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostUserNameTip());
    dockerHostUsernameTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = dockerHostUsernameTextField.getText();
        if (text == null || text.isEmpty() || !AzureDockerValidationUtils.validateDockerHostUserName(text)) {
          dockerHostUsernameLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        } else {
          dockerHostUsernameLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        }
      }
    });
    dockerHostUsernameTextField.getDocument().addDocumentListener(resetDialogButtonsState(null));
    dockerHostFirstPwdField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = new String(dockerHostFirstPwdField.getPassword());
        if (dockerHostFirstPwdField.getPassword().length > 0 && !text.isEmpty() && !AzureDockerValidationUtils.validateDockerHostPassword(text)) {
          dockerHostFirstPwdLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        } else {
          dockerHostFirstPwdLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        }
      }
    });
    dockerHostFirstPwdField.getDocument().addDocumentListener(resetDialogButtonsState(null));
    dockerHostFirstPwdLabel.setVisible(false);
    dockerHostFirstPwdField.setToolTipText(AzureDockerValidationUtils.getDockerHostPasswordTip());
    dockerHostSecondPwdField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String pwd1 = new String(dockerHostFirstPwdField.getPassword());
        String pwd2 = new String(dockerHostSecondPwdField.getPassword());
        if (dockerHostSecondPwdField.getPassword().length > 0 && !pwd2.isEmpty() && !pwd2.equals(pwd1)) {
          dockerHostFirstPwdLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        } else {
          dockerHostFirstPwdLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        }
      }
    });
    dockerHostSecondPwdField.getDocument().addDocumentListener(resetDialogButtonsState(null));
    dockerHostSecondPwdField.setToolTipText(AzureDockerValidationUtils.getDockerHostPasswordTip());

    dockerHostKeepSshRadioButton.setText(editableHost.originalDockerHost.hasSSHLogIn ? "Use current keys" : "None");
    dockerHostKeepSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportSshRadioButton.setEnabled(false);
      }
    });
    dockerHostAutoSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportSshRadioButton.setEnabled(false);
      }
    });
    dockerHostImportSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportSshRadioButton.setEnabled(true);
      }
    });

    dockerHostImportSSHBrowseTextField.addActionListener(UIUtils.createFileChooserListener(dockerHostImportSSHBrowseTextField, project,
        FileChooserDescriptorFactory.createSingleFolderDescriptor()));
    dockerHostImportSSHBrowseTextField.getTextField().setToolTipText(AzureDockerValidationUtils.getDockerHostSshDirectoryTip());
    dockerHostImportSSHBrowseTextField.getTextField().setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = dockerHostImportSSHBrowseTextField.getText();
        if (text == null || text.isEmpty() || !AzureDockerValidationUtils.validateDockerHostSshDirectory(text)) {
          dockerHostImportSSHBrowseLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        } else {
          dockerHostImportSSHBrowseLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        }
      }
    });


  }

  private void initDefaultUI() {
    String currentUserAuth = editableHost.originalDockerHost.certVault.vmUsername;
    if (editableHost.originalDockerHost.hasPwdLogIn) {
      dockerHostFirstPwdField.setText(editableHost.originalDockerHost.certVault.vmPwd);
      dockerHostSecondPwdField.setText(editableHost.originalDockerHost.certVault.vmPwd);
    }
    if (editableHost.originalDockerHost.hasSSHLogIn) {
      dockerHostKeepSshRadioButton.setSelected(true);
    } else {
      dockerHostAutoSshRadioButton.setSelected(true);
    }
    if (editableHost.isUpdated) {
      currentUserAuth += " (updating...)";
      dockerHostFirstPwdField.setEnabled(false);
      dockerHostSecondPwdField.setEnabled(false);
      dockerHostUsernameTextField.setEnabled(false);
      dockerHostKeepSshRadioButton.setEnabled(false);
      dockerHostAutoSshRadioButton.setEnabled(false);
      dockerHostImportSshRadioButton.setEnabled(false);
    }
    dockerHostUsernameTextField.setText(currentUserAuth);
    dockerHostImportSSHBrowseTextField.setEnabled(false);
  }

  private void updateUIWithKeyvault(String keyvault) {
    // TODO: call into dockerManager to retrieve the keyvault secrets
  }

  public JPanel getMainPanel() {
    return mainPanel;
  }

  private void setDialogButtonsState(boolean buttonsState) {
  }

  public ValidationInfo doValidate(boolean shakeOnError) { return null;}

  public DocumentListener resetDialogButtonsState(JComponent componentLabel) {
    return new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        setDialogButtonsState(true);
        if (componentLabel != null) {
          componentLabel.setVisible(false);
        }
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        setDialogButtonsState(true);
        if (componentLabel != null) {
          componentLabel.setVisible(false);
        }
      }
    };
  }

}
