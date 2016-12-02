package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.model.Subscription;

import java.io.IOException;
import java.util.List;

public class EmulatorClusterDetail implements IClusterDetail {

	private String clusterName;
	private String userName;
	private String passWord;
	private String livyEndpoint;
	private String sshEndpoint;
	private String sparkHistoryEndpoint;
	private String ambariEndpoint;

	public EmulatorClusterDetail(String clusterName, String userName, String passWord, String livyEndpoint, String sshEndpoint, String sparkHistoryEndpoint, String ambariEndpoint) {
		this.clusterName = clusterName;
		this.userName = userName;
		this.passWord = passWord;
		this.livyEndpoint = livyEndpoint;
		this.sshEndpoint = sshEndpoint;
		this.sparkHistoryEndpoint = sparkHistoryEndpoint;
		this.ambariEndpoint = ambariEndpoint;
	}

	public String getSparkHistoryEndpoint() { return sparkHistoryEndpoint; }

	public String getAmbariEndpoint() { return ambariEndpoint; }

	public String getSSHEndpoint() { return sshEndpoint; }

	@Override
		public boolean isEmulator() {
			return true;
		}

	@Override
		public boolean isConfigInfoAvailable() {
			return false;
		}

	@Override
		public String getName() {
			return clusterName;
		}

	@Override
		public String getState() {
			return null;
		}

	@Override
		public String getLocation() {
			return null;
		}

	@Override
		public String getConnectionUrl() {
			return livyEndpoint;
		}

	@Override
		public String getCreateDate() {
			return null;
		}

	@Override
		public ClusterType getType() {
			return null;
		}

	@Override
		public String getVersion() {
			return null;
		}

	@Override
		public Subscription getSubscription() {
			return null;
		}

	@Override
		public int getDataNodes() {
			return 0;
		}

	@Override
		public String getHttpUserName() throws HDIException {
			return userName;
		}

	@Override
		public String getHttpPassword() throws HDIException {
			return passWord;
		}

	@Override
		public String getOSType() {
			return null;
		}

	@Override
		public String getResourceGroup() {
			return null;
		}

	@Override
		public HDStorageAccount getStorageAccount() throws HDIException {
			return null;
		}

	@Override
		public List<HDStorageAccount> getAdditionalStorageAccounts() {
			return null;
		}

	@Override
		public void getConfigurationInfo(Object project) throws IOException, HDIException, AzureCmdException {

		}
}
