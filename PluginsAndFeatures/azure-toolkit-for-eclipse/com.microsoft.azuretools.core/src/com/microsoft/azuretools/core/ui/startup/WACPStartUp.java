/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azuretools.core.ui.startup;

import java.io.File;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.w3c.dom.Document;

import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.azurecommons.util.GetHashMac;
import com.microsoft.azuretools.azurecommons.util.ParserXMLUtility;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.telemetry.AppInsightsCustomEvent;
import com.microsoft.azuretools.core.utils.FileUtil;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;

/**
 * This class gets executed after the Workbench initializes.
 */
public class WACPStartUp implements IStartup {
	private String _hashmac = GetHashMac.GetHashMac();

	public void earlyStartup() {
		initialize();

		Collection<String> obsoletePackages = Activator.getDefault().getObsoletePackages();
		if (obsoletePackages != null && !obsoletePackages.isEmpty()) {
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Need to clean packages",
							"You have an older version of the Azure Toolkit for Eclipse installed which "
									+ "could not be automatically upgraded. You should uninstall all the listed "
									+ "components manually, and reinstall from http://dl.microsoft.com/eclipse\n\n"
									+ StringUtils.join(obsoletePackages, "\n"));

				}

			});
		}
	}

	/**
	 * Method verifies presence of com.microsoft.azuretools.core folder and
	 * data.xml file. It updates or creates property elements in data.xml as per
	 * scenarios.
	 */
	private void initialize() {
		try {
			String pluginInstLoc = String.format("%s%s%s", PluginUtil.pluginFolder, File.separator,
					Messages.commonPluginID);
			final String dataFile = String.format("%s%s%s", pluginInstLoc, File.separator, Messages.dataFileName);
			if (new File(pluginInstLoc).exists()) {
				if (new File(dataFile).exists()) {
					String version = DataOperations.getProperty(dataFile, Messages.version);
					if (version == null || version.isEmpty()) {
						// proceed with setValues method as no version specified
						setValues(dataFile);
					} else {
						String curVersion = Activator.getDefault().getBundle().getVersion().toString();
						// compare version
						if (curVersion.equalsIgnoreCase(version)) {
							// Case of normal eclipse restart
							// check preference-value & installation-id exists
							// or not else copy values
							String prefValue = DataOperations.getProperty(dataFile, Messages.prefVal);
							String hdinsightPrefValue = DataOperations.getProperty(dataFile, Messages.hdinshgtPrefVal);
							String instID = DataOperations.getProperty(dataFile, Messages.instID);

							if (StringHelper.isNullOrWhiteSpace(prefValue)
									|| StringHelper.isNullOrWhiteSpace(hdinsightPrefValue)) {
								setValues(dataFile, StringHelper.isNullOrWhiteSpace(prefValue),
										StringHelper.isNullOrWhiteSpace(hdinsightPrefValue));
							} else if (instID == null || instID.isEmpty()) {
								Document doc = ParserXMLUtility.parseXMLFile(dataFile);
								DataOperations.updatePropertyValue(doc, Messages.instID, _hashmac);
								ParserXMLUtility.saveXMLFile(dataFile, doc);
							} else if (instID != null && !instID.isEmpty()) {
								if (!GetHashMac.IsValidHashMacFormat(instID)) {
									Document doc = ParserXMLUtility.parseXMLFile(dataFile);
									DataOperations.updatePropertyValue(doc, Messages.instID, _hashmac);
									ParserXMLUtility.saveXMLFile(dataFile, doc);
								}
							}
						} else {
							// proceed with setValues method. Case of new plugin installation
							setValues(dataFile);
						}
					}
				} else {
					// copy file and proceed with setValues method
					FileUtil.copyResourceFile(Messages.dataFileEntry, dataFile);
					setValues(dataFile);
				}
			} else {
				new File(pluginInstLoc).mkdir();
				FileUtil.copyResourceFile(Messages.dataFileEntry, dataFile);
				setValues(dataFile);
			}
		} catch (Exception ex) {
			Activator.getDefault().log(ex.getMessage(), ex);
		}
	}

	private void setValues(final String dateFile) throws Exception {
		setValues(dateFile, true, true);
	}

	/**
	 * Method updates or creates property elements in data.xml
	 * 
	 * @param dataFile
	 * @throws Exception
	 */
	private void setValues(final String dataFile, final boolean isAzureToolKit, final boolean isHDInsight)
			throws Exception {
		final Document doc = ParserXMLUtility.parseXMLFile(dataFile);
		if (isAzureToolKit) {
			DataOperations.updatePropertyValue(doc, Messages.prefVal, "true");
		}

		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				if (isHDInsight && !Activator.getDefault().isHDInsightEnabled()) {
					boolean isShowHDInsightTips = true;
					HDInsightHelpDlg hdInsightHelpDlg = new HDInsightHelpDlg(Display.getDefault().getActiveShell());
					if (hdInsightHelpDlg.open() == Window.CANCEL && !hdInsightHelpDlg.isShowTipsStatus()) {
						isShowHDInsightTips = false;
						DataOperations.updatePropertyValue(doc, Messages.hdinshgtPrefVal,
								String.valueOf(isShowHDInsightTips));
					}
				}
			}
		});

		DataOperations.updatePropertyValue(doc, Messages.version,
				Activator.getDefault().getBundle().getVersion().toString());
		DataOperations.updatePropertyValue(doc, Messages.instID, _hashmac);
		ParserXMLUtility.saveXMLFile(dataFile, doc);
		String prefVal = DataOperations.getProperty(dataFile, Messages.prefVal);
		if (prefVal != null && !prefVal.isEmpty() && prefVal.equals("true")) {
			AppInsightsCustomEvent.create(Messages.telAgrEvtName, "");
		}
	}
}
