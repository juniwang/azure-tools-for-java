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
package com.microsoft.tooling.msservices.serviceexplorer.azure.storage;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.List;
import java.util.Map;

public class StorageNode extends RefreshableNode {
    private static final String STORAGE_ACCOUNT_ICON_PATH = "storageaccount.png";

    private final StorageAccount storageAccount;
    private String subscriptionId;

    public StorageNode(Node parent, String subscriptionId, StorageAccount storageAccount) {
        super(storageAccount.name(), storageAccount.name(), parent, STORAGE_ACCOUNT_ICON_PATH,  true);

        this.subscriptionId = subscriptionId;
        this.storageAccount = storageAccount;

        loadActions();
    }

    public class DeleteStorageAccountAction extends AzureNodeActionPromptListener {
        public DeleteStorageAccountAction() {
            super(StorageNode.this,
                    String.format("This operation will delete storage account %s.\nAre you sure you want to continue?", storageAccount.name()),
                    "Deleting Storage Account");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e)
                throws AzureCmdException {
            try {
                AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                // not signed in
                if (azureManager == null) {
                    return;
                }
                Azure azure = azureManager.getAzure(subscriptionId);
                azure.storageAccounts().deleteByGroup(storageAccount.resourceGroupName(), storageAccount.name());
                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // instruct parent node to remove this node
                        getParent().removeDirectChildNode(StorageNode.this);
                    }
                });
            } catch (Exception ex) {
                DefaultLoader.getUIHelper().showException("An error occurred while attempting to delete storage account.", ex,
                        "MS Services - Error Deleting Storage Account", false, true);
            }
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }
    }

    @Override
    protected void refreshItems()
            throws AzureCmdException {
        removeAllChildNodes();

        try {
            List<BlobContainer> containerList = StorageClientSDKManager.getManager()
                    .getBlobContainers(StorageClientSDKManager.getConnectionString(storageAccount));
            for (BlobContainer blobContainer : containerList) {
                addChildNode(new ContainerNode(this, storageAccount, blobContainer));
            }
        } catch (AzureCmdException ex) {
            throw new AzureCmdException(ex.getMessage(), ex);
        }
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        this.load();
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        addAction("Delete", new DeleteStorageAccountAction());
        return super.initActions();
    }

    public StorageAccount getStorageAccount() {
        return storageAccount;
    }
}