package com.microsoftopentechnologies.wacommon.startup;

import java.awt.Checkbox;
import java.net.URL;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.microsoftopentechnologies.wacommon.Activator;
import com.microsoftopentechnologies.wacommon.utils.Messages;


public class HDInsightHelpDlg extends Dialog {
	private boolean isShowTips = true;
	
	public boolean isShowTipsStatus() {
		return isShowTips;
	}
	
	protected HDInsightHelpDlg(Shell parentShell) {
		super(parentShell);
	}
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.hdinsgihtPrefTil);
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control ctrl = super.createButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setText("Yes");
		getButton(IDialogConstants.CANCEL_ID).setText("No");
		return ctrl;
	}

	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(1, false);
		container.setLayout(gridLayout);

		Link urlLink = new Link(container, SWT.LEFT);
		urlLink.setText(Messages.hdinsightPerenceQueMsg);

		urlLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					PlatformUI.getWorkbench().getBrowserSupport().
					getExternalBrowser().openURL(new URL(event.text));
				} catch (Exception ex) {
					Activator.getDefault().log(Messages.lnkOpenErrMsg, ex);
				}
			}
		});
		final Button checkButton = new Button(container, SWT.CHECK);
		checkButton.setText("Do not ask again");
		checkButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				isShowTips = !checkButton.getSelection();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
		return super.createContents(parent);
	}
}
