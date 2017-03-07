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
package com.microsoft.tooling.msservices.serviceexplorer.azure.webapps;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.List;
import java.util.Map;

public class WebappsModule extends AzureRefreshableNode {
	private static final String WEBAPPS_MODULE_ID = WebappsModule.class.getName();
	private static final String WEB_RUN_ICON = "website.png";
	private static final String WEB_STOP_ICON = "stopWebsite.png";
	private static final String BASE_MODULE_NAME = "Web Apps";
	private static final String RUN_STATUS = "Running";

	public WebappsModule(Node parent) {
		super(WEBAPPS_MODULE_ID, BASE_MODULE_NAME, parent, WEB_RUN_ICON);
	}

	@Override
	protected void refreshItems() throws AzureCmdException {
		removeAllChildNodes();
		if (AzureModel.getInstance().getResourceGroupToWebAppMap() == null) {
			try {
				AzureModelController.updateResourceGroupMaps(null);
			} catch (Exception ex) {
				DefaultLoader.getUIHelper().logError("Error updating webapps cache", ex);
			}
			DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
				@Override
				public void run() {
					fillWebappsNodes();
				}
			});
		} else {
			fillWebappsNodes();
		}

//			if (webSiteConfigMapTemp != null && !webSiteConfigMapTemp.isEmpty()) {
//				List<WebSite> webSiteList = new ArrayList<WebSite>(webSiteConfigMapTemp.keySet());
//				Collections.sort(webSiteList, new Comparator<WebSite>() {
//					@Override
//					public int compare(WebSite ws1, WebSite ws2) {
//						return ws1.getName().compareTo(ws2.getName());
//					}
//				});
//				for (WebSite webSite : webSiteList) {
//					if (webSite.getStatus().equalsIgnoreCase(runStatus)) {
//						addChildNode(new WebappNode(this, webSite, WEB_RUN_ICON));
//					} else {
//						addChildNode(new WebappNode(this, webSite, WEB_STOP_ICON));
//					}
//				}
//			}
//		}
	}

	private void fillWebappsNodes() {
		Map<SubscriptionDetail, List<ResourceGroup>> srgMap = AzureModel.getInstance().getSubscriptionToResourceGroupMap();
		Map<ResourceGroup, List<WebApp>> rgwaMap = AzureModel.getInstance().getResourceGroupToWebAppMap();
		if (srgMap != null) {
			for (SubscriptionDetail sd : srgMap.keySet()) {
				if (!sd.isSelected()) continue;

				for (ResourceGroup rg : srgMap.get(sd)) {
					for (WebApp webApp : rgwaMap.get(rg)) {
						addChildNode(new WebappNode(this, sd.getSubscriptionId(), webApp, rg,
								RUN_STATUS.equalsIgnoreCase(webApp.inner().state()) ? WEB_RUN_ICON : WEB_STOP_ICON));
					}
				}
			}
		}
	}
}