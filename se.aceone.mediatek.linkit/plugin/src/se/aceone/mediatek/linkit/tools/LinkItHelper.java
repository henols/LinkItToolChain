package se.aceone.mediatek.linkit.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

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
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.WriteAccessException;
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

import se.aceone.mediatek.linkit.common.LinkItConst;
import se.aceone.mediatek.linkit.common.LinkItPreferences;
import se.aceone.mediatek.linkit.xml.config.Packageinfo;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.APIAuth;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Namelist;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Output;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Userinfo;

@SuppressWarnings("restriction")
public abstract class LinkItHelper extends Common {

	private IProject project;
	private String gccLocation;

	public LinkItHelper(IProject project) {
		this.project = project;
	}

	public abstract String getEnvironmentPath();

	public boolean checkEnvironment() {
		String envPath = getEnvironmentPath();
		if (envPath == null) {
			return false;
		}

		return checkSysIni(envPath);
	}

	public abstract String getCompilerPath();

	public static boolean checkSysIni(String envPath) {
		IPath linkitEnv = new Path(envPath).append("tools");
		File sysini = new File(linkitEnv.toOSString(), "sys.ini");
		return sysini.isFile();
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
	public boolean removeInvalidIncludeFolders(ICConfigurationDescription configurationDescription) {
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
	public void linkFolderToFolder(IProject project, IPath source, IPath target) {

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

	public void addTheNatures(IProject project) throws CoreException {
		IProjectDescription description = project.getDescription();

		String[] newnatures = new String[4];
		newnatures[0] = LinkItConst.Cnatureid;
		newnatures[1] = LinkItConst.CCnatureid;
		newnatures[2] = LinkItConst.Buildnatureid;
		newnatures[3] = LinkItConst.Scannernatureid;
		// newnatures[4] = LinkItConst.LinkItNatureID;
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
	public void addResourceToProject(IContainer container, IPath path, InputStream contentStream, IProgressMonitor monitor) throws CoreException {
		final IFile file = container.getFile(path);
		if (file.exists()) {
			file.setContents(contentStream, true, true, monitor);
		} else {
			file.create(contentStream, true, monitor);
		}

	}

	public MessageConsole findConsole(String name) {
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
	 * @param project
	 *            the project the folder is added to
	 * @param newFolderName
	 *            the new folder to create (can contain subfolders)
	 * @param linklocation
	 *            if null a local folder is created using newFolderName if not
	 *            null a link folder is created with the name newFolderName and
	 *            pointing to linklocation
	 * @return nothing
	 * @throws CoreException
	 */
	public void createNewFolder(IProject project, String newFolderName, URI linklocation) throws CoreException {
		// IPath newFolderPath = Project.getFullPath().append(newFolderName);
		final IFolder newFolderHandle = project.getFolder(newFolderName);
		if (linklocation != null) {
			newFolderHandle.createLink(linklocation, IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
		} else {
			newFolderHandle.create(0, true, null);
		}

	}

	public void setDirtyFlag(IProject project, ICConfigurationDescription cfgDescription) {
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

	/**
	 * creates links to the root files and folders of the source location
	 * 
	 * @param source
	 *            the location where the files are that need to be linked to
	 * @param target
	 *            the location where the links are to be created
	 */
	public void linkDirectory(IProject project, IPath source, IPath target) {

		File[] a = source.toFile().listFiles();
		if (a == null) {
			Common.log(new Status(IStatus.INFO, LinkItConst.CORE_PLUGIN_ID, "The folder you want to link to '" + source + "' does not contain any files.", null));
			return;
		}
		for(File f : a) {
			if (f.isDirectory()) {
				linkFolderToFolder(project, source.append(f.getName()), target.append(f.getName()));
			} else {
				linkFileToFolder(project, source.append(f.getName()), target);
			}
		}
	}

	protected void linkFileToFolder(IProject project, IPath source, IPath target) {
		final IFile newFileHandle = project.getFile(target.append(source.lastSegment()));
		try {
			newFileHandle.createLink(source, IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ICProjectDescription setCProjectDescription(IProject project, boolean isManagedBuild, IProgressMonitor monitor) throws CoreException {

		ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
		ICProjectDescription des = mngr.createProjectDescription(project, false, false);
		IManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);

		ManagedProject mProj = new ManagedProject(des);
		info.setManagedProject(mProj);
		monitor.worked(20);

		String linkitDefaultToolChainGcc = getToolChainId();
		IToolChain toolChain = ManagedBuildManager.getExtensionToolChain(linkitDefaultToolChainGcc);
		String toolChainChildId = ManagedBuildManager.calculateChildId(linkitDefaultToolChainGcc, null);

		IConfiguration configuration = ManagedBuildManager.getExtensionConfiguration(LINKIT_CONFIGURATION);

		IConfiguration cfg = new Configuration(mProj, (ToolChain)toolChain, toolChainChildId, LINKIT_CONFIGURATION_NAME);
		cfg.setCleanCommand(configuration.getCleanCommand());

		IBuilder bld = cfg.getEditableBuilder();
		if (bld != null) {
			bld.setManagedBuildOn(isManagedBuild);
			cfg.setArtifactName("${ProjName}");
		} else {
			System.out.println("Messages.StdProjectTypeHandler_3");
		}
		CConfigurationData data = cfg.getConfigurationData();
		ICConfigurationDescription cfgDes = des.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);

		setDefaultLanguageSettingsProviders(project, linkitDefaultToolChainGcc, cfg, cfgDes);

		monitor.worked(50);
		mngr.setProjectDescription(project, des);
		return des;
	}

	protected String getToolChainId() {
		return LINKIT_DEFAULT_TOOL_CHAIN_GCC;
	}

	private void setDefaultLanguageSettingsProviders(IProject project, String toolChainId, IConfiguration cfg, ICConfigurationDescription cfgDescription) {
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

	private List<ILanguageSettingsProvider> getDefaultLanguageSettingsProviders(IConfiguration cfg, ICConfigurationDescription cfgDescription) {
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

	public void setMacros(ICProjectDescription projectDescription, String devBoard) {
		addMacro(projectDescription, "__GNUC__", null, ICSettingEntry.BUILTIN);
		addMacro(projectDescription, devBoard, null);
	}

	public void addMacro(ICProjectDescription projectDescription, String macro, String value) {
		addMacro(projectDescription, macro, value, ICSettingEntry.NONE);
	}

	public void addMacro(ICProjectDescription projectDescription, String macro, String value, int flags) {
		ICConfigurationDescription configurationDescription = projectDescription.getDefaultSettingConfiguration();
		addMacro(configurationDescription, macro, value, flags);
	}

	public void addMacro(ICConfigurationDescription configurationDescription, String macro, String value, int flags) {
		// find all languages
		for(ICFolderDescription folderDescription : configurationDescription.getFolderDescriptions()) {
			ICLanguageSetting[] settings = folderDescription.getLanguageSettings();

			// Add include path to all languages
			for(ICLanguageSetting setting : settings) {
				String langId = setting.getLanguageId();
				if (langId != null && langId.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
					List<ICLanguageSettingEntry> macros = new ArrayList<ICLanguageSettingEntry>();
					macros.addAll(setting.getSettingEntriesList(ICSettingEntry.MACRO));
					macros.add(new CMacroEntry(macro, value, flags | ICSettingEntry.READONLY));
					setting.setSettingEntries(ICSettingEntry.MACRO, macros);
				}
			}
		}
	}

	public void setEnvironmentVariables(ICResourceDescription resourceDescription, String devBoard) throws FileNotFoundException, IOException {
		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		ICConfigurationDescription configuration = resourceDescription.getConfiguration();

		contribEnv.addVariable(new EnvironmentVariable(DEV_BOARD, devBoard), configuration);
		contribEnv.addVariable(new EnvironmentVariable(SIZETOOL, ARM_NONE_EABI_SIZE), configuration);

		contribEnv.addVariable(new EnvironmentVariable(COMPILER_TOOL_PATH, new Path(COMPILER_TOOL_PATH_GCC).toPortableString()), configuration);

		Path linkitEnv = new Path(getEnvironmentPath());
		System.out.println(LINK_IT_SDK + "=" + linkitEnv);
		contribEnv.addVariable(new EnvironmentVariable(LINK_IT_SDK, linkitEnv.toPortableString()), configuration);
		IPath toolPath = linkitEnv.append("tools");
		contribEnv.addVariable(new EnvironmentVariable(TOOL_PATH, toolPath.toPortableString()), configuration);
		File sysini = new File(toolPath.toOSString(), "sys.ini");
		LineNumberReader numberReader = new LineNumberReader(new FileReader(sysini));
		String line;
		while ((line = numberReader.readLine()) != null) {
			if (!line.trim().isEmpty() && !line.startsWith("[")) {
				int i = line.indexOf("=");
				if (i > 0) {
					String key = line.substring(0, i).trim().toUpperCase();
					if (!key.startsWith("RVCT") && !key.startsWith("ADS") && !key.startsWith("VTP")) {
						String value = line.substring(i + 1).trim();
						if (value.startsWith("\"") && value.endsWith("\"")) {
							value = value.substring(1, value.length() - 1).trim();
						}
						value = value.replace('\\', '/');
						System.out.println(key + "=" + value);
						if (key.equals(GCCLOCATION)) {
							gccLocation = value;
						}
						contribEnv.addVariable(new EnvironmentVariable(key, value), configuration);
					}
				}
			}
		}
		numberReader.close();
		contribEnv.addVariable(new EnvironmentVariable(COMPILER_PATH, new Path(getCompilerPath()).toPortableString()), configuration);
	}

	public void buildPathVariables(IProject project, ICResourceDescription resourceDescription) throws CoreException {
		IPathVariableManager pathMan = project.getPathVariableManager();
		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

		ICConfigurationDescription configuration = resourceDescription.getConfiguration();
		String lib = getBuildEnvironmentVariable(configuration, LIBRARY, null);
		String toolPath = getBuildEnvironmentVariable(configuration, TOOL_PATH, null);
		IPath libLink10 = new Path(toolPath).append(lib).append(LINKIT10);
		URI uri = URIUtil.toURI(libLink10.toPortableString());
		pathMan.setURIValue(LINKIT10, uri);

		contribEnv.addVariable(new EnvironmentVariable(LINKIT10, libLink10.toPortableString()), configuration);

	}

	public void addSourceFolder(ICConfigurationDescription configurationDescription, IPath includePath) throws WriteAccessException, CoreException {
		// find all languages
		ICConfigurationDescription configuration = configurationDescription.getConfiguration();
		List<ICSourceEntry> srcFolders = new ArrayList<ICSourceEntry>();
		for(ICSourceEntry entry : configuration.getSourceEntries()) {
			srcFolders.add(entry);
		}
		srcFolders.add(new CSourceEntry(includePath, null, ICSettingEntry.RESOLVED));
		configuration.setSourceEntries(srcFolders.toArray(new CSourceEntry[0]));
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
	public void addIncludeFolder(ICProjectDescription projectDescription, IPath includePath) {
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
	public void addIncludeFolder(ICConfigurationDescription configurationDescription, IPath includePath) {
		// find all languages
		ICFolderDescription folderDescription = configurationDescription.getRootFolderDescription();
		ICLanguageSetting[] settings = folderDescription.getLanguageSettings();

		// Add include path to all languages
		for(ICLanguageSetting setting : settings) {
			String langId = setting.getLanguageId();
			if (langId != null && langId.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
				List<ICLanguageSettingEntry> includes = new ArrayList<ICLanguageSettingEntry>();
				includes.addAll(setting.getSettingEntriesList(ICSettingEntry.INCLUDE_PATH));
				includes.add(new CIncludePathEntry(includePath, ICSettingEntry.READONLY));
				setting.setSettingEntries(ICSettingEntry.INCLUDE_PATH, includes);
			}
		}
	}

	public void setIncludePaths(ICProjectDescription projectDescriptor, ICResourceDescription resourceDescription) {
		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		ICConfigurationDescription configuration = resourceDescription.getConfiguration();

		ICConfigurationDescription configurationDescription = projectDescriptor.getDefaultSettingConfiguration();

		addIncludeFolder(projectDescriptor, projectDescriptor.getProject().getFolder("ResID").getProjectRelativePath());

		IPath toolPath = new Path(getBuildEnvironmentVariable(configurationDescription, TOOL_PATH, null));
		IPath envInclude = toolPath.append(getBuildEnvironmentVariable(configurationDescription, INCLUDE, null));
		addIncludeFolder(projectDescriptor, envInclude);

		setCompilerIncludePaths(projectDescriptor, contribEnv, configuration);
	}

	protected void setCompilerIncludePaths(ICProjectDescription projectDescriptor, IContributedEnvironment contribEnv, ICConfigurationDescription configuration) {
		IPath compilerLocation = new Path(getCompilerPath());
		IPath armIncl = compilerLocation.append("arm-none-eabi/include");
		addIncludeFolder(projectDescriptor, armIncl);
		armIncl = armIncl.append("c++/4.9.3");
		addIncludeFolder(projectDescriptor, armIncl);
		IPath armThumb = armIncl.append("arm-none-eabi/thumb");
		addIncludeFolder(projectDescriptor, armThumb);

		contribEnv.addVariable(new EnvironmentVariable(ARM_NONE_EABI_THUMB, armThumb.toPortableString()), configuration);

		addIncludeFolder(projectDescriptor, armIncl.append("backward"));
		IPath libGcc = compilerLocation.append("lib/gcc/arm-none-eabi/4.9.3");
		addIncludeFolder(projectDescriptor, libGcc.append("include"));
		addIncludeFolder(projectDescriptor, libGcc.append("include-fixed"));
	}

	public void setSourcePaths(ICProjectDescription projectDescriptor) throws WriteAccessException, CoreException {
		IProject project = projectDescriptor.getProject();
		ICConfigurationDescription configurationDescription = projectDescriptor.getDefaultSettingConfiguration();
		ICConfigurationDescription configuration = configurationDescription.getConfiguration();

		addSourceFolder(configuration, project.getFolder("src").getFullPath());
		addSourceFolder(configuration, project.getFolder("LinkIt").getFullPath());

	}

	public void copyProjectResources(ICProjectDescription projectDescriptor, IProgressMonitor monitor) throws CoreException, IOException, JAXBException {
		ICConfigurationDescription configurationDescription = projectDescriptor.getDefaultSettingConfiguration();
		IPath toolPath = new Path(getBuildEnvironmentVariable(configurationDescription, TOOL_PATH, null));
		IProject project = projectDescriptor.getProject();

		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("LINKIT20TEMPLATE", project.getName());
		replacements.put("WIZARDTEMPLATE", project.getName().toUpperCase() + "_H");

		IPath wiz = toolPath.append("Wizard").append("LINKIT20WIZARD");
		IPath srcPath = wiz.append("LINKIT20BASIC").append("LINKIT20TEMPLATE.proj");
		IPath outPath = new Path(project.getName() + ".proj");
		addResourceToProject(monitor, project, srcPath, outPath);

		String projectType = "LINKITEMPTY";
		IPath projType = wiz.append("LINKITVXP").append(projectType);

		outPath = new Path("src/" + project.getName() + ".c");
		addResourceToProject(monitor, project, projType.append("LINKIT20TEMPLATE.c"), outPath, replacements);

		outPath = new Path("src/" + project.getName() + ".h");
		addResourceToProject(monitor, project, projType.append("LINKIT20TEMPLATE.h"), outPath, replacements);

		JAXBContext jaxbContext = JAXBContext.newInstance(Packageinfo.class);

		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Packageinfo packageinfo = (Packageinfo)jaxbUnmarshaller.unmarshal(new File(projType.append("config.xml").toOSString()));
		Userinfo userinfo = packageinfo.getUserinfo();
		userinfo.setDeveloper(LinkItPreferences.getDeveloper());
		userinfo.setAppname(LinkItPreferences.getAppName());
		userinfo.setAppversion(LinkItPreferences.getAppVersion());

		BigInteger appid = BigInteger.valueOf(LinkItPreferences.getAppId());
		userinfo.setAppid(appid);
		APIAuth apiAuth = packageinfo.getAPIAuth();
		apiAuth.setDefaultliblist(LinkItPreferences.getDefaultLibraryList());

		Namelist namelist = packageinfo.getNamelist();
		namelist.setEnglish(project.getName());
		namelist.setChinese(project.getName());
		namelist.setCht(project.getName());

		Output output = packageinfo.getOutput();
		output.setType(BigInteger.valueOf(0));
		output.setDevice(BigInteger.valueOf(0));

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		javax.xml.bind.Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8"); // NOI18N
		marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(packageinfo, os);

		outPath = new Path("config.xml");
		addResourceToProject(project, outPath, new ByteArrayInputStream(os.toByteArray()), monitor);

		IPath res = projType.append("res");

		srcPath = res.append("ref_list_LINKIT20TEMPLATE.txt");
		outPath = new Path("res/ref_list_" + project.getName() + ".txt");
		addResourceToProject(monitor, project, srcPath, outPath);

		srcPath = res.append("LINKIT20TEMPLATE.res.xml");
		outPath = new Path("res/" + project.getName() + ".res.xml");
		addResourceToProject(monitor, project, srcPath, outPath, replacements);

		IPath resId = projType.append("ResID");

		srcPath = resId.append("ResID.h");
		outPath = new Path("ResID/ResID.h");
		addResourceToProject(monitor, project, srcPath, outPath);

	}

	protected void addResourceToProject(IProgressMonitor monitor, IProject project, IPath srcPath, IPath outPath, Map<String, String> replace) throws CoreException, IOException {
		InputStream contentStream;
		if (replace != null) {
			String lineSep = System.getProperty("line.separator");
			InputStreamReader is = new InputStreamReader(new FileInputStream(new File(srcPath.toOSString())));
			StringBuilder sb = new StringBuilder();
			BufferedReader br = new BufferedReader(is);
			String read;
			while ((read = br.readLine()) != null) {
				sb.append(read);
				sb.append(lineSep);
			}
			br.close();
			String res = sb.toString();
			for(String key : replace.keySet()) {
				res = res.replaceAll(key, replace.get(key));
			}
			contentStream = new ByteArrayInputStream(res.getBytes());
		} else {
			contentStream = new FileInputStream(new File(srcPath.toOSString()));

		}
		addResourceToProject(project, outPath, contentStream, monitor);
	}

	protected void addResourceToProject(IProgressMonitor monitor, IProject project, IPath srcPath, IPath outPath) throws CoreException, IOException {
		addResourceToProject(monitor, project, srcPath, outPath, null);
	}

	protected IProject getProject() {
		return project;
	}

	public void setGccLocation(String gccLocation) {
		this.gccLocation = gccLocation;
	}

	public String getGccLocation() {
		return gccLocation;
	}

	protected String getIncludeVar() {
		return "GCCINCLUDE";
	}


}
