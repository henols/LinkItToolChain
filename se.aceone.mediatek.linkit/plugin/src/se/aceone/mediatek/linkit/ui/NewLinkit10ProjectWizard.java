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

import java.io.IOException;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import se.aceone.mediatek.linkit.Activator;
import se.aceone.mediatek.linkit.tools.Common;
import se.aceone.mediatek.linkit.tools.LinkIt10HelperGCC;
import se.aceone.mediatek.linkit.tools.LinkIt10HelperRVTC;
import se.aceone.mediatek.linkit.tools.LinkIt10HelperRVTCLib;
import se.aceone.mediatek.linkit.tools.LinkItHelper;

public class NewLinkit10ProjectWizard extends NewLinkitProjectWizard {

	private ConfigureLinkIt10ProjectWizardPage configPage;

	public NewLinkit10ProjectWizard() {
		super();
	}

	@Override
	/**
	 * adds pages to the wizard. We are using the standard project wizard of Eclipse
	 */
	public void addPages() {
		//
		// We assume everything is OK as it is tested in the handler
		// create each page and fill in the title and description
		// first page to fill in the project name
		//
		mWizardPage = new WizardNewProjectCreationPage("New LinkIt SDK 1.0 Tool Chain Project");
		mWizardPage.setDescription("Create a new LinkItIt SDK 1.0 Tool Chain Project.");
		mWizardPage.setTitle("New LinkItIt SDK 1.0 Tool Chain Project");
		AbstractUIPlugin plugin = Activator.getDefault();
		ImageRegistry imageRegistry = plugin.getImageRegistry();
		Image myImage = imageRegistry.get(Activator.CPU_64PX);
		ImageDescriptor image = ImageDescriptor.createFromImage(myImage);
		mWizardPage.setImageDescriptor(image);
		//
		// /
		addPage(mWizardPage);

		configPage = new ConfigureLinkIt10ProjectWizardPage();
		addPage(configPage);
	}

	/**
	 * This creates the project in the workspace.
	 * 
	 * @param description
	 * @param projectHandle
	 * @param monitor
	 * @throws OperationCanceledException
	 */
	protected void createProject(IProjectDescription description, IProject project, IProgressMonitor monitor) throws OperationCanceledException {

		monitor.beginTask("", 2000);
		try {
			boolean staticLibrary = configPage.isStaticLibrary();
			if (staticLibrary) {
				System.out.println("Static Library");
				helper = new LinkIt10HelperRVTCLib(project);
			} else {
				System.out.println("VXP");
				helper = new LinkIt10HelperRVTC(project);
			}
			if (!helper.checkEnvironment()) {
				MultiStatus status = new MultiStatus(CORE_PLUGIN_ID, IStatus.ERROR, "Enviroment for LinkIt SDK 1.0 are not configuerd.", null);
				String envPath = helper.getEnvironmentPath();
				status.add( new Status(IStatus.ERROR ,CORE_PLUGIN_ID,"Environment Path: "+  envPath));
				status.add( new Status(IStatus.ERROR ,CORE_PLUGIN_ID,"Found sys.ini file: "+  LinkItHelper.checkSysIni(envPath)));
				Common.log(status);
				OperationCanceledException exception = new OperationCanceledException("Enviroment for LinkIt SDK 1.0 are not configuerd.");
				throw exception;
			}

			IPathVariableManager manager = project.getWorkspace().getPathVariableManager();
			if (manager.getURIValue(LinkIt10HelperGCC.LINK_IT_SDK10) == null) {
				manager.setURIValue(LinkIt10HelperGCC.LINK_IT_SDK10, URIUtil.toURI(helper.getEnvironmentPath()));
			}

			project.create(description, new SubProgressMonitor(monitor, 1000));

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1000));

			// Creates the .cproject file with the configurations
			ICProjectDescription projectDescription = helper.setCProjectDescription(project, true, monitor);

			// Add the C C++ AVR and other needed Natures to the project
			helper.addTheNatures(project);

			// Set the environment variables
			// ICProjectDescription projectDescription = CoreModel.getDefault().getProjectDescription(project);

			ICConfigurationDescription defaultConfigDescription = projectDescription.getConfigurationByName(LINKIT_CONFIGURATION_NAME);
			projectDescription.setActiveConfiguration(defaultConfigDescription);

			ICResourceDescription resourceDescription = defaultConfigDescription.getResourceDescription(new Path(""), true);

			String devBoard = "__LINKIT_SDK__";
			try {
				helper.setEnvironmentVariables(resourceDescription, devBoard);
			} catch (IOException e) {
				String message = "Failed to set environmet variables " + project.getName();
				Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, message, e));
				throw new OperationCanceledException(message);
			}

			helper.buildPathVariables(project, resourceDescription);

			// IPathVariableManager pathMan = project.getPathVariableManager();
			// URI uri = pathMan.resolveURI(pathMan.getURIValue(LINKIT10));
			// helper.createNewFolder(project, "LinkIt", URIUtil.toURI(new Path(uri.getPath()).append("src")));

			helper.createNewFolder(project, "src", null);
			if (!staticLibrary) {
				helper.createNewFolder(project, "LinkIt", null);
				helper.createNewFolder(project, "res", null);
				helper.createNewFolder(project, "ResID", null);
			}
			helper.setIncludePaths(projectDescription, resourceDescription);

			helper.setMacros(projectDescription, devBoard);

			helper.setSourcePaths(projectDescription);

			helper.copyProjectResources(projectDescription, monitor);

			projectDescription.setActiveConfiguration(defaultConfigDescription);
			projectDescription.setCdtProjectCreated();
			CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(project, projectDescription, true, null);

			monitor.done();

		} catch (Exception e) {
			String message = "Failed to create project " + project.getName();
			Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, message, e));
			throw new OperationCanceledException(message);
		}

	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

}
