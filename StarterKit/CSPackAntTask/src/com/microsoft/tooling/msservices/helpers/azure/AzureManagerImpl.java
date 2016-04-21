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
package com.microsoft.tooling.msservices.helpers.azure;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Optional;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.microsoft.applicationinsights.management.rest.ApplicationInsightsManagementClient;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.azure.management.resources.ResourceManagementClient;
import com.microsoft.azure.management.resources.models.ResourceGroupExtended;
import com.microsoft.azure.management.websites.WebSiteManagementClient;
import com.microsoft.azure.management.websites.models.WebHostingPlan;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.components.PluginSettings;
import com.microsoft.tooling.msservices.helpers.IDEHelper.ArtifactDescriptor;
import com.microsoft.tooling.msservices.helpers.IDEHelper.ProjectDescriptor;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.Nullable;
import com.microsoft.tooling.msservices.helpers.XmlHelper;
import com.microsoft.tooling.msservices.helpers.auth.AADManagerImpl;
import com.microsoft.tooling.msservices.helpers.auth.UserInfo;
import com.microsoft.tooling.msservices.helpers.azure.rest.AzureAADHelper;
import com.microsoft.tooling.msservices.helpers.azure.rest.AzureCertificateHelper;
import com.microsoft.tooling.msservices.helpers.azure.rest.MobileServiceRestManager;
import com.microsoft.tooling.msservices.helpers.azure.rest.RestServiceManager.ContentType;
import com.microsoft.tooling.msservices.helpers.azure.rest.RestServiceManagerBaseImpl;
import com.microsoft.tooling.msservices.helpers.azure.rest.model.CustomAPIData;
import com.microsoft.tooling.msservices.helpers.azure.rest.model.JobData;
import com.microsoft.tooling.msservices.helpers.azure.rest.model.LogData;
import com.microsoft.tooling.msservices.helpers.azure.rest.model.MobileServiceData;
import com.microsoft.tooling.msservices.helpers.azure.rest.model.TableColumnData;
import com.microsoft.tooling.msservices.helpers.azure.rest.model.TableData;
import com.microsoft.tooling.msservices.helpers.azure.rest.model.TablePermissionsData;
import com.microsoft.tooling.msservices.helpers.azure.rest.model.TableScriptData;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKHelper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.SDKRequestCallback;
import com.microsoft.tooling.msservices.helpers.tasks.CancellableTask;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.ms.Column;
import com.microsoft.tooling.msservices.model.ms.CustomAPI;
import com.microsoft.tooling.msservices.model.ms.CustomAPIPermissions;
import com.microsoft.tooling.msservices.model.ms.Job;
import com.microsoft.tooling.msservices.model.ms.LogEntry;
import com.microsoft.tooling.msservices.model.ms.MobileService;
import com.microsoft.tooling.msservices.model.ms.PermissionItem;
import com.microsoft.tooling.msservices.model.ms.Script;
import com.microsoft.tooling.msservices.model.ms.SqlDb;
import com.microsoft.tooling.msservices.model.ms.SqlServer;
import com.microsoft.tooling.msservices.model.ms.Table;
import com.microsoft.tooling.msservices.model.ms.TablePermissions;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.AffinityGroup;
import com.microsoft.tooling.msservices.model.vm.CloudService;
import com.microsoft.tooling.msservices.model.vm.Location;
import com.microsoft.tooling.msservices.model.vm.VirtualMachine;
import com.microsoft.tooling.msservices.model.vm.VirtualMachineImage;
import com.microsoft.tooling.msservices.model.vm.VirtualMachineSize;
import com.microsoft.tooling.msservices.model.vm.VirtualNetwork;
import com.microsoft.tooling.msservices.model.ws.WebHostingPlanCache;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSiteConfiguration;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings.FTPPublishProfile;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings.PublishProfile;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper.EventWaitHandle;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.core.OperationStatusResponse;
import com.microsoft.windowsazure.management.ManagementClient;
import com.microsoft.windowsazure.management.compute.ComputeManagementClient;
import com.microsoft.windowsazure.management.compute.models.DeploymentCreateParameters;
import com.microsoft.windowsazure.management.compute.models.DeploymentGetResponse;
import com.microsoft.windowsazure.management.compute.models.DeploymentSlot;
import com.microsoft.windowsazure.management.compute.models.ServiceCertificateListResponse;
import com.microsoft.windowsazure.management.models.SubscriptionGetResponse;
import com.microsoft.windowsazure.management.network.NetworkManagementClient;
import com.microsoft.windowsazure.management.storage.StorageManagementClient;
import com.microsoftopentechnologies.azuremanagementutil.rest.SubscriptionTransformer;

import sun.misc.BASE64Encoder;

public class AzureManagerImpl extends AzureManagerBaseImpl implements AzureManager {
    private interface AzureSDKClientProvider<V extends Closeable> {
        @NotNull
        V getSSLClient(@NotNull Subscription subscription)
                throws Throwable;

        @NotNull
        V getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                throws Throwable;
    }

    private static AzureManager instance;

    private String accessToken; // this field to be used from cspack ant task only

    private ReentrantReadWriteLock subscriptionsChangedLock = new ReentrantReadWriteLock(true);

