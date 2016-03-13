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
package com.microsoft.intellij.ui.libraries;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.ui.AzureAbstractPanel;
import com.microsoft.intellij.ui.NewCertificateDialog;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.wacommon.commoncontrols.NewCertificateDialogData;
import com.microsoftopentechnologies.azurecommons.wacommonutil.CerPfxUtil;
import com.microsoftopentechnologies.azuremanagementutil.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

class LibraryPropertiesPanel implements AzureAbstractPanel {
    private static final int BUFF_SIZE = 1024;

    private JPanel rootPanel;
    private JCheckBox depCheck;
    private JPanel acsFilterPanel;
    private JTextField acsTxt;
    private JTextField relTxt;
    private TextFieldWithBrowseButton certTxt;
    private JButton newCertBtn;
    private JTextPane certInfoTxt;
    private JCheckBox embedCertCheck;
    private JCheckBox requiresHttpsCheck;
    private JLabel libraryVersion;
    private JLabel location;
    private AzureLibrary azureLibrary;
    private Module module;

    private boolean isEdit;

    public LibraryPropertiesPanel(Module module, AzureLibrary azureLibrary, boolean isEdit, boolean isExported) {
        this.module = module;
        this.azureLibrary = azureLibrary;
        this.isEdit = isEdit;
        init();
        depCheck.setSelected(isExported);
    }

    public void init() {
        acsTxt.setText(message("acsTxt"));
        certTxt.getTextField().getDocument().addDocumentListener(createCertTxtListener());
        Messages.configureMessagePaneUi(certInfoTxt, message("embedCertDefTxt"));
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
            @Override
            public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                return file.isDirectory() || (file.getExtension() != null && (file.getExtension().equals("cer") || file.getExtension().equals(".CER")));
            }

