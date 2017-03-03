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
package com.microsoft.azure.hdinsight.sdk.storage;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;

public class HDStorageAccount extends ClientStorageAccount implements IHDIStorageAccount {
    private String fullStorageBlobName;
    private boolean isDefaultStorageAccount;
    private String defaultContainer;
    private IClusterDetail clusterDetail;

    public HDStorageAccount(IClusterDetail clusterDetail, String name, String key, boolean isDefault, String defaultContainer) {
        super(name.replace(".blob.core.windows.net", ""));
        this.setPrimaryKey(key);
        this.fullStorageBlobName = name;
        this.isDefaultStorageAccount = isDefault;
        this.defaultContainer = defaultContainer;
        this.clusterDetail = clusterDetail;
    }

    @Override
    public String getSubscriptionId() {
        return this.clusterDetail == null ? "" : this.clusterDetail.getSubscription().getSubscriptionId();
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public StorageAccountTypeEnum getAccountType() {
        return StorageAccountTypeEnum.BLOB;
    }

    @Override
    public String getDefaultContainerOrRootPath() {
        return defaultContainer;
    }

    public String getFullStorageBlobName() {
        return fullStorageBlobName;
    }

    public boolean isDefaultStorageAccount() {
        return isDefaultStorageAccount;
    }

    public String getDefaultContainer() {
        return defaultContainer;
    }
}
