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
package com.microsoft.intellij.ui.libraries;

public class AzureLibrary {
    public static AzureLibrary ACS_FILTER = new AzureLibrary("Azure Access Control Services Filter (by Microsoft)",
            "ACSAuthFilter.jar", new String[]{"ACSAuthFilter.jar"});
    public static AzureLibrary QPID_CLIENT = new AzureLibrary("Package for Apache Qpid Client Libraries for JMS (by Microsoft)",
            "com-microsoftopentechnologies-qpid", new String[]{});
    public static AzureLibrary AZURE_LIBRARIES = new AzureLibrary("Package for Microsoft Azure Libraries for Java (by Microsoft)",
            "com-microsoftopentechnologies-windowsazure-tools-sdk",
            new String[]{
                    "azure-core-0.9.2.jar",
                    "azure-svc-mgmt-0.9.2.jar",
                    "azure-svc-mgmt-compute-0.9.2.jar",
                    "azure-svc-mgmt-network-0.9.2.jar",
                    "azure-svc-mgmt-storage-0.9.2.jar",
                    "azure-storage-4.0.0.jar",
                    "commons-lang3-3.4.jar",
                    "commons-lang-2.6.jar",
                    "guava-18.0.jar",
                    "jackson-core-2.6.0.jar",
                    "jackson-core-asl-1.9.2.jar",
                    "jackson-jaxrs-1.9.2.jar",
                    "jackson-mapper-asl-1.9.2.jar",
                    "jackson-xc-1.9.2.jar",
                    "javax.inject-1.jar",
                    "jcip-annotations-1.0.jar",
                    "jersey-client-1.13.jar",
                    "jersey-core-1.13.jar",
                    "jersey-json-1.13.jar",
                    "jettison-1.1.jar",
                    "mail-1.4.5.jar"
            });
    public static AzureLibrary[] LIBRARIES = new AzureLibrary[]{ACS_FILTER, QPID_CLIENT, AZURE_LIBRARIES};

    private String name;
    private String location;
    private String[] files;

    public AzureLibrary(String name, String location, String[] files) {
        this.name = name;
        this.location = location;
        this.files = files;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String[] getFiles() {
        return files;
    }
}
