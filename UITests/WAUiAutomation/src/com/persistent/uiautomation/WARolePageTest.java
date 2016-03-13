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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class WARolePageTest {

	private static SWTWorkbenchBot	wabot;

	@BeforeClass
	public static void beforeClass() throws Exception {
		wabot = new SWTWorkbenchBot();
		try {
			wabot.viewByTitle(Messages.welCm).close();
		} catch (Exception e) {
		}
	}

	@Before
	public void setUp() throws Exception {
		wabot.closeAllShells();
		if(Utility.isProjExist(Messages.projWithEp)) {
			//delete existing project
			Utility.selProjFromExplorer(Messages.projWithEp).select();
			Utility.deleteSelectedProject();
		}
		Utility.createProjWithEp(Messages.projWithEp);
		wabot.sleep(1000);
		Utility.getPropertyPage(Messages.projWithEp, Messages.rolesPage);
	}

	@After
	public void cleanUp() throws Exception {
		if(Utility.isProjExist(Messages.projWithEp)) {
			Utility.selProjFromExplorer(Messages.projWithEp).select();
			Utility.deleteSelectedProject();
		}
	}

	private static SWTBotShell openRoleWinFromPropPage(String roleName) {
		SWTBotShell shRole = Utility.selectGeneralPage(roleName);
		return shRole;
	}
	
	@Test
	public void testWaViewRole() throws Exception {
		assertEquals("testWaViewRole", Messages.role1, wabot.table().getTableItem(0).getText());
		wabot.sleep(1000);
		SWTBotShell sh = wabot.activeShell();
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(sh));
	}

	@Test
	public void testWaEditDisable() throws Exception {
		assertFalse("testWaEditDisable",wabot.button(Messages.roleEditBtn).isEnabled());
		SWTBotShell sh = wabot.activeShell();
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(sh));
	}

	@Test
	public void testWaRemoveDisable() throws Exception {
		assertFalse("testWaRemoveDisable",wabot.button(Messages.roleRemBtn).isEnabled());
		SWTBotShell sh = wabot.activeShell();
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(sh));    }

	@Test
	public void testWaEditEnable() throws Exception {
		wabot.table().getTableItem(0).select();
		assertTrue("testWaEditEnable",wabot.button(Messages.roleEditBtn).isEnabled());
		SWTBotShell sh = wabot.activeShell();
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(sh));
	}

	@Test
	public void testWaRemoveEnable() throws Exception {
		assertFalse("testWaRemoveEnable",wabot.button(Messages.roleRemBtn).isEnabled());
		SWTBotShell sh = wabot.activeShell();
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(sh));
	}

	@Test
	public void testEditRoleName() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		wabot.textWithLabel(Messages.roleNameLbl).setText(Messages.waRole);
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(shRole));
		assertEquals("testEditRoleName", Messages.waRole,
				wabot.table().getTableItem(1).getText());
		SWTBotShell sh = wabot.activeShell();
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(sh));
	}

	@Test
	public void testEditVmSize() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		wabot.comboBoxWithLabel(Messages.vmSizeLbl).setSelection("Large");
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(shRole));
		assertEquals("testEditVmSize", "Large", wabot.table().cell(1,1));
		SWTBotShell sh = wabot.activeShell();
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(sh));
	}

	@Test
	public void testEditInstances() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		wabot.textWithLabel(Messages.instance).setFocus();
		wabot.textWithLabel(Messages.instance).setText("3");
		wabot.button("OK").setFocus();
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(shRole));
		assertEquals("testEditInstances", "3",
				wabot.table().getTableItem(1).getText(2));
		SWTBotShell sh = wabot.activeShell();
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(sh));
	}

	@Test
	public void testEditInputEpPort() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		//Change Public Port
		wabot.table().click(0, 2);
		wabot.text("1", 0).setText("33");
		//Change Private Port
		wabot.table().click(0, 3);
		wabot.text("1", 0).setText("44");
		wabot.button(Messages.roleAddBtn).setFocus();
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(shRole));
		//verify
		role = wabot.table().getTableItem(1).select().getText();
		shRole = openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		assertEquals("testEditEpInpToInt public port", "33",
				wabot.table().cell(0, 2));
		assertEquals("testEditEpInpToInt private port", "44",
				wabot.table().cell(0, 3));
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(shRole));
		SWTBotShell sh = wabot.activeShell();
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(sh));
	}

	@Test
	public void testEditInternalEpPort() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		//Change Public Port
		wabot.table().click(1, 3);
		wabot.text("3", 0).setText("33");
		//click on other cell to save the changes from previous cell
		wabot.button(Messages.roleAddBtn).setFocus();
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(shRole));
		//verify
		role= wabot.table().getTableItem(1).select().getText();
		shRole = openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		assertEquals("testEditInternalEpPort private port", "33",
				wabot.table().cell(1, 3));
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(shRole));
		SWTBotShell sh = wabot.activeShell();
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(sh));
	}

	@Test
	public void testPublicPortOfInternalEp() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		assertEquals("testPublicPortToInternalEp", "N/A",
				wabot.table().getTableItem(1).getText(2));
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(shRole));
		wabot.button("OK").click();
	}

	@Test
	public void testEditEpInpToInt() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		wabot.activeShell().activate();
		wabot.table().click(0, 1);
		wabot.ccomboBox().setSelection(1);
		wabot.button("OK").click();
		//verify
		role = wabot.table().getTableItem(1).select().getText();
		shRole = openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		assertEquals("testEditEpInpToInt", Messages.typeIntrnl,
				wabot.table().getTableItem(1).getText(1));
		wabot.button("OK").click(); //Edit Role dialog box
		wabot.waitUntil(shellCloses(shRole));
		wabot.button(Messages.cnclBtn).click(); //Role property dialog box
	}

	@Test
	public void testEditEpIntEpToInput() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		wabot.table().click(1, 1);
		wabot.ccomboBox().setSelection(0);
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(shRole));
		//verify
		role = wabot.table().getTableItem(1).select().getText();
		shRole = openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		assertEquals("testEditEpIntEpToInput", Messages.typeInpt,
				wabot.table().getTableItem(0).getText(1));
		wabot.button("OK").click();  //Edit Role dialog box
		wabot.waitUntil(shellCloses(shRole));
		wabot.button("OK").click(); //Role property dialog box
	}


	@Test
	public void testInvalidEndpointName() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("IntEndPt", Messages.typeInpt, "11", "13");
		SWTBotShell sh = wabot.shell(Messages.invalidEpName).activate();
		String msg = sh.getText();
		wabot.button("OK").click();
		wabot.button(Messages.cnclBtn).click();
		shRole.close();
		assertTrue("testInvalidEndpointName ", msg.equals(Messages.invalidEpName));
		wabot.shell("Properties for " + Messages.projWithEp).close();
	}

	@Test
	public void testEmptyEndpointName() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("", Messages.typeInpt, "11", "13");
		SWTBotShell sh = wabot.shell(Messages.invalidEpName).activate();
		String msg = sh.getText();
		wabot.button("OK").click();
		wabot.button(Messages.cnclBtn).click();
		shRole.close();
		assertTrue("testEmptyEndpointName", msg.equals(Messages.invalidEpName));
		wabot.shell("Properties for " + Messages.projWithEp).close();
	}


	@Test
	public void testInvalidEndpointPort() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("NewEp", Messages.typeInpt, "test", "13");
		SWTBotShell sh = wabot.shell(Messages.invalidEpPort).activate();
		String msg = sh.getText();
		wabot.button("OK").click();
		wabot.button(Messages.cnclBtn).click();
		shRole.close();
		assertTrue("testInvalidEndpointPort", msg.equals(Messages.invalidEpPort));
		wabot.shell("Properties for " + Messages.projWithEp).close();
	}

	@Test
	public void testWithExistingEndpointPort() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("NewEp", Messages.typeInpt, "1", "1");
		SWTBotShell sh = wabot.shell(Messages.invalidEpPort).activate();
		String msg = sh.getText();
		wabot.button("OK").click();
		wabot.button(Messages.cnclBtn).click();
		shRole.close();
		assertTrue("testInvalidEndpointPort", msg.equals(Messages.invalidEpPort));
		wabot.shell("Properties for " + Messages.projWithEp).close();
	}

	@Test
	public void testVmSizeValues() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		SWTBotTree properties = shRole.bot().tree();
		properties.getTreeItem(Messages.roleTreeRoot).select();
		String[] val = wabot.comboBoxWithLabel(Messages.vmSizeLbl).items();
		String[] expectedVal = {"A9", "A8", "A7", "A6", "A5", "ExtraLarge", "Large", "Medium", "Small",
		"ExtraSmall"};
		wabot.button("OK").click();
		wabot.waitUntil(shellCloses(shRole));
		assertArrayEquals("testVmSizeValues", expectedVal, val);
		wabot.shell("Properties for " + Messages.projWithEp).close();
	}

	@Test
	public void testInvalidRolename() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		SWTBotTree properties = shRole.bot().tree();
		properties.getTreeItem(Messages.roleTreeRoot).select();
		wabot.textWithLabel(Messages.roleTxtLbl).setText("");
		assertFalse("testEditEpInstances",
				wabot.button("OK").isEnabled());
		wabot.shell(Messages.propPageTtl + " " + role).activate().close();
		wabot.waitUntil(shellCloses(shRole));
		wabot.shell("Properties for " + Messages.projWithEp).close();
	}
	
	@Test
	public void testInvalidEpName() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		wabot.table().click(0, 0);
		wabot.text("IntEndPt").typeText("InlEndPt");
		wabot.button(Messages.roleAddBtn).click();
		SWTBotShell sh = wabot.shell(Messages.invalidEpName).activate();
		String msg = sh.getText();
		wabot.button("OK").click();
		wabot.shell(Messages.addEpTtl).activate();
		wabot.button(Messages.cnclBtn).click();
		wabot.button("OK").click();
		wabot.button("OK").click();
		assertEquals("testEditEpInstances", msg,
				Messages.invalidEpName);
	}


	@Test
	public void testEmptyEpName() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		openRoleWinFromPropPage(role).bot().tree();
		Utility.selPageNode(role, Messages.endptPage);
		wabot.table().click(1, 0);
		wabot.text("InlEndPt").setText("");
		wabot.button(Messages.roleAddBtn).click();
		SWTBotShell sh = wabot.shell(Messages.invalidEpName).activate();
		String msg = sh.getText();
		wabot.button("OK").click();
		wabot.button(Messages.cnclBtn).click();
		wabot.button(Messages.cnclBtn).click();
		assertEquals("testEmptyEpName", msg,
				Messages.invalidEpName);
		wabot.shell("Properties for " + Messages.projWithEp).close();
	}


	@Test
	public void testEditExistingEpPort() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		//Change Public Port
		wabot.table().click(1, 3);
		wabot.text("3", 0).typeText("1");
		wabot.button(Messages.roleAddBtn).click();
		SWTBotShell sh = wabot.shell(Messages.invalidEpPort).activate();
		String msg = sh.getText();
		wabot.button("OK").click();
		wabot.button(Messages.cnclBtn).click();
		wabot.button(Messages.cnclBtn).click();
		assertEquals("testEmptyEpName", msg,
				Messages.invalidEpPort);
		wabot.shell("Properties for " + Messages.projWithEp).close();
	}

	@Test
	public void testRemoveEndpoint() throws Exception {
		String role = wabot.table().getTableItem(1).select().getText();
		SWTBotShell shRole = openRoleWinFromPropPage(role);
		Utility.selPageNode(role, Messages.endptPage);
		wabot.table().getTableItem(1).select();
		wabot.button(Messages.roleRemBtn).click();
		wabot.shell(Messages.delEndPtTtl).activate();
		wabot.button("Yes").click();
		assertFalse("testRemoveEndpoint", wabot.table()
				.containsItem("InlEndPt"));
		shRole.close();
		wabot.button(Messages.cnclBtn).click();
	}

	@Test
	public void testRemoveRole() throws Exception {
		wabot.table().getTableItem(1).select();
		wabot.button(Messages.roleRemBtn).click();
		wabot.shell(Messages.delRoleTtl).activate();
		wabot.button("Yes").click();
		assertFalse("testRemoveEndpoint", wabot.table()
				.containsItem("WorkerRole2"));
		wabot.button(Messages.cnclBtn).click();
	}

	@Test
	// (New Test Cases for 1.6) test case 1
	public void testRoleDialogPresent() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.selGeneralPageUsingCnxtMenu(Messages.projWithEp);
		assertTrue("testRoleDialogPresent", wabot.
				label(Messages.roleNameLbl).isVisible()
				&& wabot.label(Messages.vmSizeLbl).isVisible()
				&& wabot.label(Messages.instance).isVisible()
				&& wabot.textWithLabel(Messages.roleNameLbl).
				getText().equals(Messages.role1)
				&& wabot.comboBoxWithLabel(Messages.vmSizeLbl).
				getText().equals("Small")
				&& wabot.textWithLabel(Messages.instance).
				getText().equals("1"));
		propShell.close();
	}


	@Test
	// (New Test Cases for 1.6) test case 3
	public void testEndPtPagePresent() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		assertTrue("testEndPtPagePresent", wabot.table().
				cell(0, 0).equals("IntEndPt")
				&& wabot.table().cell(0, 1).equals(Messages.typeInpt)
				&& wabot.table().cell(0, 2).equals("1")
				&& wabot.table().cell(0, 3).equals("1")
				&& wabot.table().
				cell(1, 0).equals("InlEndPt")
				&& wabot.table().cell(1, 1).equals(Messages.typeIntrnl)
				&& wabot.table().cell(1, 2).equals("N/A")
				&& wabot.table().cell(1, 3).equals("3"));
		propShell.close();
	}


	@Test
	// (New Test Cases for 1.6) test case 6
	// Same as (New Test Cases for 1.6) test case 1
	public void testRoleDialogOpen() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.selGeneralPageUsingCnxtMenu(Messages.projWithEp);
		assertTrue("testRoleDialogPresent", wabot.
				label(Messages.roleNameLbl).isVisible()
				&& wabot.label(Messages.vmSizeLbl).isVisible()
				&& wabot.label(Messages.instance).isVisible()
				&& wabot.textWithLabel(Messages.roleNameLbl).
				getText().equals(Messages.role1)
				&& wabot.comboBoxWithLabel(Messages.vmSizeLbl).
				getText().equals("Small")
				&& wabot.textWithLabel(Messages.instance).
				getText().equals("1"));
		propShell.close();
	}


	@Test
	// (New Test Cases for 1.6) test case 8
	public void testEditDialogPresent() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		SWTBotShell editShell = wabot.shell(Messages.endPtEditTtl);
		editShell.activate();
		String confirmMsg = editShell.getText();
		assertTrue("testEditDialogPresent",
				confirmMsg.equals(Messages.endPtEditTtl));
		editShell.close();
		propShell.close();
	}


	@Test
	// (New Test Cases for 1.6) test case 9
	public void testEditEndPtOkPressed() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		SWTBotShell editShell = wabot.shell(Messages.endPtEditTtl);
		editShell.activate();
		wabot.comboBox().setSelection(Messages.typeIntrnl);
		wabot.button("OK").click();
		assertTrue("testEditEndPtOkPressed", wabot.table().
				cell(0, 2).equals("N/A")
				&& wabot.table().cell(0, 1).equals(Messages.typeIntrnl));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.6) test case 10
	public void testEditEndPtCancelPressed() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		SWTBotShell editShell = wabot.shell(Messages.endPtEditTtl);
		editShell.activate();
		wabot.comboBox().setSelection(Messages.typeIntrnl);
		wabot.button(Messages.cnclBtn).click();
		assertTrue("testEditEndPtCancelPressed", wabot.table().
				cell(0, 2).equals("1")
				&& wabot.table().cell(0, 1).equals(Messages.typeInpt));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 102
	public void testInstncEndPtEntryInTable() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.table().click(0, 1);
		String[] endPtType = wabot.ccomboBox().items();
		List<String> typeList = Arrays.asList(endPtType);
		assertTrue("testInstncEndPtEntryInTable", typeList.contains(Messages.typeInstnc));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 103
	public void testPublicPortTable() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.table().click(0, 1);
		wabot.ccomboBox().setSelection(Messages.typeInstnc);
		wabot.table().click(0, 0);
		assertTrue("testPublicPortTable", wabot.table().cell(0, 2).contains("-"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 104
	public void testInstncEndPtEntryInDlg() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		SWTBotShell shEp = wabot.shell(Messages.addEpTtl);
		shEp.activate();
		String[] endPtType = wabot.comboBox().items();
		List<String> typeList = Arrays.asList(endPtType);
		assertTrue("testInstncEndPtEntryInDlg", typeList.contains(Messages.typeInstnc));
		shEp.close();
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 105
	public void testPublicPortDlgRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "13-15", "16");
		assertTrue("testPublicPortDlg", wabot.table().cell(0, 2).contains("13-15"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 106
	public void testPublicPortDlgNtRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "13", "16");
		assertTrue("testPublicPortDlgNtRange", wabot.table().cell(0, 2).contains("13-13"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 107
	public void testPrvtPortCnstr() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		SWTBotShell shEp = wabot.shell(Messages.addEpTtl);
        shEp.activate();
        wabot.textWithLabel("Name:").setText("InstncTest");
        wabot.comboBox().setSelection(Messages.typeInstnc);
        wabot.textWithLabel("Public port range:").typeText("13");
        wabot.textWithLabel("Name:").setFocus();
        wabot.textWithLabel("-").setText("16");
        assertTrue("testPrvtPortCnstr", wabot.textWithLabel("Private port:").
        		getText().equalsIgnoreCase("13"));
        shEp.close();
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 108
	public void testInputToInstanceDlg() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		SWTBotShell shEp = wabot.shell(Messages.editEpTtl);
		shEp.activate();
		wabot.comboBox().setSelection(Messages.typeInstnc);
		wabot.button("OK").click();
		assertTrue("testInputToInstanceDlg", wabot.table().
				cell(0, 1).contains(Messages.typeInstnc)
				&& wabot.table().cell(0, 2).contains("1-1"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 109
	public void testInternalToInstanceDlg() throws Exception {
		 //Functionality changed in v1.8,
		 //we do not auto prompt public port to user
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.table().select(1);
		wabot.button(Messages.roleEditBtn).click();
		SWTBotShell shEp = wabot.shell(Messages.editEpTtl);
		shEp.activate();
		wabot.comboBox().setSelection(Messages.typeInstnc);
		wabot.textWithLabel("Public port range:").setText("16");
		wabot.button("OK").click();
		assertTrue("testInternalToInstanceDlg", wabot.table().
				cell(1, 1).contains(Messages.typeInstnc)
				&& wabot.table().cell(1, 2).contains("16-16"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 110
	public void testInputToInstanceTable() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.table().click(0, 1);
		wabot.ccomboBox().setSelection(Messages.typeInstnc);
		wabot.table().click(0, 0);
		assertTrue("testInputToInstanceTable", wabot.table().
				cell(0, 1).contains(Messages.typeInstnc)
				&& wabot.table().cell(0, 2).contains("1-1"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 111
	public void testInternalToInstanceTable() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.table().click(1, 1);
		wabot.ccomboBox().setSelection(Messages.typeInstnc);
		wabot.table().click(0, 0);
		assertTrue("testInternalToInstanceTable", wabot.table().
				cell(1, 1).contains(Messages.typeInstnc)
				&& wabot.table().cell(1, 2).contains("3-3"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 112
	public void testInstanceToInputDlg() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "13-15", "16");
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		SWTBotShell shEp = wabot.shell(Messages.editEpTtl);
		shEp.activate();
		wabot.comboBox().setSelection(Messages.typeInpt);
		wabot.button("OK").click();
		assertTrue("testInstanceToInputDlg", wabot.table().
				cell(0, 1).contains(Messages.typeInpt)
				&& wabot.table().cell(0, 2).contains("13"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 113
	public void testInstanceToInternalDlg() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "13-15", "16");
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		SWTBotShell shEp = wabot.shell(Messages.editEpTtl);
		shEp.activate();
		wabot.comboBox().setSelection(Messages.typeIntrnl);
		wabot.button("OK").click();
		assertTrue("testInstanceToInternalDlg", wabot.table().
				cell(0, 1).contains(Messages.typeIntrnl)
				&& wabot.table().cell(0, 2).contains("N/A"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 114
	public void testInPlaceInstanceToInput() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "13-15", "16");
		wabot.table().click(0, 1);
		wabot.ccomboBox().setSelection(Messages.typeInpt);
		wabot.table().click(0, 0);
		assertTrue("testInPlaceInstanceToInput", wabot.table().
				cell(0, 1).contains(Messages.typeInpt)
				&& wabot.table().cell(0, 2).contains("13"));
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.7) test case 115
	public void testInPlaceInstanceToInternal() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "13-15", "16");
		wabot.table().click(0, 1);
		wabot.ccomboBox().setSelection(Messages.typeIntrnl);
		wabot.table().click(0, 0);
		assertTrue("testInPlaceInstanceToInternal", wabot.table().
				cell(0, 1).contains(Messages.typeIntrnl)
				&& wabot.table().cell(0, 2).contains("N/A"));
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.7) test case 116
	public void testSamePrtPortIntrnl() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "13-15", "3");
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		assertTrue("testSamePrtPortIntrnl",
				errMsg.equals(Messages.invalidEpPort));
		// Cancel Add End Point shell
		wabot.shell(Messages.addEpTtl).activate();
		wabot.button(Messages.cnclBtn).click();
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 117
	public void testSamePblPort() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "1-3", "16");
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		assertTrue("testSamePblPort",
				errMsg.equals(Messages.invalidEpPort));
		wabot.shell(Messages.addEpTtl).activate();
		wabot.button(Messages.cnclBtn).click();
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 118
	public void testInPlaceEditSamePrtPortIntrl() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "13-15", "26");
		wabot.table().click(2, 3);
	    wabot.text("26", 0).typeText("3");
	    wabot.button(Messages.roleAddBtn).click();
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testInPlaceEditSamePrtPortIntrl",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 119
	public void testInPlaceEditSamePblPort() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "13-15", "16");
		wabot.table().click(2, 2);
	    wabot.text("13-15", 0).typeText("1-3");
	    wabot.button(Messages.roleAddBtn).click();
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testInPlaceEditSamePblPort",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 126
	public void testAddOvrlpngPblPortRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "5-10", "16");
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest1", Messages.typeInstnc, "8-12", "17");
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
		wabot.shell(Messages.addEpTtl).activate();
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testAddOvrlpngPblPortRange",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 127
	public void testInPlaceEditOvrlpngPblPortRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "5-10", "16");
		wabot.table().click(0, 1);
		wabot.ccomboBox().setSelection(Messages.typeInstnc);
		wabot.table().click(0, 0);
		wabot.table().click(0, 2);
	    wabot.text("1-1", 0).typeText("8-12");
	    wabot.button(Messages.roleAddBtn).click();
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testInPlaceEditOvrlpngPblPortRange",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 128
	public void testEditOvrlpngPblPortRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "5-10", "16");
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		wabot.shell(Messages.editEpTtl).activate();
		wabot.comboBox().setSelection(Messages.typeInstnc);
		wabot.textWithLabel("Public port range:").setText("8-12");
		wabot.button("OK").click();
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testEditOvrlpngPblPortRange",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 129
	public void testEditSamePblPort() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "5-10", "16");
		wabot.table().select(2);
		wabot.button(Messages.roleEditBtn).click();
		wabot.shell(Messages.editEpTtl).activate();
		wabot.comboBox().setSelection(Messages.typeInstnc);
		wabot.textWithLabel("Public port range:").setText("1");
		wabot.button("OK").click();
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testEditSamePblPort",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 130
	public void testAddMaxMinPblPortRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "10-6", "16");
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
		wabot.shell(Messages.addEpTtl).activate();
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testAddMax_MinPblPortRange",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 131
	public void testInPlaceEditMaxMinPblPortRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "5-10", "16");
		wabot.table().click(0, 2);
	    wabot.text("5-10", 0).typeText("10-6");
	    wabot.button(Messages.roleAddBtn).click();
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testInPlaceEditMax_MinPblPortRange",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 132
	public void testEditMaxMinPblPortRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "5-10", "16");
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		wabot.shell(Messages.editEpTtl).activate();
		wabot.textWithLabel("Public port range:").setText("10-6");
		wabot.button("OK").click();
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testEditMax_MinPblPortRange",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 133
	public void testAddNegPblPortRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "-10-6", "16");
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
		wabot.shell(Messages.addEpTtl).activate();
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testAddNegPblPortRange",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 134
	public void testInPlaceEditNegPblPortRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "5-10", "16");
		wabot.table().click(0, 2);
	    wabot.text("5-10", 0).typeText("-10-6");
	    wabot.button(Messages.roleAddBtn).click();
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testInPlaceEditNegPblPortRange",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 135
	public void testEditNegPblPortRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "5-10", "16");
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		wabot.shell(Messages.editEpTtl).activate();
		wabot.textWithLabel("Public port range:").setText("-10-6");
		wabot.button("OK").click();
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testEditNegPblPortRange",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 136
	public void testAddCharPblPortRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "a-6", "16");
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
		wabot.shell(Messages.addEpTtl).activate();
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testAddNegPblPortRange",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 137
	public void testInPlaceEditCharPblPortRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "5-10", "16");
		wabot.table().click(0, 2);
	    wabot.text("5-10", 0).typeText("a-6");
	    wabot.button(Messages.roleAddBtn).click();
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testInPlaceEditCharPblPortRange",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 138
	public void testEditCharPblPortRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "5-10", "16");
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		wabot.shell(Messages.editEpTtl).activate();
		wabot.textWithLabel("Public port range:").setText("a");
		wabot.textWithLabel("-").setText("6");
		wabot.button("OK").click();
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
        wabot.button(Messages.cnclBtn).click();
        assertTrue("testEditCharPblPortRange",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 139
	public void testEditPrvPubOverlap() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "12-15", "16");
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		wabot.shell(Messages.editEpTtl).activate();
		wabot.textWithLabel("Public port range:").setText("12");
		wabot.textWithLabel("-").setText("16");
		wabot.textWithLabel("Private port:").setText("17");
		wabot.button("OK").click();
		// Again change endpoint
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		wabot.shell(Messages.editEpTtl).activate();
		wabot.textWithLabel("Public port range:").setText("12");
		wabot.textWithLabel("-").setText("17");
		wabot.textWithLabel("Private port:").setText("17");
		wabot.button("OK").click();
        assertTrue("testEditPrvPubOverlap", wabot.table().
				cell(0, 2).contains("12-17")
				&& wabot.table().cell(0, 3).contains("17"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 167
	public void testEditPubPrtMin() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInpt, "12", "12");
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		wabot.shell(Messages.editEpTtl).activate();
		wabot.comboBox().setSelection(Messages.typeInstnc);
		wabot.textWithLabel("Public port range:").setText("10");
		wabot.textWithLabel("-").setText("12");
		wabot.textWithLabel("Private port:").setText("12");
		wabot.button("OK").click();
		// Again change endpoint
        assertTrue("testEditPubPrtMin", wabot.table().
				cell(0, 2).contains("10-12")
				&& wabot.table().cell(0, 3).contains("12"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.7) test case 168
	public void testEditPubPrtMax() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInpt, "12", "12");
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		wabot.shell(Messages.editEpTtl).activate();
		wabot.comboBox().setSelection(Messages.typeInstnc);
		wabot.textWithLabel("Public port range:").setText("12");
		wabot.textWithLabel("-").setText("16");
		wabot.button("OK").click();
		// Again change endpoint
		assertTrue("testEditPubPrtMax", wabot.table().
				cell(0, 2).contains("12-16")
				&& wabot.table().cell(0, 3).contains("12"));
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.7) test case 252
	public void testSamePrtPortInput() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.table().click(0, 3);
	    wabot.text("1", 0).typeText("10");
	    wabot.table().click(0, 0);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "13-15", "10");
		assertTrue("testSamePrtPortInput", wabot.table().cell(2, 3).
				equalsIgnoreCase("10"));
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.7) test case 253
	public void testInPlaceEditSamePrtPortInput() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.table().click(0, 3);
	    wabot.text("1", 0).typeText("10");
	    wabot.table().click(0, 0);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "13-15", "16");
		wabot.table().click(2, 3);
		wabot.text("16", 0).typeText("10");
		wabot.table().select(2);
		assertTrue("testInPlaceEditSamePrtPortInput", wabot.table().cell(2, 3).
				equalsIgnoreCase("10"));
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.7) test case 254
	public void testEditSamePrtPortInput() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.table().click(0, 3);
	    wabot.text("1", 0).typeText("10");
	    wabot.table().click(0, 0);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "5-9", "16");
		wabot.table().select(2);
		wabot.button(Messages.roleEditBtn).click();
		wabot.shell(Messages.editEpTtl).activate();
		wabot.comboBox().setSelection(Messages.typeInstnc);
		wabot.textWithLabel("Private port:").setText("10");
		wabot.button("OK").click();
        assertTrue("testEditSamePrtPortInput", wabot.table().
        		cell(2, 3).equalsIgnoreCase("10"));
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.7) test case 255
	public void testEditSamePrtPortIntrl() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstncTest", Messages.typeInstnc, "5-10", "26");
		wabot.table().select(2);
		wabot.button(Messages.roleEditBtn).click();
		wabot.shell(Messages.editEpTtl).activate();
		wabot.comboBox().setSelection(Messages.typeInstnc);
		wabot.textWithLabel("Private port:").setText("3");
		wabot.button("OK").click();
		SWTBotShell errShell = wabot.shell(Messages.invalidEpPort);
		errShell.activate();
		String errMsg = errShell.getText();
		errShell.close();
		// Cancel Add End Point shell
		wabot.button(Messages.cnclBtn).click();
		assertTrue("testEditSamePrtPortIntrl",
				errMsg.equals(Messages.invalidEpPort));
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.8) test case 49
	public void testInternalEndPtControls() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		wabot.shell(Messages.addEpTtl).activate();
		wabot.comboBox().setSelection(Messages.typeIntrnl);
		assertTrue("testInternalEndPtControls",
				wabot.label(Messages.prvPortRngLbl).isEnabled()
				&& wabot.textWithLabel(Messages.prvPortRngLbl).isEnabled()
				&& !wabot.label(Messages.pubPortRngLbl).isEnabled()
				&& !wabot.textWithLabel(Messages.pubPortRngLbl).isEnabled()
				// Public port second text box
				&& !wabot.text(2).isEnabled());
		wabot.button(Messages.cnclBtn).click();
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.8) test case 50
	public void testInputEndPtControls() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		wabot.shell(Messages.addEpTtl).activate();
		wabot.comboBox().setSelection(Messages.typeInpt);
		assertTrue("testInputEndPtControls",
				wabot.label(Messages.prvPortLbl).isEnabled()
				&& wabot.textWithLabel(Messages.prvPortLbl).isEnabled()
				&& wabot.label(Messages.pubPortLbl).isEnabled()
				&& wabot.textWithLabel(Messages.pubPortLbl).isEnabled()
				&& !wabot.label("-").isEnabled()
				// Public port second text box
				&& !wabot.text(2).isEnabled()
				// Private port second text box
				&& !wabot.text(4).isEnabled());
		wabot.button(Messages.cnclBtn).click();
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.8) test case 51
	public void testInstanceEndPtControls() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		wabot.shell(Messages.addEpTtl).activate();
		wabot.comboBox().setSelection(Messages.typeInstnc);
		assertTrue("testInstanceEndPtControls",
				wabot.label(Messages.prvPortLbl).isEnabled()
				&& wabot.textWithLabel(Messages.prvPortLbl).isEnabled()
				&& wabot.label(Messages.pubPortRngLbl).isEnabled()
				&& wabot.textWithLabel(Messages.pubPortRngLbl).isEnabled()
				// Public port second text box
				&& wabot.text(2).isEnabled()
				// Private port second text box
				&& !wabot.text(4).isEnabled());
		wabot.button(Messages.cnclBtn).click();
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.8) test case 52
	public void testAddInternalEp() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InternalPt", Messages.typeIntrnl, "N/A", "16");
		assertTrue("testAddInternalEp", wabot.table().containsItem("InternalPt")
				&& wabot.table().cell(0, 3).equals("16"));
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.8) test case 53
	public void testAddInternalEpRange() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InternalPt", Messages.typeIntrnl, "N/A", "16-18");
		assertTrue("testAddInternalEpRange", wabot.table().containsItem("InternalPt")
				&& wabot.table().cell(0, 3).equals("16-18"));
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.8) test case 54
	public void testAddInternalEpValInSecondTxtBox() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		SWTBotShell shEp = wabot.shell(Messages.addEpTtl);
		shEp.activate();
		wabot.textWithLabel("Name:").setText("InternalPt");
		wabot.comboBox().setSelection(Messages.typeIntrnl);
		// set port in second text box
		wabot.text(4).setText("16");
		wabot.button("OK").click();
		SWTBotShell errorShell = wabot.shell(
				Messages.invalidEpPort).activate();
		boolean error = errorShell.getText().
				equals(Messages.invalidEpPort);
		wabot.button("OK").click();
		assertTrue("testAddInternalEpValInSecondTxtBox", error);
		wabot.button(Messages.cnclBtn).click();
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.8) test case 55
	public void testAddInternalEpRngValFirstSecontTxtBox() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		SWTBotShell shEp = wabot.shell(Messages.addEpTtl);
		shEp.activate();
		wabot.textWithLabel("Name:").setText("InternalPt");
		wabot.comboBox().setSelection(Messages.typeIntrnl);
		// set port in first text box
		wabot.textWithLabel(Messages.prvPortRngLbl).setText("16-18");
		wabot.button("OK").click();
		SWTBotShell errorShell = wabot.shell(
				Messages.invalidEpPort).activate();
		boolean error = errorShell.getText().
				equals(Messages.invalidEpPort);
		wabot.button("OK").click();
		// Set port in second text box
		wabot.textWithLabel(Messages.prvPortRngLbl).setText("");
		wabot.text(4).setText("16-18");
		wabot.button("OK").click();
		errorShell = wabot.shell(
				Messages.invalidEpPort).activate();
		boolean error1 = errorShell.getText().
				equals(Messages.invalidEpPort);
		wabot.button("OK").click();
		assertTrue("testAddInternalEpRngValFirstSecontTxtBox",
				error && error1);
		wabot.button(Messages.cnclBtn).click();
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.8) test case 56
	public void testAddInternalEpValInvalid() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		SWTBotShell shEp = wabot.shell(Messages.addEpTtl);
		shEp.activate();
		wabot.textWithLabel("Name:").setText("InternalPt");
		wabot.comboBox().setSelection(Messages.typeIntrnl);
		// port having special characters
		wabot.textWithLabel(Messages.prvPortRngLbl).setText("1#*");
		wabot.button("OK").click();
		SWTBotShell errorShell = wabot.shell(
				Messages.invalidEpPort).activate();
		boolean specialChar = errorShell.getText().
				equals(Messages.invalidEpPort);
		wabot.button("OK").click();
		// port having alphabets
		wabot.textWithLabel(Messages.prvPortRngLbl).setText("1a");
		wabot.button("OK").click();
		errorShell = wabot.shell(
				Messages.invalidEpPort).activate();
		boolean alphabets = errorShell.getText().
				equals(Messages.invalidEpPort);
		wabot.button("OK").click();
		// port number < 1
		wabot.textWithLabel(Messages.prvPortRngLbl).setText("0");
		wabot.button("OK").click();
		errorShell = wabot.shell(
				Messages.invalidEpPort).activate();
		boolean lessThanRng = errorShell.getText().
				equals(Messages.invalidEpPort);
		wabot.button("OK").click();
		// port number > 65535
		wabot.textWithLabel(Messages.prvPortRngLbl).setText("65536");
		wabot.button("OK").click();
		errorShell = wabot.shell(
				Messages.invalidEpPort).activate();
		boolean grtThanRng = errorShell.getText().
				equals(Messages.invalidEpPort);
		wabot.button("OK").click();
		assertTrue("testAddInternalEpValInvalid", specialChar
				&& alphabets
				&& lessThanRng
				&& grtThanRng);
		wabot.button(Messages.cnclBtn).click();
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.8) test case 57
	public void testAddInternalEpValUsed() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		SWTBotShell shEp = wabot.shell(Messages.addEpTtl);
		shEp.activate();
		wabot.textWithLabel("Name:").setText("InternalPt");
		wabot.comboBox().setSelection(Messages.typeIntrnl);
		// port value which is already used
		wabot.textWithLabel(Messages.prvPortRngLbl).setText("3");
		wabot.button("OK").click();
		SWTBotShell errorShell = wabot.shell(
				Messages.invalidEpPort).activate();
		boolean usedErr = errorShell.getText().
				equals(Messages.invalidEpPort);
		wabot.button("OK").click();
		assertTrue("testAddInternalEpValUsed", usedErr);
		wabot.button(Messages.cnclBtn).click();
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.8) test case 58
	public void testInternalEndPtNameUniqueInRole() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InlEndPt", Messages.typeIntrnl, "N/A", "16");
		// Check end point of same name present under role1 and role2
		boolean role1 = wabot.table().containsItem("InlEndPt");
		wabot.button("OK").click();
		propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		boolean role2 = wabot.table().containsItem("InlEndPt");
		assertTrue("testInternalEndPtNameUniqueInRole",
				role1 && role2);
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.8) test case 59
	public void testPrivatePortUniqueInRole() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InlEndPt", Messages.typeIntrnl, "N/A", "1-14");
		// Check end point of same port present under role1 and role2
		boolean role1 = wabot.table().cell(0, 3).equals("1-14");
		wabot.button("OK").click();
		propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		boolean role2 = wabot.table().cell(0, 3).equals("1");
		assertTrue("testPrivatePortUniqueInRole",
				role1 && role2);
		propShell.close();
	}
	
	@Test
	// (New Test Cases for 1.8) test case 60
	public void testPubPortUniqueInProj() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role2, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		SWTBotShell shEp = wabot.shell(Messages.addEpTtl);
		shEp.activate();
		wabot.textWithLabel("Name:").setText("abc");
		wabot.comboBox().setSelection(Messages.typeInpt);
		// public port value which is already used
		wabot.textWithLabel(Messages.pubPortLbl).setText("1");
		wabot.textWithLabel(Messages.prvPortLbl).setText("16");
		wabot.button("OK").click();
		SWTBotShell errorShell = wabot.shell(
				Messages.invalidEpPort).activate();
		boolean usedErr = errorShell.getText().
				equals(Messages.invalidEpPort);
		wabot.button("OK").click();
		assertTrue("testPubPortUniqueInProj", usedErr);
		wabot.button(Messages.cnclBtn).click();
		propShell.close();
	}
	
	// @Test
	// (New Test Cases for 1.8) test case 61
	// Need to implement in PML
	public void testPrivateInputAndInstSame() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InputEp", Messages.typeInpt, "4", "4");
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstanceEp", Messages.typeInstnc, "14", "4");
		assertTrue("testPrivateInputAndInstSame",
				wabot.table().cell(1, 3).equals("4")
				&& wabot.table().cell(2, 3).equals("4"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.8) test case 62
	public void testPrivateInputAndInstSame1() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InputEp", Messages.typeInpt, "14", "4");
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstanceEp", Messages.typeInstnc, "15", "4");
		assertTrue("testPrivateInputAndInstSame1",
				wabot.table().cell(0, 3).equals("4")
				&& wabot.table().cell(1, 3).equals("4"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.8.1) test case
	public void testPrivatePublicInstSame() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstanceEp", Messages.typeInstnc, "81-85", "90");
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstanceEpNxt", Messages.typeInstnc, "91-95", "100");
		wabot.table().select(0);
		wabot.button(Messages.roleEditBtn).click();
		SWTBotShell shEp = wabot.shell(Messages.editEpTtl);
		shEp.activate();
		wabot.textWithLabel("Private port:").setText("91");
		wabot.button("OK").click();
		wabot.table().select(1);
		wabot.button(Messages.roleEditBtn).click();
		wabot.shell(Messages.editEpTtl).activate();
		wabot.textWithLabel("Private port:").setText("81");
		wabot.button("OK").click();
		assertTrue("testPrivatePublicInstSame",
				wabot.table().cell(0, 3).equals("91")
				&& wabot.table().cell(1, 3).equals("81"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.8.1) test case
	public void testPrivatePublicInstSameInPlaceEdt() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstanceEp", Messages.typeInstnc, "81-85", "90");
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstanceEpNxt", Messages.typeInstnc, "91-95", "100");
		wabot.table().click(0, 3);
		wabot.text("90", 0).typeText("91");
		wabot.table().click(0, 0);
		wabot.table().click(1, 3);
		wabot.text("100", 0).typeText("81");
		wabot.table().click(0, 0);
		assertTrue("testPrivatePublicInstSameInPlaceEdt",
				wabot.table().cell(0, 3).equals("91")
				&& wabot.table().cell(1, 3).equals("81"));
		propShell.close();
	}

	@Test
	// (New Test Cases for 1.8.1) test case
	public void testPrivatePublicInstSameAdd() throws Exception {
		Utility.closeProjPropertyPage(Messages.projWithEp);
		SWTBotShell propShell = Utility.
				selectPageUsingContextMenu(Messages.projWithEp,
						Messages.role1, Messages.endptPage);
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstanceEp", Messages.typeInstnc, "81-85", "90");
		wabot.button(Messages.roleAddBtn).click();
		Utility.addEp("InstanceEpNxt", Messages.typeInstnc, "91-95", "81");
		assertTrue("testPrivatePublicInstSameAdd",
				wabot.table().cell(0, 3).equals("90")
				&& wabot.table().cell(1, 3).equals("81"));
		propShell.close();
	}
}
