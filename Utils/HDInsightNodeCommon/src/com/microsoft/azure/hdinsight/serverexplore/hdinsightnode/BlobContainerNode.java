package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.TelemetryCommon;
import com.microsoft.azure.hdinsight.common.TelemetryManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

import java.util.Map;

public class BlobContainerNode extends Node {
    private static final String CONTAINER_MODULE_ID = BlobContainerNode.class.getName();
    private static final String ICON_PATH = CommonConst.BlobContainerIConPath;
    private static final String DEFAULT_CONTAINER_FLAG = "(default)";

    private ClientStorageAccount storageAccount;
    private BlobContainer blobContainer;

    public BlobContainerNode(Node parent, ClientStorageAccount storageAccount, BlobContainer blobContainer) {
        this(parent, storageAccount, blobContainer, false);
    }

    public BlobContainerNode(Node parent, ClientStorageAccount storageAccount, BlobContainer blobContainer, boolean isDefaultContainer) {
        super(CONTAINER_MODULE_ID, isDefaultContainer ? blobContainer.getName() + DEFAULT_CONTAINER_FLAG : blobContainer.getName(), parent, ICON_PATH);
        this.storageAccount = storageAccount;
        this.blobContainer = blobContainer;
    }

    public class RefreshAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            DefaultLoader.getUIHelper().refreshBlobs(getProject(), storageAccount, blobContainer);
        }
    }

    public class ViewBlobContainer extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            onNodeClick(e);
        }
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        TelemetryManager.postEvent(TelemetryCommon.HDInsightExplorerContainerOpen, null, null);
        final Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(getProject(), storageAccount, blobContainer);
        if (openedFile == null) {
            DefaultLoader.getUIHelper().openItem(getProject(), storageAccount, blobContainer, " [Container]", "BlobContainer", CommonConst.BlobContainerIConPath);
        } else {
            DefaultLoader.getUIHelper().openItem(getProject(), openedFile);
        }
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        return ImmutableMap.of(
                "Refresh", RefreshAction.class,
                "View Blob Container", ViewBlobContainer.class
                );
    }
}
