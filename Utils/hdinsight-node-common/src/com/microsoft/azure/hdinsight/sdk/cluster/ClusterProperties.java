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
package com.microsoft.azure.hdinsight.sdk.cluster;

import java.util.List;

public class ClusterProperties {
    private String clusterVersion;
    private String osType;
    private ClusterDefinition clusterDefinition;
    private ComputeProfile computeProfile;
    private String provisioningState;
    private String clusterState;
    private String createdDate;
    private QuotaInfo quotaInfo;
    private List<ConnectivityEndpoint> connectivityEndpoints;

    public String getClusterVersion(){
        return clusterVersion;
    }

    public String getOsType(){
        return osType;
    }

    public String getProvisioningState(){
        return provisioningState;
    }

    public String getClusterState(){
        return clusterState;
    }

    public ClusterDefinition getClusterDefinition() {
        return clusterDefinition;
    }

    public ComputeProfile getComputeProfile(){
        return computeProfile;
    }

    public String getCreatedDate(){
        return createdDate;
    }

    public QuotaInfo getQuotaInfo(){
        return quotaInfo;
    }

    public List<ConnectivityEndpoint> getConnectivityEndpoints(){
        return connectivityEndpoints;
    }
}
