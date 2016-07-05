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
package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.CommonConstant;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.StringHelper;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;

import java.awt.*;
import java.net.URI;
import java.util.UUID;

public class ClusterNode extends AzureRefreshableNode {
    private static final String CLUSTER_MODULE_ID = ClusterNode.class.getName();
    private static final String ICON_PATH = CommonConst.ClusterIConPath;
    private static boolean isHDInsightPlugin;

    private IClusterDetail clusterDetail;

    static {
        isHDInsightPlugin = ClusterNode.class.getClassLoader().toString().contains("intellij");
    }

    public ClusterNode(Node parent, IClusterDetail clusterDetail) {
        super(CLUSTER_MODULE_ID, getClusterNameWitStatus(clusterDetail), parent, ICON_PATH, true);
        this.clusterDetail = clusterDetail;
        this.loadActions();
        this.load();
    }

    @Override
    protected void loadActions() {
        super.loadActions();

        addAction("Open Spark History UI", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                String sparkHistoryUrl = String.format("https://%s.azurehdinsight.net/sparkhistory", clusterDetail.getName());
                openUrlLink(sparkHistoryUrl);
            }
        });

        addAction("Open Cluster Management Portal(Ambari)", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                String ambariUrl = String.format(CommonConstant.DEFAULT_CLUSTER_ENDPOINT, clusterDetail.getName());
                openUrlLink(ambariUrl);
            }
        });

        if (clusterDetail instanceof ClusterDetail) {
            addAction("Open Jupyter Notebook", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    String jupyterUrl = String.format("https://%s.azurehdinsight.net/jupyter/tree", clusterDetail.getName());
                    openUrlLink(jupyterUrl);
                }
            });

            addAction("Open Azure Management Portal", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    String resourceGroupName = clusterDetail.getResourceGroup();
                    if (resourceGroupName != null) {
                        String webPortHttpLink = String.format("https://portal.azure.com/#resource/subscriptions/%s/resourcegroups/%s/providers/Microsoft.HDInsight/clusters/%s",
                                clusterDetail.getSubscription().getId(),
                                resourceGroupName,
                                clusterDetail.getName());
                        openUrlLink(webPortHttpLink);
                    } else {
                        DefaultLoader.getUIHelper().showError("Failed to get resource group name.", "HDInsight Explorer");
                    }
                }
            });
        }

        if (clusterDetail instanceof HDInsightAdditionalClusterDetail) {
            addAction("Unlink", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    boolean choice = DefaultLoader.getUIHelper().showConfirmation("Do you really want to unlink the HDInsight cluster?",
                            "Unlink HDInsight Cluster", new String[]{"Yes", "No"}, null);
                    if(choice) {
                        ClusterManagerEx.getInstance().removeHDInsightAdditionalCluster((HDInsightAdditionalClusterDetail)clusterDetail);
                        ((HDInsightRootModule) getParent()).refreshWithoutAsync();
                    }
                }
            });
        }
    }

    @Override
    protected void refresh(@NotNull EventHelper.EventStateHandle eventState)
            {
        removeAllChildNodes();

        // disable JobView Node on Eclipse Plugin
        if(isHDInsightPlugin) {
            final String uuid = UUID.randomUUID().toString();
            JobViewManager.registerJovViewNode(uuid, clusterDetail);
            JobViewNode jobViewNode = new JobViewNode(this, uuid);
            addChildNode(jobViewNode);
        }

        //TelemetryManager.postEvent(TelemetryCommon.HDInsightExplorerSparkNodeExpand, null, null);
        RefreshableNode storageAccountNode = new StorageAccountFolderNode(this, clusterDetail);
        addChildNode(storageAccountNode);
    }

    private static String getClusterNameWitStatus(IClusterDetail clusterDetail) {
        String state = clusterDetail.getState();
        if(!StringHelper.isNullOrWhiteSpace(state) && !state.equalsIgnoreCase("Running")) {
            return String.format("%s (State:%s)", clusterDetail.getName(), state);
        }
        return clusterDetail.getName();
    }

    private void openUrlLink(String linkUrl) {
        if (clusterDetail != null && !StringHelper.isNullOrWhiteSpace(clusterDetail.getName())) {
            try {
                Desktop.getDesktop().browse(new URI(linkUrl));
            } catch (Exception exception) {
                DefaultLoader.getUIHelper().showError(exception.getMessage(), "HDInsight Explorer");
            }
        }
    }
}
