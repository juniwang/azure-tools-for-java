/**
 * Copyright 2014 Persistent Systems Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.persistent.uiautomation;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;


public class Utility {

	protected static SWTWorkbenchBot wabot = new SWTWorkbenchBot();

	protected static boolean isProjExist(String name) {
		try {
			SWTBotView pkgExplorer = getProjExplorer();
			SWTBotTree tree = pkgExplorer.bot().tree();
			tree.getTreeItem(name);
			return true;
		} catch (WidgetNotFoundException e) {
			return false;
		}
	}

	protected static SWTBotView getProjExplorer() {
		SWTBotView view;
		if (wabot.activePerspective().getLabel().equals("Resource")
				|| wabot.activePerspective().getLabel().equals("Java EE")) {
			view = wabot.viewByTitle("Project Explorer");
		} else {
			view = wabot.viewByTitle("Package Explorer");
		}
		return view;
	}

	//create project
	public static boolean createProject(String projName) throws Exception {
		wabot.menu("File").menu("New").menu(Messages.newWaPrjName).click();
		SWTBotShell sh = wabot.shell(Messages.newWAProjTtl);
		wabot.textWithLabel(Messages.projNm).setText(projName);
		wabot.button(Messages.finBtn).click();
		wabot.waitUntil(shellCloses(sh));
		wabot.sleep(10000);
		return isProjExist(projName);
	}

	//create project with JDK and server configured
	public static boolean createProjectSrvJDK(String projName, String srvName) throws Exception {
		createProject(Messages.test);
		wabot.menu("File").menu("New").menu(Messages.newWaPrjName).click();
		SWTBotShell sh = wabot.shell(Messages.newWAProjTtl);
		wabot.textWithLabel(Messages.projNm).setText(projName);
		wabot.button(Messages.nxtBtn).click();
		wabot.checkBox(Messages.jdkChkBxTxt).click();
		// Directly set JDK path as Native dialog box can not be handled
		// May fail if directory is not present in real
		wabot.textInGroup(Messages.emltrGrp).setText(getLoc(Messages.test, "\\deploy"));
		wabot.tabItem(Messages.srv).activate();
		wabot.checkBox(Messages.srvChkBxTxt).click();
		wabot.comboBoxWithLabel(Messages.sel).setSelection(srvName);
		wabot.textInGroup(Messages.emltrGrp).setText(getLoc(Messages.test, "\\cert"));
		wabot.button(Messages.finBtn).click();
		wabot.waitUntil(shellCloses(sh));
		wabot.sleep(10000);
		return isProjExist(projName);
	}

	//create project with JDK configured
	public static boolean createProjectJDK(String projName) throws Exception {
		createProject(Messages.test);
		wabot.menu("File").menu("New").menu(Messages.newWaPrjName).click();
		SWTBotShell sh = wabot.shell(Messages.newWAProjTtl);
		wabot.textWithLabel(Messages.projNm).setText(projName);
		wabot.button(Messages.nxtBtn).click();
		wabot.checkBox(Messages.jdkChkBxTxt).click();
		// Directly set JDK path as Native dialog box can not be handled
		// May fail if directory is not present in real
		wabot.textInGroup(Messages.emltrGrp).
		setText(getLoc(Messages.test, "\\cert"));
		wabot.button(Messages.finBtn).click();
		wabot.waitUntil(shellCloses(sh));
		wabot.sleep(10000);
		return isProjExist(projName);
	}

	public static SWTBotTreeItem selProjFromExplorer(String projName) {
		SWTBotView packageExplorer = getProjExplorer();
		SWTBotTree tree = packageExplorer.bot().tree();
		return tree.getTreeItem(projName).select();
	}

	public static void renameSelectedResource(String newName) {
		wabot.menu("File").menu("Rename...").click();
		SWTBotShell shell = wabot.shell("Rename Resource");
		shell.activate();
		wabot.textWithLabel("New name:").setText(newName);
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(shell));
		wabot.sleep(10000);
	}

	public static void deleteSelectedProject() {
		wabot.menu("Edit").menu("Delete").click();
		SWTBotShell shell = wabot.shell("Delete Resources");
		shell.activate();
		wabot.checkBox("Delete project contents on disk (cannot be undone)").select();
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(shell));
		wabot.sleep(10000);
	}

	public static void disableRemoteAccess() {
		wabot.checkBox(Messages.remoteCheckText).click();
	}
	
	public static void enableRemoteAccess() {
		wabot.checkBox(Messages.remoteCheckText).select();
	}
	
	public static void enterRemoteAccessDetails(String projName) {
		wabot.textWithLabel(Messages.raUnameLbl).setText(Messages.test);
		wabot.textWithLabel(Messages.raPwdLbl).setText(Messages.raStrPwd);
		wabot.textWithLabel(Messages.raCnfPwdLbl).setText(Messages.raStrPwd);
		wabot.textWithLabel(Messages.raPathLbl).
		setText(getCertOfOtherProject(projName));
	}

	public static SWTBotTreeItem getPropertyPage(String project, String page) {
		Utility.selProjFromExplorer(project);
		wabot.menu("File").menu("Properties").click();
		SWTBotShell shell1 = wabot.shell(String.format("%s%s%s",
				Messages.propPageTtl, " " ,project));
		shell1.activate();
		SWTBotTree properties = shell1.bot().tree();
		SWTBotTreeItem  item = properties.getTreeItem(Messages.waPage);
		List<String> pageList = item.expand().getNodes();
		if (pageList.contains(page)) {
			item = properties.getTreeItem(Messages.waPage).expand().getNode(page);
		}
		return item.select();
	}

	public static void closeProjPropertyPage(String projectName) {
		SWTBotShell sh = wabot.shell(String.format("%s%s%s",
				Messages.propPageTtl, " " ,projectName));
		sh.close();
		wabot.waitUntil(shellCloses(sh));
	}

	public static String getCertOfOtherProject(String projName) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject project = root.getProject(projName);
		IResource resource = project.findMember("\\cert\\SampleRemoteAccessPublic.cer");
		return resource.getLocation().toOSString();
	}

	public static String getPfxOfOtherProject(String projName) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject project = root.getProject(projName);
		IResource resource = project.findMember("\\cert\\SampleRemoteAccessPrivate.pfx");
		return resource.getLocation().toOSString();
	}

	public static void disableDebugOption(String projName) {
		wabot.checkBox(Messages.debugLabel).click();
	}
	public static void enableDebugOption(String projName) {
		wabot.checkBox(Messages.debugLabel).select();
	}

	public static void createJavaProject(String projName) throws Exception {
		if (wabot.activePerspective().getLabel().equals("Resource")) {
			wabot.menu("Window").menu("Open Perspective").menu("Java").click();
			wabot.menu("File").menu("New").menu("Java Project").click();
			SWTBotShell sh = wabot.shell("New Java Project").activate();
			wabot.textWithLabel(Messages.projNm).setText(projName);
			wabot.button(Messages.finBtn).click();
			wabot.waitUntil(shellCloses(sh));
			wabot.sleep(10000);
			wabot.menu("Window").menu("Open Perspective").menu("Other...").click();
			SWTBotShell sh1 = wabot.shell("Open Perspective").activate();
			wabot.table().select("Resource (default)");
			wabot.button("OK").click();
			wabot.waitUntil(shellCloses(sh1));
			wabot.sleep(5000);
		} else {
			wabot.menu("File").menu("New").menu("Java Project").click();
			SWTBotShell sh = wabot.shell("New Java Project").activate();
			wabot.textWithLabel(Messages.projNm).setText(projName);
			wabot.button(Messages.finBtn).click();
			wabot.waitUntil(shellCloses(sh));
			wabot.sleep(10000);
		}
	}

	// create dynamic web project
	public static void createDynamicWebProj(String projName) throws Exception {
		if (wabot.activePerspective().getLabel().equals("Resource")) {
			wabot.menu("Window").menu("Open Perspective").menu("Java EE").click();
			wabot.menu("File").menu("New").menu("Dynamic Web Project").click();
			SWTBotShell sh = wabot.shell("New Dynamic Web Project").activate();
			wabot.textWithLabel(Messages.projNm).setText(projName);
			wabot.button(Messages.finBtn).click();
			wabot.waitUntil(shellCloses(sh));
			wabot.sleep(10000);
			wabot.menu("Window").menu("Open Perspective").menu("Other...").click();
			SWTBotShell sh1 = wabot.shell("Open Perspective").activate();
			wabot.table().select("Resource (default)");
			wabot.button("OK").click();
			wabot.waitUntil(shellCloses(sh1));
			wabot.sleep(5000);
		} else {
			wabot.menu("File").menu("New").menu("Dynamic Web Project").click();
			SWTBotShell sh = wabot.shell("New Dynamic Web Project").activate();
			wabot.textWithLabel(Messages.projNm).setText(projName);
			wabot.button(Messages.finBtn).click();
			wabot.waitUntil(shellCloses(sh));
			wabot.sleep(10000);
		}
	}

	// create enterprise application project
	public static void createEnterpriseAppProj(String projName) throws Exception {
		if (wabot.activePerspective().getLabel().equals("Resource")) {
			wabot.menu("Window").menu("Open Perspective").menu("Java EE").click();
			wabot.menu("File").menu("New").menu("Enterprise Application Project").click();
			SWTBotShell projShell = wabot.shell("New EAR Application Project").activate();
			wabot.textWithLabel(Messages.projNm).setText(projName);
			wabot.button(Messages.nxtBtn).click();
			wabot.checkBox().click();
			wabot.button("New Module...").click();
			SWTBotShell moduleShell = wabot.shell("Create default Java EE modules.").activate();
			wabot.button(Messages.finBtn).click();
			wabot.waitUntil(shellCloses(moduleShell));
			projShell.activate();
			wabot.button(Messages.finBtn).click();
			wabot.waitUntil(shellCloses(projShell));
			wabot.sleep(10000);
			wabot.menu("Window").menu("Open Perspective").menu("Other...").click();
			SWTBotShell sh1 = wabot.shell("Open Perspective").activate();
			wabot.table().select("Resource (default)");
			wabot.button("OK").click();
			wabot.waitUntil(shellCloses(sh1));
			wabot.sleep(5000);
		} else {
			wabot.menu("File").menu("New").menu("Enterprise Application Project ").click();
			SWTBotShell sh = wabot.shell("New EAR Application Project").activate();
			wabot.textWithLabel(Messages.projNm).setText(projName);
			wabot.button(Messages.finBtn).click();
			wabot.waitUntil(shellCloses(sh));
			wabot.sleep(10000);
		}
	}

	static public void addEp(String name, String type,
			String pubPort, String priPort) {
		SWTBotShell shEp = wabot.shell(Messages.addEpTtl);
		shEp.activate();
		wabot.textWithLabel("Name:").setText(name);
		/*
		 * IMP : Set Type first as according to type
		 * we decide controls (label and text boxes) to show.
		 */
		wabot.comboBox().setSelection(type);
		/*
		 * Endpoint type is Internal,
		 * then we have private port range.
		 */
		if (type.equalsIgnoreCase(Messages.typeIntrnl)) {
			/*
			 * If '-' is present, then split the string
			 * & get two values then assign it to proper text boxes.
			 */
			if (priPort.contains("-")) {
				String[] priPortRng = priPort.split("-");
				String priPortStart = priPortRng[0];
				String priPortEnd = priPortRng[1];
				wabot.textWithLabel(Messages.prvPortRngLbl).setText(priPortStart);
				/*
				 * As end point dialog has two '-', by default
				 * first occurrence of '-' that is of public port
				 * is recognized. Hence for private port range's
				 * second text box we need to use index.
				 */
				wabot.text(4).setText(priPortEnd);
			} else {
				wabot.textWithLabel(Messages.prvPortRngLbl).setText(priPort);
			}
		} else if(!priPort.isEmpty()) {
			wabot.textWithLabel(Messages.prvPortLbl).setText(priPort);
		}
		/*
		 * Endpoint type is InstanceInput,
		 * then we have public port range.
		 */
		if (type.equalsIgnoreCase(Messages.typeInpt)) {
			wabot.textWithLabel(Messages.pubPortLbl).setText(pubPort);
		} else if (type.equalsIgnoreCase(Messages.typeInstnc)) {
			/*
			 * If '-' is present, then split the string
			 * & get two values then assign it to proper text boxes.
			 */
			if (pubPort.contains("-")) {
				String[] pubPortRng = pubPort.split("-");
				String pubPortStart = pubPortRng[0];
				String pubPortEnd = pubPortRng[1];
				wabot.textWithLabel(Messages.pubPortRngLbl).setText(pubPortStart);
				// '-' of  of public port is recognized.
				wabot.textWithLabel("-").setText(pubPortEnd);
			} else {
				wabot.textWithLabel(Messages.pubPortRngLbl).setText(pubPort);
			}
		}
		wabot.button("OK").click();
	}
	
	public static void addCert(String name, String thumb) {
		SWTBotShell shEp = wabot.shell(Messages.addCertTtl);
		shEp.activate();
		wabot.textWithLabel("Name:").setText(name);
		wabot.textWithLabel(Messages.thumb).setText(thumb);
		wabot.button("OK").click();
	}

	public static void createProjWithEp(String projName) {
		wabot.menu("File").menu("New").menu(Messages.newWaPrjName).click();
		SWTBotShell projWiz = wabot.shell(Messages.newWAProjTtl).activate();
		wabot.textWithLabel(Messages.projNm).setText(projName);
		wabot.button(Messages.finBtn).click();
		wabot.waitUntil(shellCloses(projWiz));
		wabot.sleep(10000);
		// Select project and property page
		getPropertyPage(projName, Messages.rolesPage);
		// Add role
		wabot.button(Messages.roleAddBtn).click();
		SWTBotShell genPage = wabot.activeShell().activate();
		SWTBotTree properties = genPage.bot().tree();
		properties.getTreeItem(Messages.roleTreeRoot).expand()
		.getNode(Messages.endptPage).select();
		wabot.activeShell().activate();
		//input
		wabot.button(Messages.roleAddBtn).click();
		addEp("IntEndPt", Messages.typeInpt, "1", "1");
		//Internal Ep
		wabot.button(Messages.roleAddBtn).click();
		addEp("InlEndPt", Messages.typeIntrnl, "2", "3");
		wabot.button("OK").click();
		wabot.button("OK").click();
	}

	public static void addEnvVar(String envVarName,String envVarvalue) {
		wabot.button(Messages.roleAddBtn).click();
		SWTBotShell envVarShell = wabot.shell(Messages.envVarAddTtl);
		envVarShell.activate();
		wabot.textWithLabel(Messages.envVarNameLbl).setText(envVarName);
		wabot.textWithLabel(Messages.envVarValLbl).setText(envVarvalue);
		wabot.button("OK").click();
	}

	public static void createProjWithEnvVar(String projName) {
		wabot.menu("File").menu("New").menu(Messages.newWaPrjName).click();
		SWTBotShell projShell = wabot.shell(Messages.newWAProjTtl);
		projShell.activate();
		wabot.textWithLabel(Messages.projNm).setText(projName);
		wabot.button(Messages.finBtn).click();
		wabot.waitUntil(shellCloses(projShell));
		wabot.sleep(10000);
		// Select project and property page
		getPropertyPage(projName, Messages.rolesPage);
		//Add role with Environment Variable
		wabot.button(Messages.roleAddBtn).click();
		wabot.activeShell().activate();
		SWTBotTree properties = wabot.activeShell().bot().tree();
		properties.getTreeItem(Messages.roleTreeRoot).expand()
		.getNode(Messages.envVarPage).select();
		wabot.activeShell().activate();
		//Add Environment Variable
		addEnvVar(Messages.envVarName,Messages.envVarVal);
		wabot.button("OK").click();
		wabot.button("OK").click();
	}

	public static SWTBotShell selectPageUsingContextMenu(
			String projName, String role, String page) {
		SWTBotTreeItem proj1 = Utility.
				selProjFromExplorer(projName);
		SWTBotTreeItem workerRoleNode = proj1.expand().
				getNode(role).select();
		workerRoleNode.contextMenu("Properties").click();
		SWTBotShell propShell = wabot.
				shell(String.format("%s%s%s", Messages.
						propPageTtl, " ", role));
		propShell.activate();
		SWTBotTree properties = propShell.bot().tree();
		properties.getTreeItem(Messages.roleTreeRoot).expand().
		getNode(page).select();
		return propShell;
	}

	public static void createProjWithLocalStorage(String projName) {
		wabot.menu("File").menu("New").menu(Messages.newWaPrjName).click();
		SWTBotShell projShell = wabot.shell(Messages.newWAProjTtl);
		projShell.activate();
		wabot.textWithLabel(Messages.projNm).setText(projName);
		wabot.button(Messages.finBtn).click();
		wabot.waitUntil(shellCloses(projShell));
		wabot.sleep(10000);
		// Select project and property page
		getPropertyPage(projName, Messages.rolesPage);
		//Add role with Local Storage
		wabot.button(Messages.roleAddBtn).click();
		wabot.activeShell().activate();
		SWTBotTree properties = wabot.activeShell().bot().tree();
		properties.getTreeItem(Messages.roleTreeRoot).expand()
		.getNode(Messages.lclStrPage).select();
		//Add Local Storage entry
		wabot.button(Messages.roleAddBtn).click();
		wabot.button("OK").click();
		wabot.button("OK").click();
		wabot.button("OK").click();
	}

	public static String getLoc(String projName, String member) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject project = root.getProject(projName);
		IResource resource = project.findMember(member);
		return resource.getLocation().toOSString();
	}

	public static void addCmpnt(String frmProj, String toAdd) {
		wabot.button(Messages.roleAddBtn).click();
		wabot.shell(Messages.cmpntAddTtl).activate();
		wabot.textWithLabel(Messages.frmPathLbl).typeText(
				Utility.getLoc(frmProj, toAdd));
		wabot.sleep(1000);
		wabot.textWithLabel(Messages.asNameLbl).setFocus();
		wabot.button("OK").click();
	}

	public static SWTBotShell selectGeneralPage(String roleName) {
		wabot.button(Messages.roleEditBtn).click();
		SWTBotShell propShell = wabot.shell(String.format("%s%s%s",
				Messages.propPageTtl, " ", roleName));
		propShell.activate();
		SWTBotTree properties = propShell.bot().tree();
		properties.getTreeItem(Messages.roleTreeRoot).select();
		return propShell;
	}
	
	public static SWTBotShell selGeneralPageUsingCnxtMenu(String projName) {
		SWTBotTreeItem proj1 = Utility.
				selProjFromExplorer(projName);
		SWTBotTreeItem workerRoleNode = proj1.expand().
				getNode(Messages.role1).select();
		workerRoleNode.contextMenu("Properties").click();
		SWTBotShell propShell = wabot.
				shell(String.format("%s%s%s",
						Messages.propPageTtl, " ", Messages.role1));
		propShell.activate();
		SWTBotTree properties = propShell.bot().tree();
		properties.getTreeItem(Messages.roleTreeRoot).select();
		return propShell;
	}
	
	public static SWTBotShell selectPageViaRoleEdit(String roleName, String page) {
		wabot.button(Messages.roleEditBtn).click();
		SWTBotShell propShell = wabot.shell(String.format("%s%s%s",
				Messages.propPageTtl, " ", roleName));
		propShell.activate();
		SWTBotTree properties = propShell.bot().tree();
		properties.getTreeItem(Messages.roleTreeRoot).expand().getNode(page).select();
		return propShell;
	}
	
	public static SWTBotShell selPageNode(String role, String page) {
		SWTBotShell propShell = wabot.
				shell(String.format("%s%s%s",
						Messages.propPageTtl, " ", role));
		propShell.activate();
		SWTBotTree properties = propShell.bot().tree();
		properties.getTreeItem(Messages.roleTreeRoot).expand().
		getNode(page).select();
		return propShell;
	}
}

