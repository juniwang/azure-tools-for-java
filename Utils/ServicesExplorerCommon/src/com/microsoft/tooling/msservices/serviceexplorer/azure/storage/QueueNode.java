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

import com.google.common.collect.ImmutableMap;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManagerImpl;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.Queue;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper.EventStateHandle;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.Map;

public class QueueNode extends Node {
    public class RefreshAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            DefaultLoader.getUIHelper().refreshQueue(getProject(), storageAccount, queue);
        }
    }

    public class ViewQueue extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            onNodeClick(e);
        }
    }

    public class DeleteQueue extends AzureNodeActionPromptListener {
        public DeleteQueue() {
            super(QueueNode.this,
                    String.format("Are you sure you want to delete the queue \"%s\"?", queue.getName()),
                    "Deleting Queue");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e, @NotNull EventStateHandle stateHandle)
                throws AzureCmdException {
            Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(getProject(), storageAccount, queue);

            if (openedFile != null) {
                DefaultLoader.getIdeHelper().closeFile(getProject(), openedFile);
            }

            try {
                StorageClientSDKManagerImpl.getManager().deleteQueue(storageAccount, queue);

                parent.removeAllChildNodes();
                ((QueueModule) parent).load();
            } catch (AzureCmdException ex) {
                DefaultLoader.getUIHelper().showException("An error occurred while attempting to delete queue", ex,
                        "MS Services - Error Deleting Queue", false, true);
            }
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e)
                throws AzureCmdException {
        }
    }

    public class ClearQueue extends AzureNodeActionPromptListener {
        public ClearQueue() {
            super(QueueNode.this,
                    String.format("Are you sure you want to clear the queue \"%s\"?", queue.getName()),
                    "Clearing Queue");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e, @NotNull EventStateHandle stateHandle)
                throws AzureCmdException {
            try {
                StorageClientSDKManagerImpl.getManager().clearQueue(storageAccount, queue);

                DefaultLoader.getUIHelper().refreshQueue(getProject(), storageAccount, queue);
            } catch (AzureCmdException ex) {
                DefaultLoader.getUIHelper().showException("An error occurred while attempting to clear queue.", ex,
                        "MS Services - Error Clearing Queue", false, true);
            }
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e)
                throws AzureCmdException {
        }
    }

    private static final String QUEUE_MODULE_ID = QueueNode.class.getName();
    private static final String ICON_PATH = "container.png";
    private final Queue queue;
    private final ClientStorageAccount storageAccount;

    public QueueNode(QueueModule parent, ClientStorageAccount storageAccount, Queue queue) {
        super(QUEUE_MODULE_ID, queue.getName(), parent, ICON_PATH, true);

        this.storageAccount = storageAccount;
        this.queue = queue;

        loadActions();
    }

    @Override
    protected void onNodeClick(NodeActionEvent ex) {
        final Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(getProject(), storageAccount, queue);

        if (openedFile == null) {
            DefaultLoader.getUIHelper().openItem(getProject(), storageAccount, queue, " [Queue]", "Queue", "container.png");
        } else {
            DefaultLoader.getUIHelper().openItem(getProject(), openedFile);
        }
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        return ImmutableMap.of(
                "Refresh", RefreshAction.class,
                "View Queue", ViewQueue.class,
                "Delete", DeleteQueue.class,
                "Clear Queue", ClearQueue.class
        );
    }
}