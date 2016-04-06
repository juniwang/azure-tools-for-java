package com.microsoft.azure.hdinsight.projects;


import com.microsoft.azure.hdinsight.common.DefaultLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.jar.JarFile;

public class SparkLibraryInfo {
    private String localPath;
    private String title;
    private String version;

    private static final String TITLE_LABEL = "Implementation-Title";
    private static final String VERSION_LABEL = "Implementation-Version";

    public SparkLibraryInfo(@NotNull String path) throws Exception{
        if(path.endsWith("!/")) {
            path = path.substring(0, path.length() - 2);
        }

        this.localPath = path.toLowerCase();
        JarFile jarFile = new JarFile(this.localPath);
        title = jarFile.getManifest().getMainAttributes().getValue(TITLE_LABEL);
        version = jarFile.getManifest().getMainAttributes().getValue(VERSION_LABEL);
    }

    @NotNull
    public String getLocalPath() {
        return localPath;
    }

    @Override
    public int hashCode() {
        return title.hashCode() + version.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(obj instanceof SparkLibraryInfo) {
            SparkLibraryInfo other = (SparkLibraryInfo)obj;
            return this.title.equals(other.title) && this.version.equals(version);
        }

        return false;
    }
}
