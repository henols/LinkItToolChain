package se.aceone.mediatek.linkit.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.language.settings.providers.ScannerDiscoveryLegacySupport;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.CMacroEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedProject;
import org.eclipse.cdt.managedbuilder.internal.core.ToolChain;
import org.eclipse.cdt.managedbuilder.language.settings.providers.AbstractBuiltinSpecsDetector;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

/**
 * ArduinoHelpers is a static class containing general purpose functions
 * 
 * @author Jan Baeyens
 */
public class LinkItHelpers extends Common {

	public static boolean checkEnvironment() {
		return getEnvironmentPath() != null;
	}

	public static String getEnvironmentPath() {
		String linkitEnv = System.getenv().get("LinkItSDK20");
		if (linkitEnv == null) {
			linkitEnv = System.getenv().get(LINK_IT_SDK20);
		}
		return linkitEnv;
	}

	/**
	 * Removes include folders that are not valid. This method does not save the
	 * configurationDescription description
	 * 
	 * @param configurationDescription
	 *            the configuration that is checked
	 * @return true is a include path has been removed. False if the include
	 *         path remains unchanged.
	 */
	public static boolean removeInvalidIncludeFolders(ICConfigurationDescription configurationDescription) {
		// find all languages
		ICFolderDescription folderDescription = configurationDescription.getRootFolderDescription();
		ICLanguageSetting[] languageSettings = folderDescription.getLanguageSettings();
		boolean hasChange = false;
		// Add include path to all languages
		for(int idx = 0; idx < languageSettings.length; idx++) {
			ICLanguageSetting lang = languageSettings[idx];
			String LangID = lang.getLanguageId();
			if (LangID != null) {
				if (LangID.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
					ICLanguageSettingEntry[] OrgIncludeEntries = lang.getSettingEntries(ICSettingEntry.INCLUDE_PATH);
					ICLanguageSettingEntry[] OrgIncludeEntriesFull = lang.getResolvedSettingEntries(ICSettingEntry.INCLUDE_PATH);
					int copiedEntry = 0;
					for(int curEntry = 0; curEntry < OrgIncludeEntries.length; curEntry++) {
						IPath cusPath = ((CIncludePathEntry)OrgIncludeEntriesFull[curEntry]).getFullPath();
						if ((ResourcesPlugin.getWorkspace().getRoot().exists(cusPath)) || (((CIncludePathEntry)OrgIncludeEntries[curEntry]).isBuiltIn())) {
							OrgIncludeEntries[copiedEntry++] = OrgIncludeEntries[curEntry];
						} else {
							Common.log(new Status(IStatus.WARNING, LinkItConst.CORE_PLUGIN_ID, "Removed invalid include path" + cusPath, null));
						}
					}
					if (copiedEntry != OrgIncludeEntries.length) // do not save
					// if nothing
					// has changed
					{
						ICLanguageSettingEntry[] IncludeEntries = new ICLanguageSettingEntry[copiedEntry];
						System.arraycopy(OrgIncludeEntries, 0, IncludeEntries, 0, copiedEntry);
						lang.setSettingEntries(ICSettingEntry.INCLUDE_PATH, IncludeEntries);
						hasChange = true;

					}
				}
			}
		}
		return hasChange;
	}

	/**
	 * Creates a folder and links the folder to an existing folder Parent
	 * folders of the target folder are created if needed. In case this method
	 * fails an error is logged.
	 * 
	 * @param project
	 *            the project the newly created folder will belong to
	 * @param target
	 *            the folder name relative to the project
	 * @param source
	 *            the fully qualified name of the folder to link to
	 */
	public static void LinkFolderToFolder(IProject project, IPath source, IPath target) {

		// create target parent folder and grandparents
		IPath ParentFolders = new Path(target.toString()).removeLastSegments(1);
		for(int curfolder = ParentFolders.segmentCount() - 1; curfolder >= 0; curfolder--) {
			try {
				createNewFolder(project, ParentFolders.removeLastSegments(curfolder).toString(), null);
			} catch (CoreException e) {// ignore this error as the parent
				// folders may have been created yet
			}
		}

		// create the actual link
		try {
			createNewFolder(project, target.toString(), URIUtil.toURI(source));
		} catch (CoreException e) {
			Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Could not create folder " + target, e));
		}
	}

	/**
	 * addTheNatures replaces all existing natures by the natures needed for a
	 * arduino project
	 * 
	 * @param project
	 *            The project where the natures need to be added to
	 * @throws CoreException
	 */
	public static void addTheNatures(IProject project) throws CoreException {
		IProjectDescription description = project.getDescription();

		String[] newnatures = new String[4];
		newnatures[0] = LinkItConst.Cnatureid;
		newnatures[1] = LinkItConst.CCnatureid;
		newnatures[2] = LinkItConst.Buildnatureid;
		newnatures[3] = LinkItConst.Scannernatureid;
//		newnatures[4] = ArduinoConst.LinkItNatureID;
		description.setNatureIds(newnatures);
		project.setDescription(description, new NullProgressMonitor());
	}