    private AzureManagerImpl() {
        authDataLock.writeLock().lock();

        try {
            aadManager = AADManagerImpl.getManager();

            loadSubscriptions();
            loadUserInfo();
            loadSSLSocketFactory(); // todo????

            removeInvalidUserInfo();
            removeUnusedSubscriptions();

            storeSubscriptions();
            storeUserInfo();

            accessTokenByUser = new HashMap<UserInfo, String>();
            lockByUser = new HashMap<UserInfo, ReentrantReadWriteLock>();
            subscriptionsChangedHandles = new HashSet<EventWaitHandleImpl>();
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    private AzureManagerImpl(String accessToken) {
        authDataLock.writeLock().lock();

        try {
//            aadManager = AADManagerImpl.getManager();

            this.accessToken = accessToken;
//            loadSubscriptions();
//            loadUserInfo();
//            loadSSLSocketFactory(); // todo????
//
//            removeInvalidUserInfo();
//            removeUnusedSubscriptions();
//
//            storeSubscriptions();
//            storeUserInfo();
//
//            accessTokenByUser = new HashMap<UserInfo, String>();
//            lockByUser = new HashMap<UserInfo, ReentrantReadWriteLock>();
//            subscriptionsChangedHandles = new HashSet<EventWaitHandleImpl>();
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    /**
     * This method for now is supposed to be used from cspack ant task only
     */
    public static synchronized AzureManager initManager(String accessToken) {
        instance = new AzureManagerImpl(accessToken);
        return instance;
    }

    @NotNull
    public static synchronized AzureManager getManager() {
        if (instance == null) {
            gson = new GsonBuilder().enableComplexMapKeySerialization().create();
            instance = new AzureManagerImpl();
        }

        return instance;
    }

    @Override
    public void authenticate() throws AzureCmdException {
        final PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();
        final String managementUri = settings.getAzureServiceManagementUri();

        final UserInfo userInfo = aadManager.authenticate(managementUri, "Sign in to your Azure account");
        setUserInfo(userInfo);

        List<Subscription> subscriptions = requestWithToken(userInfo, new RequestCallback<List<Subscription>>() {
            @Override
            public List<Subscription> execute()
                    throws Throwable {
                String accessToken = getAccessToken(userInfo);
                String subscriptionsXML = AzureAADHelper.executeRequest(managementUri,
                        "subscriptions",
                        ContentType.Json,
                        "GET",
                        null,
                        accessToken,
                        new RestServiceManagerBaseImpl() {
                            @NotNull
                            @Override
                            public String executePollRequest(@NotNull String managementUrl,
                                                             @NotNull String path,
                                                             @NotNull ContentType contentType,
                                                             @NotNull String method,
                                                             @Nullable String postData,
                                                             @NotNull String pollPath,
                                                             @NotNull HttpsURLConnectionProvider sslConnectionProvider)
                                    throws AzureCmdException {
                                throw new UnsupportedOperationException();
                            }
                        });

                return parseSubscriptionsXML(subscriptionsXML);
            }
        });

        for (Subscription subscription : subscriptions) {
            UserInfo subscriptionUser = new UserInfo(subscription.getTenantId(), userInfo.getUniqueName());
            aadManager.authenticate(subscriptionUser, managementUri, "Sign in to your Azure account");

            updateSubscription(subscription, subscriptionUser);
        }
    }

    @Override
    public boolean authenticated() {
        return getUserInfo() != null;
    }

    @Override
    public boolean authenticated(@NotNull String subscriptionId) {
        return !hasSSLSocketFactory(subscriptionId) && hasUserInfo(subscriptionId);
    }

    @Override
    public void clearAuthentication() {
        setUserInfo(null);
    }

    @Override
    public void importPublishSettingsFile(@NotNull String publishSettingsFilePath)
            throws AzureCmdException {
        List<Subscription> subscriptions = importSubscription(publishSettingsFilePath);

        for (Subscription subscription : subscriptions) {
            try {
                SSLSocketFactory sslSocketFactory = initSSLSocketFactory(subscription.getManagementCertificate());
                updateSubscription(subscription, sslSocketFactory);
            } catch (Exception ex) {
                throw new AzureCmdException("Error importing publish settings", ex);
            }
        }
    }

    @Override
    public boolean usingCertificate() {
        authDataLock.readLock().lock();

        try {
            return sslSocketFactoryBySubscriptionId.size() > 0;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @Override
    public boolean usingCertificate(@NotNull String subscriptionId) {
        return hasSSLSocketFactory(subscriptionId);
    }

    @Override
    public void clearImportedPublishSettingsFiles() {
        authDataLock.writeLock().lock();

        try {
            sslSocketFactoryBySubscriptionId.clear();
            removeUnusedSubscriptions();
            storeSubscriptions();
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    @NotNull
    @Override
    public List<Subscription> getFullSubscriptionList()
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            List<Subscription> result = new ArrayList<Subscription>();

            for (Subscription subscription : subscriptions.values()) {
                result.add(subscription);
            }

            return result;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    @Override
    public List<Subscription> getSubscriptionList() {
        authDataLock.readLock().lock();

        try {
            List<Subscription> result = new ArrayList<Subscription>();

            for (Subscription subscription : subscriptions.values()) {
                if (subscription.isSelected()) {
                    result.add(subscription);
                }
            }

            return result;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @Override
    public void setSelectedSubscriptions(@NotNull List<String> selectedList)
            throws AzureCmdException {
        authDataLock.writeLock().lock();

        try {
            for (String subscriptionId : subscriptions.keySet()) {
                Subscription subscription = subscriptions.get(subscriptionId);
                subscription.setSelected(selectedList.contains(subscriptionId));
            }

            storeSubscriptions();
        } finally {
            authDataLock.writeLock().unlock();
        }

        notifySubscriptionsChanged();
    }

    @NotNull
    @Override
    public EventWaitHandle registerSubscriptionsChanged()
            throws AzureCmdException {
        subscriptionsChangedLock.writeLock().lock();

        try {
            EventWaitHandleImpl handle = new EventWaitHandleImpl();

            subscriptionsChangedHandles.add(handle);

            return handle;
        } finally {
            subscriptionsChangedLock.writeLock().unlock();
        }
    }

    @Override
    public void unregisterSubscriptionsChanged(@NotNull EventWaitHandle handle)
            throws AzureCmdException {
        if (!(handle instanceof EventWaitHandleImpl)) {
            throw new AzureCmdException("Invalid handle instance");
        }

        subscriptionsChangedLock.writeLock().lock();

        try {
            subscriptionsChangedHandles.remove(handle);
        } finally {
            subscriptionsChangedLock.writeLock().unlock();
        }

        ((EventWaitHandleImpl) handle).signalEvent();
    }

    @NotNull
    @Override
    public List<SqlDb> getSqlDb(@NotNull String subscriptionId, @NotNull SqlServer server)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/sqlservers/servers/%s/databases?contentview=generic",
                    subscriptionId, server.getName());

            String xml = executeGetRequest(subscriptionId, path);

            List<SqlDb> res = new ArrayList<SqlDb>();
            NodeList nl = (NodeList) XmlHelper.getXMLValue(xml, "//ServiceResource", XPathConstants.NODESET);

            for (int i = 0; i != nl.getLength(); i++) {

                SqlDb sqls = new SqlDb();
                sqls.setName(XmlHelper.getChildNodeValue(nl.item(i), "Name"));
                sqls.setEdition(XmlHelper.getChildNodeValue(nl.item(i), "Edition"));
                sqls.setServer(server);
                res.add(sqls);
            }

            return res;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting database list", t);
        }
    }

    @NotNull
    @Override
    public List<SqlServer> getSqlServers(@NotNull String subscriptionId)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/sqlservers/servers", subscriptionId);
            String xml = executeGetRequest(subscriptionId, path);

            List<SqlServer> res = new ArrayList<SqlServer>();

            NodeList nl = (NodeList) XmlHelper.getXMLValue(xml, "//Server", XPathConstants.NODESET);

            for (int i = 0; i != nl.getLength(); i++) {
                SqlServer sqls = new SqlServer();

                sqls.setAdmin(XmlHelper.getChildNodeValue(nl.item(i), "AdministratorLogin"));
                sqls.setName(XmlHelper.getChildNodeValue(nl.item(i), "Name"));
                sqls.setRegion(XmlHelper.getChildNodeValue(nl.item(i), "Location"));
                res.add(sqls);
            }

            return res;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting server list", t);
        }
    }

    @NotNull
    @Override
    public List<MobileService> getMobileServiceList(@NotNull String subscriptionId)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices", subscriptionId);
            String json = executeGetRequest(subscriptionId, path);

            Type type = new TypeToken<ArrayList<MobileServiceData>>() {
            }.getType();
            List<MobileServiceData> tempRes = new Gson().fromJson(json, type);

            List<MobileService> res = new ArrayList<MobileService>();

            for (MobileServiceData item : tempRes) {
                MobileService ser = new MobileService();

                ser.setName(item.getName());
                ser.setType(item.getType());
                ser.setState(item.getState());
                ser.setSelfLink(item.getSelflink());
                ser.setAppUrl(item.getApplicationUrl());
                ser.setAppKey(item.getApplicationKey());
                ser.setMasterKey(item.getMasterKey());
                ser.setWebspace(item.getWebspace());
                ser.setRegion(item.getRegion());
                ser.setMgmtPortalLink(item.getManagementPortalLink());
                ser.setSubcriptionId(subscriptionId);

                if (item.getPlatform() != null && item.getPlatform().equals("dotNet")) {
                    ser.setRuntime(MobileService.NET_RUNTIME);
                } else {
                    ser.setRuntime(MobileService.NODE_RUNTIME);
                }

                for (MobileServiceData.Table table : item.getTables()) {
                    Table t = new Table();
                    t.setName(table.getName());
                    t.setSelfLink(table.getSelflink());
                    ser.getTables().add(t);
                }

                res.add(ser);
            }

            return res;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting service list", t);
        }
    }

    @Override
    public void createMobileService(@NotNull String subscriptionId, @NotNull String region,
                                    @NotNull String username, @NotNull String password,
                                    @NotNull String mobileServiceName,
                                    @Nullable String server, @Nullable String database)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/applications", subscriptionId);

            String JSONParameter;

            if (database == null || server == null) {
                String zumoServerId = UUID.randomUUID().toString().replace("-", "");
                String zumoDBId = UUID.randomUUID().toString().replace("-", "");
                String dbName = mobileServiceName + "_db";

                JSONParameter = "{'SchemaVersion':'2012-05.1.0','Location':'" + region + "','ExternalResources':{},'InternalResources':{'ZumoMobileService':" +
                        "{'ProvisioningParameters':{'Name':'" + mobileServiceName + "','Location':'" + region + "'},'ProvisioningConfigParameters':{'Server':{'StringConcat':" +
                        "[{'ResourceReference':'ZumoSqlServer_" + zumoServerId + ".Name'},'.database.windows.net']},'Database':{'ResourceReference':'ZumoSqlDatabase_" +
                        zumoDBId + ".Name'},'AdministratorLogin':'" + username + "','AdministratorLoginPassword':'" + password + "'},'Version':'2012-05-21.1.0'," +
                        "'Name':'ZumoMobileService','Type':'Microsoft.WindowsAzure.MobileServices.MobileService'},'ZumoSqlServer_" + zumoServerId +
                        "':{'ProvisioningParameters':{'AdministratorLogin':'" + username + "','AdministratorLoginPassword':'" + password + "','Location':'" + region +
                        "'},'ProvisioningConfigParameters':{'FirewallRules':[{'Name':'AllowAllWindowsAzureIps','StartIPAddress':'0.0.0.0','EndIPAddress':'0.0.0.0'}]}," +
                        "'Version':'1.0','Name':'ZumoSqlServer_" + zumoServerId + "','Type':'Microsoft.WindowsAzure.SQLAzure.Server'},'ZumoSqlDatabase_" + zumoDBId +
                        "':{'ProvisioningParameters':{'Name':'" + dbName + "','Edition':'WEB','MaxSizeInGB':'1','DBServer':{'ResourceReference':'ZumoSqlServer_" +
                        zumoServerId + ".Name'},'CollationName':'SQL_Latin1_General_CP1_CI_AS'},'Version':'1.0','Name':'ZumoSqlDatabase_" + zumoDBId +
                        "','Type':'Microsoft.WindowsAzure.SQLAzure.DataBase'}}}";
            } else {
                String zumoServerId = UUID.randomUUID().toString().replace("-", "");
                String zumoDBId = UUID.randomUUID().toString().replace("-", "");

                JSONParameter = "{'SchemaVersion':'2012-05.1.0','Location':'West US','ExternalResources':{'ZumoSqlServer_" + zumoServerId + "':{'Name':'ZumoSqlServer_" + zumoServerId
                        + "'," + "'Type':'Microsoft.WindowsAzure.SQLAzure.Server','URI':'https://management.core.windows.net:8443/" + subscriptionId
                        + "/services/sqlservers/servers/" + server + "'}," + "'ZumoSqlDatabase_" + zumoDBId + "':{'Name':'ZumoSqlDatabase_" + zumoDBId +
                        "','Type':'Microsoft.WindowsAzure.SQLAzure.DataBase'," + "'URI':'https://management.core.windows.net:8443/" + subscriptionId
                        + "/services/sqlservers/servers/" + server + "/databases/" + database + "'}}," + "'InternalResources':{'ZumoMobileService':{'ProvisioningParameters'" +
                        ":{'Name':'" + mobileServiceName + "','Location':'" + region + "'},'ProvisioningConfigParameters':{'Server':{'StringConcat':[{'ResourceReference':'ZumoSqlServer_"
                        + zumoServerId + ".Name'}," + "'.database.windows.net']},'Database':{'ResourceReference':'ZumoSqlDatabase_" + zumoDBId + ".Name'},'AdministratorLogin':" +
                        "'" + username + "','AdministratorLoginPassword':'" + password + "'},'Version':'2012-05-21.1.0','Name':'ZumoMobileService','Type':" +
                        "'Microsoft.WindowsAzure.MobileServices.MobileService'}}}";
            }

            String xmlParameter = String.format("<?xml version=\"1.0\" encoding=\"utf-8\"?><Application xmlns=\"http://schemas.microsoft.com/windowsazure\"><Name>%s</Name>" +
                            "<Label>%s</Label><Description>%s</Description><Configuration>%s</Configuration></Application>",
                    mobileServiceName + "mobileservice", mobileServiceName, mobileServiceName, new BASE64Encoder().encode(JSONParameter.getBytes()));

            executePollRequest(subscriptionId, path, ContentType.Xml, "POST", xmlParameter, String.format("/%s/operations/", subscriptionId));

            String xml = executeGetRequest(subscriptionId, String.format("/%s/applications/%s", subscriptionId, mobileServiceName + "mobileservice"));
            NodeList statusNode = ((NodeList) XmlHelper.getXMLValue(xml, "//Application/State", XPathConstants.NODESET));

            if (!(statusNode.getLength() > 0 && statusNode.item(0).getTextContent().equals("Healthy"))) {
                deleteMobileService(subscriptionId, mobileServiceName);

                String errors = ((String) XmlHelper.getXMLValue(xml, "//FailureCode[text()]", XPathConstants.STRING));
                String errorMessage = ((String) XmlHelper.getXMLValue(errors, "//Message[text()]", XPathConstants.STRING));
                throw new AzureCmdException("Error creating service", errorMessage);
            }
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error creating service", t);
        }
    }

    @Override
    public void deleteMobileService(@NotNull String subscriptionId, @NotNull String mobileServiceName) {
        String mspath = String.format("/%s/services/mobileservices/mobileservices/%s?deletedata=true",
                subscriptionId, mobileServiceName);

        try {
            executePollRequest(subscriptionId, mspath, ContentType.Json, "DELETE", null, String.format("/%s/operations/", subscriptionId));
        } catch (Throwable ignored) {
        }

        String appPath = String.format("/%s/applications/%smobileservice", subscriptionId, mobileServiceName);

        try {
            executePollRequest(subscriptionId, appPath, ContentType.Xml, "DELETE", null, String.format("/%s/operations/", subscriptionId));
        } catch (Throwable ignored) {
        }
    }

    @NotNull
    @Override
    public List<Table> getTableList(@NotNull String subscriptionId, @NotNull String mobileServiceName)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables", subscriptionId, mobileServiceName);
            String json = executeGetRequest(subscriptionId, path);

            Type type = new TypeToken<ArrayList<TableData>>() {
            }.getType();
            List<TableData> tempRes = new Gson().fromJson(json, type);

            List<Table> res = new ArrayList<Table>();

            for (TableData item : tempRes) {
                Table t = new Table();
                t.setName(item.getName());
                t.setSelfLink(item.getSelflink());

                res.add(t);
            }

            return res;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting table list", t);
        }
    }

    @Override
    public void createTable(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String tableName,
                            @NotNull TablePermissions permissions)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables", subscriptionId, mobileServiceName);

            String postData = "{\"insert\":\"" + PermissionItem.getPermitionString(permissions.getInsert())
                    + "\",\"read\":\"" + PermissionItem.getPermitionString(permissions.getRead())
                    + "\",\"update\":\"" + PermissionItem.getPermitionString(permissions.getUpdate())
                    + "\",\"delete\":\"" + PermissionItem.getPermitionString(permissions.getDelete())
                    + "\",\"name\":\"" + tableName + "\",\"idType\":\"string\"}";

            executeRequest(subscriptionId, path, ContentType.Json, "POST", postData);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error creating table", t);
        }
    }

    @Override
    public void updateTable(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String tableName,
                            @NotNull TablePermissions permissions)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables/%s/permissions",
                    subscriptionId, mobileServiceName, tableName);

            String postData = "{\"insert\":\"" + PermissionItem.getPermitionString(permissions.getInsert())
                    + "\",\"read\":\"" + PermissionItem.getPermitionString(permissions.getRead())
                    + "\",\"update\":\"" + PermissionItem.getPermitionString(permissions.getUpdate())
                    + "\",\"delete\":\"" + PermissionItem.getPermitionString(permissions.getDelete())
                    + "\"}";

            executeRequest(subscriptionId, path, ContentType.Json, "PUT", postData);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error updating table", t);
        }
    }

    @NotNull
    @Override
    public Table showTableDetails(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String tableName)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables/%s",
                    subscriptionId, mobileServiceName, tableName);
            String json = executeGetRequest(subscriptionId, path);
            Gson gson = new Gson();
            TableData tempRes = gson.fromJson(json, TableData.class);

            Table t = new Table();
            t.setName(tempRes.getName());
            t.setSelfLink(tempRes.getSelflink());

            TablePermissionsData restTablePermissions = gson.fromJson(executeGetRequest(subscriptionId, path + "/permissions"),
                    TablePermissionsData.class);

            TablePermissions tablePermissions = new TablePermissions();
            tablePermissions.setInsert(PermissionItem.getPermitionType(restTablePermissions.getInsert()));
            tablePermissions.setUpdate(PermissionItem.getPermitionType(restTablePermissions.getUpdate()));
            tablePermissions.setRead(PermissionItem.getPermitionType(restTablePermissions.getRead()));
            tablePermissions.setDelete(PermissionItem.getPermitionType(restTablePermissions.getDelete()));
            t.setTablePermissions(tablePermissions);

            Type colType = new TypeToken<ArrayList<TableColumnData>>() {
            }.getType();
            List<TableColumnData> colList = gson.fromJson(executeGetRequest(subscriptionId, path + "/columns"),
                    colType);
            if (colList != null) {
                for (TableColumnData column : colList) {
                    Column c = new Column();
                    c.setName(column.getName());
                    c.setType(column.getType());
                    c.setSelfLink(column.getSelflink());
                    c.setIndexed(column.isIndexed());
                    c.setZumoIndex(column.isZumoIndex());

                    t.getColumns().add(c);
                }
            }

            Type scrType = new TypeToken<ArrayList<TableScriptData>>() {
            }.getType();
            List<TableScriptData> scrList = gson.fromJson(executeGetRequest(subscriptionId, path + "/scripts"),
                    scrType);

            if (scrList != null) {
                for (TableScriptData script : scrList) {
                    Script s = new Script();

                    s.setOperation(script.getOperation());
                    s.setBytes(script.getSizeBytes());
                    s.setSelfLink(script.getSelflink());
                    s.setName(String.format("%s.%s", tempRes.getName(), script.getOperation()));

                    t.getScripts().add(s);
                }
            }

            return t;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting table data", t);
        }
    }

    @Override
    public void downloadTableScript(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                    @NotNull String scriptName, @NotNull String downloadPath)
            throws AzureCmdException {
        try {
            String tableName = scriptName.split("\\.")[0];
            String operation = scriptName.split("\\.")[1];

            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables/%s/scripts/%s/code",
                    subscriptionId, mobileServiceName, tableName, operation);
            String script = executeGetRequest(subscriptionId, path);

            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(downloadPath), "utf-8"));
            writer.write(script);
            writer.flush();
            writer.close();
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error download script", t);
        }
    }

    @Override
    public void uploadTableScript(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                  @NotNull String scriptName, @NotNull String filePath)
            throws AzureCmdException {
        try {
            String tableName = scriptName.split("\\.")[0];
            String operation = scriptName.split("\\.")[1];

            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables/%s/scripts/%s/code",
                    subscriptionId, mobileServiceName, tableName, operation);
            String file = readFile(filePath);

            executeRequest(subscriptionId, path, ContentType.Text, "PUT", file);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error upload script", t);
        }
    }

    @NotNull
    @Override
    public List<CustomAPI> getAPIList(@NotNull String subscriptionId, @NotNull String mobileServiceName)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis",
                    subscriptionId, mobileServiceName);
            String json = executeGetRequest(subscriptionId, path);

            Type type = new TypeToken<ArrayList<CustomAPIData>>() {
            }.getType();
            List<CustomAPIData> tempRes = new Gson().fromJson(json, type);

            List<CustomAPI> res = new ArrayList<CustomAPI>();

            for (CustomAPIData item : tempRes) {
                CustomAPI c = new CustomAPI();
                c.setName(item.getName());
                CustomAPIPermissions permissions = new CustomAPIPermissions();
                permissions.setPutPermission(PermissionItem.getPermitionType(item.getPut()));
                permissions.setPostPermission(PermissionItem.getPermitionType(item.getPost()));
                permissions.setGetPermission(PermissionItem.getPermitionType(item.getGet()));
                permissions.setDeletePermission(PermissionItem.getPermitionType(item.getDelete()));
                permissions.setPatchPermission(PermissionItem.getPermitionType(item.getPatch()));
                c.setCustomAPIPermissions(permissions);
                res.add(c);
            }

            return res;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting API list", t);
        }
    }

    @Override
    public void downloadAPIScript(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                  @NotNull String scriptName, @NotNull String downloadPath)
            throws AzureCmdException {
        try {
            String apiName = scriptName.split("\\.")[0];

            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis/%s/script",
                    subscriptionId, mobileServiceName, apiName);
            String script = executeGetRequest(subscriptionId, path);

            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(downloadPath), "utf-8"));
            writer.write(script);
            writer.flush();
            writer.close();
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting API list", t);
        }
    }

    @Override
    public void uploadAPIScript(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                @NotNull String scriptName, @NotNull String filePath)
            throws AzureCmdException {
        try {
            String apiName = scriptName.split("\\.")[0];
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis/%s/script",
                    subscriptionId, mobileServiceName, apiName);
            String file = readFile(filePath);

            executeRequest(subscriptionId, path, ContentType.Text, "PUT", file);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error upload script", t);
        }
    }

    @Override
    public void createCustomAPI(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                @NotNull String tableName, @NotNull CustomAPIPermissions permissions)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis",
                    subscriptionId, mobileServiceName);
            String postData = "{\"get\":\"" + permissions.getGetPermission()
                    + "\",\"put\":\"" + permissions.getPutPermission()
                    + "\",\"post\":\"" + permissions.getPostPermission()
                    + "\",\"patch\":\"" + permissions.getPatchPermission()
                    + "\",\"delete\":\"" + permissions.getDeletePermission()
                    + "\",\"name\":\"" + tableName + "\"}";
            executeRequest(subscriptionId, path, ContentType.Json, "POST", postData);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error creating API", t);
        }
    }

    @Override
    public void updateCustomAPI(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                @NotNull String tableName, @NotNull CustomAPIPermissions permissions)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis/%s",
                    subscriptionId, mobileServiceName, tableName);
            String postData = "{\"get\":\"" + permissions.getGetPermission()
                    + "\",\"put\":\"" + permissions.getPutPermission()
                    + "\",\"post\":\"" + permissions.getPostPermission()
                    + "\",\"patch\":\"" + permissions.getPatchPermission()
                    + "\",\"delete\":\"" + permissions.getDeletePermission()
                    + "\"}";
            executeRequest(subscriptionId, path, ContentType.Json, "PUT", postData);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error updating API", t);
        }
    }

    @Override
    public void deleteTable(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                            @NotNull String tableName) throws AzureCmdException {

        String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables/%s",
                subscriptionId, mobileServiceName, tableName);
        try {
            executeRequest(subscriptionId, path, ContentType.Xml, "DELETE", null);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error deleting table", t);
        }
    }

    @Override
    public void deleteCustomApi(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                @NotNull String apiName) throws AzureCmdException {

        String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis/%s",
                subscriptionId, mobileServiceName, apiName);
        try {
            executeRequest(subscriptionId, path, ContentType.Xml, "DELETE", null);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error deleting API", t);
        }
    }

    @Override
    public void deleteJob(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                          @NotNull String jobName) throws AzureCmdException {

        String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs/%s",
                subscriptionId, mobileServiceName, jobName);
        try {
            executeRequest(subscriptionId, path, ContentType.Xml, "DELETE", null);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error deleting job", t);
        }
    }

    @NotNull
    @Override
    public List<Job> listJobs(@NotNull String subscriptionId, @NotNull String mobileServiceName)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs",
                    subscriptionId, mobileServiceName);
            String json = executeGetRequest(subscriptionId, path);

            Type type = new TypeToken<ArrayList<JobData>>() {
            }.getType();
            List<JobData> tempRes = new Gson().fromJson(json, type);

            List<Job> res = new ArrayList<Job>();

            for (JobData item : tempRes) {
                Job j = new Job();
                j.setAppName(item.getAppName());
                j.setName(item.getName());
                j.setEnabled(item.getStatus().equals("enabled"));
                j.setId(UUID.fromString(item.getId()));

                if (item.getIntervalPeriod() > 0) {
                    j.setIntervalPeriod(item.getIntervalPeriod());
                    j.setIntervalUnit(item.getIntervalUnit());
                }

                res.add(j);
            }

            return res;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting job list", t);
        }
    }

    @Override
    public void createJob(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String jobName,
                          int interval, @NotNull String intervalUnit, @NotNull String startDate)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs",
                    subscriptionId, mobileServiceName);
            String postData = "{\"name\":\"" + jobName + "\""
                    + (
                    intervalUnit.equals("none") ? "" : (",\"intervalUnit\":\"" + intervalUnit
                            + "\",\"intervalPeriod\":" + String.valueOf(interval)
                            + ",\"startTime\":\"" + startDate + "\""))
                    + "}";
            executeRequest(subscriptionId, path, ContentType.Json, "POST", postData);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error creating jobs", t);
        }
    }

    @Override
    public void updateJob(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String jobName,
                          int interval, @NotNull String intervalUnit, @NotNull String startDate, boolean enabled)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs/%s",
                    subscriptionId, mobileServiceName, jobName);
            String postData = "{"
                    + "\"status\":\"" + (enabled ? "enabled" : "disabled") + "\""
                    + (
                    intervalUnit.equals("none") ? "" : (",\"intervalUnit\":\"" + intervalUnit
                            + "\",\"intervalPeriod\":" + String.valueOf(interval)
                            + ",\"startTime\":\"" + startDate + "\""))
                    + "}";

            if (intervalUnit.equals("none")) {
                postData = "{\"status\":\"disabled\"}";
            }

            executeRequest(subscriptionId, path, ContentType.Json, "PUT", postData);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error updating job", t);
        }
    }

    @Override
    public void downloadJobScript(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                  @NotNull String scriptName, @NotNull String downloadPath)
            throws AzureCmdException {
        try {
            String jobName = scriptName.split("\\.")[0];

            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs/%s/script",
                    subscriptionId, mobileServiceName, jobName);
            String script = executeGetRequest(subscriptionId, path);

            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(downloadPath), "utf-8"));
            writer.write(script);
            writer.flush();
            writer.close();
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error download script", t);
        }
    }

    @Override
    public void uploadJobScript(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                @NotNull String scriptName, @NotNull String filePath)
            throws AzureCmdException {
        try {
            String jobName = scriptName.split("\\.")[0];
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs/%s/script",
                    subscriptionId, mobileServiceName, jobName);
            String file = readFile(filePath);

            executeRequest(subscriptionId, path, ContentType.Text, "PUT", file);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error upload script", t);
        }
    }

    @NotNull
    @Override
    public List<LogEntry> listLog(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                  @NotNull String runtime)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/logs?$top=10",
                    subscriptionId, mobileServiceName);
            String json = executeGetRequest(subscriptionId, path);

            LogData tempRes = new Gson().fromJson(json, LogData.class);

            List<LogEntry> res = new ArrayList<LogEntry>();

            for (LogData.LogEntry item : tempRes.getResults()) {
                LogEntry logEntry = new LogEntry();

                logEntry.setMessage(item.getMessage());
                logEntry.setSource(item.getSource());
                logEntry.setType(item.getType());

                SimpleDateFormat ISO8601DATEFORMAT;

                if (MobileService.NODE_RUNTIME.equals(runtime)) {
                    ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
                } else {
                    ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
                }
                logEntry.setTimeCreated(ISO8601DATEFORMAT.parse(item.getTimeCreated()));

                res.add(logEntry);
            }

            return res;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting log", t);
        }
    }

    @NotNull
    @Override
    public List<CloudService> getCloudServices(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.getCloudServices(subscriptionId));
    }

    @NotNull
    @Override
    public List<VirtualMachine> getVirtualMachines(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.getVirtualMachines(subscriptionId));
    }

    @NotNull
    @Override
    public VirtualMachine refreshVirtualMachineInformation(@NotNull VirtualMachine vm)
            throws AzureCmdException {
        return requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.refreshVirtualMachineInformation(vm));
    }

    @Override
    public void startVirtualMachine(@NotNull VirtualMachine vm)
            throws AzureCmdException {
        requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.startVirtualMachine(vm));
    }

    @Override
    public void shutdownVirtualMachine(@NotNull VirtualMachine vm, boolean deallocate)
            throws AzureCmdException {
        requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.shutdownVirtualMachine(vm, deallocate));
    }

    @Override
    public void restartVirtualMachine(@NotNull VirtualMachine vm)
            throws AzureCmdException {
        requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.restartVirtualMachine(vm));
    }

    @Override
    public void deleteVirtualMachine(@NotNull VirtualMachine vm, boolean deleteFromStorage)
            throws AzureCmdException {
        requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.deleteVirtualMachine(vm, deleteFromStorage));
    }

    @NotNull
    @Override
    public byte[] downloadRDP(@NotNull VirtualMachine vm) throws AzureCmdException {
        return requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.downloadRDP(vm));
    }

    @NotNull
    @Override
    public List<StorageAccount> getStorageAccounts(@NotNull String subscriptionId, boolean detailed)
            throws AzureCmdException {
        return requestStorageSDK(subscriptionId, AzureSDKHelper.getStorageAccounts(subscriptionId, detailed));
    }

    @NotNull
    @Override
    public Boolean checkStorageNameAvailability(@NotNull final String subscriptionId, final String storageAccountName)
            throws AzureCmdException {
        return requestStorageSDK(subscriptionId, AzureSDKHelper.checkStorageNameAvailability(storageAccountName));
    }

    @NotNull
    @Override
    public List<VirtualMachineImage> getVirtualMachineImages(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.getVirtualMachineImages());
    }

    @NotNull
    @Override
    public List<VirtualMachineSize> getVirtualMachineSizes(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestManagementSDK(subscriptionId, AzureSDKHelper.getVirtualMachineSizes());
    }

    @NotNull
    @Override
    public List<Location> getLocations(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestManagementSDK(subscriptionId, AzureSDKHelper.getLocations());
    }
    
    @NotNull
    @Override
    public SubscriptionGetResponse getSubscription(@NotNull Configuration config) throws AzureCmdException {
    	return AzureSDKHelper.getSubscription(config);
    }

    @NotNull
    @Override
    public List<AffinityGroup> getAffinityGroups(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestManagementSDK(subscriptionId, AzureSDKHelper.getAffinityGroups());
    }

    @NotNull
    @Override
    public List<VirtualNetwork> getVirtualNetworks(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestNetworkSDK(subscriptionId, AzureSDKHelper.getVirtualNetworks(subscriptionId));
    }

    @Override
    public OperationStatusResponse createStorageAccount(@NotNull StorageAccount storageAccount)
            throws AzureCmdException {
        return requestStorageSDK(storageAccount.getSubscriptionId(), AzureSDKHelper.createStorageAccount(storageAccount));
    }

    @Override
    public void createCloudService(@NotNull CloudService cloudService)
            throws AzureCmdException {
        requestComputeSDK(cloudService.getSubscriptionId(), AzureSDKHelper.createCloudService(cloudService));
    }

    @Override
    public CloudService getCloudServiceDetailed(@NotNull CloudService cloudService) throws AzureCmdException {
        return requestComputeSDK(cloudService.getSubscriptionId(), AzureSDKHelper.getCloudServiceDetailed(cloudService.getSubscriptionId(), cloudService.getName()));
    }

    @NotNull
    @Override
    public Boolean checkHostedServiceNameAvailability(@NotNull final String subscriptionId, final String hostedServiceName)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.checkHostedServiceNameAvailability(hostedServiceName));
    }

    @Override
    public void createVirtualMachine(@NotNull VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
                                     @NotNull StorageAccount storageAccount, @NotNull String virtualNetwork,
                                     @NotNull String username, @NotNull String password, @NotNull byte[] certificate)
            throws AzureCmdException {
        requestComputeSDK(virtualMachine.getSubscriptionId(), AzureSDKHelper.createVirtualMachine(virtualMachine,
                vmImage, storageAccount, virtualNetwork, username, password, certificate));
    }

    @Override
    public void createVirtualMachine(@NotNull VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
                                     @NotNull String mediaLocation, @NotNull String virtualNetwork,
                                     @NotNull String username, @NotNull String password, @NotNull byte[] certificate)
            throws AzureCmdException {
        requestComputeSDK(virtualMachine.getSubscriptionId(), AzureSDKHelper.createVirtualMachine(virtualMachine,
                vmImage, mediaLocation, virtualNetwork, username, password, certificate));
    }

    @Override
    public OperationStatusResponse createDeployment(@NotNull String subscriptionId, @NotNull String serviceName, @NotNull String slotName, @NotNull DeploymentCreateParameters parameters,
                                                    @NotNull String unpublish)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.createDeployment(serviceName, slotName, parameters, unpublish));
    }

    @Override
    public OperationStatusResponse deleteDeployment(@NotNull String subscriptionId, @NotNull String serviceName, @NotNull String deploymentName, boolean deleteFromStorage)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.deleteDeployment(serviceName, deploymentName, deleteFromStorage));
    }

    public DeploymentGetResponse getDeploymentBySlot(@NotNull String subscriptionId, @NotNull String serviceName, @NotNull DeploymentSlot deploymentSlot)
    		throws AzureCmdException {
    	return requestComputeSDK(subscriptionId, AzureSDKHelper.getDeploymentBySlot(serviceName, deploymentSlot));
    }

    @Override
    public OperationStatusResponse waitForStatus(@NotNull String subscriptionId, @NotNull OperationStatusResponse operationStatusResponse)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.waitForStatus(operationStatusResponse));
    }

    @NotNull
    @Override
    public StorageAccount refreshStorageAccountInformation(@NotNull StorageAccount storageAccount)
            throws AzureCmdException {
        return requestStorageSDK(storageAccount.getSubscriptionId(),
                AzureSDKHelper.refreshStorageAccountInformation(storageAccount));
    }

    @Override
    public String createServiceCertificate(@NotNull String subscriptionId, @NotNull String serviceName,
                                           @NotNull byte[] data, @NotNull String password, boolean needThumbprint)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.createServiceCertificate(serviceName, data, password, needThumbprint));
    }

    @Override
    public List<ServiceCertificateListResponse.Certificate> getCertificates(@NotNull String subscriptionId, @NotNull String serviceName)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.getCertificates(serviceName));
    }


    @Override
    public void deleteStorageAccount(@NotNull ClientStorageAccount storageAccount)
            throws AzureCmdException {
        requestStorageSDK(storageAccount.getSubscriptionId(), AzureSDKHelper.deleteStorageAccount(storageAccount));
    }
    
    @Override
    public ResourceGroupExtended createResourceGroup(@NotNull String subscriptionId, @NotNull String name, @NotNull String location)
    		throws AzureCmdException {
    	return requestResourceManagementSDK(subscriptionId, AzureSDKHelper.createResourceGroup(name, location));
    }
    
    @Override
    public List<String> getResourceGroupNames(@NotNull String subscriptionId) throws AzureCmdException {
    	return requestResourceManagementSDK(subscriptionId, AzureSDKHelper.getResourceGroupNames());
    }

    @NotNull
    @Override
    public List<WebSite> getWebSites(@NotNull String subscriptionId, @NotNull String webSpaceName)
            throws AzureCmdException {
        return requestWebSiteSDK(subscriptionId, AzureSDKHelper.getWebSites(webSpaceName));
    }

    @NotNull
    @Override
    public List<WebHostingPlanCache> getWebHostingPlans(@NotNull String subscriptionId, @NotNull String webSpaceName)
            throws AzureCmdException {
        return requestWebSiteSDK(subscriptionId, AzureSDKHelper.getWebHostingPlans(webSpaceName));
    }

    @NotNull
    @Override
    public WebSiteConfiguration getWebSiteConfiguration(@NotNull String subscriptionId, @NotNull String webSpaceName,
                                                        @NotNull String webSiteName)
            throws AzureCmdException {
        return requestWebSiteSDK(subscriptionId, AzureSDKHelper.getWebSiteConfiguration(webSpaceName, webSiteName));
    }

    @NotNull
    @Override
    public WebSitePublishSettings getWebSitePublishSettings(@NotNull String subscriptionId, @NotNull String webSpaceName,
                                                            @NotNull String webSiteName)
            throws AzureCmdException {
        return requestWebSiteSDK(subscriptionId, AzureSDKHelper.getWebSitePublishSettings(webSpaceName, webSiteName));
    }

    @Override
    public void restartWebSite(@NotNull String subscriptionId, @NotNull String webSpaceName, @NotNull String webSiteName)
            throws AzureCmdException {
        requestWebSiteSDK(subscriptionId, AzureSDKHelper.restartWebSite(webSpaceName, webSiteName));
    }

    @Override
    public void stopWebSite(@NotNull String subscriptionId, @NotNull String webSpaceName, @NotNull String webSiteName)
            throws AzureCmdException {
        requestWebSiteSDK(subscriptionId, AzureSDKHelper.stopWebSite(webSpaceName, webSiteName));
    }

    @Override
    public void startWebSite(@NotNull String subscriptionId, @NotNull String webSpaceName, @NotNull String webSiteName)
            throws AzureCmdException {
        requestWebSiteSDK(subscriptionId, AzureSDKHelper.startWebSite(webSpaceName, webSiteName));
    }

    @NotNull
    @Override
    public WebSite createWebSite(@NotNull String subscriptionId, @NotNull WebHostingPlanCache webHostingPlan, @NotNull String webSiteName)
    		throws AzureCmdException {
    	return requestWebSiteSDK(subscriptionId, AzureSDKHelper.createWebSite(webHostingPlan, webSiteName));
    }

    @NotNull
    @Override
	public Void deleteWebSite(@NotNull String subscriptionId, @NotNull String webSpaceName, @NotNull String webSiteName) throws AzureCmdException {
    	return requestWebSiteSDK(subscriptionId, AzureSDKHelper.deleteWebSite(webSpaceName, webSiteName));
    }

    @Override
    public WebSite getWebSite(@NotNull String subscriptionId, @NotNull final String webSpaceName, @NotNull String webSiteName)
            throws AzureCmdException {
        return requestWebSiteSDK(subscriptionId, AzureSDKHelper.getWebSite(webSpaceName, webSiteName));
    }

    @NotNull
    @Override
    public WebSiteConfiguration updateWebSiteConfiguration(@NotNull String subscriptionId,
    		@NotNull String webSpaceName,
    		@NotNull String webSiteName,
    		@NotNull String location,
    		@NotNull WebSiteConfiguration webSiteConfiguration) throws AzureCmdException {
    	return requestWebSiteSDK(subscriptionId, AzureSDKHelper.updateWebSiteConfiguration(webSpaceName, webSiteName, location, webSiteConfiguration));
    }

    @NotNull
    @Override
    public WebHostingPlan createWebHostingPlan(@NotNull String subscriptionId, @NotNull WebHostingPlanCache webHostingPlan)
    		throws AzureCmdException {
    	return requestWebSiteSDK(subscriptionId, AzureSDKHelper.createWebHostingPlan(webHostingPlan));
    }

    @Nullable
    @Override
    public ArtifactDescriptor getWebArchiveArtifact(@NotNull ProjectDescriptor projectDescriptor)
            throws AzureCmdException {
        ArtifactDescriptor artifactDescriptor = null;

        for (ArtifactDescriptor descriptor : DefaultLoader.getIdeHelper().getArtifacts(projectDescriptor)) {
            if ("war".equals(descriptor.getArtifactType())) {
                artifactDescriptor = descriptor;
                break;
            }
        }

        return artifactDescriptor;
    }

    @Override
    public void deployWebArchiveArtifact(@NotNull final ProjectDescriptor projectDescriptor,
    		@NotNull final ArtifactDescriptor artifactDescriptor,
    		@NotNull final WebSite webSite,
    		@NotNull final boolean isDeployRoot) {
    	ListenableFuture<String> future = DefaultLoader.getIdeHelper().buildArtifact(projectDescriptor, artifactDescriptor);

    	Futures.addCallback(future, new FutureCallback<String>() {
    		@Override
    		public void onSuccess(final String artifactPath) {
    			try {
    				DefaultLoader.getIdeHelper().runInBackground(projectDescriptor, "Deploying web app", "Deploying web app...", new CancellableTask() {
    					@Override
    					public void run(CancellationHandle cancellationHandle) throws Throwable {
    						AzureManager manager = AzureManagerImpl.getManager();
    						manager.publishWebArchiveArtifact(webSite.getSubscriptionId(), webSite.getWebSpaceName(), webSite.getName(),
    								artifactPath, isDeployRoot, artifactDescriptor.getName());
    					}

    					@Override
    					public void onCancel() {
    					}

    					@Override
    					public void onSuccess() {
    					}

    					@Override
    					public void onError(@NotNull Throwable throwable) {
    						DefaultLoader.getUIHelper().showException("An error occurred while attempting to deploy web app.",
    								throwable, "MS Services - Error Deploying Web App", false, true);
    					}
    				});
    			} catch (AzureCmdException ex) {
    				String msg = "An error occurred while attempting to deploy web app." + "\n" + "(Message from Azure:" + ex.getMessage() + ")";
    				DefaultLoader.getUIHelper().showException(msg,
    						ex, "MS Services - Error Deploying Web App", false, true);
    			}
    		}

    		@Override
    		public void onFailure(Throwable throwable) {
    			DefaultLoader.getUIHelper().showException("An error occurred while attempting to build web archive artifact.", throwable,
    					"MS Services - Error Building WAR Artifact", false, true);
    		}
    	});
    }

    @Override
    public void publishWebArchiveArtifact(@NotNull String subscriptionId, @NotNull String webSpaceName,
    		@NotNull String webSiteName, @NotNull String artifactPath,
    		@NotNull boolean isDeployRoot, @NotNull String artifactName) throws AzureCmdException {
    	WebSitePublishSettings webSitePublishSettings = getWebSitePublishSettings(subscriptionId, webSpaceName, webSiteName);
    	WebSitePublishSettings.FTPPublishProfile publishProfile = null;
    	for (PublishProfile pp : webSitePublishSettings.getPublishProfileList()) {
    		if (pp instanceof FTPPublishProfile) {
    			publishProfile = (FTPPublishProfile) pp;
    			break;
    		}
    	}

    	if (publishProfile == null) {
    		throw new AzureCmdException("Unable to retrieve FTP credentials to publish web site");
    	}

    	URI uri;

    	try {
    		uri = new URI(publishProfile.getPublishUrl());
    	} catch (URISyntaxException e) {
    		throw new AzureCmdException("Unable to parse FTP Publish Url information", e);
    	}

    	final FTPClient ftp = new FTPClient();

    	try {
    		ftp.connect(uri.getHost());
    		final int replyCode = ftp.getReplyCode();

    		if (!FTPReply.isPositiveCompletion(replyCode)) {
    			ftp.disconnect();
    			throw new AzureCmdException("Unable to connect to FTP server");
    		}

    		if (!ftp.login(publishProfile.getUserName(), publishProfile.getPassword())) {
    			ftp.logout();
    			throw new AzureCmdException("Unable to login to FTP server");
    		}

    		ftp.setFileType(FTP.BINARY_FILE_TYPE);

    		if (publishProfile.isFtpPassiveMode()) {
    			ftp.enterLocalPassiveMode();
    		}

    		String targetDir = getAbsolutePath(uri.getPath());
    		targetDir += "/webapps";

    		InputStream input = new FileInputStream(artifactPath);
    		if (isDeployRoot) {
    			removeFtpDirectory(ftp, "/site/wwwroot/webapps/ROOT", "");
    			ftp.storeFile(targetDir + "/ROOT.war", input);
    		} else {
    			artifactName = artifactName.replaceAll("[^a-zA-Z0-9_-]+","");
    			removeFtpDirectory(ftp, "/site/wwwroot/webapps/" + artifactName, "");
    			ftp.storeFile(targetDir + "/" + artifactName + ".war", input);
    		}
    		input.close();
    		ftp.logout();
    	} catch (IOException e) {
    		throw new AzureCmdException("Unable to connect to the FTP server", e);
    	} finally {
    		if (ftp.isConnected()) {
    			try {
    				ftp.disconnect();
    			} catch (IOException ignored) {
    			}
    		}
    	}
    }

    public static void removeFtpDirectory(FTPClient ftpClient, String parentDir,
    		String currentDir) throws IOException {
    	String dirToList = parentDir;
    	if (!currentDir.equals("")) {
    		dirToList += "/" + currentDir;
    	}
    	FTPFile[] subFiles = ftpClient.listFiles(dirToList);
    	if (subFiles != null && subFiles.length > 0) {
    		for (FTPFile ftpFile : subFiles) {
    			String currentFileName = ftpFile.getName();
    			if (currentFileName.equals(".") || currentFileName.equals("..")) {
    				// skip parent directory and the directory itself
    				continue;
    			}
    			String filePath = parentDir + "/" + currentDir + "/" + currentFileName;
    			if (currentDir.equals("")) {
    				filePath = parentDir + "/" + currentFileName;
    			}

    			if (ftpFile.isDirectory()) {
    				// remove the sub directory
    				removeFtpDirectory(ftpClient, dirToList, currentFileName);
    			} else {
    				// delete the file
    				ftpClient.deleteFile(filePath);
    			}
    		}
    		// remove the empty directory
    		ftpClient.removeDirectory(dirToList);
    	}
    }

    @NotNull
    private List<Subscription> parseSubscriptionsXML(@NotNull String subscriptionsXML)
            throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        NodeList subscriptionList = (NodeList) XmlHelper.getXMLValue(subscriptionsXML, "//Subscription", XPathConstants.NODESET);

        ArrayList<Subscription> subscriptions = new ArrayList<Subscription>();

        for (int i = 0; i < subscriptionList.getLength(); i++) {
            Subscription subscription = new Subscription();
            subscription.setName(XmlHelper.getChildNodeValue(subscriptionList.item(i), "SubscriptionName"));
            subscription.setId(XmlHelper.getChildNodeValue(subscriptionList.item(i), "SubscriptionID"));
            subscription.setTenantId(XmlHelper.getChildNodeValue(subscriptionList.item(i), "AADTenantID"));
            subscription.setMaxStorageAccounts(Integer.valueOf(XmlHelper.getChildNodeValue(subscriptionList.item(i), "MaxStorageAccounts")));
            subscription.setMaxHostedServices(Integer.valueOf(XmlHelper.getChildNodeValue(subscriptionList.item(i), "MaxHostedServices")));
            subscription.setSelected(true);

            subscriptions.add(subscription);
        }

        return subscriptions;
    }

    private List<Subscription> importSubscription(@NotNull String publishSettingsFilePath)
            throws AzureCmdException {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(publishSettingsFilePath));
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            String publishSettingsFile = sb.toString();
            String managementCertificate = null;
            String serviceManagementUrl = null;
            boolean isPublishSettings2 = true;
            Node publishProfile = (Node) XmlHelper.getXMLValue(publishSettingsFile, "//PublishProfile", XPathConstants.NODE);
            if (XmlHelper.getAttributeValue(publishProfile, "SchemaVersion") == null
                    || !XmlHelper.getAttributeValue(publishProfile, "SchemaVersion").equals("2.0")) {
                isPublishSettings2 = false;
                managementCertificate = XmlHelper.getAttributeValue(publishProfile, "ManagementCertificate");
                serviceManagementUrl = XmlHelper.getAttributeValue(publishProfile, "Url");
            }
            NodeList subscriptionNodes = (NodeList) XmlHelper.getXMLValue(publishSettingsFile, "//Subscription",
                    XPathConstants.NODESET);
            List<Subscription> subscriptions = new ArrayList<Subscription>();
            for (int i = 0; i < subscriptionNodes.getLength(); i++) {
                Node subscriptionNode = subscriptionNodes.item(i);
                Subscription subscription = new Subscription();
                subscription.setName(XmlHelper.getAttributeValue(subscriptionNode, "Name"));
                subscription.setId(XmlHelper.getAttributeValue(subscriptionNode, "Id"));
                if (isPublishSettings2) {
                    subscription.setManagementCertificate(XmlHelper.getAttributeValue(subscriptionNode, "ManagementCertificate"));
                    subscription.setServiceManagementUrl(XmlHelper.getAttributeValue(subscriptionNode, "ServiceManagementUrl"));
                } else {
                    subscription.setManagementCertificate(managementCertificate);
                    subscription.setServiceManagementUrl(serviceManagementUrl);
                }
                subscription.setSelected(true);
                Configuration config = AzureSDKHelper.getConfiguration(new File(publishSettingsFilePath), subscription.getId());
                SubscriptionGetResponse response = getSubscription(config);
                com.microsoftopentechnologies.azuremanagementutil.model.Subscription sub = SubscriptionTransformer.transform(response);
                subscription.setMaxStorageAccounts(sub.getMaxStorageAccounts());
                subscription.setMaxHostedServices(sub.getMaxHostedServices());
                subscriptions.add(subscription);
            }
            return subscriptions;
        } catch (Exception ex) {
            if (ex instanceof AzureCmdException) {
                throw (AzureCmdException) ex;
            }

            throw new AzureCmdException("Error importing subscriptions from publish settings file", ex);
        }
    }

    private void updateSubscription(@NotNull Subscription subscription, @NotNull UserInfo userInfo)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            String subscriptionId = subscription.getId();
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
            subscriptionLock.writeLock().lock();

            try {
                if (subscriptions.containsKey(subscriptionId)) {
                    subscriptions.get(subscriptionId).setTenantId(subscription.getTenantId());
                } else {
                    subscriptions.put(subscriptionId, subscription);
                }

                setUserInfo(subscriptionId, userInfo);
                storeSubscriptions();
            } finally {
                subscriptionLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void updateSubscription(@NotNull Subscription subscription, @NotNull SSLSocketFactory sslSocketFactory)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            String subscriptionId = subscription.getId();
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
            subscriptionLock.writeLock().lock();

            try {
                if (subscriptions.containsKey(subscriptionId)) {
                    Subscription existingSubscription = subscriptions.get(subscriptionId);
                    existingSubscription.setManagementCertificate(subscription.getManagementCertificate());
                    existingSubscription.setServiceManagementUrl(subscription.getServiceManagementUrl());
                } else {
                    subscriptions.put(subscriptionId, subscription);
                }

                setSSLSocketFactory(subscriptionId, sslSocketFactory);
                storeSubscriptions();
            } finally {
                subscriptionLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void notifySubscriptionsChanged() {
        subscriptionsChangedLock.readLock().lock();

        try {
            for (EventWaitHandleImpl handle : subscriptionsChangedHandles) {
                handle.signalEvent();
            }
        } finally {
            subscriptionsChangedLock.readLock().unlock();
        }
    }

    private void setUserInfo(@Nullable UserInfo userInfo) {
        authDataLock.writeLock().lock();

        try {
            this.userInfo = userInfo;
            userInfoBySubscriptionId.clear();
            removeUnusedSubscriptions();

            storeSubscriptions();
            storeUserInfo();
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    @NotNull
    private Subscription getSubscription(@NotNull String subscriptionId)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, false);
            subscriptionLock.readLock().lock();

            try {
                return subscriptions.get(subscriptionId);
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private boolean hasUserInfo(@NotNull String subscriptionId) {
        authDataLock.readLock().lock();

        try {
            Optional<ReentrantReadWriteLock> optionalRWLock = getSubscriptionLock(subscriptionId);

            if (!optionalRWLock.isPresent()) {
                return false;
            }

            ReentrantReadWriteLock subscriptionLock = optionalRWLock.get();
            subscriptionLock.readLock().lock();

            try {
                return userInfoBySubscriptionId.containsKey(subscriptionId);
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void setUserInfo(@NotNull String subscriptionId, @NotNull UserInfo userInfo)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
            subscriptionLock.writeLock().lock();

            try {
                userInfoBySubscriptionId.put(subscriptionId, userInfo);

                storeUserInfo();
            } finally {
                subscriptionLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private boolean hasSSLSocketFactory(@NotNull String subscriptionId) {
        authDataLock.readLock().lock();

        try {
            Optional<ReentrantReadWriteLock> optionalRWLock = getSubscriptionLock(subscriptionId);

            if (!optionalRWLock.isPresent()) {
                return false;
            }

            ReentrantReadWriteLock subscriptionLock = optionalRWLock.get();
            subscriptionLock.readLock().lock();

            try {
                return sslSocketFactoryBySubscriptionId.containsKey(subscriptionId);
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    private Optional<SSLSocketFactory> getSSLSocketFactory(@NotNull String subscriptionId)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, false);
            subscriptionLock.readLock().lock();

            try {
                if (!sslSocketFactoryBySubscriptionId.containsKey(subscriptionId)) {
                    return Optional.absent();
                }

                return Optional.of(sslSocketFactoryBySubscriptionId.get(subscriptionId));
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void setSSLSocketFactory(@NotNull String subscriptionId, @NotNull SSLSocketFactory sslSocketFactory)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
            subscriptionLock.writeLock().lock();

            try {
                sslSocketFactoryBySubscriptionId.put(subscriptionId, sslSocketFactory);
            } finally {
                subscriptionLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private boolean hasAccessToken() {
        authDataLock.readLock().lock();

        try {
            return !(accessToken == null || accessToken.isEmpty());
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    private Optional<ReentrantReadWriteLock> getSubscriptionLock(@NotNull String subscriptionId) {
        subscriptionMapLock.readLock().lock();

        try {
            if (lockBySubscriptionId.containsKey(subscriptionId)) {
                return Optional.of(lockBySubscriptionId.get(subscriptionId));
            } else {
                return Optional.absent();
            }
        } finally {
            subscriptionMapLock.readLock().unlock();
        }
    }

    @NotNull
    private String executeGetRequest(@NotNull String subscriptionId, @NotNull String path)
            throws AzureCmdException {
        return executeRequest(subscriptionId, path, ContentType.Json, "GET", null);
    }

    @NotNull
    private String executeRequest(@NotNull String subscriptionId,
                                  @NotNull final String path,
                                  @NotNull final ContentType contentType,
                                  @NotNull final String method,
                                  @Nullable final String postData)
            throws AzureCmdException {
        Subscription subscription = getSubscription(subscriptionId);

        Optional<SSLSocketFactory> optionalSSLSocketFactory = getSSLSocketFactory(subscriptionId);

        if (optionalSSLSocketFactory.isPresent()) {
            SSLSocketFactory sslSocketFactory = optionalSSLSocketFactory.get();
            return AzureCertificateHelper.executeRequest(subscription.getServiceManagementUrl(), path, contentType,
                    method, postData, sslSocketFactory, MobileServiceRestManager.getManager());
        } else {
            final PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();
            final String managementUri = settings.getAzureServiceManagementUri();
            final UserInfo userInfo = getUserInfo(subscriptionId);
            return requestWithToken(userInfo, new RequestCallback<String>() {
                @Override
                public String execute()
                        throws Throwable {
                    String accessToken = getAccessToken(userInfo);
                    return AzureAADHelper.executeRequest(managementUri, path, contentType,
                            method, postData, accessToken, MobileServiceRestManager.getManager());
                }
            });
        }
    }

    @NotNull
    private String executePollRequest(@NotNull String subscriptionId,
                                      @NotNull final String path,
                                      @NotNull final ContentType contentType,
                                      @NotNull final String method,
                                      @Nullable final String postData,
                                      @NotNull final String pollPath)
            throws AzureCmdException {
        Subscription subscription = getSubscription(subscriptionId);

        Optional<SSLSocketFactory> optionalSSLSocketFactory = getSSLSocketFactory(subscriptionId);

        if (optionalSSLSocketFactory.isPresent()) {
            SSLSocketFactory sslSocketFactory = optionalSSLSocketFactory.get();
            return AzureCertificateHelper.executePollRequest(subscription.getServiceManagementUrl(), path, contentType,
                    method, postData, pollPath, sslSocketFactory, MobileServiceRestManager.getManager());
        } else {
            final PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();
            final String managementUri = settings.getAzureServiceManagementUri();
            final UserInfo userInfo = getUserInfo(subscriptionId);
            return requestWithToken(userInfo, new RequestCallback<String>() {
                @Override
                public String execute()
                        throws Throwable {
                    String accessToken = getAccessToken(userInfo);
                    return AzureAADHelper.executePollRequest(managementUri, path, contentType,
                            method, postData, pollPath, accessToken, MobileServiceRestManager.getManager());
                }
            });
        }
    }

    @NotNull
    private <T> T requestComputeSDK(@NotNull final String subscriptionId,
                                    @NotNull final SDKRequestCallback<T, ComputeManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<ComputeManagementClient>() {
            @NotNull
            @Override
            public ComputeManagementClient getSSLClient(@NotNull Subscription subscription)
                    throws Throwable {
                return AzureSDKHelper.getComputeManagementClient(subscription.getId(),
                        subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
            }

            @NotNull
            @Override
            public ComputeManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                    throws Throwable {
                return AzureSDKHelper.getComputeManagementClient(subscriptionId,
                        accessToken);
            }
        });
    }

    @NotNull
    private <T> T requestStorageSDK(@NotNull final String subscriptionId,
                                    @NotNull final SDKRequestCallback<T, StorageManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<StorageManagementClient>() {
            @NotNull
            @Override
            public StorageManagementClient getSSLClient(@NotNull Subscription subscription)
                    throws Throwable {
                return AzureSDKHelper.getStorageManagementClient(subscription.getId(),
                        subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
            }

            @NotNull
            @Override
            public StorageManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                    throws Throwable {
                return AzureSDKHelper.getStorageManagementClient(subscriptionId,
                        accessToken);
            }
        });
    }

    @NotNull
    private <T> T requestNetworkSDK(@NotNull final String subscriptionId,
    		@NotNull final SDKRequestCallback<T, NetworkManagementClient> requestCallback)
    				throws AzureCmdException {
    	return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<NetworkManagementClient>() {
    		@NotNull
    		@Override
    		public NetworkManagementClient getSSLClient(@NotNull Subscription subscription)
    				throws Throwable {
    			return AzureSDKHelper.getNetworkManagementClient(subscription.getId(),
    					subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
    		}

    		@NotNull
    		@Override
    		public NetworkManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
    				throws Throwable {
    			return AzureSDKHelper.getNetworkManagementClient(subscriptionId,
    					accessToken);
    		}
    	});
    }

    @NotNull
    private <T> T requestWebSiteSDK(@NotNull final String subscriptionId,
    		@NotNull final SDKRequestCallback<T, WebSiteManagementClient> requestCallback)
    				throws AzureCmdException {
    	return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<WebSiteManagementClient>() {
    		@NotNull
    		@Override
    		public WebSiteManagementClient getSSLClient(@NotNull Subscription subscription)
    				throws Throwable {
    			return AzureSDKHelper.getWebSiteManagementClient(subscription.getId(),
    					subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
    		}

    		@NotNull
    		@Override
    		public WebSiteManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
    				throws Throwable {
    			return AzureSDKHelper.getWebSiteManagementClient(subscriptionId,
    					accessToken);
    		}
    	});
    }
    
    @NotNull
    private <T> T requestResourceManagementSDK(@NotNull final String subscriptionId,
                                    @NotNull final SDKRequestCallback<T, ResourceManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<ResourceManagementClient>() {
            @NotNull
            @Override
            public ResourceManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                    throws Throwable {
                return AzureSDKHelper.getResourceManagementClient(subscriptionId, accessToken);
            }

			@Override
			public ResourceManagementClient getSSLClient(Subscription subscription) throws Throwable {
				return AzureSDKHelper.getResourceManagementClient(subscription.getId(),
                        subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
			}
        });
    }
    

    @NotNull
    private <T> T requestManagementSDK(@NotNull final String subscriptionId,
                                       @NotNull final SDKRequestCallback<T, ManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<ManagementClient>() {
            @NotNull
            @Override
            public ManagementClient getSSLClient(@NotNull Subscription subscription)
                    throws Throwable {
                return AzureSDKHelper.getManagementClient(subscription.getId(),
                        subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
            }

            @NotNull
            @Override
            public ManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                    throws Throwable {
                return AzureSDKHelper.getManagementClient(subscriptionId,
                        accessToken);
            }
        });
    }
    
    @NotNull
    private <T> T requestApplicationInsightsSDK(@NotNull final String subscriptionId,
                                       @NotNull final SDKRequestCallback<T, ApplicationInsightsManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<ApplicationInsightsManagementClient>() {
            @NotNull
            @Override
            public ApplicationInsightsManagementClient getSSLClient(@NotNull Subscription subscription)
                    throws Throwable {
            	// Application insights does not support publish settings file as authentication
                return null;
            }

            @NotNull
            @Override
            public ApplicationInsightsManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
            		throws Throwable {
                return AzureSDKHelper.getApplicationManagementClient(getUserInfo(subscriptionId).getTenantId(), accessToken);
            }
        });
    }

    @NotNull
    private <T, V extends Closeable> T requestAzureSDK(@NotNull final String subscriptionId,
                                                       @NotNull final SDKRequestCallback<T, V> requestCallback,
                                                       @NotNull final AzureSDKClientProvider<V> clientProvider)
            throws AzureCmdException {
        if (hasSSLSocketFactory(subscriptionId)) {
            try {
                Subscription subscription = getSubscription(subscriptionId);
                V client = clientProvider.getSSLClient(subscription);

                try {
                    return requestCallback.execute(client);
                } finally {
                    client.close();
                }
            } catch (Throwable t) {
                if (t instanceof AzureCmdException) {
                    throw (AzureCmdException) t;
                } else if (t instanceof ExecutionException) {
                    throw new AzureCmdException(t.getCause().getMessage(), t.getCause());
                }

                throw new AzureCmdException(t.getMessage(), t);
            }
        } else if (hasAccessToken()) {
            V client = null;
            try {
                client = clientProvider.getAADClient(subscriptionId, accessToken);
                return requestCallback.execute(client);
            } catch (Throwable throwable) {
                throw new AzureCmdException(throwable.getMessage(), throwable);
            } finally {
                try {
                    if (client != null) {
                        client.close();
                    }
                } catch (IOException e) {
                    throw new AzureCmdException(e.getMessage(), e);
                }
            }
        } else {
            final UserInfo userInfo = getUserInfo(subscriptionId);
            PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();

            com.microsoft.tooling.msservices.helpers.auth.RequestCallback<T> aadRequestCB =
                    new com.microsoft.tooling.msservices.helpers.auth.RequestCallback<T>() {
                        @NotNull
                        @Override
                        public T execute(@NotNull String accessToken) throws Throwable {
                            if (!hasAccessToken(userInfo) ||
                                    !accessToken.equals(getAccessToken(userInfo))) {
                                ReentrantReadWriteLock userLock = getUserLock(userInfo, true);
                                userLock.writeLock().lock();

                                try {
                                    if (!hasAccessToken(userInfo) ||
                                            !accessToken.equals(getAccessToken(userInfo))) {
                                        setAccessToken(userInfo, accessToken);
                                    }
                                } finally {
                                    userLock.writeLock().unlock();
                                }
                            }

                            V client = clientProvider.getAADClient(subscriptionId, accessToken);

                            try {
                                return requestCallback.execute(client);
                            } finally {
                                client.close();
                            }
                        }
                    };

            return aadManager.request(userInfo,
                    settings.getAzureServiceManagementUri(),
                    "Sign in to your Azure account",
                    aadRequestCB);
        }
    }

    @NotNull
    private <T> T requestWithToken(@NotNull final UserInfo userInfo, @NotNull final RequestCallback<T> requestCallback)
            throws AzureCmdException {
        PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();

        com.microsoft.tooling.msservices.helpers.auth.RequestCallback<T> aadRequestCB =
                new com.microsoft.tooling.msservices.helpers.auth.RequestCallback<T>() {
                    @NotNull
                    @Override
                    public T execute(@NotNull String accessToken) throws Throwable {
                        if (!hasAccessToken(userInfo) ||
                                !accessToken.equals(getAccessToken(userInfo))) {
                            ReentrantReadWriteLock userLock = getUserLock(userInfo, true);
                            userLock.writeLock().lock();

                            try {
                                if (!hasAccessToken(userInfo) ||
                                        !accessToken.equals(getAccessToken(userInfo))) {
                                    setAccessToken(userInfo, accessToken);
                                }
                            } finally {
                                userLock.writeLock().unlock();
                            }
                        }

                        return requestCallback.execute();
                    }
                };

        return aadManager.request(userInfo,
                settings.getAzureServiceManagementUri(),
                "Sign in to your Azure account",
                aadRequestCB);
    }

    @NotNull
    private static String readFile(@NotNull String filePath)
            throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));

        try {
            return CharStreams.toString(in);
        } finally {
            in.close();
        }
    }

    @NotNull
    private static String getAbsolutePath(@NotNull String dir) {
        return "/" + dir.trim().replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
    }
    
    @Override
    public List<Resource> getApplicationInsightsResources(@NotNull String subscriptionId) throws AzureCmdException {
    	return requestApplicationInsightsSDK(subscriptionId, AzureSDKHelper.getApplicationInsightsResources(subscriptionId));
    }
    
    @Override
    public List<String> getLocationsForApplicationInsights(@NotNull String subscriptionId) throws AzureCmdException {
    	return requestApplicationInsightsSDK(subscriptionId, AzureSDKHelper.getLocationsForApplicationInsights());
    }
    
    @Override
    public Resource createApplicationInsightsResource(@NotNull String subscriptionId,
    		@NotNull String resourceGroupName,
    		@NotNull String resourceName,
    		@NotNull String location) throws AzureCmdException {
    	return requestApplicationInsightsSDK(subscriptionId, AzureSDKHelper.createApplicationInsightsResource(subscriptionId,
    			resourceGroupName, resourceName, location));
    }
}