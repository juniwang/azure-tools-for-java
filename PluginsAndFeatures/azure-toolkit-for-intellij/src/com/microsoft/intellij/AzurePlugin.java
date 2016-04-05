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
package com.microsoft.intellij;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.PlatformUtils;
import com.intellij.util.containers.HashSet;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResourceRegistry;
import com.microsoft.intellij.ui.libraries.AILibraryHandler;
import com.microsoft.intellij.ui.libraries.AzureLibrary;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.intellij.util.AppInsightsCustomEvent;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.util.WAHelper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureToolkitFilter;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.azurecommons.xmlhandling.DataOperations;
import com.microsoftopentechnologies.azurecommons.deploy.DeploymentEventArgs;
import com.microsoftopentechnologies.azurecommons.deploy.DeploymentEventListener;
import com.microsoftopentechnologies.azurecommons.wacommonutil.FileUtil;
import com.microsoftopentechnologies.azurecommons.xmlhandling.ParseXMLUtilMethods;
import com.microsoftopentechnologies.windowsazure.tools.cspack.Utils;

import javax.swing.event.EventListenerList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;


public class AzurePlugin extends AbstractProjectComponent {
    private static final Logger LOG = Logger.getInstance("#com.microsoft.intellij.AzurePlugin");
    public static final String PLUGIN_ID = "azure-toolkit-for-intellij";
    public static final String COMPONENTSETS_VERSION = "2.9.0"; // todo: temporary fix!
    public static final String PLUGIN_VERSION = "1.3";
    private static final String PREFERENCESETS_VERSION = "2.9.0";
    public static final String AZURE_LIBRARIES_VERSION = "0.9.2";
    public static final String QPID_LIBRARIES_VERSION = "0.19.0";
    public final static int REST_SERVICE_MAX_RETRY_COUNT = 7;

    // User-agent header for Azure SDK calls
    public static final String USER_AGENT = "Azure Toolkit for IntelliJ, v%s";

    public static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
    public static boolean IS_ANDROID_STUDIO = "AndroidStudio".equals(PlatformUtils.getPlatformPrefix());

    private static final String COMPONENTSETS_TYPE = "COMPONENTSETS";
    private static final String PREFERENCESETS_TYPE = "PREFERENCESETS";

    public static File cmpntFile = new File(WAHelper.getTemplateFile(message("cmpntFileName")));
    public static String prefFilePath = WAHelper.getTemplateFile(message("prefFileName"));
    public static String pluginFolder = String.format("%s%s%s", PathManager.getPluginsPath(), File.separator, AzurePlugin.PLUGIN_ID);

    private static final EventListenerList DEPLOYMENT_EVENT_LISTENERS = new EventListenerList();
    public static List<DeploymentEventListener> depEveList = new ArrayList<DeploymentEventListener>();

    String dataFile = WAHelper.getTemplateFile(message("dataFileName"));

    private final AzureSettings azureSettings;
    public static Project project;

    public AzurePlugin(Project project) {
        super(project);
        this.project = project;
        this.azureSettings = AzureSettings.getSafeInstance(project);
        AzureToolkitFilter.setUserAgent(String.format(USER_AGENT, PLUGIN_VERSION));
    }

    public void projectOpened() {
        initializeAIRegistry();
    }

    public void projectClosed() {
    }

    /**
     * Method is called after plugin is already created and configured. Plugin can start to communicate with
     * other plugins only in this method.
     */
    public void initComponent() {
        if (!IS_ANDROID_STUDIO) {
            LOG.info("Starting Azure Plugin");
            try {
                azureSettings.loadStorage();
                //this code is for copying componentset.xml in plugins folder
                copyPluginComponents();
                initializeTelemetry();
                clearTempDirectory();
                loadWebappsSettings();
            } catch (Exception e) {
            /* This is not a user initiated task
               So user should not get any exception prompt.*/
                LOG.error(AzureBundle.message("expErlStrtUp"), e);
            }
        }
    }