	/**
	 * This method adds the content of a content stream to a file
	 * 
	 * @param container
	 *            used as a reference to the file
	 * @param path
	 *            The path to the file relative from the container
	 * @param contentStream
	 *            The stream to put in the file
	 * @param monitor
	 *            A monitor to show progress
	 * @throws CoreException
	 */
	public static void addFileToProject(IContainer container, Path path, InputStream contentStream, IProgressMonitor monitor) throws CoreException {
		final IFile file = container.getFile(path);
		if (file.exists()) {
			file.setContents(contentStream, true, true, monitor);
		} else {
			file.create(contentStream, true, monitor);
		}

	}

	public static void setProjectPathVariables(IProject project, IPath platformPath) {
		IPath PinPath = platformPath.append(LinkItConst.VARIANTS_FOLDER);
		IPath arduinoHardwareLibraryPath = platformPath.append(LinkItConst.LIBRARY_PATH_SUFFIX);
		IPathVariableManager pathMan = project.getPathVariableManager();
		try {
			pathMan.setURIValue(LinkItConst.WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB, URIUtil.toURI(arduinoHardwareLibraryPath));
			pathMan.setURIValue(LinkItConst.PATH_VARIABLE_NAME_ARDUINO_PLATFORM, URIUtil.toURI(platformPath));
			pathMan.setURIValue(LinkItConst.PATH_VARIABLE_NAME_ARDUINO_PINS, URIUtil.toURI(PinPath));
		} catch (CoreException e) {
			Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Failed to create the path variable variables. The setup will not work properly", e));
			e.printStackTrace();
		}
	}

