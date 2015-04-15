package se.aceone.mediatek.linkit.ui;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICExclusionPatternPathEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
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

public class NewLinkitProjectWizard extends Wizard implements INewWizard, IExecutableExtension {

	private static final String LINK_IT_SDK20 = "LinkItSDK20";
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
			project.create(description, new SubProgressMonitor(monitor, 1000));

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1000));

			// Get the Build Configurations (names and toolchain IDs) from the
			// property page
//			ArrayList<ConfigurationDescriptor> cfgNamesAndTCIds = mBuildCfgPage.getBuildConfigurationDescriptors();
			String name = "Default";
			String toolChainId = "se.aceone.mediatek.linkit.toolChain.default";
			// Creates the .cproject file with the configurations
			LinkItHelpers.setCProjectDescription(project, toolChainId, name, true, monitor);

			// Add the C C++ AVR and other needed Natures to the project
			LinkItHelpers.addTheNatures(project);

			// Add the Arduino folder
			LinkItHelpers.createNewFolder(project, "arduino", null);

			// Set the environment variables
			ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project);

//			ICConfigurationDescription configurationDescription = prjDesc.getConfigurationByName(name);
//				mArduinoPage.saveAllSelections(configurationDescription);
//				ArduinoHelpers.setTheEnvironmentVariables(project, configurationDescription, false);

			// Set the path variables
//			ArduinoHelpers.setProjectPathVariables(project, mArduinoPage.getPlatformFolder());

			// Intermediately save or the adding code will fail
			// Release is the active config (as that is the "IDE" Arduino
			// type....)
			ICConfigurationDescription defaultConfigDescription = prjDesc.getConfigurationByName(name);
			prjDesc.setActiveConfiguration(defaultConfigDescription);

			// Insert The Arduino Code
			// NOTE: Not duplicated for debug (the release reference is just to
			// get at some environment variables)
//			LinkItHelpers.addArduinoCodeToProject(project, defaultConfigDescription);

			//
			// add the correct files to the project
			//
//			mNewArduinoSketchWizardCodeSelectionPage.createFiles(project, monitor);
			//
			// add the libraries to the project if needed
			//
//			mNewArduinoSketchWizardCodeSelectionPage.importLibraries(project, prjDesc.getConfigurations());

			ICResourceDescription cfgd = defaultConfigDescription.getResourceDescription(new Path(""), true);
			ICExclusionPatternPathEntry[] entries = cfgd.getConfiguration().getSourceEntries();
			if (entries.length == 1) {
				Path exclusionPath[] = new Path[2];
				exclusionPath[0] = new Path("Libraries/*/?xamples");
				exclusionPath[1] = new Path("Libraries/*/?xtras");
				ICExclusionPatternPathEntry newSourceEntry = new CSourceEntry(entries[0].getFullPath(), exclusionPath, ICSettingEntry.VALUE_WORKSPACE_PATH);
				ICSourceEntry[] out = null;
				out = new ICSourceEntry[1];
				out[0] = (ICSourceEntry)newSourceEntry;
				try {
					cfgd.getConfiguration().setSourceEntries(out);
				} catch (CoreException e) {
					// ignore
				}

			} else {
				// this should not happen
			}

			// set warning levels default on
			IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
			IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
			contribEnv.addVariable(new EnvironmentVariable(LinkItConst.ENV_KEY_JANTJE_WARNING_LEVEL, LinkItConst.ENV_KEY_WARNING_LEVEL_ON), cfgd.getConfiguration());

			String linkitEnv = System.getenv().get(LINK_IT_SDK20);
			if(linkitEnv == null){
				linkitEnv = System.getenv().get(LINK_IT_SDK20.toUpperCase());
			}
			linkitEnv = linkitEnv.replace('\\', '/');
			System.out.println(LINK_IT_SDK20 + "=" + linkitEnv);
			contribEnv.addVariable(new EnvironmentVariable(LINK_IT_SDK20.toUpperCase(), linkitEnv), cfgd.getConfiguration());
			File sysini = new File(linkitEnv, "/tools/sys.ini");
			try {

				LineNumberReader numberReader = new LineNumberReader(new FileReader(sysini));
				String line;
				while ((line = numberReader.readLine()) != null) {
					if (!line.trim().isEmpty() && !line.startsWith("[")) {
						int i = line.indexOf("=");
						if (i > 0) {
							String key = line.substring(0, i).trim().toUpperCase();
							String value = line.substring(i + 1).trim();
							if (value.startsWith("\"") && value.endsWith("\"")) {
								value = value.substring(1, value.length() - 1).trim();
							}
							value = value.replace('\\', '/');
							System.out.println(key + "=" + value);
							contribEnv.addVariable(new EnvironmentVariable(key, value), cfgd.getConfiguration());
						}
					}
				}
				numberReader.close();
			} catch (IOException e) {
				Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Failed to create project " + project.getName(), e));
				throw new OperationCanceledException();
			}
			
			String fullPath = linkitEnv+"\\tools\\gcc-arm-none-eabi-4_9-2014q4-20141203-win32\\lib\\gcc\\arm-none-eabi\\4.9.3\\include";
			File f = new File(fullPath);
			System.out.println(f.exists());
			IPath path = new Path(fullPath);
			
			LinkItHelpers.addIncludeFolder(prjDesc, path);

			prjDesc.setActiveConfiguration(defaultConfigDescription);
			prjDesc.setCdtProjectCreated();
			CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(project, prjDesc, true, null);

			monitor.done();

		} catch (CoreException e) {
			Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Failed to create project " + project.getName(), e));
			throw new OperationCanceledException();
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