    private void initializeTelemetry() throws Exception {
        if (new File(dataFile).exists()) {
            String version = DataOperations.getProperty(dataFile, message("pluginVersion"));
            if (version == null || version.isEmpty()) {
                // proceed with setValues method as no version specified
                setValues(dataFile);
            } else {
                String curVersion = PLUGIN_VERSION;
                // compare version
                if (curVersion.equalsIgnoreCase(version)) {
                    // Case of normal IntelliJ restart
                    // check preference-value & installation-id exists or not else copy values
                    String prefValue = DataOperations.getProperty(dataFile, message("prefVal"));
                    String instID = DataOperations.getProperty(dataFile, message("instID"));
                    if (prefValue == null || prefValue.isEmpty()) {
                        setValues(dataFile);
                    } else if (instID == null || instID.isEmpty()) {
                        Document doc = ParseXMLUtilMethods.parseFile(dataFile);
                        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                        DataOperations.updatePropertyValue(doc, message("instID"), dateFormat.format(new Date()));
                        ParseXMLUtilMethods.saveXMLDocument(dataFile, doc);
                    }
                } else {
                    // proceed with setValues method. Case of new plugin installation
                    setValues(dataFile);
                }
            }
        } else {
            // copy file and proceed with setValues method
            copyResourceFile(message("dataFileName"), dataFile);
            setValues(dataFile);
        }
    }

