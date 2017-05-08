package com.microsoft.azuretools.azureexplorer.editors;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.azureexplorer.hdinsight.*;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;

public class JobViewEditor extends EditorPart {

	private IClusterDetail clusterDetail;
	private String uuid;

	@Override
	public void doSave(IProgressMonitor iProgressMonitor) {
		AppInsightsClient.create("HDInsight.Spark.CloseJobviewPage", null);
	}

	@Override
	public void doSaveAs() {
		AppInsightsClient.create("HDInsight.Spark.CloseJobviewPage", null);
	}

	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		AppInsightsClient.create("HDInsight.Spark.OpenJobviewPage", null);
		setSite(site);
		setInput(input);
		clusterDetail = ((JobViewInput) input).getClusterDetail();
		uuid = ((JobViewInput) input).getUuid();
		setPartName(clusterDetail.getName() + " Spark JobView");
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite composite) {
		composite.setLayout(new FillLayout());
	
		final String indexPath = PluginUtil.pluginFolder + "/com.microsoft.azuretools.hdinsight/html"
				+ "/hdinsight/job/html/index.html";
		File indexFile = new File(indexPath);
		if(indexFile.exists()) {
			final String queryString = "?projectid=" + uuid + "&engintype=javafx&sourcetype=eclipse&clustername=" + clusterDetail.getName();
			final String webUrl = "file:///" + indexPath.replace("\\", "/") + queryString;
			FxClassLoader.loadJavaFxForJobView(composite, webUrl);
		} else {
			DefaultLoader.getUIHelper().showError("HDInsight Job view index page not exist!", "job view");
		}
	}

	@Override
	public void setFocus() {
	}

}