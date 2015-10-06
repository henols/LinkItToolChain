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

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import se.aceone.mediatek.linkit.common.LinkItConst;
import se.aceone.mediatek.linkit.tools.LinkItHelper;

public abstract class NewLinkitProjectWizard extends Wizard implements LinkItConst, INewWizard, IExecutableExtension {

	protected WizardNewProjectCreationPage mWizardPage; // first page of the
														// dialog
	private IConfigurationElement mConfig;
	private IProject mProject;

	protected LinkItHelper helper;

	/**
	 * this method is required by IWizard otherwise nothing will actually happen
	 */
	@Override
	public boolean performFinish() {
		//
		// if the project is filled in then we are done
		//
		if (mProject != null) {
			return true;
		}
		//
		// get an IProject handle to our project
		//
		final IProject projectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject((mWizardPage.getProjectName()));
		//
		// let's validate it
		//
		try {
			//
			// get the URL if it is filled in. This depends on the check box
			// "use defaults" is checked
			// or not
			//
			URI projectURI = (!mWizardPage.useDefaults()) ? mWizardPage.getLocationURI() : null;
			//
			// get the workspace name
			//
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			//
			// the project descriptions is set equal to the name of the project
			//
			final IProjectDescription desc = workspace.newProjectDescription(projectHandle.getName());
			//
			// get our workspace location
			//
			desc.setLocationURI(projectURI);

			/*
			 * Just like the ExampleWizard, but this time with an operation
			 * object that modifies workspaces.
			 */
			WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
				@Override
				protected void execute(IProgressMonitor monitor) throws CoreException {
					//
					// actually create the project
					//
					createProject(desc, projectHandle, monitor);
				}
			};

			/*
			 * This isn't as robust as the code in the
			 * BasicNewProjectResourceWizard class. Consider beefing this up to
			 * improve error handling.
			 */
			getContainer().run(false, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		//
		// so the project is created we can start
		//
		mProject = projectHandle;

		if (mProject == null) {
			return false;
		}
		//
		// so now we set Eclipse to the right perspective and switch to our just
		// created
		// project
		//
		BasicNewProjectResourceWizard.updatePerspective(mConfig);
		IWorkbenchWindow TheWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		BasicNewResourceWizard.selectAndReveal(mProject, TheWindow);

		return true;
	}

	/**
	 * This creates the project in the workspace.
	 * 
	 * @param description
	 * @param projectHandle
	 * @param monitor
	 * @throws OperationCanceledException
	 */
	protected abstract void createProject(IProjectDescription description, IProject project, IProgressMonitor monitor) throws OperationCanceledException ;


	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		mConfig = config;
	}

}
