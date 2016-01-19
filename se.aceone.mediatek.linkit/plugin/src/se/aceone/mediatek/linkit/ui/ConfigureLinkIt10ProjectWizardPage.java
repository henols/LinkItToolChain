/*
 * LinkIt Tool Chain, an eclipse plugin for LinkIt SDK 1.0 and 2.0
 * 
 * Copyright © 2015 Henrik Olsson (henols@gmail.com)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.aceone.mediatek.linkit.ui;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class ConfigureLinkIt10ProjectWizardPage extends WizardPage {

	public ConfigureLinkIt10ProjectWizardPage() {
		super("Configure LinkIt 1.0 Project"); //$NON-NLS-1$
		setPageComplete(true);
		setTitle("Configure LinkIt 1.0 Project");
		setDescription("Configure a LinkIt 1.0 Project");
	}

	// widgets

	private Button staticLib;

	/**
	 * (non-Javadoc) Method declared on IDialogPage.
	 */
	public void createControl(Composite parent) {

		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NULL);

		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(parent.getFont());

		createCopyResourceGroup(composite);

		// Show description on opening
		setErrorMessage(null);
		setMessage(null);
		setControl(composite);
	}

	private final void createCopyResourceGroup(Composite parent) {

		Font dialogFont = parent.getFont();

		// browse button
		this.staticLib = new Button(parent, SWT.CHECK);
		this.staticLib.setText("Static Library");
		this.staticLib.setFont(dialogFont);
		setButtonLayoutData(this.staticLib);

	}

	public boolean isStaticLibrary() {
		return staticLib.getSelection();
	}

	public se.aceone.mediatek.linkit.tools.Compiler getCompiler() {
		return null;
	}

}
