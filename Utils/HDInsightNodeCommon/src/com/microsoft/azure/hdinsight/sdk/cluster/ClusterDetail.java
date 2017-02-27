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

import com.microsoft.azure.hdinsight.sdk.common.CommonConstant;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.ADLSStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClusterDetail implements IClusterDetail {

    private static final String ADL_HOME_PREFIX = "adl://home";
    private static final String ADLS_HOME_HOST_NAME = "dfs.adls.home.hostname";
    private static final String ADLS_HOME_MOUNTPOINT = "dfs.adls.home.mountpoint";

    private final String WorkerNodeName = "workernode";
    private final String DefaultFS = "fs.defaultFS";
    private final String FSDefaultName = "fs.default.name";
    private final String StorageAccountKeyPrefix = "fs.azure.account.key.";
    private final String StorageAccountNamePattern = "^wasb://(.*)@(.*)$";
    private final String ResourceGroupStartTag = "resourceGroups/";
    private final String ResourceGroupEndTag = "/providers/";

    private SubscriptionDetail subscription;
    private ClusterRawInfo clusterRawInfo;

    private int dataNodes;
    private String userName;
    private String passWord;
    private IHDIStorageAccount defaultStorageAccount;
    private List<HDStorageAccount> additionalStorageAccounts;

    private boolean isConfigInfoAvailable = false;

    public ClusterDetail(SubscriptionDetail paramSubscription, ClusterRawInfo paramClusterRawInfo){
        this.subscription = paramSubscription;
        this.clusterRawInfo = paramClusterRawInfo;
        ExtractInfoFromComputeProfile();
    }

    public boolean isEmulator () { return false; }

    public boolean isConfigInfoAvailable(){
        return isConfigInfoAvailable;
    }

    public String getName(){
        return this.clusterRawInfo.getName();
    }

    @Override
    public String getSparkVersion() {
        ClusterProperties clusterProperties = clusterRawInfo.getProperties();
        if(clusterProperties == null) {
            return null;
        }

        String clusterVersion = clusterProperties.getClusterVersion();
        if(clusterVersion.startsWith("3.3")){
            return "1.5.2";
        } else if(clusterVersion.startsWith("3.4")) {
            return "1.6.2";
        } else if(clusterVersion.startsWith("3.5")){
            ComponentVersion componentVersion = clusterProperties.getClusterDefinition().getComponentVersion();
            return componentVersion == null ? "1.6.2" : componentVersion.getSpark();
        }
        return null;
    }

    public String getState(){
        ClusterProperties clusterProperties = this.clusterRawInfo.getProperties();
        return clusterProperties == null ? null : clusterProperties.getClusterState();
    }

    public String getLocation(){
        return this.clusterRawInfo.getLocation();
    }

    public String getConnectionUrl(){
        return String.format(CommonConstant.DEFAULT_CLUSTER_ENDPOINT, getName());
    }

    public String getCreateDate() {
        ClusterProperties clusterProperties = this.clusterRawInfo.getProperties();
        return clusterProperties == null ? null : clusterProperties.getCreatedDate();
    }

    public ClusterType getType(){
        ClusterType type =  null;
        try {
            type = ClusterType.valueOf(this.clusterRawInfo.getProperties().getClusterDefinition().getKind().toLowerCase());
        } catch (IllegalArgumentException e) {
            type = ClusterType.unkown;
        }
        return type == null ? ClusterType.unkown : type;
    }

    public String getResourceGroup(){
        String clusterId = clusterRawInfo.getId();
        int rgNameStart = clusterId.indexOf(ResourceGroupStartTag) + ResourceGroupStartTag.length();
        int rgNameEnd = clusterId.indexOf(ResourceGroupEndTag);
        if (rgNameStart != -1 && rgNameEnd != -1 && rgNameEnd > rgNameStart)
        {
            return clusterId.substring(rgNameStart, rgNameEnd);
        }

        return null;
    }

    public String getVersion(){
        ClusterProperties clusterProperties = this.clusterRawInfo.getProperties();
        return clusterProperties == null ? null : clusterProperties.getClusterVersion();
    }

    public SubscriptionDetail getSubscription(){
        return subscription;
    }

    public int getDataNodes(){
        return dataNodes;
    }

    public String getHttpUserName() throws HDIException {
        if(userName == null){
            throw new HDIException("username is null, please call getConfigurationInfo first");
        }

        return userName;
    }

    public String getHttpPassword() throws HDIException{
        if(passWord == null){
            throw new HDIException("passWord is null, please call getConfigurationInfo first");
        }

        return passWord;
    }

    public String getOSType(){
        ClusterProperties clusterProperties = this.clusterRawInfo.getProperties();
        return clusterProperties == null ? null : clusterProperties.getOsType();
    }

    public IHDIStorageAccount getStorageAccount() throws HDIException{
        if(defaultStorageAccount == null){
            throw new HDIException("default storage account is null, please call getConfigurationInfo first");
        }

        return this.defaultStorageAccount;
    }

    public List<HDStorageAccount> getAdditionalStorageAccounts(){
        return this.additionalStorageAccounts;
    }

    private void ExtractInfoFromComputeProfile(){
        List<Role> roles = this.clusterRawInfo.getProperties().getComputeProfile().getRoles();
        for(Role role : roles){
            if(role.getName().equals(WorkerNodeName)){
                this.dataNodes = role.getTargetInstanceCount();
                break;
            }
        }
    }

    public void getConfigurationInfo(Object project) throws IOException, HDIException, AzureCmdException {
        IClusterOperation clusterOperation = new ClusterOperationImpl(project);
        ClusterConfiguration clusterConfiguration =
                clusterOperation.getClusterConfiguration(subscription, clusterRawInfo.getId());
        if(clusterConfiguration != null && clusterConfiguration.getConfigurations() != null){
            Configurations configurations = clusterConfiguration.getConfigurations();
            Gateway gateway = configurations.getGateway();
            if(gateway != null){
                this.userName = gateway.getUsername();
                this.passWord = gateway.getPassword();
            }

            Map<String,String> coresSiteMap = configurations.getCoresite();
            if(coresSiteMap!= null){
                this.defaultStorageAccount = getDefaultStorageAccount(coresSiteMap);
                this.additionalStorageAccounts = getAdditionalStorageAccounts(coresSiteMap);
            }
        }

        isConfigInfoAvailable = true;
    }

    private IHDIStorageAccount getDefaultStorageAccount(Map<String, String> coresiteMap) throws HDIException{
        String containerAddress = null;
        if(coresiteMap.containsKey(DefaultFS)){
            containerAddress = coresiteMap.get(DefaultFS);
        }else if(coresiteMap.containsKey(FSDefaultName)){
            containerAddress = coresiteMap.get(FSDefaultName);
        }

        if(containerAddress == null){
            throw new HDIException("Failed to get default storage account");
        }

        //for adls
        if(ADL_HOME_PREFIX.equalsIgnoreCase(containerAddress)) {
            String accountName = "";
            String defaultRootPath = "";
            if(coresiteMap.containsKey(ADLS_HOME_HOST_NAME)) {
                accountName = coresiteMap.get(ADLS_HOME_HOST_NAME).split("\\.")[0];
            }
            if(coresiteMap.containsKey(ADLS_HOME_MOUNTPOINT)) {
                defaultRootPath = coresiteMap.get(ADLS_HOME_MOUNTPOINT);
            }
            return new ADLSStorageAccount(this, accountName, true, defaultRootPath);
        } else {
            String storageAccountName = getStorageAccountName(containerAddress);
            if(storageAccountName == null){
                throw new HDIException("Failed to get default storage account name");
            }

            String defaultContainerName = getDefaultContainerName(containerAddress);

            String keyNameOfDefaultStorageAccountKey = StorageAccountKeyPrefix + storageAccountName;
            String storageAccountKey = null;
            if(coresiteMap.containsKey(keyNameOfDefaultStorageAccountKey)){
                storageAccountKey = coresiteMap.get(keyNameOfDefaultStorageAccountKey);
            }

            if(storageAccountKey == null){
                throw new HDIException("Failed to get default storage account key");
            }

            return new HDStorageAccount(this, storageAccountName, storageAccountKey,true, defaultContainerName);
        }
    }

    private List<HDStorageAccount> getAdditionalStorageAccounts(Map<String, String> coresiteMap){
        if(coresiteMap.size() <= 2)
        {
            return null;
        }

        List<HDStorageAccount> storageAccounts = new ArrayList<>();
        for (Map.Entry<String, String> entry : coresiteMap.entrySet()){
            if(entry.getKey().toLowerCase().equals(DefaultFS) || entry.getKey().toLowerCase().equals(FSDefaultName)){
                continue;
            }

            if(entry.getKey().contains(StorageAccountKeyPrefix)){
                HDStorageAccount account =
                        new HDStorageAccount(this, entry.getKey().substring(StorageAccountKeyPrefix.length()), entry.getValue(), false, null);
                storageAccounts.add(account);
            }
        }

        return storageAccounts;
    }

    private String getStorageAccountName(String containerAddress){
        Pattern r = Pattern.compile(StorageAccountNamePattern);
        Matcher m = r.matcher(containerAddress);
        if(m.find())
        {
            return m.group(2);
        }

        return null;
    }

    private String getDefaultContainerName(String containerAddress){
        Pattern r = Pattern.compile(StorageAccountNamePattern);
        Matcher m = r.matcher(containerAddress);
        if(m.find())
        {
            return m.group(1);
        }

        return null;
    }
}
