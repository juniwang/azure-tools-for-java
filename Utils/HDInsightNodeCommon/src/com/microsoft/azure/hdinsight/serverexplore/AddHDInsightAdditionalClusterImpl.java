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
package com.microsoft.azure.hdinsight.serverexplore;

import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.management.storage.implementation.StorageManagementClientImpl;
import com.microsoft.tooling.msservices.helpers.StringHelper;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddHDInsightAdditionalClusterImpl {

    private static final String clusterConfigureFileUrl = "https://%s.azurehdinsight.net/api/v1/clusters/%s/configurations/service_config_versions?service_name=HDFS&service_config_version=1";

    private static final Pattern PATTERN_DEFAULT_STORAGE = Pattern.compile("\"fs\\.defaultFS\":\"wasb://([^@\"]*)@([^@\"]*)\"", Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
    private static final Pattern PATTER_STORAGE_KEY = Pattern.compile("\"fs\\.azure\\.account\\.key\\.[^\"]*\":\"[^\"]*=\"", Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
    private static final String STORAGE_ACCOUNT_NAME_PATTERN = "^wasb://(.*)@(.*)$";
    private static final String BLOB_URL_SUFFIX = ".blob.core.windows.net";

    public static HDStorageAccount getStorageAccount(String clusterName, String storageName, String storageKey, String userName, String password) throws HDIException, AzureCmdException {

        String responseMessage = getMessageByAmbari(clusterName, userName, password);
        responseMessage = responseMessage.replace(" ", "");
        if (StringHelper.isNullOrWhiteSpace(responseMessage)) {
            throw new HDIException("Failed to get storage account");
        }

        Matcher matcher = PATTERN_DEFAULT_STORAGE.matcher(responseMessage);

        String defaultContainer = "";
        if (matcher.find()) {
            defaultContainer = matcher.group(1);
        }

        if (StringHelper.isNullOrWhiteSpace(defaultContainer)) {
            throw new HDIException("Failed to get default container for storage account");
        }

        HDStorageAccount account = new HDStorageAccount(null, storageName + BLOB_URL_SUFFIX, storageKey, true, defaultContainer);

        //getting container to check the storage key is correct or not
        try {
            StorageClientSDKManager.getManager().getBlobContainers(account.getConnectionString());
        } catch (AzureCmdException e) {
            throw new AzureCmdException("Invalid Storage Key");
        }

        return account;
    }

    private static String getMessageByAmbari(String clusterName, String userName, String passwd) throws HDIException {

        String linuxClusterConfigureFileUrl = String.format(clusterConfigureFileUrl, clusterName, clusterName);

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName.trim(), passwd));
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();

        CloseableHttpResponse response = null;
        int responseCode = -1;

        try {
            response = tryGetHttpResponse(httpClient, linuxClusterConfigureFileUrl);
        } catch (UnknownHostException e1) {
            throw new HDIException("Invalid Cluster Name");
        } catch (Exception e3) {
            throw new HDIException("Something wrong with the cluster! Please try again later");
        }

        responseCode = response.getStatusLine().getStatusCode();
        if (responseCode == 200) {
            try {
                return StreamUtil.getResultFromHttpResponse(response).getMessage();
            } catch (IOException e) {
                throw new HDIException("Not support cluster");
            }
        } else if (responseCode == 401 || responseCode == 403) {
            throw new HDIException("Invalid Cluster Name or Password");
        } else {
            throw new HDIException("Something wrong with the cluster! Please try again later");
        }
    }

    //not using for now.
    private static List<ClientStorageAccount> getStorageAccountsFromResponseMessage(String responseMessage) throws StorageAccountResolveException {

        responseMessage = responseMessage.replace(" ", "");
        Matcher matcher = PATTERN_DEFAULT_STORAGE.matcher(responseMessage);
        String defaultStorageName = "";
        try {
            if (matcher.find()) {
                String str = matcher.group();
                defaultStorageName = str.split("[@.]")[2];
            }
        } catch (Exception e) {
            throw new StorageAccountResolveException();
        }

        matcher = PATTER_STORAGE_KEY.matcher(responseMessage);
        HashMap<String, String> storageKeysMap = new HashMap<String, String>();

        while (matcher.find()) {
            String str = matcher.group();
            String[] strs = str.replace("\"", "").split(":");
            String storageName = strs[0].split("\\.")[4];

            storageKeysMap.put(storageName, strs[1]);
        }

        if (StringHelper.isNullOrWhiteSpace(defaultStorageName) || !storageKeysMap.containsKey(defaultStorageName)) {
            throw new StorageAccountResolveException();
        }

        List<ClientStorageAccount> storageAccounts = new ArrayList<ClientStorageAccount>();
        storageAccounts.add(new HDStorageAccount(null, defaultStorageName, storageKeysMap.get(defaultStorageName), false, null));

        for (String storageName : storageKeysMap.keySet()) {
            if (!storageName.equals(defaultStorageName)) {
                storageAccounts.add(new HDStorageAccount(null, storageName, storageKeysMap.get(storageName), false, null));
            }
        }

        return storageAccounts;
    }

    private static CloseableHttpResponse tryGetHttpResponse(CloseableHttpClient httpClient, String url) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        return httpClient.execute(httpGet);
    }
}
