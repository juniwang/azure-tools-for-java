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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.microsoft.azure.hdinsight.sdk.common.*;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.rest.AzureAADHelper;
import com.microsoft.tooling.msservices.helpers.azure.rest.RestServiceManager;
import com.microsoft.tooling.msservices.helpers.azure.rest.RestServiceManagerBaseImpl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

public class ClusterOperationImpl implements IClusterOperation {

     private final String VERSION = "2015-03-01-preview";

     private Object project;

     public ClusterOperationImpl(Object project) {
          this.project = project;
     }

     /**
      * list hdinsight cluster
      *
      * @param subscription
      * @return cluster raw data info
      * @throws IOException
      */
     public List<ClusterRawInfo> listCluster(final SubscriptionDetail subscription) throws IOException, HDIException, AzureCmdException {
          try {
               String response = requestWithToken(subscription.getTenantId(), new RequestCallback<String>() {
                    @Override
                    public String execute(String accessToken) throws Throwable {
                         return AzureAADHelper.executeRequest(CommonConstant.HDINSIGHT_CLUSTER_URI,
                                 String.format("api/Clusters/GetAll?subscriptionIds=%s;&_=%d", subscription.getSubscriptionId(), new Date().getTime()),
                                 RestServiceManager.ContentType.Json,
                                 "GET",
                                 null,
                                 accessToken,
                                 new RestServiceManagerBaseImpl());
                    }
               });
               return new AuthenticationErrorHandler<List<ClusterRawInfo>>() {
                    @Override
                    public List<ClusterRawInfo> execute(String response) {
                         Type listType = new TypeToken<List<ClusterRawInfo>>() {
                         }.getType();
                         List<ClusterRawInfo> clusterRawInfoList = new Gson().fromJson(response, listType);
                         return clusterRawInfoList;
                    }
               }.run(response);
          } catch (Throwable th) {
               throw new AzureCmdException("Error listing HDInsight clusters", th);
          }
     }

     /**
      * get cluster configuration including http username, password, storage and additional storage account
      *
      * @param subscription
      * @param clusterId
      * @return cluster configuration info
      * @throws IOException
      */
     public ClusterConfiguration getClusterConfiguration(final SubscriptionDetail subscription, final String clusterId) throws IOException, HDIException, AzureCmdException {
          try {
               String response = requestWithToken(subscription.getTenantId(), new RequestCallback<String>() {
                    @Override
                    public String execute(String accessToken) throws Throwable {
                         return AzureAADHelper.executeRequest(CommonConstant.MANAGEMENT_URI,
                                 String.format("%s/configurations?api-version=%s", clusterId.replaceAll("/+$", ""), VERSION),
                                 null,
                                 "GET",
                                 null,
                                 accessToken,
                                 new RestServiceManagerBaseImpl());
                    }
               });
               return new AuthenticationErrorHandler<ClusterConfiguration>() {
                    @Override
                    public ClusterConfiguration execute(String response) {
                         Type listType = new TypeToken<ClusterConfiguration>() {
                         }.getType();
                         ClusterConfiguration clusterConfiguration = new Gson().fromJson(response, listType);

                         if (clusterConfiguration == null || clusterConfiguration.getConfigurations() == null) {
                              return null;
                         }

                         return clusterConfiguration;
                    }
               }.run(response);
          } catch (Throwable th) {
               throw new AzureCmdException("Error getting cluster configuration", th);
          }
     }

     @NotNull
     public <T> T requestWithToken(@NotNull String tenantId, @NotNull final RequestCallback<T> requestCallback)
             throws Throwable {
          AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
          // not signed in
          if (azureManager == null) {
               return null;
          }

          String accessToken = azureManager.getAccessToken(tenantId);
          return requestCallback.execute(accessToken);
     }
}
