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
package com.microsoft.intellij.deploy;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jcraft.jsch.Session;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.*;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.activitylog.ActivityLogToolWindowFactory;
import com.microsoft.intellij.util.AppInsightsCustomEvent;
import com.microsoft.intellij.util.PluginUtil;
import com.wacommon.utils.WACommonException;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventArgs;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.microsoft.intellij.AzurePlugin.log;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public final class DeploymentManager {

    private Project myProject;

    public DeploymentManager(Project project) {
        this.myProject = project;
    }

//    private OperationStatus waitForStatus(Configuration configuration, WindowsAzureServiceManagement service, String requestId)
//            throws Exception {
//        OperationStatusResponse op;
//        OperationStatus status = null;
//        do {
//            op = service.getOperationStatus(configuration, requestId);
//            status = op.getStatus();
//
//            log(message("deplId") + op.getId());
//            log(message("deplStatus") + op.getStatus());
//            log(message("deplHttpStatus") + op.getHttpStatusCode());
//            if (op.getError() != null) {
//                log(message("deplErrorMessage") + op.getError().getMessage());
//                throw new RestAPIException(op.getError().getMessage());
//            }
//
//            Thread.sleep(5000);
//
//        } while (status == OperationStatus.InProgress);
//
//        return status;
//    }

    /**
     * Unlike Eclipse plugin, here startDate is deployment start time, not the event timestamp
     */
    public void notifyProgress(String deploymentId, Date startDate,
                               String deploymentURL,
                               int progress,
                               String message, Object... args) {

        DeploymentEventArgs arg = new DeploymentEventArgs(this);
        arg.setId(deploymentId);
        if (deploymentURL != null) {
            try {
                new URL(deploymentURL);
            } catch (MalformedURLException e) {
                deploymentURL = null;
            }
        }
        arg.setDeploymentURL(deploymentURL);
        arg.setDeployMessage(String.format(message, args));
        arg.setDeployCompleteness(progress);
        arg.setStartTime(startDate);
        AzurePlugin.fireDeploymentEvent(arg);
    }

    private void openWindowsAzureActivityLogView(final Project project) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ToolWindowManager.getInstance(project).getToolWindow(ActivityLogToolWindowFactory.ACTIVITY_LOG_WINDOW).activate(null);
            }
        });
    }

    public void deployToWebApps(WebApp webApp, String url) {
        Date startDate = new Date();
        try {
            String msg = String.format(message("webAppDeployMsg"), webApp.name());
            notifyProgress(webApp.name(), startDate, null, 20, msg);
            Thread.sleep(2000);
            notifyProgress(webApp.name(), startDate, null, 20, msg);
            notifyProgress(webApp.name(), startDate, null, 20, msg);
            notifyProgress(webApp.name(), startDate, null, 20, msg);
            Thread.sleep(2000);
            notifyProgress(webApp.name(), startDate, url, 20, message("runStatus"), webApp.name());
        } catch (InterruptedException e) {
            notifyProgress(webApp.name(), startDate, url, 100, message("runStatus"), webApp.name());
        }
    }

    public void deployToDockerContainer(AzureDockerImageInstance dockerImageInstance, String url) {
        Date startDate = new Date();
        try {
            ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationConfig.Feature.INDENT_OUTPUT, true);

            String msg = String.format("Deploying application to Docker host %s ...", dockerImageInstance.host.name);
            notifyProgress(dockerImageInstance.host.name, startDate, null, 5, msg);
            AzureDockerHostsManager dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(null);
            Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerImageInstance.sid).azureClient;
            KeyVaultClient keyVaultClient = dockerManager.getSubscriptionsMap().get(dockerImageInstance.sid).keyVaultClient;

            if (dockerImageInstance.hasNewDockerHost) {
                msg = String.format("Creating new virtual machine %s ...", dockerImageInstance.host.name);
                notifyProgress(dockerImageInstance.host.name, startDate, null, 25, msg);
                System.out.println("Creating new virtual machine: " + new Date().toString());
                AzureDockerVMOps.createDockerHostVM(azureClient, dockerImageInstance.host);
                System.out.println("Done creating new virtual machine: " + new Date().toString());

                msg = String.format("Waiting for virtual machine to be up %s ...", dockerImageInstance.host.name);
                notifyProgress(dockerImageInstance.host.name, startDate, null, 5, msg);
                System.out.println("Waiting for virtual machine to be up: " + new Date().toString());
                AzureDockerVMOps.waitForVirtualMachineStartup(azureClient, dockerImageInstance.host);
                System.out.println("Done Waiting for virtual machine to be up: " + new Date().toString());

                msg = String.format("Configuring Docker service for %s ...", dockerImageInstance.host.name);
                notifyProgress(dockerImageInstance.host.name, startDate, null, 15, msg);
                System.out.println("Configuring Docker host: " + new Date().toString());
                AzureDockerVMOps.installDocker(dockerImageInstance.host);
                System.out.println("Done configuring Docker host: " + new Date().toString());

                System.out.println("Finished setting up Docker host");
            } else {
                msg = String.format("Using virtual machine %s ...", dockerImageInstance.host.name);
                notifyProgress(dockerImageInstance.host.name, startDate, null, 45, msg);
            }
            System.out.println(mapper.writeValueAsString(dockerImageInstance.host));

            if (dockerImageInstance.host.session == null) {
                System.out.println("Opening a remote connection to the Docker host: " + new Date().toString());
                dockerImageInstance.host.session = AzureDockerSSHOps.createLoginInstance(dockerImageInstance.host);
                System.out.println("Done opening a remote connection to the Docker host: " + new Date().toString());
            }

            if (dockerImageInstance.hasNewDockerHost && dockerImageInstance.host.certVault.hostName != null) {
                msg = String.format("Creating new key vault %s ...", dockerImageInstance.host.certVault.name);
                notifyProgress(dockerImageInstance.host.name, startDate, null, 15, msg);
                System.out.println("Creating new Docker key vault: " + new Date().toString());
                AzureDockerCertVaultOps.createOrUpdateVault(azureClient, dockerImageInstance.host.certVault, keyVaultClient);
                System.out.println("Done creating new key vault: " + new Date().toString());

                msg = String.format("Updating key vaults ...");
                notifyProgress(dockerImageInstance.host.name, startDate, null, 10, msg);
                System.out.println("Refreshing key vaults: " + new Date().toString());
                dockerManager.refreshDockerVaults();
                dockerManager.refreshDockerVaultDetails();
                System.out.println("Done refreshing key vaults: " + new Date().toString());
            }

            msg = String.format("Uploading Dockerfile and artifact %s on %s ...", dockerImageInstance.artifactName, dockerImageInstance.host.name);
            notifyProgress(dockerImageInstance.host.name, startDate, null, 10, msg);
            System.out.println("Uploading Dockerfile and artifact: " + new Date().toString());
            AzureDockerVMOps.uploadDockerfileAndArtifact(dockerImageInstance, dockerImageInstance.host.session);
            System.out.println("Uploading Dockerfile and artifact: " + new Date().toString());

            msg = String.format("Creating Docker image %s on %s ...", dockerImageInstance.dockerImageName, dockerImageInstance.host.name);
            notifyProgress(dockerImageInstance.host.name, startDate, null, 10, msg);
            System.out.println("Creating a Docker image to the Docker host: " + new Date().toString());
            AzureDockerImageOps.create(dockerImageInstance, dockerImageInstance.host.session);
            System.out.println("Done creating a Docker image to the Docker host: " + new Date().toString());

            msg = String.format("Creating Docker container %s for image %s on %s ...", dockerImageInstance.dockerContainerName, dockerImageInstance.dockerImageName, dockerImageInstance.host.name);
            notifyProgress(dockerImageInstance.host.name, startDate, null, 5, msg);
            System.out.println("Creating a Docker container to the Docker host: " + new Date().toString());
            AzureDockerContainerOps.create(dockerImageInstance, dockerImageInstance.host.session);
            System.out.println("Done creating a Docker container to the Docker host: " + new Date().toString());

            msg = String.format("Starting Docker container %s for image %s on %s ...", dockerImageInstance.dockerContainerName, dockerImageInstance.dockerImageName, dockerImageInstance.host.name);
            notifyProgress(dockerImageInstance.host.name, startDate, null, 5, msg);
            System.out.println("Starting a Docker container to the Docker host: " + new Date().toString());
            AzureDockerContainerOps.start(dockerImageInstance, dockerImageInstance.host.session);
            System.out.println("Done starting a Docker container to the Docker host: " + new Date().toString());

            msg = String.format("Updating Docker hosts ...");
            notifyProgress(dockerImageInstance.host.name, startDate, null, 25, msg);
            System.out.println("Refreshing docker hosts: " + new Date().toString());
            dockerManager.refreshDockerHostDetails();
            System.out.println("Done refreshing Docker hosts: " + new Date().toString());
            System.out.println("Done refreshing key vaults: " + new Date().toString());

            notifyProgress(dockerImageInstance.host.name, startDate, url, 100, message("runStatus"), dockerImageInstance.host.name);
        } catch (InterruptedException e) {
            notifyProgress(dockerImageInstance.host.name, startDate, url, 100, message("runStatus"), dockerImageInstance.host.name);
        } catch (Exception ee) {
            notifyProgress(dockerImageInstance.host.name, startDate, url, 100, ee.getMessage(), dockerImageInstance.host.name);
        }
    }
}
