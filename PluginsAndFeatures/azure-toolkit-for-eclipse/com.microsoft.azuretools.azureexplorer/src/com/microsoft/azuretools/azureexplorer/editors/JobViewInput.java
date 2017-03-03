package com.microsoft.azuretools.azureexplorer.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.StorageServiceTreeItem;

public class JobViewInput implements IEditorInput {
    private IClusterDetail clusterDetail;
    private String uuid;

    public JobViewInput(IClusterDetail clusterDetail, String uuid) {
        this.clusterDetail = clusterDetail;
        this.uuid = uuid;
    }
    
    public IClusterDetail getClusterDetail() {
		return clusterDetail;
	}

	public String getUuid() {
		return uuid;
	}

	@Override
    public boolean exists() {
        return false;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return null;
    }

    @Override
    public Object getAdapter(Class aClass) {
        return null;
    }
}