package se.aceone.mediatek.linkit.ui;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import se.aceone.mediatek.linkit.tools.Common;
import se.aceone.mediatek.linkit.tools.LinkItConst;
import se.aceone.mediatek.linkit.tools.LinkItHelpers;

public class NewLinkitProjectWizard extends Wizard implements LinkItConst, INewWizard, IExecutableExtension {

	private WizardNewProjectCreationPage mWizardPage; // first page of the
														// dialog
	private IConfigurationElement mConfig;
	private IProject mProject;

	public NewLinkitProjectWizard() {
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
		mWizardPage = new WizardNewProjectCreationPage("New LinkIt Tool Chain Project");
		mWizardPage.setDescription("Create a new LinkIt Tool Chain Project.");
		mWizardPage.setTitle("New LinkIt Tool Chain Project");
		//
		// /
		addPage(mWizardPage);

	}

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
	void createProject(IProjectDescription description, IProject project, IProgressMonitor monitor) throws OperationCanceledException {

		monitor.beginTask("", 2000);
		try {
			if (!LinkItHelpers.checkEnvironment()) {
				Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, "Enviroment are not configuerd."));
				throw new OperationCanceledException("Enviroment are not configuerd.");
			}

			IPathVariableManager manager = project.getWorkspace().getPathVariableManager();
			if (manager.getURIValue(LINK_IT_SDK20) == null) {
				manager.setURIValue(LINK_IT_SDK20, URIUtil.toURI(LinkItHelpers.getEnvironmentPath()));
			}

			project.create(description, new SubProgressMonitor(monitor, 1000));

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1000));

			String name = "Default";
			String toolChainId = "se.aceone.mediatek.linkit.toolChain.default";
			// Creates the .cproject file with the configurations
			LinkItHelpers.setCProjectDescription(project, toolChainId, name, true, monitor);

			// Add the C C++ AVR and other needed Natures to the project
			LinkItHelpers.addTheNatures(project);

			// Set the environment variables
			ICProjectDescription projectDescription = CoreModel.getDefault().getProjectDescription(project);

			ICConfigurationDescription defaultConfigDescription = projectDescription.getConfigurationByName(name);
			projectDescription.setActiveConfiguration(defaultConfigDescription);

			ICResourceDescription cfgd = defaultConfigDescription.getResourceDescription(new Path(""), true);
//			ICExclusionPatternPathEntry[] entries = cfgd.getConfiguration().getSourceEntries();
//			if (entries.length == 1) {
//				Path exclusionPath[] = new Path[2];
//				exclusionPath[0] = new Path("Libraries/*/?xamples");
//				exclusionPath[1] = new Path("Libraries/*/?xtras");
//				ICExclusionPatternPathEntry newSourceEntry = new CSourceEntry(entries[0].getFullPath(), exclusionPath, ICSettingEntry.VALUE_WORKSPACE_PATH);
//				ICSourceEntry[] out = null;
//				out = new ICSourceEntry[1];
//				out[0] = (ICSourceEntry)newSourceEntry;
//				try {
//					cfgd.getConfiguration().setSourceEntries(out);
//				} catch (CoreException e) {
//					// ignore
//				}
//
//			} else {
//				// this should not happen
//			}

			// set warning levels default on
			try {
				LinkItHelpers.setEnvironmentVariables(cfgd);
			} catch (IOException e) {
				String message = "Failed to set environmet variables " + project.getName();
				Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, message, e));
				throw new OperationCanceledException(message);
			}

			LinkItHelpers.buildPathVariables(project, cfgd);


			IPathVariableManager pathMan = project.getPathVariableManager();
			URI uri = pathMan.resolveURI(pathMan.getURIValue(LINKIT10));
			LinkItHelpers.createNewFolder(project, "LinkIt", URIUtil.toURI(new Path(uri.getPath()).append("src")));

			LinkItHelpers.createNewFolder(project, "src", null);
			
			LinkItHelpers.createNewFolder(project, "res", null);
			LinkItHelpers.createNewFolder(project, "ResID", null);
			
			LinkItHelpers.setIncludePaths(projectDescription, cfgd);

			LinkItHelpers.setMacros(projectDescription);

			LinkItHelpers.setSourcePaths(projectDescription);
			
			LinkItHelpers.copyProjectResources(projectDescription, monitor);
			
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

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		mConfig = config;

	}

}
