package com.microsoft.azuretools.core.utils;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.FillLayout;

public class ErrorWindow extends TitleAreaDialog {
	private Text text;
	
	public static void go(Shell parentShell, String errorDescription, String title) {
		ErrorWindow w = new ErrorWindow(parentShell);
		w.create();
		w.setTitle(title);
		w.setErrorDescription(errorDescription);
		w.open();
	}

	private void setErrorDescription(String errorDescription) {
		text.setText(errorDescription);
	}
	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	private ErrorWindow(Shell parentShell) {
		super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Error Window");
		setMessage("Error details");

		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new FillLayout(SWT.HORIZONTAL));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		text = new Text(container, SWT.BORDER | SWT.WRAP | SWT.MULTI);

		return area;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}
}
