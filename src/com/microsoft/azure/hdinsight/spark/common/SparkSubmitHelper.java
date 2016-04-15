package com.microsoft.azure.hdinsight.spark.common;

import com.google.common.base.Joiner;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.common.*;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.BlobContainer;
import com.microsoft.azure.hdinsight.sdk.storage.CallableSingleArg;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.StorageClientImpl;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

public class SparkSubmitHelper {
    private static SparkSubmitHelper ourInstance = new SparkSubmitHelper();

    private static final int MAX_INTERVAL_TIME = 5000;
    private static final int MIN_INTERVAL_TIME = 1000;
    private static final int INC_TIME = 100;

    private static final String applicationIdPattern = "Application report for ([^ ]*) \\(state: ACCEPTED\\)";

    private SparkJobLog sparkJobLog;

    public static SparkSubmitHelper getInstance() {
        return ourInstance;
    }

    private SparkSubmitHelper() {
    }

    private String JobLogFolderName = "SparkJobLog";

    public String writeLogToLocalFile(@NotNull Project project) throws IOException{
        if (sparkJobLog == null) {
            return null;
        }

        String applicationId = PluginUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().getApplicationId();
        String pluginRootPath = PluginUtil.getPluginRootDirectory();
        String folderPath = StringHelper.concat(pluginRootPath, File.separator, JobLogFolderName, File.separator, applicationId);
        String fullFileName = StringHelper.concat(folderPath, File.separator, "log.txt");

        File folder = null;
        FileWriter logFileWrite = null;
        BufferedWriter bufferedWriter = null;
        try {
            folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            logFileWrite = new FileWriter(fullFileName);
            bufferedWriter = new BufferedWriter(logFileWrite);
            for (String str : sparkJobLog.getLog()) {
                bufferedWriter.write(str);
                bufferedWriter.newLine();
            }
        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.flush();
                bufferedWriter.close();
            }

            if(logFileWrite != null) {
                logFileWrite.close();
            }

            return fullFileName;
        }
    }

    public void printRunningLogStreamingly(Project project, int id, IClusterDetail clusterDetail, Map<String, String> postEventProperty) throws IOException {
        try {
            boolean isFailedJob = false;
            boolean isKilledJob = false;

            int from_index = 0;
            int pre_index;
            int times = 0;
            PluginUtil.getSparkSubmissionToolWindowManager(project).setInfo("======================Begin printing out spark job log.=======================");
            while (true) {
                pre_index = from_index;
                if (PluginUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().isJobKilled() == true) {
                    isKilledJob = true;
                    break;
                }

                from_index = printoutJobLog(project, id, from_index, clusterDetail);
                HttpResponse statusHttpResponse = SparkBatchSubmission.getInstance().getBatchSparkJobStatus(clusterDetail.getConnectionUrl() + "/livy/batches", id);

                SparkSubmitResponse status = new Gson().fromJson(statusHttpResponse.getMessage(), new TypeToken<SparkSubmitResponse>() {
                }.getType());

                // only the lines of the log are same between two http requests, we try to get the job status
                if (from_index == pre_index) {
                    if (status.getState().toLowerCase().equals("error") || status.getState().toLowerCase().equals("success")) {
                        if (status.getState().toLowerCase().equals("error")) {
                            isFailedJob = true;
                        }

                        if (PluginUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().isJobKilled() == false) {
                            printoutJobLog(project, id, from_index, clusterDetail);
                            PluginUtil.getSparkSubmissionToolWindowManager(project).setInfo("======================Finish printing out spark job log.=======================");
                        } else {
                            isKilledJob = true;
                        }
                        break;
                    }
                }

                Thread.sleep(getIntervalTime(times));
                times++;
            }


            if (isKilledJob) {
                postEventProperty.put("IsKilled", "true");
                TelemetryManager.postEvent(TelemetryCommon.SparkSubmissionButtonClickEvent, postEventProperty, null);
                return;
            }

            if (isFailedJob) {
                postEventProperty.put("IsRunningSucceed", "false");
                PluginUtil.getSparkSubmissionToolWindowManager(project).setError("Error : Your submitted job run failed");
            } else {
                postEventProperty.put("IsRunningSucceed", "true");
                PluginUtil.getSparkSubmissionToolWindowManager(project).setInfo("The Spark application completed successfully");
            }

            TelemetryManager.postEvent(TelemetryCommon.SparkSubmissionButtonClickEvent, postEventProperty, null);

        } catch (Exception e) {
            if (PluginUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().isJobKilled() == false) {
                PluginUtil.getSparkSubmissionToolWindowManager(project).setError("Error : Failed to getting running log. Exception : " + e.toString());
            } else {
                postEventProperty.put("IsKilled", "true");
                TelemetryManager.postEvent(TelemetryCommon.SparkSubmissionButtonClickEvent, postEventProperty, null);
            }
        }
    }

    public String uploadFileToAzureBlob(Project project, String localFile, StorageAccount storageAccount, String defaultContainerName, String uniqueFolderId)
            throws HDIException, IOException {
        final File file = new File(localFile);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
                final CallableSingleArg<Void, Long> callable = new CallableSingleArg<Void, Long>() {
                    @Override
                    public Void call(Long uploadedBytes) throws Exception {
                        double progress = ((double) uploadedBytes) / file.length();
                        return null;
                    }
                };

                BlobContainer defaultContainer = getSparkClusterDefaultContainer(storageAccount, defaultContainerName);
                String path = String.format("SparkSubmission/%s/%s", uniqueFolderId, file.getName());
                String uploadedPath = String.format("wasb://%s@%s/%s", defaultContainerName, storageAccount.getFullStoragBlobName(), path);

                PluginUtil.showInfoOnSubmissionMessageWindow(project,
                        String.format("Info : Begin uploading file %s to Azure Blob Storage Account %s ...", localFile, uploadedPath));

                StorageClientImpl.getInstance().uploadBlobFileContent(
                        storageAccount,
                        defaultContainer,
                        path,
                        bufferedInputStream,
                        callable,
                        1024 * 1024,
                        file.length());

                PluginUtil.showInfoOnSubmissionMessageWindow(project, String.format("Info : Submit file to azure blob '%s' successfully.", uploadedPath));
                return uploadedPath;
            }
        }
    }

    private int printoutJobLog(Project project, int id, int from_index, IClusterDetail clusterDetail) throws IOException {
        HttpResponse httpResponse = SparkBatchSubmission.getInstance().getBatchJobFullLog(clusterDetail.getConnectionUrl() + "/livy/batches", id);
        sparkJobLog = new Gson().fromJson(httpResponse.getMessage(), new TypeToken<SparkJobLog>() {
        }.getType());

        if (!PluginUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().isApplicationGenerated()) {
            String sparkLogs = Joiner.on("").join(sparkJobLog.getLog());
            String applicationId = getApplicationIdFromYarnLog(sparkLogs);
            if (applicationId != null) {
                PluginUtil.getSparkSubmissionToolWindowManager(project).setBrowserButtonState(true);
                PluginUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().setApplicationIdGenerated();
                PluginUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().setApplicationId(applicationId);
            }
        }

        int counter = 0;
        if (sparkJobLog.getLog().size() > 0) {
            for (String line : sparkJobLog.getLog()) {
                if (counter >= from_index && !StringHelper.isNullOrWhiteSpace(line)) {
                    PluginUtil.getSparkSubmissionToolWindowManager(project).setInfo(line, true);
                }

                counter++;
            }
        }

        return sparkJobLog.getTotal();
    }

    private BlobContainer getSparkClusterDefaultContainer(StorageAccount storageAccount, String dealtContainerName) throws HDIException {
        List<BlobContainer> containerList = StorageClientImpl.getInstance().getBlobContainers(storageAccount);
        for (BlobContainer container : containerList) {
            if (container.getName().toLowerCase().equals(dealtContainerName.toLowerCase())) {
                return container;
            }
        }

        return null;
    }

    private int getIntervalTime(int times) {
        int interval = MIN_INTERVAL_TIME + times * INC_TIME;
        return interval > MAX_INTERVAL_TIME ? MAX_INTERVAL_TIME : interval;
    }

    private String getApplicationIdFromYarnLog(String yarnLog) {
        Pattern r = Pattern.compile(applicationIdPattern);
        Matcher m = r.matcher(yarnLog);
        if (m.find()) {
            return m.group(1);
        }

        return null;
    }

    public static String uploadFileToAzureBlob(@org.jetbrains.annotations.NotNull Project project, @NotNull IClusterDetail selectedClusterDetail,@NotNull String buildJarPath) throws Exception {

        PluginUtil.showInfoOnSubmissionMessageWindow(project, String.format("Info : Get target jar from %s.", buildJarPath));
        String uniqueFolderId = UUID.randomUUID().toString();

        return SparkSubmitHelper.getInstance().uploadFileToAzureBlob(project, buildJarPath,
                selectedClusterDetail.getStorageAccount(), selectedClusterDetail.getStorageAccount().getDefaultContainer(), uniqueFolderId);
    }

    public static boolean isLocalArtifactPath(String path) {
        if (StringHelper.isNullOrWhiteSpace(path)) {
            return false;
        }

        if (path.endsWith("!/")) {
            path = path.substring(0, path.length() - 2);
        }

        return path.endsWith(".jar");

    }

    public static final String HELP_LINK = "http://go.microsoft.com/fwlink/?LinkID=722349&clcid=0x409";
}
