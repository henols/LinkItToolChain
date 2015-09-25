package se.aceone.mediatek.linkit.importWizards;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;

import se.aceone.mediatek.linkit.Activator;
import se.aceone.mediatek.linkit.common.LinkItConst;
import se.aceone.mediatek.linkit.tools.Common;
import se.aceone.mediatek.linkit.tools.LinkIt10HelperGCC;
import se.aceone.mediatek.linkit.tools.LinkIt10HelperRVTC;
import se.aceone.mediatek.linkit.tools.LinkIt10HelperRVTCLib;
import se.aceone.mediatek.linkit.tools.LinkItHelper;
import se.aceone.mediatek.linkit.xml.config.Packageinfo;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Output;
import se.aceone.mediatek.linkit.xml.proj.VisualStudioProject;

public class ImportLinkIt10ProjectWizardPage extends WizardPage {

	public ImportLinkIt10ProjectWizardPage() {
		super("Import LinkIt 1.0 Project"); //$NON-NLS-1$
		setPageComplete(false);
		setTitle("Import LinkIt 1.0 Project");
		setDescription("Import an exsiting LinkIt 1.0 Project");

	}

	private FileFilter projectFilter = new FileFilter() {
		// Only accept those files that are .project
		public boolean accept(File pathName) {
			String name = pathName.getName();
			return name.endsWith(".vcproj") && !(name.startsWith("~") || name.startsWith("."));
		}
	};

	// Keep track of the directory that we browsed to last time
	// the wizard was invoked.
	private static String previouslyBrowsedDirectory = ""; //$NON-NLS-1$

	// widgets
	private Text projectNameField;

	private Text locationPathField;

	private Button browseButton;

	private Button copyResorces;

	private IProjectDescription description;

	private Listener locationModifyListener = new Listener() {
		public void handleEvent(Event e) {
			setPageComplete(validatePage());
		}
	};

	// constants
	private static final int SIZING_TEXT_FIELD_WIDTH = 250;

	/**
	 * (non-Javadoc) Method declared on IDialogPage.
	 */
	public void createControl(Composite parent) {

		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NULL);

		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(parent.getFont());

		createProjectNameGroup(composite);
		createProjectLocationGroup(composite);

		createCopyResourceGroup(composite);

