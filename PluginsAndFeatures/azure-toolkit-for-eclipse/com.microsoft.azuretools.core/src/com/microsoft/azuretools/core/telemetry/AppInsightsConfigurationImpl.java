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
package com.microsoft.azuretools.core.telemetry;

import java.io.File;
import java.util.UUID;

import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.telemetry.AppInsightsConfiguration;

public class AppInsightsConfigurationImpl implements AppInsightsConfiguration{
	static final String EVENT_NAME_PREFIX = "AzurePlugin.Eclipse.";
	static String pluginInstLoc = String.format("%s%s%s", PluginUtil.pluginFolder, File.separator,
			Messages.commonPluginID);
	static String dataFile = String.format("%s%s%s", pluginInstLoc, File.separator, Messages.dataFileName);
	static String key = "824aaa4c-052b-4c43-bdcb-48f915d71b3f";
	static String sessionId = UUID.randomUUID().toString();
	
	@Override
	public String appInsightsKey() {
		return key;
	}

	@Override
	public String sessionId() {
		return sessionId;
	}

	@Override
	public String pluginVersion() {
		return DataOperations.getProperty(dataFile, Messages.version);
	}

	@Override
	public String installationId() {
		return DataOperations.getProperty(dataFile, Messages.instID);
	}

	@Override
	public String preferenceVal() {
		return DataOperations.getProperty(dataFile, Messages.prefVal);
	}

	@Override
	public boolean validated() {
		return new File(pluginInstLoc).exists() && new File(dataFile).exists();
	}

	@Override
	public String eventNamePrefix() {
		return EVENT_NAME_PREFIX;
	}

}
