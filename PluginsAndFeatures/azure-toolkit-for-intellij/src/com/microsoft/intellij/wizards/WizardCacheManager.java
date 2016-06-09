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
package com.microsoft.intellij.wizards;

import com.intellij.openapi.project.Project;
import com.interopbridges.tools.windowsazure.WindowsAzurePackageType;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.rest.WindowsAzureRestUtils;
import com.microsoft.intellij.ui.components.WindowsAzurePage;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.CloudService;
import com.microsoft.tooling.msservices.model.vm.Location;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.management.compute.models.ServiceCertificateListResponse.Certificate;
import com.microsoftopentechnologies.azurecommons.deploy.model.CertificateUpload;
import com.microsoftopentechnologies.azurecommons.deploy.model.CertificateUploadList;
import com.microsoftopentechnologies.azurecommons.deploy.model.DeployDescriptor;
import com.microsoftopentechnologies.azurecommons.deploy.model.RemoteDesktopDescriptor;
import com.microsoftopentechnologies.azurecommons.deploy.tasks.*;
import com.microsoftopentechnologies.azurecommons.deploy.util.PublishData;
import com.microsoftopentechnologies.azurecommons.deploy.wizard.ConfigurationEventArgs;
import com.microsoftopentechnologies.azurecommons.deploy.wizard.ConfigurationEventListener;
import com.microsoftopentechnologies.azurecommons.deploy.wizard.WizardCacheManagerUtilMethods;
import com.microsoftopentechnologies.azurecommons.exception.RestAPIException;
import com.microsoftopentechnologies.azurecommons.wacommonutil.FileUtil;
import com.microsoftopentechnologies.azurecommons.wacommonutil.PreferenceSetUtil;
import com.microsoftopentechnologies.azuremanagementutil.model.KeyName;
import com.microsoftopentechnologies.azuremanagementutil.model.Subscription;
import com.microsoftopentechnologies.azuremanagementutil.rest.WindowsAzureStorageServices;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

