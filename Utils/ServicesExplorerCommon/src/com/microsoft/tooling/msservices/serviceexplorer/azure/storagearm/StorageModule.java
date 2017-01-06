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
package com.microsoft.tooling.msservices.serviceexplorer.azure.storagearm;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.ExternalStorageHelper;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper.EventStateHandle;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StorageModule extends AzureRefreshableNode {
    private static final String STORAGE_MODULE_ID = com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageModule.class.getName();
    private static final String ICON_PATH = "storage.png";
    private static final String BASE_MODULE_NAME = "Storage Accounts";

    public StorageModule(Node parent) {
        super(STORAGE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
    }

    @Override
    protected void refresh(@NotNull EventStateHandle eventState)
            throws AzureCmdException {
        removeAllChildNodes();
        List<Pair<String, String>> failedSubscriptions = new ArrayList<>();

        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                return;
            }

            SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
            System.out.println("getting sid list...");
            Set<String> sidList = subscriptionManager.getAccountSidList();
            for (String sid : sidList) {
                System.out.println("sid : " + sid);
                try {
                    Azure azure = azureManager.getAzure(sid);
                    List<com.microsoft.azure.management.storage.StorageAccount> storageAccounts = azure.storageAccounts().list();
                    if (eventState.isEventTriggered()) {
                        return;
                    }

                    for (StorageAccount sm : storageAccounts) {
                        addChildNode(new StorageNode(this, sid, sm));
                    }

                } catch (Exception ex) {
                    failedSubscriptions.add(new ImmutablePair<>(sid, ex.getMessage()));
                    continue;
                }
            }
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError("An error occurred when trying to load Storage Accounts\n\n" + ex.getMessage(), ex);
        }

//            public List<ArmStorageAccount> execute(@NotNull Azure azure) throws Throwable {
//                List<ArmStorageAccount> storageAccounts = new ArrayList<>();
//                for (StorageAccount storageAccount : azure.storageAccounts().list()){
//                    ArmStorageAccount sa = new ArmStorageAccount(storageAccount.name(), subscriptionId, storageAccount);
//
//                    sa.setProtocol("https");
//                    sa.setType(storageAccount.sku().name().toString());
//                    sa.setLocation(Strings.nullToEmpty(storageAccount.regionName()));
//                    List<StorageAccountKey> keys = storageAccount.keys();
//                    if (!(keys == null || keys.isEmpty())) {
//                        sa.setPrimaryKey(keys.get(0).value());
//                        if (keys.size() > 1) {
//                            sa.setSecondaryKey(keys.get(1).value());
//                        }
//                    }
//                    sa.setResourceGroupName(storageAccount.resourceGroupName());
//                    storageAccounts.add(sa);
//                }
//                return storageAccounts;
//            }
        // load External Accounts
        for (ClientStorageAccount clientStorageAccount : ExternalStorageHelper.getList(getProject())) {
            ClientStorageAccount storageAccount = StorageClientSDKManager.getManager().getStorageAccount(clientStorageAccount.getConnectionString());

            if (eventState.isEventTriggered()) {
                return;
            }

//            addChildNode(new ExternalStorageNode(this, storageAccount));
        }
        if (!failedSubscriptions.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("An error occurred when trying to load Storage Accounts for the subscriptions:\n\n");
            for (Pair error : failedSubscriptions) {
                errorMessage.append(error.getKey()).append(": ").append(error.getValue()).append("\n");
            }
            DefaultLoader.getUIHelper().logError("An error occurred when trying to load Storage Accounts\n\n" + errorMessage.toString(), null);
        }
    }
}
