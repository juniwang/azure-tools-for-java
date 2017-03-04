/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.utils;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.ISubscriptionSelectionListener;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import rx.*;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.*;

/**
 * Created by vlashch on 1/9/17.
 */
public class AzureModelController {

    private static ISubscriptionSelectionListener subscriptionSelectionListener = new ISubscriptionSelectionListener() {
        @Override
        public void update(boolean isSignedOut) {
            try {
                if (isSignedOut) {
                    clearAll();
                    return;
                }

                IProgressTask pt = new ProgressTask(CommonSettings.getUiFactory().getProgressTaskImpl());
                pt.work(new IWorker() {
                    @Override
                    public void work(IProgressIndicator pi) {
                        try {
                            subscriptionSelectionChanged(pi);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public String getName() {
                        return "Update Azure Local Cache Progress";
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private static synchronized void clearAll() {
        System.out.println("AzureModelController.clearAll: set null to all the maps.");
        AzureModel azureModel = AzureModel.getInstance();
        azureModel.setSubscriptionToResourceGroupMap(null);
        azureModel.setResourceGroupToWebAppMap(null);
        azureModel.setResourceGroupToAppServicePlanMap(null);
        // TODO: notify subscribers
    }

    private static synchronized void subscriptionSelectionChanged(IProgressIndicator progressIndicator) throws Exception {
        System.out.println("AzureModelController.subscriptionSelectionChanged: starting");
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) {
            System.out.println("AzureModelController.subscriptionSelectionChanged: azureManager == null -> return");
            return;
        }

        SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
        AzureModel azureModel = AzureModel.getInstance();

        Map<SubscriptionDetail, List<ResourceGroup>> srgMap = azureModel.getSubscriptionToResourceGroupMap();
        if (srgMap == null) {
            System.out.println("AzureModelController.subscriptionSelectionChanged: srgMap == null -> return");
            return;
        }
        Map <String, Subscription> sidToSubscriptionMap = azureModel.getSidToSubscriptionMap();
        if (sidToSubscriptionMap == null) {
            System.out.println("AzureModelController.subscriptionSelectionChanged: sidToSubscriptionMap == null -> return");
            return;
        }

        Map<ResourceGroup, List<WebApp>> rgwaMap = azureModel.getResourceGroupToWebAppMap();
        Map<ResourceGroup, List<AppServicePlan>> rgspMap = azureModel.getResourceGroupToAppServicePlanMap();

        System.out.println("AzureModelController.subscriptionSelectionChanged: getting subscription details...");
        List<SubscriptionDetail> sdl = subscriptionManager.getSubscriptionDetails();
        if (sdl == null) {
            System.out.println("AzureModelController.subscriptionSelectionChanged: sdl == null -> return");
            return;
        }

        for (SubscriptionDetail sd : sdl) {
            if (!srgMap.containsKey(sd)) {
                if (!sd.isSelected()) continue;

                Azure azure = azureManager.getAzure(sd.getSubscriptionId());
                // subscription locations
                List<Subscription> sl = azureManager.getSubscriptions();
                System.out.println("Updating subscription locations");
                List<Location> locl = sidToSubscriptionMap.get(sd.getSubscriptionId()).listLocations();
                Map<SubscriptionDetail, List<Location>> sdlocMap = azureModel.getSubscriptionToLocationMap();
                sdlocMap.put(sd, locl);

                // resource group maps
                List<ResourceGroup> rgList = azure.resourceGroups().list();
                srgMap.put(sd, rgList);
                updateResGrDependency(azure, rgList, progressIndicator, rgwaMap, rgspMap);
            } else {
                // find and modify the key
                for (SubscriptionDetail sdk : srgMap.keySet()) {
                    if (sdk.equals(sd)) {
                        sdk.setSelected(sd.isSelected());
                    }
                }
            }
        }
    }

    static class RgDepParams {
        ResourceGroup rg;
        List<WebApp> wal;
        List<AppServicePlan> aspl;

        public RgDepParams(ResourceGroup rg, List<WebApp> wal, List<AppServicePlan> aspl) {
            this.rg = rg;
            this.wal = wal;
            this.aspl = aspl;
        }
    }

    private static synchronized void updateResGrDependency(Azure azure,
            List<ResourceGroup> rgList,
            IProgressIndicator progressIndicator,
            Map<ResourceGroup, List<WebApp>> rgwaMap,
            Map<ResourceGroup, List<AppServicePlan>> rgspMap) {

        if (progressIndicator != null) progressIndicator.setText("Reading App Services...");
        int tasksSize = rgList.size();
        if (tasksSize == 0) return;
        Observable.from(rgList).flatMap(new Func1<ResourceGroup, Observable<? extends RgDepParams>>() {
            @Override
            public Observable<? extends RgDepParams> call(ResourceGroup rg) {
                return Observable.create(new Observable.OnSubscribe<RgDepParams>() {
                    @Override
                    public void call(Subscriber<? super RgDepParams> subscriber) {
                        List<WebApp> wal = azure.webApps().listByGroup(rg.name());
                        List<AppServicePlan> aspl = azure.appServices().appServicePlans().listByGroup(rg.name());
                        subscriber.onNext(new RgDepParams(rg, wal, aspl));
                        subscriber.onCompleted();
                    }
                }).subscribeOn(Schedulers.io());
            }
        }, tasksSize)
        .subscribeOn(Schedulers.newThread())
        .toBlocking()
        .subscribe(new Action1<RgDepParams>() {
            @Override
            public void call(RgDepParams params) {
                rgwaMap.put(params.rg, params.wal);
                rgspMap.put(params.rg, params.aspl);
            }
        });

//        for (ResourceGroup rg : rgList) {
//            if (progressIndicator != null && progressIndicator.isCanceled()) {
//                clearAll();
//                return;
//            }
//            //System.out.println("rg : " + rg);
//            if (progressIndicator != null) progressIndicator.setText("Reading resource group '" + rg.name() + "'");
//            List<WebApp> waList = azure.webApps().listByGroup(rg.name());
//            rgwaMap.put(rg, waList);
//            List<AppServicePlan> aspl = azure.appServices().appServicePlans().listByGroup(rg.name());
//            rgspMap.put(rg, aspl);
//        }
    }

    public static synchronized void updateSubscriptionMaps(IProgressIndicator progressIndicator) throws Exception {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) { return; }

        if (progressIndicator != null && progressIndicator.isCanceled()) {
            clearAll();
            throw new CanceledByUserException();
        }
        AzureModel azureModel = AzureModel.getInstance();

        // to get regions we nees subscription objects
        if (progressIndicator != null) progressIndicator.setText("Reading subscription list...");
        List<Subscription> sl = azureManager.getSubscriptions();
        // convert to map to easier find by sid
        Map <String, Subscription> sidToSubscriptionMap = azureModel.createSidToSubscriptionMap();
        for (Subscription s : sl) {
            sidToSubscriptionMap.put(s.subscriptionId(), s);
        }
        azureModel.setSidToSubscriptionMap(sidToSubscriptionMap);


        Map<SubscriptionDetail, List<Location>> sdlocMap = azureModel.createSubscriptionToRegionMap();
        Map<SubscriptionDetail, List<ResourceGroup>> sdrgMap = azureModel.createSubscriptionToResourceGroupMap();

        SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
        subscriptionManager.addListener(subscriptionSelectionListener);

        List<SubscriptionDetail> sdl = subscriptionManager.getSubscriptionDetails();
        for (SubscriptionDetail sd : sdl) {
            if (!sd.isSelected()) continue;
            if (progressIndicator != null && progressIndicator.isCanceled()) {
                clearAll();
                throw new CanceledByUserException();
            }

            System.out.println("sn : " + sd.getSubscriptionName());
            if (progressIndicator != null) progressIndicator.setText("Reading subscription '" + sd.getSubscriptionName() + "'");
            Azure azure = azureManager.getAzure(sd.getSubscriptionId());

            List<ResourceGroup> rgList = azure.resourceGroups().list();
            sdrgMap.put(sd, rgList);

            List<Location> locl = sidToSubscriptionMap.get(sd.getSubscriptionId()).listLocations();
            sdlocMap.put(sd, locl);
        }
        azureModel.setSubscriptionToResourceGroupMap(sdrgMap);
        azureModel.setSubscriptionToLocationMap(sdlocMap);
    }

    public static synchronized void updateResourceGroupMaps(IProgressIndicator progressIndicator) throws Exception {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) { return;}

        updateSubscriptionMaps(progressIndicator);

        AzureModel azureModel = AzureModel.getInstance();

        Map<ResourceGroup, List<WebApp>> rgwaMap = azureModel.createResourceGroupToWebAppMap();
        Map<ResourceGroup, List<AppServicePlan>> rgspMap = azureModel.createResourceGroupToAppServicePlanMap();
        for (SubscriptionDetail sd : azureModel.getSubscriptionToResourceGroupMap().keySet()) {
            if (progressIndicator != null && progressIndicator.isCanceled()) {
                clearAll();
                throw new CanceledByUserException();
            }
            List<ResourceGroup> rgList = azureModel.getSubscriptionToResourceGroupMap().get(sd);
            if (rgList.size() == 0)  {
                System.out.println("no resource groups found!");
                continue;
            }
            Azure azure = azureManager.getAzure(sd.getSubscriptionId());
            updateResGrDependency(azure, rgList, progressIndicator, rgwaMap, rgspMap);
        }
        azureModel.setResourceGroupToWebAppMap(rgwaMap);
        azureModel.setResourceGroupToAppServicePlanMap(rgspMap);
    }

    public static void addNewResourceGroup(SubscriptionDetail sd, ResourceGroup rg) {
        AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(sd).add(rg);
        // TODO:notify subscribers
    }

    public static void addNewWebAppToJustCreatedResourceGroup(ResourceGroup rg, WebApp webApp) {
        // presume addNewResourceGroup goes first
        AzureModel.getInstance().getResourceGroupToWebAppMap().put(rg, Arrays.asList(webApp));
        // TODO:notify subscribers
    }

    public static void addNewWebAppToExistingResourceGroup(ResourceGroup rg, WebApp webApp) {
        AzureModel.getInstance().getResourceGroupToWebAppMap().get(rg).add(webApp);
        // TODO:notify subscribers
    }

    public static void removeWebAppFromExistingResourceGroup(ResourceGroup rg, WebApp webApp) {
//        String waName = webApp.name().toLowerCase();
//        List<WebApp> wal = AzureModel.getInstance().getResourceGroupToWebAppMap().get(rg);
//        for (int i = 0; i < wal.size(); ++i) {
//            if (wal.get(i).name().toLowerCase().equals(waName)) {
//                wal.remove(i);
//                break;
//            }
//        }
        AzureModel.getInstance().getResourceGroupToWebAppMap().get(rg).remove(webApp);
        // TODO:notify subscribers
    }

    public static void addNewAppServicePlanToJustCreatedResourceGroup(ResourceGroup rg, AppServicePlan appServicePlan) {
        // presume addNewResourceGroup call goes first
        AzureModel.getInstance().getResourceGroupToAppServicePlanMap().put(rg, Arrays.asList(appServicePlan));
        // TODO:notify subscribers
    }

    public static void addNewAppServicePlanToExistingResourceGroup(ResourceGroup rg, AppServicePlan appServicePlan) {
        // presume addNewResourceGroup call goes first
        AzureModel.getInstance().getResourceGroupToAppServicePlanMap().get(rg).add(appServicePlan);
        // TODO:notify subscribers
    }

}