    private void initializeAIRegistry() {
        try {
            AzureSettings.getSafeInstance(project).loadAppInsights();
            Module[] modules = ModuleManager.getInstance(project).getModules();
            for (Module module : modules) {
                if (module != null && module.isLoaded() && ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE))) {
                    String aiXMLPath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("aiXMLPath"));
                    if (new File(aiXMLPath).exists()) {
                        AILibraryHandler handler = new AILibraryHandler();
                        handler.parseAIConfXmlPath(aiXMLPath);
                        String key = handler.getAIInstrumentationKey();
                        if (key != null && !key.isEmpty()) {
                            String unknown = message("unknown");
                            List<ApplicationInsightsResource> list =
                                    ApplicationInsightsResourceRegistry.getAppInsightsResrcList();
                            ApplicationInsightsResource resourceToAdd = new ApplicationInsightsResource(
                                    key, key, unknown, unknown, unknown, unknown, false);
                            if (!list.contains(resourceToAdd)) {
                                ApplicationInsightsResourceRegistry.getAppInsightsResrcList().add(resourceToAdd);
                            }
                        }
                    }
                }
            }
            AzureSettings.getSafeInstance(project).saveAppInsights();
        } catch (Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
        }
    }

    private void setValues(final String dataFile) throws Exception {
        final Document doc = ParseXMLUtilMethods.parseFile(dataFile);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                boolean accepted = Messages.showYesNoDialog(message("preferenceQueMsg"), message("preferenceQueTtl"), null) == Messages.YES;
                DataOperations.updatePropertyValue(doc, message("prefVal"), String.valueOf(accepted));

                DataOperations.updatePropertyValue(doc, message("pluginVersion"), PLUGIN_VERSION);
                DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                DataOperations.updatePropertyValue(doc, message("instID"), dateFormat.format(new Date()));
                try {
                    ParseXMLUtilMethods.saveXMLDocument(dataFile, doc);
                } catch (Exception ex) {
                    LOG.error(message("error"), ex);
                }
                if (accepted) {
                    AppInsightsCustomEvent.create(message("telAgrEvtName"), "");
                }
            }
        }, ModalityState.defaultModalityState());
    }

    /**
     *  Delete %proj% directory from temporary folder during IntelliJ start
     *  To fix #2943 : Hang invoking a new Azure project,
     *  PML does not delete .cspack.jar everytime new azure project is created.
     *  Hence its necessary to delete %proj% directory when plugin with newer version is installed.
     * @throws Exception
     */
    private void clearTempDirectory() throws Exception {
        String tmpPath = System.getProperty("java.io.tmpdir");
        String projPath = String.format("%s%s%s", tmpPath, File.separator, "%proj%");
        File projFile = new File(projPath);
        if (projFile != null) {
            WAEclipseHelperMethods.deleteDirectory(projFile);
        }
    }

    private void loadWebappsSettings() {
        StartupManager.getInstance(project).runWhenProjectIsInitialized(
                new Runnable() {
                    @Override
                    public void run() {
                        Module[] modules = ModuleManager.getInstance(project).getModules();
                        Set<String> javaModules = new HashSet<String>();
                        for (Module module : modules) {
                            if (ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE))) {
                                javaModules.add(module.getName());
                            }
                        }
                        Set<String> keys = AzureSettings.getSafeInstance(project).getPropertyKeys();
                        for (String key : keys) {
                            if (key.endsWith(".webapps")) {
                                String projName = key.substring(0, key.lastIndexOf("."));
                                if (!javaModules.contains(projName)) {
                                    AzureSettings.getSafeInstance(project).unsetProperty(key);
                                }
                            }
                        }
                    }
                });
    }

    private void telemetryAI() {
        ModuleManager.getInstance(project).getModules();
    }

    public String getComponentName() {
        return "MSOpenTechTools.AzurePlugin";
    }

    /**
     * Copies MS Open Tech Tools for Azure
     * related files in azure-toolkit-for-intellij plugin folder at startup.
     */
    private void copyPluginComponents() {
        try {
            String pluginInstLoc = String.format("%s%s%s", PathManager.getPluginsPath(), File.separator, PLUGIN_ID);

            String cmpntFile = String.format("%s%s%s", pluginInstLoc,
                    File.separator, AzureBundle.message("cmpntFileName"));
            String starterKit = String.format("%s%s%s", pluginInstLoc,
                    File.separator, AzureBundle.message("starterKitFileName"));
            String enctFile = String.format("%s%s%s", pluginInstLoc,
                    File.separator, message("encFileName"));
            String prefFile = String.format("%s%s%s", pluginInstLoc,
                    File.separator, AzureBundle.message("prefFileName"));

            // upgrade component sets and preference sets
            upgradePluginComponent(cmpntFile, AzureBundle.message("cmpntFileEntry"), AzureBundle.message("oldCmpntFileEntry"), COMPONENTSETS_TYPE);
            upgradePluginComponent(prefFile, AzureBundle.message("prefFileEntry"), AzureBundle.message("oldPrefFileEntry"), PREFERENCESETS_TYPE);

            // Check for WAStarterKitForJava.zip
            if (new File(starterKit).exists()) {
                new File(starterKit).delete();
            }
            // Check for encutil.exe
            if (new File(enctFile).exists()) {
                new File(enctFile).delete();
            }
            copyResourceFile(message("starterKitEntry"), starterKit);
            copyResourceFile(message("encFileName"), enctFile);
            for (AzureLibrary azureLibrary : AzureLibrary.LIBRARIES) {
                if (!new File(pluginInstLoc + File.separator + azureLibrary.getLocation()).exists()) {
                    for (String entryName : Utils.getJarEntries(pluginInstLoc + File.separator + "lib" + File.separator + PLUGIN_ID + ".jar", azureLibrary.getLocation())) {
                        new File(pluginInstLoc + File.separator + entryName).getParentFile().mkdirs();
                        copyResourceFile(entryName, pluginInstLoc + File.separator + entryName);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * Checks for pluginComponent file.
     * If exists checks its version.
     * If it has latest version then no upgrade action is needed,
     * else checks with older componentsets.xml,
     * if identical then deletes existing and copies new one
     * else renames existing and copies new one.
     *
     * @param pluginComponentPath
     * @param resource
     * @param componentType
     * @throws Exception
     */
    private void upgradePluginComponent(String pluginComponentPath, String resource,
                                        String oldResource, String componentType) throws Exception {
        File pluginComponentFile = new File(pluginComponentPath);
        if (pluginComponentFile.exists()) {
            String pluginComponentVersion = null;
            String resourceFileVersion = null;
//        	File resourceFile = new File(((PluginClassLoader)AzurePlugin.class.getClassLoader()).findResource(resource).toURI());
            try {
                if (COMPONENTSETS_TYPE.equals(componentType)) {
                    pluginComponentVersion = WindowsAzureProjectManager.getComponentSetsVersion(pluginComponentFile);
                    resourceFileVersion = COMPONENTSETS_VERSION; //WindowsAzureProjectManager.getComponentSetsVersion(resourceFile);
                } else {
                    pluginComponentVersion = WindowsAzureProjectManager.getPreferenceSetsVersion(pluginComponentFile);
                    resourceFileVersion = PREFERENCESETS_VERSION; //WindowsAzureProjectManager.getPreferenceSetsVersion(resourceFile);
                }
            } catch (Exception e) {
                LOG.error("Error occured while getting version of plugin component " + componentType + ", considering version as null");
            }
            if ((pluginComponentVersion != null
                    && !pluginComponentVersion.isEmpty())
                    && pluginComponentVersion.equals(resourceFileVersion)) {
                // Do not do anything
            } else {
                // Check with old plugin component for upgrade scenarios
                URL oldPluginComponentUrl = ((PluginClassLoader) AzurePlugin.class.getClassLoader()).findResource(oldResource);
//                InputStream oldPluginComponentIs = AzurePlugin.class.getResourceAsStream(oldResourceFile);
                boolean isIdenticalWithOld = WAHelper.isFilesIdentical(oldPluginComponentUrl, pluginComponentFile);
                if (isIdenticalWithOld) {
                    // Delete old one
                    pluginComponentFile.delete();
                } else {
                    // Rename old one
                    DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                    Date date = new Date();
                    WAHelper.copyFile(pluginComponentPath, pluginComponentPath + ".old" + dateFormat.format(date));
                }
                copyResourceFile(resource, pluginComponentPath);
            }
        } else {
            copyResourceFile(resource, pluginComponentPath);
        }
    }

    /**
     * Method copies specified file from plugin resources
     *
     * @param resourceFile
     * @param destFile
     */
    public static void copyResourceFile(String resourceFile, String destFile) {
        try {
            InputStream is = ((PluginClassLoader) AzurePlugin.class.getClassLoader()).findResource(resourceFile).openStream();
            File outputFile = new File(destFile);
            FileOutputStream fos = new FileOutputStream(outputFile);
            FileUtil.writeFile(is, fos);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static void fireDeploymentEvent(DeploymentEventArgs args) {
        Object[] list = DEPLOYMENT_EVENT_LISTENERS.getListenerList();

        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == DeploymentEventListener.class) {
                ((DeploymentEventListener) list[i + 1]).onDeploymentStep(args);
            }
        }
    }

    public static void addDeploymentEventListener(DeploymentEventListener listener) {
        DEPLOYMENT_EVENT_LISTENERS.add(DeploymentEventListener.class, listener);
    }

    public static void removeDeploymentEventListener(DeploymentEventListener listener) {
        DEPLOYMENT_EVENT_LISTENERS.remove(DeploymentEventListener.class, listener);
    }

    // todo: move field somewhere?
    public static void removeUnNecessaryListener() {
        for (int i = 0; i < depEveList.size(); i++) {
            removeDeploymentEventListener(depEveList.get(i));
        }
        depEveList.clear();
    }

    public static void log(String message, Exception ex) {
        LOG.error(message, ex);
        LOG.info(message);
    }

    public static void log(String message) {
        LOG.info(message);
    }
}