	public static MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for(int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName())) return (MessageConsole)existing[i];
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[]{ myConsole });
		return myConsole;
	}

	/**
	 * Creates a new folder resource as a link or local
	 * 
	 * @param Project
	 *            the project the folder is added to
	 * @param newFolderName
	 *            the new folder to create (can contain subfolders)
	 * @param linklocation
	 *            if null a local folder is created using newFolderName if not
	 *            null a link folder is created with the name newFolderName and
	 *            pointing
	 *            to linklocation
	 * @return nothing
	 * @throws CoreException
	 */
	public static void createNewFolder(IProject Project, String newFolderName, URI linklocation) throws CoreException {
		// IPath newFolderPath = Project.getFullPath().append(newFolderName);
		final IFolder newFolderHandle = Project.getFolder(newFolderName);
		if (linklocation != null) {
			newFolderHandle.createLink(linklocation, IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
		} else {
			newFolderHandle.create(0, true, null);
		}

	}

	/**
	 * Set the project to force a rebuild. This method is called after the
	 * arduino settings have been updated. Note the only way I found I could get
	 * this to work is by deleting the build folder Still then the "indexer
	 * needs to recheck his includes from the language provider which still is
	 * not working
	 * 
	 * @param project
	 */
	public static void setDirtyFlag(IProject project, ICConfigurationDescription cfgDescription) {
		IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
		if (buildInfo == null) {
			return; // Project is not a managed build project
		}

		IFolder buildFolder = project.getFolder(cfgDescription.getName());
		if (buildFolder.exists()) {
			try {
				buildFolder.delete(true, null);
			} catch (CoreException e) {
				Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "failed to delete the folder " + cfgDescription.getName(), e));
			}
		}

		List<ILanguageSettingsProvider> providers;
		if (cfgDescription instanceof ILanguageSettingsProvidersKeeper) {
			providers = new ArrayList<ILanguageSettingsProvider>(((ILanguageSettingsProvidersKeeper)cfgDescription).getLanguageSettingProviders());
			for(ILanguageSettingsProvider provider : providers) {
				if ((provider instanceof AbstractBuiltinSpecsDetector)) { // basically
					// check
					// for
					// working
					// copy
					// clear and reset isExecuted flag
					((AbstractBuiltinSpecsDetector)provider).clear();
				}
			}
		}
	}

	private static String makeEnvVar(String string) {
		return "${" + string + "}";
	}

	/**
	 * Give the string entered in the com port try to extract a host. If no host
	 * is found return null yun at xxx.yyy.zzz (arduino yun) returns
	 * yun.local
	 * 
	 * @param mComPort
	 * @return
	 */
	public static String getHostFromComPort(String mComPort) {
		String host = mComPort.split(" ")[0];
		if (host.equals(mComPort)) return null;
		return host;
	}

	/**
	 * creates links to the root files and folders of the source location
	 * 
	 * @param source
	 *            the location where the files are that need to be linked to
	 * @param target
	 *            the location where the links are to be created
	 */
	public static void linkDirectory(IProject project, IPath source, IPath target) {

		File[] a = source.toFile().listFiles();
		if (a == null) {
			Common.log(new Status(IStatus.INFO, LinkItConst.CORE_PLUGIN_ID, "The folder you want to link to '" + source + "' does not contain any files.", null));
			return;
		}
		for(File f : a) {
			if (f.isDirectory()) {
				LinkFolderToFolder(project, source.append(f.getName()), target.append(f.getName()));
			} else {
				final IFile newFileHandle = project.getFile(target.append(f.getName()));
				try {
					newFileHandle.createLink(source.append(f.getName()), IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public static void setCProjectDescription(IProject project, String toolChainId, String name, boolean isManagedBuild, IProgressMonitor monitor) throws CoreException {

		ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
		ICProjectDescription des = mngr.createProjectDescription(project, false, false);
		IManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
		ManagedProject mProj = new ManagedProject(des);
		info.setManagedProject(mProj);
		monitor.worked(20);

		// Iterate across the configurations
		IToolChain toolChain = ManagedBuildManager.getExtensionToolChain(toolChainId);
		String childId = ManagedBuildManager.calculateChildId(toolChainId, null);
		IConfiguration cfg = new Configuration(mProj, (ToolChain)toolChain, childId, name);
		IBuilder bld = cfg.getEditableBuilder();
		if (bld != null) {
			bld.setManagedBuildOn(isManagedBuild);
			cfg.setArtifactName("${ProjName}");
		} else {
			System.out.println("Messages.StdProjectTypeHandler_3");
		}
		CConfigurationData data = cfg.getConfigurationData();
		ICConfigurationDescription cfgDes = des.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);

		setDefaultLanguageSettingsProviders(project, toolChainId, cfg, cfgDes);

		monitor.worked(50);
		mngr.setProjectDescription(project, des);

	}

	private static void setDefaultLanguageSettingsProviders(IProject project, String toolChainId, IConfiguration cfg, ICConfigurationDescription cfgDescription) {
		// propagate the preference to project properties
		boolean isPreferenceEnabled = ScannerDiscoveryLegacySupport.isLanguageSettingsProvidersFunctionalityEnabled(null);
		ScannerDiscoveryLegacySupport.setLanguageSettingsProvidersFunctionalityEnabled(project, isPreferenceEnabled);

		if (cfgDescription instanceof ILanguageSettingsProvidersKeeper) {
			ILanguageSettingsProvidersKeeper lspk = (ILanguageSettingsProvidersKeeper)cfgDescription;

			lspk.setDefaultLanguageSettingsProvidersIds(new String[]{ toolChainId });

			List<ILanguageSettingsProvider> providers = getDefaultLanguageSettingsProviders(cfg, cfgDescription);
			lspk.setLanguageSettingProviders(providers);
		}
	}

	private static List<ILanguageSettingsProvider> getDefaultLanguageSettingsProviders(IConfiguration cfg, ICConfigurationDescription cfgDescription) {
		List<ILanguageSettingsProvider> providers = new ArrayList<ILanguageSettingsProvider>();
		String[] ids = cfg != null ? cfg.getDefaultLanguageSettingsProviderIds() : null;

		if (ids == null) {
			// Try with legacy providers
			ids = ScannerDiscoveryLegacySupport.getDefaultProviderIdsLegacy(cfgDescription);
		}

		if (ids != null) {
			for(String id : ids) {
				ILanguageSettingsProvider provider = null;
				if (!LanguageSettingsManager.isPreferShared(id)) {
					provider = LanguageSettingsManager.getExtensionProviderCopy(id, false);
				}
				if (provider == null) {
					provider = LanguageSettingsManager.getWorkspaceProvider(id);
				}
				providers.add(provider);
			}
		}

		return providers;
	}

	public static void addMacro(ICProjectDescription projectDescription, String macro, String value) {
		ICConfigurationDescription configurationDescription = projectDescription.getDefaultSettingConfiguration();
		addMacro(configurationDescription, macro, value);
	}

	public static void addMacro(ICConfigurationDescription configurationDescription, String macro, String value) {
		// find all languages
		for(ICFolderDescription folderDescription : configurationDescription.getFolderDescriptions()) {
			ICLanguageSetting[] settings = folderDescription.getLanguageSettings();

			// Add include path to all languages
			for(ICLanguageSetting setting : settings) {
				String langId = setting.getLanguageId();
//			if (langId != null && langId.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
				List<ICLanguageSettingEntry> macros = new ArrayList<ICLanguageSettingEntry>();
				macros.addAll(setting.getSettingEntriesList(ICSettingEntry.MACRO));
				macros.add(new CMacroEntry(macro, value,  ICSettingEntry.BUILTIN));
				setting.setSettingEntries(ICSettingEntry.MACRO, macros);
//			}
			}
		}
	}

	public static void setEnvironmentVariables(ICResourceDescription resourceDescription) throws FileNotFoundException, IOException {
		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		ICConfigurationDescription configuration = resourceDescription.getConfiguration();

		contribEnv.addVariable(new EnvironmentVariable(DEV_BOARD, HDK_LINKIT_ONE_V1), configuration);

		Path linkitEnv = new Path(getEnvironmentPath());
		System.out.println(LINK_IT_SDK20 + "=" + linkitEnv);
		contribEnv.addVariable(new EnvironmentVariable(LINK_IT_SDK20, linkitEnv.toString()), configuration);

		File sysini = new File(linkitEnv.toOSString(), "/tools/sys.ini");
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
					contribEnv.addVariable(new EnvironmentVariable(key, value), configuration);
				}
			}
		}
		numberReader.close();
	}

	public static void buildPathVariables(IProject project, ICResourceDescription resourceDescription) throws CoreException {
		IPathVariableManager pathMan = project.getPathVariableManager();
		Path libLink10 = new Path(makeEnvVar(LINK_IT_SDK20) + "/lib/LINKIT10");
		pathMan.setURIValue(LINKIT10, URIUtil.toURI(libLink10));

		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		ICConfigurationDescription configuration = resourceDescription.getConfiguration();

		contribEnv.addVariable(new EnvironmentVariable(LINKIT10, libLink10.toPortableString()), configuration);

	}

	/**
	 * This method adds the provided path to the include path of all
	 * configurations and languages.
	 * 
	 * @param project
	 *            The project to add it to
	 * @param includePath
	 *            The path to add to the include folders
	 * @see addLibraryDependency
	 *      {@link #addLibraryDependency(IProject, IProject)}
	 */
	public static void addIncludeFolder(ICProjectDescription projectDescription, IPath includePath) {
		ICConfigurationDescription configurationDescription = projectDescription.getDefaultSettingConfiguration();
		addIncludeFolder(configurationDescription, includePath);

	}

	/**
	 * This method is the internal working class that adds the provided
	 * includepath to all configurations and languages.
	 * 
	 * @param configurationDescription
	 *            The configuration description of the project to add it to
	 * @param includePath
	 *            The path to add to the include folders
	 * @see addLibraryDependency
	 *      {@link #addLibraryDependency(IProject, IProject)}
	 */
	public static void addIncludeFolder(ICConfigurationDescription configurationDescription, IPath includePath) {
		// find all languages
		ICFolderDescription folderDescription = configurationDescription.getRootFolderDescription();
		ICLanguageSetting[] settings = folderDescription.getLanguageSettings();

		// Add include path to all languages
		for(ICLanguageSetting setting : settings) {
			String langId = setting.getLanguageId();
			if (langId != null && langId.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
				List<ICLanguageSettingEntry> includes = new ArrayList<ICLanguageSettingEntry>();
				includes.addAll(setting.getSettingEntriesList(ICSettingEntry.INCLUDE_PATH));
				includes.add(new CIncludePathEntry(includePath, ICSettingEntry.LOCAL));
				setting.setSettingEntries(ICSettingEntry.INCLUDE_PATH, includes);
			}
		}
	}

	public static void setIncludePaths(ICProjectDescription projectDescriptor, ICResourceDescription resourceDescription) {
		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		ICConfigurationDescription configuration = resourceDescription.getConfiguration();

		ICConfigurationDescription configurationDescription = projectDescriptor.getDefaultSettingConfiguration();

		IPath env = new Path(getBuildEnvironmentVariable(configurationDescription, LINK_IT_SDK20, null));
		IPath envInclude = env.append("include");
		addIncludeFolder(projectDescriptor, envInclude);
		IPath gccLocation = env.append(getBuildEnvironmentVariable(configurationDescription, "GCCLOCATION", null));
		IPath armIncl = gccLocation.append("arm-none-eabi/include");
		addIncludeFolder(projectDescriptor, armIncl);
		armIncl = armIncl.append("c++/4.9.3");
		addIncludeFolder(projectDescriptor, armIncl);
		IPath armThumb = armIncl.append("arm-none-eabi/thumb");
		addIncludeFolder(projectDescriptor, armThumb);

		contribEnv.addVariable(new EnvironmentVariable(ARM_NONE_EABI_THUMB, armThumb.toPortableString()), configuration);

		addIncludeFolder(projectDescriptor, armIncl.append("backward"));
		IPath libGcc = gccLocation.append("lib/gcc/arm-none-eabi/4.9.3");
		addIncludeFolder(projectDescriptor, libGcc.append("include"));
		addIncludeFolder(projectDescriptor, libGcc.append("include-fixed"));
	}

}