            @Override
            public boolean isFileSelectable(VirtualFile file) {
                return (file.getExtension() != null && (file.getExtension().equals("cer") || file.getExtension().equals(".CER")));
            }
        };
        fileChooserDescriptor.setTitle("Select Certificate");
        newCertBtn.addActionListener(createNewCertListener());
        certTxt.addActionListener(UIUtils.createFileChooserListener(certTxt, null, fileChooserDescriptor));
        requiresHttpsCheck.addActionListener(createRequiredHttpsCheckListener());

        if (isEdit()) {
            // Edit library scenario
            try {
//                com.intellij.psi.search.PsiShortNamesCache.getInstance(module.getProject()).getFilesByName("web.xml");
                ACSFilterHandler editHandler = new ACSFilterHandler(String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("xmlPath")));
                Map<String, String> paramMap = editHandler.getAcsFilterParams();
                acsTxt.setText(paramMap.get(message("acsAttr")));
                relTxt.setText(paramMap.get(message("relAttr")));
                if (paramMap.get(message("certAttr")) != null) {
                    certTxt.setText(paramMap.get(message("certAttr")));
                    certInfoTxt.setText(getCertInfo(certTxt.getText()));
                } else {
                    certInfoTxt.setText(getEmbeddedCertInfo());
                    embedCertCheck.setSelected(true);
                }

                requiresHttpsCheck.setSelected(!Boolean.valueOf(paramMap.get(message("allowHTTPAttr"))));
            } catch (Exception e) {
                AzurePlugin.log(e.getMessage(), e);
            }
        } else {
            // Add library scenario
            depCheck.setSelected(true);
        }
    }

    private ActionListener createNewCertListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewCertificateDialogData data = new NewCertificateDialogData();
                NewCertificateDialog dialog = new NewCertificateDialog(data, "", module.getProject());
                dialog.show();
                if (dialog.isOK()) {
                    String certPath = data.getCerFilePath();
                    certTxt.setText(certPath != null ? certPath.replace('\\', '/') : certPath);
                    certInfoTxt.setText(getCertInfo(certTxt.getText()));
                }
            }
        };
    }

    private DocumentListener createCertTxtListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleUpdate();
            }

            private void handleUpdate() {
                String certInfo = getCertInfo(certTxt.getText());
                if (certInfo != null)
                    certInfoTxt.setText(certInfo);
                else
                    certInfoTxt.setText("");
            }
        };
    }

    private ActionListener createRequiredHttpsCheckListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (requiresHttpsCheck.isSelected()) {
                    //Do nothing
                } else {
                    int choice = Messages.showYesNoDialog(message("requiresHttpsDlgMsg"), message("requiresHttpsDlgTitle"), Messages.getQuestionIcon());
                    if (choice == Messages.NO) {
                        requiresHttpsCheck.setSelected(true);
                    }
                }
            }
        };
    }

    public JComponent prepare() {
        acsFilterPanel.setVisible(azureLibrary == AzureLibrary.ACS_FILTER);
        libraryVersion.setText(azureLibrary.getName());
        location.setText((String.format("%s%s%s", AzurePlugin.pluginFolder, File.separator, azureLibrary.getLocation())));
        rootPanel.revalidate();
        return rootPanel;
    }

    public boolean onFinish() {
        if (azureLibrary == AzureLibrary.ACS_FILTER) {
            if (doValidate() != null) {
                return false;
            }
            configureDeployment();
        }
        return true;
    }

    @Override
    public JComponent getPanel() {
        return prepare();
    }

    @Override
    public String getDisplayName() {
        return message("edtLbrTtl");
    }

    @Override
    public boolean doOKAction() {
        try {
            configureDeployment();
            return true;
        } catch (Exception ex) {
            PluginUtil.displayErrorDialogAndLog(message("error"), "Error saving configuration", ex);
            return false;
        }
    }

    @Override
    public String getSelectedValue() {
        return null;
    }

    public boolean isExported() {
        return depCheck.isSelected();
    }

    public ValidationInfo doValidate() {
        boolean isEdit = isEdit();
        StringBuilder errorMessage = new StringBuilder();

        // Display error if acs login page URL is null. Applicable for first time and edit scenarios.
        if (acsTxt.getText().isEmpty() || acsTxt.getText().equalsIgnoreCase(message("acsTxt")))
            errorMessage.append(message("acsTxtErr")).append("\n");

        // Display error if relying part realm is null. Applicable for first time and edit scenarios.
        if (relTxt.getText().isEmpty())
            errorMessage.append(message("relTxtErr")).append("\n");

        // if certificate location does not end with .cer then display error
        if (!certTxt.getText().isEmpty() && !certTxt.getText().toLowerCase().endsWith(".cer"))
            errorMessage.append(message("certTxtInvalidExt")).append("\n");

        // Display error if cert location is empty for first time and for edit scenarios if
        // embedded cert option is not selected
        if ((!isEdit && certTxt.getText().isEmpty()) || (isEdit && certTxt.getText().isEmpty() && !embedCertCheck.isSelected()))
            errorMessage.append(message("certTxtErr")).append("\n");

        // For first time , if embedded cert option is selected , display error if file does not exist at source
        if (!isEdit && !certTxt.getText().isEmpty() && embedCertCheck.isSelected() == true) {
            if (!new File(CerPfxUtil.getCertificatePath(certTxt.getText())).exists()) {
                errorMessage.append(message("acsNoValidCert")).append("\n");
            }
        }
        if (errorMessage.length() > 0) {
            return new ValidationInfo(errorMessage.toString());
        } else {
            return null;
        }
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    /**
     * Method generates key using
     * Advanced Encryption Standard algorithm.
     *
     * @return String
     * @throws Exception
     */
    private String generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey secretKey = keyGenerator.generateKey();

        byte[] keyInBytes = secretKey.getEncoded();
        String key = Base64.encode(keyInBytes);
        return key;
    }

    /**
     * Method creates web.xml (deployment descriptor)
     * in WebContent\WEB-INF folder of dynamic web project
     * if does not present already.
     *
     * @return String
     */
    private String createWebXml() {
        String path = null;
        try {
            File cmpntFileLoc = new File(String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("depDirLoc")));
            String cmpntFile = String.format("%s%s%s", cmpntFileLoc, File.separator, message("depFileName"));
            if (!cmpntFileLoc.exists()) {
                cmpntFileLoc.mkdirs();
            }
            AzurePlugin.copyResourceFile(message("resFileLoc"), cmpntFile);
            path = cmpntFile;
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("acsErrTtl"), message("fileCrtErrMsg"), e);
        }
        return new File(path).getPath();
    }

    private String getEmbeddedCertInfo() {
        String webinfLoc = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("depDirLoc"));
        String certLoc = String.format("%s%s%s", webinfLoc, File.separator, message("acsCertLoc"));
        return getCertInfo(certLoc);
    }

    public static void copy(File source, final File destination) throws IOException {
        InputStream instream = null;
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            String[] kid = source.list();
            for (int i = 0; i < kid.length; i++) {
                copy(new File(source, kid[i]),
                        new File(destination, kid[i]));
            }
        } else {
            //InputStream instream = null;
            OutputStream out = null;
            try {
                if (destination != null && destination.isFile() && !destination.getParentFile().exists())
                    destination.getParentFile().mkdirs();

                instream = new FileInputStream(source);
                out = new FileOutputStream(destination);
                byte[] buf = new byte[BUFF_SIZE];
                int len = instream.read(buf);

                while (len > 0) {
                    out.write(buf, 0, len);
                    len = instream.read(buf);
                }
            } finally {
                if (instream != null) {
                    instream.close();
                }
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    public void removeEmbedCert() {
        String webinfLoc = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("depDirLoc"));
        String certLoc = String.format("%s%s%s", webinfLoc, File.separator, message("acsCertLoc"));
        File destination = new File(certLoc);
        if (destination.exists())
            destination.delete();
        if (destination.getParentFile().exists() && destination.getParentFile().list().length == 0)
            destination.getParentFile().delete();
    }

    private static String getCertInfo(String certURL) {
        X509Certificate acsCert = CerPfxUtil.getCert(certURL, null);
        if (acsCert != null) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            StringBuilder certInfo = new StringBuilder();
            certInfo.append(String.format("%1$-10s", "Subject")).append(" : ").append(acsCert.getSubjectDN()).append("\n");
            certInfo.append(String.format("%1$-11s", "Issuer")).append(" : ").append(acsCert.getIssuerDN()).append("\n");
            certInfo.append(String.format("%1$-13s", "Valid")).append(" : ").append(dateFormat.format(acsCert.getNotBefore())).
                    append(" to ").append(dateFormat.format(acsCert.getNotAfter()));
            return certInfo.toString();
        } else {
            return null;
        }
    }

    /**
     * Method adds ACS filter and filter mapping tags in web.xml
     * and saves input values given on ACS library page.
     * In case of edit, populates previously set values.
     */
    private void configureDeployment() {
        ACSFilterHandler handler = null;
        try {
            String xmlPath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("xmlPath"));
            File webXml = new File(xmlPath);
            if (webXml.exists()) {
                handler = new ACSFilterHandler(xmlPath);
                handler.setAcsFilterParams(message("acsAttr"), acsTxt.getText());
                handler.setAcsFilterParams(message("relAttr"), relTxt.getText());
                if (!embedCertCheck.isSelected()) {
                    handler.setAcsFilterParams(message("certAttr"), certTxt.getText());
                    if (getEmbeddedCertInfo() != null)
                        removeEmbedCert();
                } else {
                    handler.removeParamsIfExists(message("certAttr"));
                    if (!certTxt.getText().isEmpty()) {
                        String webinfLoc = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("depDirLoc"));
                        String certLoc = String.format("%s%s%s", webinfLoc, File.separator, message("acsCertLoc"));
                        File destination = new File(certLoc);
                        if (!destination.getParentFile().exists())
                            destination.getParentFile().mkdir();
                        copy(new File(CerPfxUtil.getCertificatePath(certTxt.getText())), destination);
                    }
                }
                handler.setAcsFilterParams(message("secretKeyAttr"), generateKey());
                handler.setAcsFilterParams(message("allowHTTPAttr"), requiresHttpsCheck.isSelected() ? "false" : "true");
            } else {
                int choice = Messages.showYesNoDialog(message("depDescMsg"), message("depDescTtl"), Messages.getQuestionIcon());
                if (choice == Messages.YES) {
                    String path = createWebXml();
                    //copy cert into WEB-INF/cert/_acs_signing.cer location if embed cert is selected
                    if (embedCertCheck.isSelected()) {
                        String webinfLoc = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("depDirLoc"));
                        String certLoc = String.format("%s%s%s", webinfLoc, File.separator, message("acsCertLoc"));
                        File destination = new File(certLoc);
                        if (!destination.getParentFile().exists())
                            destination.getParentFile().mkdir();
                        copy(new File(CerPfxUtil.getCertificatePath(certTxt.getText())), destination);
                    }
                    handler = new ACSFilterHandler(path);
                    handler.setAcsFilterParams(message("acsAttr"), acsTxt.getText());
                    handler.setAcsFilterParams(message("relAttr"), relTxt.getText());
                    if (!embedCertCheck.isSelected()) { //Do not make entry if embed cert is selected
                        handler.setAcsFilterParams(message("certAttr"), certTxt.getText());
                        if (getEmbeddedCertInfo() != null)
                            removeEmbedCert();
                    }
                    handler.setAcsFilterParams(message("secretKeyAttr"), generateKey());
                    handler.setAcsFilterParams(message("allowHTTPAttr"), requiresHttpsCheck.isSelected() ? "false" : "true");
                } else {
                    return;
                }
            }
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("acsErrTtl"), message("acsErrMsg"), e);
        }
        try {
            handler.save();
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("acsErrTtl"), message("saveErrMsg"), e);
        }
    }

    /**
     * @return current window is edit or not
     */
    private boolean isEdit() {
        return isEdit;
    }

    public String getHelpId() {
        return "acs_config_dialog";
    }
}