		validatePage();
		// Show description on opening
		setErrorMessage(null);
		setMessage(null);
		setControl(composite);
	}

	/**
	 * Creates the project location specification controls.
	 *
	 * @param parent
	 *            the parent composite
	 */
	private final void createProjectLocationGroup(Composite parent) {

		// project specification group
		Composite projectGroup = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		projectGroup.setLayout(layout);
		projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		projectGroup.setFont(parent.getFont());

		// new project label
		Label projectContentsLabel = new Label(projectGroup, SWT.NONE);
		projectContentsLabel.setText(DataTransferMessages.WizardExternalProjectImportPage_projectContentsLabel);
		projectContentsLabel.setFont(parent.getFont());

		createUserSpecifiedProjectLocationGroup(projectGroup);
	}

	/**
	 * Creates the project name specification controls.
	 *
	 * @param parent
	 *            the parent composite
	 */
	private final void createProjectNameGroup(Composite parent) {

		Font dialogFont = parent.getFont();

		// project specification group
		Composite projectGroup = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		projectGroup.setFont(dialogFont);
		projectGroup.setLayout(layout);
		projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// new project label
		Label projectLabel = new Label(projectGroup, SWT.NONE);
		projectLabel.setText(DataTransferMessages.WizardExternalProjectImportPage_nameLabel);
		projectLabel.setFont(dialogFont);

		// new project name entry field
		projectNameField = new Text(projectGroup, SWT.BORDER | SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = SIZING_TEXT_FIELD_WIDTH;
		projectNameField.setLayoutData(data);
		projectNameField.setFont(dialogFont);
		projectNameField.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
	}

	private final void createCopyResourceGroup(Composite parent) {

		Font dialogFont = parent.getFont();

		// browse button
		this.copyResorces = new Button(parent, SWT.CHECK);
		this.copyResorces.setText("Copy resources to workspace");
		this.copyResorces.setFont(dialogFont);
		setButtonLayoutData(this.copyResorces);

		locationPathField.addListener(SWT.Modify, locationModifyListener);
	}

	/**
	 * Creates the project location specification controls.
	 *
	 * @param projectGroup
	 *            the parent composite
	 */
	private void createUserSpecifiedProjectLocationGroup(Composite projectGroup) {

		Font dialogFont = projectGroup.getFont();

		// project location entry field
		this.locationPathField = new Text(projectGroup, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = SIZING_TEXT_FIELD_WIDTH;
		this.locationPathField.setLayoutData(data);
		this.locationPathField.setFont(dialogFont);

		// browse button
		this.browseButton = new Button(projectGroup, SWT.PUSH);
		this.browseButton.setText(DataTransferMessages.DataTransfer_browse);
		this.browseButton.setFont(dialogFont);
		setButtonLayoutData(this.browseButton);

		this.browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleLocationBrowseButtonPressed();
			}
		});

		locationPathField.addListener(SWT.Modify, locationModifyListener);
	}

	/**
	 * Returns the current project location path as entered by the user, or its anticipated initial value.
	 *
	 * @return the project location path, its anticipated initial value, or <code>null</code> if no project location
	 *         path is known
	 */
	public IPath getLocationPath() {
		return new Path(getProjectLocationFieldValue());
	}

	/**
	 * Creates a project resource handle for the current project name field value.
	 * <p>
	 * This method does not create the project resource; this is the responsibility of <code>IProject::create</code>
	 * invoked by the new project resource wizard.
	 * </p>
	 *
	 * @return the new project resource handle
	 */
	public IProject getProjectHandle() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(getProjectName());
	}

	/**
	 * Returns the current project name as entered by the user, or its anticipated initial value.
	 *
	 * @return the project name, its anticipated initial value, or <code>null</code> if no project name is known
	 */
	public String getProjectName() {
		return getProjectNameFieldValue();
	}

	public boolean copyResources() {
		return copyResorces.getSelection();
	}

	/**
	 * Returns the value of the project name field with leading and trailing spaces removed.
	 * 
	 * @return the project name in the field
	 */
	private String getProjectNameFieldValue() {
		if (projectNameField == null) {
			return ""; //$NON-NLS-1$
		}

		return projectNameField.getText().trim();
	}

	/**
	 * Returns the value of the project location field with leading and trailing spaces removed.
	 * 
	 * @return the project location directory in the field
	 */
	private String getProjectLocationFieldValue() {
		return locationPathField.getText().trim();
	}

	/**
	 * Open an appropriate directory browser
	 */
	private void handleLocationBrowseButtonPressed() {
		DirectoryDialog dialog = new DirectoryDialog(locationPathField.getShell(), SWT.SHEET);
		dialog.setMessage(DataTransferMessages.WizardExternalProjectImportPage_directoryLabel);

		String dirName = getProjectLocationFieldValue();
		if (dirName.length() == 0) {
			dirName = previouslyBrowsedDirectory;
		}

		if (dirName.length() == 0) {
			dialog.setFilterPath(getWorkspace().getRoot().getLocation().toOSString());
		} else {
			File path = new File(dirName);
			if (path.exists()) {
				dialog.setFilterPath(new Path(dirName).toOSString());
			}
		}

		String selectedDirectory = dialog.open();
		if (selectedDirectory != null) {
			previouslyBrowsedDirectory = selectedDirectory;
			locationPathField.setText(previouslyBrowsedDirectory);
			setProjectName(projectFile(previouslyBrowsedDirectory));
		}
	}

	/**
	 * Returns whether this page's controls currently all contain valid values.
	 *
	 * @return <code>true</code> if all controls are valid, and <code>false</code> if at least one is invalid
	 */
	private boolean validatePage() {

		String locationFieldContents = getProjectLocationFieldValue();

		if (locationFieldContents.equals("")) { //$NON-NLS-1$
			setErrorMessage(null);
			setMessage(DataTransferMessages.WizardExternalProjectImportPage_projectLocationEmpty);
			return false;
		}

		IPath path = new Path(""); //$NON-NLS-1$
		if (!path.isValidPath(locationFieldContents)) {
			setErrorMessage(DataTransferMessages.WizardExternalProjectImportPage_locationError);
			return false;
		}

		File projectFile = projectFile(locationFieldContents);
		if (projectFile == null) {
			setErrorMessage(NLS.bind(DataTransferMessages.WizardExternalProjectImportPage_notAProject, locationFieldContents));
			return false;
		}
		setProjectName(projectFile);

		if (getProjectHandle().exists()) {
			setErrorMessage(DataTransferMessages.WizardExternalProjectImportPage_projectExistsMessage);
			return false;
		}

		setErrorMessage(null);
		setMessage(null);
		return true;
	}

	private IWorkspace getWorkspace() {
		IWorkspace workspace = IDEWorkbenchPlugin.getPluginWorkspace();
		return workspace;
	}

	/**
	 * Return whether or not the specifed location is a prefix of the root.
	 */
	private boolean isPrefixOfRoot(IPath locationPath) {
		return Platform.getLocation().isPrefixOf(locationPath);
	}

	/**
	 * Set the project name using either the name of the parent of the file or the name entry in the xml for the file
	 */
	private void setProjectName(File projectFile) {

		// If there is no file or the user has already specified forget it
		if (projectFile == null) {
			return;
		}
		VisualStudioProject visualStudioProject = null;
		try {
			JAXBContext context = JAXBContext.newInstance(VisualStudioProject.class);
			Unmarshaller nmarshaller = context.createUnmarshaller();
			visualStudioProject = (VisualStudioProject) nmarshaller.unmarshal(projectFile);

		} catch (Exception exception) {
			// no good couldn't get the name
			exception.printStackTrace();
		}

		if (visualStudioProject == null) {
			this.description = null;
			this.projectNameField.setText(""); //$NON-NLS-1$
		} else {
			this.projectNameField.setText(visualStudioProject.getName());
		}
	}

	private boolean isStaticLib(File projectFile) {

		// If there is no file or the user has already specified forget it
		if (projectFile == null) {
			return false;
		}
		Packageinfo config = null;
		try {
			JAXBContext context = JAXBContext.newInstance(Packageinfo.class);
			Unmarshaller nmarshaller = context.createUnmarshaller();
			config = (Packageinfo) nmarshaller.unmarshal(projectFile);

		} catch (Exception exception) {
			// no good couldn't get the name
			exception.printStackTrace();
		}

		if (config == null) {
			return false;
		}
		Output output = config.getOutput();
		return output != null && output.getType() != null && output.getType().intValue() == 3;
	}

	/**
	 * Return a.project file from the specified location. If there isn't one return null.
	 */
	private File projectFile(String locationFieldContents) {
		File directory = new File(locationFieldContents);
		if (directory.isFile()) {
			return null;
		}

		File[] files = directory.listFiles(this.projectFilter);
		if (files != null && files.length == 1) {
			return files[0];
		}

		return null;
	}

	/**
	 * Creates a new project resource with the selected name.
	 * <p>
	 * In normal usage, this method is invoked after the user has pressed Finish on the wizard; the enablement of the
	 * Finish button implies that all controls on the pages currently contain valid values.
	 * </p>
	 *
	 * @return the created project resource, or <code>null</code> if the project was not created
	 */
	IProject createExistingProject() {

		String projectName = projectNameField.getText();
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProject project = workspace.getRoot().getProject(projectName);
		final IPath locationPath = getLocationPath();
		final boolean copyResources = copyResources();
		this.description = workspace.newProjectDescription(projectName);
		// If it is under the root use the default location
		if (isPrefixOfRoot(locationPath) || copyResources) {
			this.description.setLocation(null);
		} else {
			this.description.setLocation(locationPath);
		}

		// create the new project operation
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {

			protected void execute(IProgressMonitor monitor) throws CoreException {
				monitor.beginTask("", 2000); //$NON-NLS-1$
				project.create(description, new SubProgressMonitor(monitor, 1000));
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}

				IPath configPath = locationPath.append("config.xml");
				LinkItHelper helper; 
				boolean staticLib = isStaticLib(new File(configPath.toOSString()));
				if(staticLib){
					helper = new LinkIt10HelperRVTCLib(project);
				}else{
					helper = new LinkIt10HelperRVTC(project);
				}
				// helper = new LinkIt10HelperRVTC(project);

				if (!helper.checkEnvironment()) {
					Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Enviroment for LinkIt SDK 1.0 are not configuerd."));
					throw new OperationCanceledException("Enviroment for LinkIt SDK 1.0 are not configuerd.");
				}

				project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1000));
				if (copyResources) {
					copyProjectToWorkspace(project, locationPath, monitor);
				}
				IPathVariableManager manager = project.getWorkspace().getPathVariableManager();
				if (manager.getURIValue(LinkIt10HelperGCC.LINK_IT_SDK10) == null) {
					manager.setURIValue(LinkIt10HelperGCC.LINK_IT_SDK10, URIUtil.toURI(helper.getEnvironmentPath()));
				}

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

				ICConfigurationDescription defaultConfigDescription = projectDescription.getConfigurationByName(LinkItConst.LINKIT_CONFIGURATION_NAME);
				projectDescription.setActiveConfiguration(defaultConfigDescription);

				ICResourceDescription resourceDescription = defaultConfigDescription.getResourceDescription(new Path(""), true);

				String devBoard = "__LINKIT_SDK__";
				try {
					helper.setEnvironmentVariables(resourceDescription, devBoard);
				} catch (IOException e) {
					String message = "Failed to set environmet variables " + project.getName();
					Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, message, e));
					throw new OperationCanceledException(message);
				}

				helper.buildPathVariables(project, resourceDescription);

				if (!staticLib &&!project.getFolder("LinkIt").exists()) {
					helper.createNewFolder(project, "LinkIt", null);
				}
				helper.setIncludePaths(projectDescription, resourceDescription);

				helper.setMacros(projectDescription, devBoard);

				helper.setSourcePaths(projectDescription, false);

				projectDescription.setActiveConfiguration(defaultConfigDescription);
				projectDescription.setCdtProjectCreated();
				CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(project, projectDescription, true, monitor);

				monitor.done();

			}
		};

		// run the new project creation operation
		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return null;
		} catch (InvocationTargetException e) {
			// ie.- one of the steps resulted in a core exception
			Throwable t = e.getTargetException();
			if (t instanceof CoreException) {
				if (((CoreException) t).getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
					MessageDialog.open(MessageDialog.ERROR, getShell(), DataTransferMessages.WizardExternalProjectImportPage_errorMessage,
							NLS.bind(DataTransferMessages.WizardExternalProjectImportPage_caseVariantExistsError, projectName), SWT.SHEET);
				} else {
					ErrorDialog.openError(getShell(), DataTransferMessages.WizardExternalProjectImportPage_errorMessage, null, ((CoreException) t).getStatus());
				}
			}
			return null;
		}

		return project;
	}

	private void copyProjectToWorkspace(IContainer resource, IPath locationPath, IProgressMonitor monitor) throws CoreException {
		File locPath = locationPath.toFile();
		if (locPath.isDirectory()) {
			File[] listFiles = locPath.listFiles();
			for (File child : listFiles) {
				String name = child.getName();
				if (!name.startsWith(".")) {
					if (child.isDirectory()) {
						IFolder folder = resource.getFolder(new Path(name));
						folder.create(true, true, monitor);
						IPath path = new Path(child.getAbsolutePath());
						copyProjectToWorkspace(folder, path, monitor);
					} else if (child.isFile()) {
						IFile file = resource.getFile(new Path(name));
						try {
							file.create(new FileInputStream(child), true, monitor);
						} catch (FileNotFoundException e) {
							throw new CoreException(new Status(IStatus.ERROR, Activator.ID, "Failed to copy file", e));
						}
					}
				}
			}
		}
	}

	/*
	 * see @DialogPage.setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			this.locationPathField.setFocus();
		}
	}

}