import static com.microsoft.intellij.AzurePlugin.log;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public final class WizardCacheManager {

    private static final WizardCacheManager INSTANCE = new WizardCacheManager();

    private static final List<PublishData> PUBLISHS = new ArrayList<PublishData>();

    private static PublishData currentPublishData;
    private static KeyName currentAccessKey;
    private static String currentStorageService;
    private static String currentHostedService;
    private static String deployFile;
    private static String deployConfigFile;
    private static String deployState;
    private static WindowsAzurePackageType deployMode;
    private static String unpublish;
    private static RemoteDesktopDescriptor remoteDesktopDescriptor;
    private static CertificateUploadList certList;
    private static boolean displayHttpsLink = false;
    private static Map<String, String> publishSettingsPerSubscriptionMap = new HashMap<String, String>();

    public static WizardCacheManager getInstrance() {
        return INSTANCE;
    }

    public static List<CertificateUpload> getList() {
        return certList.getList();
    }

    private WizardCacheManager() {

        WindowsAzurePage.addConfigurationEventListener(new ConfigurationEventListener() {

            @Override
            public void onConfigurationChanged(ConfigurationEventArgs config) {
                try {
                    notifyConfiguration(config);
                } catch (RestAPIException e) {
                    log(message("error"), e);
                }
            }
        });
    }

    public static DeployDescriptor collectConfiguration() {

        DeployDescriptor deployDescriptor = new DeployDescriptor(deployMode,
                currentPublishData.getCurrentSubscription().getId(),
                getCurrentStorageAcount(), currentAccessKey,
                getCurentHostedService(), deployFile, deployConfigFile,
                deployState, remoteDesktopDescriptor,
                checkSchemaVersionAndReturnUrl(),
                unpublish, certList, displayHttpsLink, currentPublishData.getCurrentConfiguration());

        remoteDesktopDescriptor = null;

        return deployDescriptor;
    }

    public static WindowsAzureStorageServices createStorageServiceHelper() {
        return WizardCacheManagerUtilMethods.createStorageServiceHelper(currentPublishData, currentStorageService, currentAccessKey);
    }

    public static List<Location> getLocation() {
        return WizardCacheManagerUtilMethods.getLocation(currentPublishData);
    }

    public static String getCurrentDeplyFile() {
        return deployFile;
    }

    public static boolean getDisplayHttpsLink() {
        return displayHttpsLink;
    }


    public static String getCurrentDeployConfigFile() {
        return deployConfigFile;
    }

    public static String getCurrentDeplyState() {
        return deployState;
    }

    public static String getUnpublish() {
        return unpublish;
    }

    public static RemoteDesktopDescriptor getCurrentRemoteDesktopDescriptor() {
        return remoteDesktopDescriptor;
    }

    public static PublishData getCurrentPublishData() {
        return currentPublishData;
    }

    public static Collection<PublishData> getPublishDatas() {

        return PUBLISHS;
    }

    public static Subscription findSubscriptionByName(String subscriptionName) {
        return WizardCacheManagerUtilMethods.findSubscriptionByName(subscriptionName, PUBLISHS);
    }

    public static PublishData findPublishDataBySubscriptionId(String subscriptionId) {
        return WizardCacheManagerUtilMethods.findPublishDataBySubscriptionId(subscriptionId, PUBLISHS);
    }

    public static String findSubscriptionNameBySubscriptionId(String subscriptionId) {
        return WizardCacheManagerUtilMethods.findSubscriptionNameBySubscriptionId(subscriptionId, PUBLISHS);
    }

    public static void clearSubscriptions() {
        PUBLISHS.clear();
        setCurrentPublishData(null);
    }

    public static void removeSubscription(String subscriptionId) {

        if (subscriptionId == null) {
            return;
        }

        PublishData publishData = findPublishDataBySubscriptionId(subscriptionId);

        if (publishData == null) {
            return;
        }

        List<Subscription> subs = publishData.getPublishProfile().getSubscriptions();
        int index = WizardCacheManagerUtilMethods.getIndexOfPublishData(subscriptionId, PUBLISHS);

        for (int i = 0; i < subs.size(); i++) {
            Subscription s = subs.get(i);

            if (s.getSubscriptionID().equals(subscriptionId)) {
                publishData.getPublishProfile().getSubscriptions().remove(i);
                PUBLISHS.set(index, publishData);
                if (publishData.getPublishProfile().getSubscriptions().size() == 0) {
                    PUBLISHS.remove(publishData);
                    /*
                     * If all subscriptions are removed
					 * set current subscription to null.
					 */
                    setCurrentPublishData(null);
                }
                break;
            }
        }
    }

    public static void changeCurrentSubscription(PublishData publishData, String subscriptionId) {
        WizardCacheManagerUtilMethods.changeCurrentSubscription(publishData, subscriptionId);
    }

    public static StorageAccount getCurrentStorageAcount() {
        return WizardCacheManagerUtilMethods.getCurrentStorageAcount(currentPublishData, currentStorageService);
    }

    public static CloudService getCurentHostedService() {
        return WizardCacheManagerUtilMethods.getCurentHostedService(currentPublishData, currentHostedService);
    }

    public static CloudService getHostedServiceFromCurrentPublishData(final String hostedServiceName) {
        return WizardCacheManagerUtilMethods.getHostedServiceFromCurrentPublishData(hostedServiceName, currentPublishData);
    }

    /**
     * Method uses REST API and returns already uploaded certificates
     * from currently selected cloud service on wizard.
     *
     * @return
     */
    public static List<Certificate> fetchUploadedCertificates() throws AzureCmdException {
        return WizardCacheManagerUtilMethods.fetchUploadedCertificates(currentPublishData, currentHostedService);
    }

    public static CloudService createHostedService(final String hostedServiceName, final String label, final String location, final String description)
            throws Exception {
        CloudService cloudService = new CloudService(hostedServiceName, location, "", currentPublishData.getCurrentSubscription().getId(), description);
        CloudService hostedService = WizardCacheManagerUtilMethods.createHostedService(cloudService, currentPublishData);
        currentPublishData.getServicesPerSubscription().get(currentPublishData.getCurrentSubscription().getId()).add(hostedService);
        return hostedService;
    }

    public static StorageAccount createStorageAccount(String name, String label, String location, String description) throws Exception {
        Subscription subscription = currentPublishData.getCurrentSubscription();
        com.microsoft.tooling.msservices.model.storage.StorageAccount storageAccount =
                WizardCacheManagerUtilMethods.createStorageAccount(name, label, location, description, currentPublishData, AzurePlugin.prefFilePath);
        // remove previous mock if existed
        for (com.microsoft.tooling.msservices.model.storage.StorageAccount sa : currentPublishData.getStoragesPerSubscription().get(subscription.getId())) {
            if (name.equals(sa.getName())) {
                currentPublishData.getStoragesPerSubscription().get(subscription.getId()).remove(sa);
                break;
            }
        }
        currentPublishData.getStoragesPerSubscription().get(subscription.getId()).add(storageAccount);
        return storageAccount;
    }

    public static boolean isHostedServiceNameAvailable(final String hostedServiceName) throws Exception {
        return WizardCacheManagerUtilMethods.isHostedServiceNameAvailable(hostedServiceName, currentPublishData);
    }

    public static boolean isStorageAccountNameAvailable(final String storageAccountName) throws Exception {
        return WizardCacheManagerUtilMethods.isStorageAccountNameAvailable(storageAccountName, currentPublishData);
    }

    public static StorageAccount createStorageServiceMock(String storageAccountNameToCreate, String storageAccountLocation, String description) {
        StorageAccount storageService = WizardCacheManagerUtilMethods.createStorageServiceMock(storageAccountNameToCreate, storageAccountLocation, description);

        currentPublishData.getStoragesPerSubscription().get(currentPublishData.getCurrentSubscription().getId()).add(storageService);
        return storageService;
    }

    public static CloudService createHostedServiceMock(String hostedServiceNameToCreate, String hostedServiceLocation, String description) {
        Subscription subscription = currentPublishData.getCurrentSubscription();
        CloudService hostedService = WizardCacheManagerUtilMethods.createHostedServiceMock(hostedServiceNameToCreate, hostedServiceLocation, description);
        currentPublishData.getServicesPerSubscription().get(subscription.getId()).add(hostedService);
        return hostedService;
    }

    public static List<CloudService> getHostedServices() {
        return WizardCacheManagerUtilMethods.getHostedServices(currentPublishData);
    }

    private void notifyConfiguration(ConfigurationEventArgs config) throws RestAPIException {
        if (ConfigurationEventArgs.DEPLOY_FILE.equals(config.getKey())) {
            deployFile = config.getValue().toString();
        } else if (ConfigurationEventArgs.DEPLOY_CONFIG_FILE.equals(config.getKey())) {
            deployConfigFile = config.getValue().toString();
        } else if (ConfigurationEventArgs.DEPLOY_STATE.equals(config.getKey())) {
            deployState = config.getValue().toString();
        } else if (ConfigurationEventArgs.SUBSCRIPTION.equals(config.getKey())) {
            PublishData publishData = (PublishData) config.getValue();
            if (publishData.isInitialized() == false && publishData.isInitializing().compareAndSet(false, true)) {
//				CacheAccountWithProgressWindow settings = new CacheAccountWithProgressWindow(null, publishData, Display.getDefault().getActiveShell(), null);
//				Display.getDefault().syncExec(settings);
            }
        } else if (ConfigurationEventArgs.HOSTED_SERVICE.equals(config.getKey())) {
            CloudService hostedService = (CloudService) config.getValue();
            if (hostedService != null)
                currentHostedService = hostedService.getName();
        } else if (ConfigurationEventArgs.STORAGE_ACCOUNT.equals(config.getKey())) {

            StorageAccount storageService = (StorageAccount) config.getValue();

            if (storageService != null) {
                currentStorageService = storageService.getName();
            }
        } else if (ConfigurationEventArgs.REMOTE_DESKTOP.equals(config.getKey())) {
            remoteDesktopDescriptor = (RemoteDesktopDescriptor) config.getValue();
        } else if (ConfigurationEventArgs.CERTIFICATES.equals(config.getKey())) {
            certList = (CertificateUploadList) config.getValue();
        } else if (ConfigurationEventArgs.DEPLOY_MODE.equals(config.getKey())) {
            deployMode = (WindowsAzurePackageType) config.getValue();
        } else if (ConfigurationEventArgs.UN_PUBLISH.equals(config.getKey())) {
            unpublish = config.getValue().toString();
        } else if (ConfigurationEventArgs.STORAGE_ACCESS_KEY.equals(config.getKey())) {
            String value = config.getValue().toString();

            if (value != null && !value.isEmpty()) {
                currentAccessKey = KeyName.valueOf(value);
            } else {
                currentAccessKey = KeyName.Primary;
            }
        } else if (ConfigurationEventArgs.CONFIG_HTTPS_LINK.equals(config.getKey())) {
            String value = config.getValue().toString();

            if (value != null && !value.isEmpty()) {
                displayHttpsLink = Boolean.parseBoolean(value.trim());
            }
        }
    }

    public static CloudService getHostedServiceWithDeployments(CloudService hostedService, Object projectObject) throws Exception {
        return WizardCacheManagerUtilMethods.getHostedServiceWithDeployments(hostedService, projectObject);
    }

    public static void setCurrentPublishData(PublishData currentSubscription2) {
        currentPublishData = currentSubscription2;
    }

    public static void cachePublishData(File publishSettingsFile, PublishData publishData, LoadingAccoutListener listener, Project project) throws RestAPIException, IOException {
        boolean canceled = false;
        List<Subscription> subscriptions = null;
        int OPERATIONS_TIMEOUT = 60 * 5;

        if (publishData == null) {
            return;
        } else {
            subscriptions = publishData.getPublishProfile().getSubscriptions();
        }

        if (subscriptions == null) {
            return;
        }
        String schemaVer = publishData.getPublishProfile().getSchemaVersion();
        boolean isNewSchema = schemaVer != null && !schemaVer.isEmpty() && schemaVer.equalsIgnoreCase("2.0");
        // URL if schema version is 1.0
        String url = publishData.getPublishProfile().getUrl();
        Map<String, Configuration> configurationPerSubscription = new HashMap<String, Configuration>();
        for (Subscription subscription : subscriptions) {
            if (isNewSchema) {
                // publishsetting file is of schema version 2.0
                url = subscription.getServiceManagementUrl();
            }
            if (url == null || url.isEmpty()) {
                try {
                    url = PreferenceSetUtil.getManagementURL(PreferenceSetUtil.getSelectedPreferenceSetName(AzurePlugin.prefFilePath), AzurePlugin.prefFilePath);
                    url = url.substring(0, url.lastIndexOf("/"));
                } catch (Exception e) {
                    log(e.getMessage());
                }
            }
            // We always import subscription for Azure Explorer and create java keystore first, so no need to create it again
//            Configuration configuration = (publishSettingsFile == null) ?
//                    WindowsAzureRestUtils.loadConfiguration(subscription.getId(), url) :
//                    WindowsAzureRestUtils.getConfiguration(publishSettingsFile, subscription.getId());
            Configuration configuration = WindowsAzureRestUtils.loadConfiguration(subscription.getId(), url);
            configurationPerSubscription.put(subscription.getId(), configuration);
            if (publishSettingsFile != null) {
                //copy file to user home
                String outFile = System.getProperty("user.home") + File.separator + ".azure" + File.separator + publishSettingsFile.getName();
                try {
                    // copy file to user home
                    FileUtil.writeFile(new FileInputStream(publishSettingsFile), new FileOutputStream(outFile));
                    // put an entry into global cache
                    publishSettingsPerSubscriptionMap.put(subscription.getId(), outFile);
                } catch (IOException e) {
                    // Ignore error
                    e.printStackTrace();
                }
            }
        }
        publishData.setConfigurationPerSubscription(configurationPerSubscription);

        if (publishData.isInitialized() == false && publishData.isInitializing().compareAndSet(false, true)) {

            List<Future<?>> loadServicesFutures = null;
//            Future<?> loadSubscriptionsFuture = null;
            try {
                List<Subscription> subBackup = publishData.getPublishProfile().getSubscriptions();
//
//                // thread pool size is number of subscriptions
//                ScheduledExecutorService subscriptionThreadPool = Executors.newScheduledThreadPool(subscriptions.size());
//
//                LoadingSubscriptionTask loadingSubscriptionTask = new LoadingSubscriptionTask(publishData);
//                loadingSubscriptionTask.setSubscriptionIds(subscriptions);
//                if (listener != null) {
//                    loadingSubscriptionTask.addLoadingAccountListener(listener);
//                }
//
//                loadSubscriptionsFuture = subscriptionThreadPool.submit(new LoadingTaskRunner(loadingSubscriptionTask));
//                loadSubscriptionsFuture.get(OPERATIONS_TIMEOUT, TimeUnit.SECONDS);
                subscriptions.clear();
                List<com.microsoft.tooling.msservices.model.Subscription> loadedSubscriptions = AzureManagerImpl.getManager(project).getSubscriptionList();
                for (com.microsoft.tooling.msservices.model.Subscription subscription : loadedSubscriptions) {
                    Subscription profileSubscription =
                            new com.microsoftopentechnologies.azuremanagementutil.model.Subscription();
                    profileSubscription.setSubscriptionID(subscription.getId());
                    profileSubscription.setSubscriptionName(subscription.getName());
                    profileSubscription.setMaxStorageAccounts(subscription.getMaxStorageAccounts());
                    profileSubscription.setMaxHostedServices(subscription.getMaxHostedServices());
                    profileSubscription.setServiceManagementUrl(subscription.getServiceManagementUrl());
                    publishData.getPublishProfile().getSubscriptions().add(profileSubscription);
                }
                if (!loadedSubscriptions.isEmpty() && !(listener == null)) {
                    listener.onLoadedSubscriptions();
                }
				/*
                 * add explicitly management URL and certificate which was removed
				 * Changes are did to support both publish setting schema versions.
				 */
                if (isNewSchema) {
                    for (int i = 0; i < subBackup.size(); i++) {
                        publishData.getPublishProfile().getSubscriptions().get(i).
                                setServiceManagementUrl(subBackup.get(i).getServiceManagementUrl());
                        publishData.getPublishProfile().getSubscriptions().get(i).
                                setManagementCertificate(subBackup.get(i).getManagementCertificate());
                    }
                }

                if (publishData.getCurrentSubscription() == null && publishData.getPublishProfile().getSubscriptions().size() > 0) {
                    publishData.setCurrentSubscription(publishData.getPublishProfile().getSubscriptions().get(0));
                }

                // thread pool size is 3 to load hosted services, locations and storage accounts.
                ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(3);
                loadServicesFutures = new ArrayList<Future<?>>();

                // Hosted services
                LoadingHostedServicesTask loadingHostedServicesTask = new LoadingHostedServicesTask(publishData, project);
                if (listener != null) {
                    loadingHostedServicesTask.addLoadingAccountListener(listener);
                }
                Future<?> submitHostedServices = threadPool.submit(new LoadingTaskRunner(loadingHostedServicesTask));
                loadServicesFutures.add(submitHostedServices);

                // locations
                LoadingLocationsTask loadingLocationsTask = new LoadingLocationsTask(publishData, project);
                if (listener != null) {
                    loadingLocationsTask.addLoadingAccountListener(listener);
                }
                Future<?> submitLocations = threadPool.submit(new LoadingTaskRunner(loadingLocationsTask));
                loadServicesFutures.add(submitLocations);

                // storage accounts
                LoadingStorageAccountTask loadingStorageAccountTask = new LoadingStorageAccountTask(publishData, project);
                if (listener != null) {
                    loadingStorageAccountTask.addLoadingAccountListener(listener);
                }
                Future<?> submitStorageAccounts = threadPool.submit(new LoadingTaskRunner(loadingStorageAccountTask));
                loadServicesFutures.add(submitStorageAccounts);

                for (Future<?> future : loadServicesFutures) {
                    future.get(OPERATIONS_TIMEOUT, TimeUnit.SECONDS);
                }

                try {
                    String chinaMngmntUrl = PreferenceSetUtil.getManagementURL("windowsazure.cn (China)", AzurePlugin.prefFilePath);
                    chinaMngmntUrl = chinaMngmntUrl.substring(0, chinaMngmntUrl.lastIndexOf("/"));
                    if (url.equals(chinaMngmntUrl)) {
                        for (Subscription sub : publishData.getPublishProfile().getSubscriptions()) {
                            List<StorageAccount> services = publishData.getStoragesPerSubscription().get(sub.getId());
                            for (StorageAccount strgService : services) {
                                if (strgService.getBlobsUri().startsWith("https://")) {
                                    strgService.setBlobsUri(strgService.getBlobsUri().replaceFirst("https://", "http://"));
                                }
                                if (strgService.getQueuesUri().startsWith("https://")) {
                                    strgService.setQueuesUri(strgService.getQueuesUri().replaceFirst("https://", "http://"));
                                }
                                if (strgService.getTablesUri().startsWith("https://")) {
                                    strgService.setTablesUri(strgService.getTablesUri().replaceFirst("https://", "http://"));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore error
                }
            } catch (Exception e) {
//                if (loadSubscriptionsFuture != null) {
//                    loadSubscriptionsFuture.cancel(true);
//                }
                if (loadServicesFutures != null) {
                    for (Future<?> future : loadServicesFutures) {
                        future.cancel(true);
                    }
                }
                PluginUtil.displayErrorDialogInAWTAndLog("Error caching subscriptions", "Error caching subscriptions", e);
                canceled = true;
            }
        }


        if (publishData.getPublishProfile().getSubscriptions().size() > 0) {
            if (!empty(publishData) && !canceled) {
                removeDuplicateSubscriptions(publishData);
                PUBLISHS.add(publishData);
                publishData.isInitializing().compareAndSet(true, false);
                currentPublishData = publishData;
            }
        }
    }

    private static void removeDuplicateSubscriptions(PublishData publishData) {

        Set<String> subscriptionIdsToRemove = new HashSet<String>();

        List<Subscription> subscriptionsOfPublishDataToCache = publishData.getPublishProfile().getSubscriptions();
        for (Subscription subscriptionOfPublishDataToCache : subscriptionsOfPublishDataToCache) {
            for (PublishData pd : PUBLISHS) {
                for (Subscription existingSubscription : pd.getPublishProfile().getSubscriptions()) {
                    if (existingSubscription.getId().equals(subscriptionOfPublishDataToCache.getId())) {
                        subscriptionIdsToRemove.add(existingSubscription.getId());
                    }
                }
            }
        }

        for (String subscriptionId : subscriptionIdsToRemove) {
            removeSubscription(subscriptionId);
        }

        List<PublishData> emptyPublishDatas = new ArrayList<PublishData>();
        for (PublishData pd : PUBLISHS) {
            if (pd.getPublishProfile().getSubscriptions().isEmpty()) {
                emptyPublishDatas.add(pd);
            }
        }

        for (PublishData emptyData : emptyPublishDatas) {
            PUBLISHS.remove(emptyData);
        }
    }

    private static boolean empty(PublishData data) {
        return WizardCacheManagerUtilMethods.empty(data);
    }

    public static StorageAccount getStorageAccountFromCurrentPublishData(String storageAccountName) {
        return WizardCacheManagerUtilMethods.getStorageAccountFromCurrentPublishData(storageAccountName, currentPublishData);
    }

    private static String checkSchemaVersionAndReturnUrl() {
        return WizardCacheManagerUtilMethods.checkSchemaVersionAndReturnUrl(currentPublishData);
    }

    public static String getPublishSettingsPath(String subscriptionID) {
        return publishSettingsPerSubscriptionMap.get(subscriptionID);
    }

    public static Map<String, String> getPublishSettingsPerSubscription() {
        return publishSettingsPerSubscriptionMap;
    }

    public static void addPublishSettingsPerSubscription(Map<String, String> publishSettingsPerSubscription) {
        publishSettingsPerSubscriptionMap.putAll(publishSettingsPerSubscription);
    }
}
