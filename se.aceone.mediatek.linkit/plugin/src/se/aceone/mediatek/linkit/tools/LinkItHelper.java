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
import java.io.Serializable;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
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
import se.aceone.mediatek.linkit.xml.config.ObjectFactory;
import se.aceone.mediatek.linkit.xml.config.Packageinfo;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.APIAuth;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Namelist;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Output;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Userinfo;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Vxp;

@SuppressWarnings("restriction")
public abstract class LinkItHelper extends Common {

	private IProject project;
	protected Compiler compiler;

	// private String gccLocation;

	public LinkItHelper(IProject project, Compiler compiler) {
		this.project = project;
		this.compiler = compiler;
		compiler.setLinkItHelper(this);
	}

	public abstract String getEnvironmentPath();

	public boolean checkEnvironment() {
		String envPath = getEnvironmentPath();
		if (envPath == null) {
			return false;
		}

		return checkSysIni(envPath);
	}

	public final String getCompilerPath() {
		return compiler.getCompilerPath();
	}

	public static boolean checkSysIni(String envPath) {
		IPath linkitEnv = new Path(envPath).append("tools");
		File sysini = new File(linkitEnv.toOSString(), "sys.ini");
		return sysini.isFile();
	}

	/**
	 * Removes include folders that are not valid. This method does not save the configurationDescription description
	 * 
	 * @param configurationDescription
	 *            the configuration that is checked
	 * @return true is a include path has been removed. False if the include path remains unchanged.
	 */
	public boolean removeInvalidIncludeFolders(ICConfigurationDescription configurationDescription) {
		// find all languages
		ICFolderDescription folderDescription = configurationDescription.getRootFolderDescription();
		ICLanguageSetting[] languageSettings = folderDescription.getLanguageSettings();
		boolean hasChange = false;
		// Add include path to all languages
		for (int idx = 0; idx < languageSettings.length; idx++) {
			ICLanguageSetting lang = languageSettings[idx];
			String LangID = lang.getLanguageId();
			if (LangID != null) {
				if (LangID.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
					ICLanguageSettingEntry[] OrgIncludeEntries = lang.getSettingEntries(ICSettingEntry.INCLUDE_PATH);
					ICLanguageSettingEntry[] OrgIncludeEntriesFull = lang.getResolvedSettingEntries(ICSettingEntry.INCLUDE_PATH);
					int copiedEntry = 0;
					for (int curEntry = 0; curEntry < OrgIncludeEntries.length; curEntry++) {
						IPath cusPath = ((CIncludePathEntry) OrgIncludeEntriesFull[curEntry]).getFullPath();
						if ((ResourcesPlugin.getWorkspace().getRoot().exists(cusPath)) || (((CIncludePathEntry) OrgIncludeEntries[curEntry]).isBuiltIn())) {
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
	 * Creates a folder and links the folder to an existing folder Parent folders of the target folder are created if
	 * needed. In case this method fails an error is logged.
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
		for (int curfolder = ParentFolders.segmentCount() - 1; curfolder >= 0; curfolder--) {
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
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
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
	 *            if null a local folder is created using newFolderName if not null a link folder is created with the
	 *            name newFolderName and pointing to linklocation
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
			providers = new ArrayList<ILanguageSettingsProvider>(((ILanguageSettingsProvidersKeeper) cfgDescription).getLanguageSettingProviders());
			for (ILanguageSettingsProvider provider : providers) {
				if ((provider instanceof AbstractBuiltinSpecsDetector)) { // basically
					// check
					// for
					// working
					// copy
					// clear and reset isExecuted flag
					((AbstractBuiltinSpecsDetector) provider).clear();
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
		for (File f : a) {
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

		IConfiguration cfg = new Configuration(mProj, (ToolChain) toolChain, toolChainChildId, LINKIT_CONFIGURATION_NAME);
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

	protected final String getToolChainId() {
		return compiler.getToolChainId();
	}

	private void setDefaultLanguageSettingsProviders(IProject project, String toolChainId, IConfiguration cfg, ICConfigurationDescription cfgDescription) {
		// propagate the preference to project properties
		boolean isPreferenceEnabled = ScannerDiscoveryLegacySupport.isLanguageSettingsProvidersFunctionalityEnabled(null);
		ScannerDiscoveryLegacySupport.setLanguageSettingsProvidersFunctionalityEnabled(project, isPreferenceEnabled);

		if (cfgDescription instanceof ILanguageSettingsProvidersKeeper) {
			ILanguageSettingsProvidersKeeper lspk = (ILanguageSettingsProvidersKeeper) cfgDescription;

			lspk.setDefaultLanguageSettingsProvidersIds(new String[] { toolChainId });

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
			for (String id : ids) {
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

	public final void setMacros(ICProjectDescription projectDescription, String devBoard) {
		compiler.setMacros(projectDescription);
		setHelperMacros(projectDescription, devBoard);
	}

	void setHelperMacros(ICProjectDescription projectDescription, String devBoard) {
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
		for (ICFolderDescription folderDescription : configurationDescription.getFolderDescriptions()) {
			ICLanguageSetting[] settings = folderDescription.getLanguageSettings();

			// Add include path to all languages
			for (ICLanguageSetting setting : settings) {
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

		contribEnv.addVariable(new EnvironmentVariable(COMPILER_TOOL_PATH, compiler.getToolPath()), configuration);

		Path linkitEnv = new Path(getEnvironmentPath());
		System.out.println(LINK_IT_SDK + "=" + linkitEnv);
		contribEnv.addVariable(new EnvironmentVariable(LINK_IT_SDK, linkitEnv.toPortableString()), configuration);
		IPath toolPath = linkitEnv.append("tools");
		contribEnv.addVariable(new EnvironmentVariable(TOOL_PATH, toolPath.toPortableString()), configuration);
		File sysini = new File(toolPath.toOSString(), "sys.ini");
		LineNumberReader numberReader = new LineNumberReader(new FileReader(sysini));
		String line;
		Map<String, String> envVars = new HashMap<String, String>();
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
					envVars.put(key, value);
				}
			}
		}

		String name = compiler.getName();
		List<String> names = new ArrayList<String>();
		names.add("RVCT");
		names.add("ADS");
		names.add("VTP");
		names.add("GCC");
		names.remove(name);

		for (String key : envVars.keySet()) {
			boolean ignore = false;
			for (String string : names) {
				if(key.startsWith(string)){
					ignore = true;
					break;
				}
			}
			if (!ignore) {
				String value = envVars.get(key);
				System.out.println(key + "=" + value);
				contribEnv.addVariable(new EnvironmentVariable(key, value), configuration);
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
		addSourceFolder(configurationDescription, includePath, null);
	}

	public void addSourceFolder(ICConfigurationDescription configuration, IPath includePath, IPath[] exclutionPattern) throws WriteAccessException,
			CoreException {
		// find all languages
		List<ICSourceEntry> srcFolders = new ArrayList<ICSourceEntry>();
		for (ICSourceEntry entry : configuration.getSourceEntries()) {
			if (!entry.getFullPath().equals(includePath)) {
				srcFolders.add(entry);
			}
		}
		srcFolders.add(new CSourceEntry(includePath, exclutionPattern, ICSettingEntry.RESOLVED));
		configuration.setSourceEntries(srcFolders.toArray(new CSourceEntry[0]));
	}

	/**
	 * This method adds the provided path to the include path of all configurations and languages.
	 * 
	 * @param project
	 *            The project to add it to
	 * @param includePath
	 *            The path to add to the include folders
	 * @see addLibraryDependency {@link #addLibraryDependency(IProject, IProject)}
	 */
	public void addIncludeFolder(ICProjectDescription projectDescription, IPath includePath) {
		ICConfigurationDescription configurationDescription = projectDescription.getDefaultSettingConfiguration();
		addIncludeFolder(configurationDescription, includePath);
	}

	/**
	 * This method is the internal working class that adds the provided includepath to all configurations and languages.
	 * 
	 * @param configurationDescription
	 *            The configuration description of the project to add it to
	 * @param includePath
	 *            The path to add to the include folders
	 * @see addLibraryDependency {@link #addLibraryDependency(IProject, IProject)}
	 */
	public void addIncludeFolder(ICConfigurationDescription configurationDescription, IPath includePath) {
		// find all languages
		ICFolderDescription folderDescription = configurationDescription.getRootFolderDescription();
		ICLanguageSetting[] settings = folderDescription.getLanguageSettings();

		// Add include path to all languages
		for (ICLanguageSetting setting : settings) {
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

		compiler.setIncludePaths(projectDescriptor, contribEnv, configuration);
	}

	public void setSourcePaths(ICProjectDescription projectDescriptor) throws WriteAccessException, CoreException {
		setSourcePaths(projectDescriptor, true);
	}

	public void setSourcePaths(ICProjectDescription projectDescriptor, boolean useSrcFoleder) throws WriteAccessException, CoreException {
		IProject project = projectDescriptor.getProject();
		ICConfigurationDescription configurationDescription = projectDescriptor.getDefaultSettingConfiguration();
		ICConfigurationDescription configuration = configurationDescription.getConfiguration();

		if (useSrcFoleder) {
			addSourceFolder(configuration, project.getFolder("src").getFullPath());
		} else {
			IPath[] ex = { new Path("arm"), new Path("config") };
			addSourceFolder(configuration, project.getFullPath(), ex);
		}
		addSourceFolder(configuration, project.getFolder("LinkIt").getFullPath());
	}

	abstract public void copyProjectResources(ICProjectDescription projectDescriptor, IProgressMonitor monitor) throws CoreException, IOException,
			JAXBException;

	protected void addResourceToProject(IProgressMonitor monitor, IProject project, IPath srcPath, IPath outPath, Map<String, String> replace)
			throws CoreException, IOException {
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
			for (String key : replace.keySet()) {
				res = res.replaceAll(key, replace.get(key));
			}
			contentStream = new ByteArrayInputStream(res.getBytes());
		} else {
			contentStream = new FileInputStream(new File(srcPath.toOSString()));

		}
		addResourceToProject(project, outPath, contentStream, monitor);
	}

	protected void createConfig(IProject project, IPath projType, IProgressMonitor monitor) throws JAXBException, PropertyException, CoreException {
		IPath outPath;
		JAXBContext jaxbContext = JAXBContext.newInstance(Packageinfo.class);

		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Packageinfo packageinfo = (Packageinfo) jaxbUnmarshaller.unmarshal(new File(projType.append("config.xml").toOSString()));
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
		output.setType(getOutputType());
		output.setDevice(BigInteger.valueOf(0));

		ObjectFactory config = new ObjectFactory();

		Vxp vxp = packageinfo.getVxp();
		vxp.setVenus(BigInteger.valueOf(1));
		vxp.setSdkversion(BigInteger.valueOf(10));
		vxp.setIotWearable(BigInteger.valueOf(2));

		List<JAXBElement<? extends Serializable>> targetConfig = packageinfo.getTargetconfig().getMemOrSupportbgOrUserfont();
		targetConfig.add(config.createPackageinfoTargetconfigAutoadaptable("checked" ));
		
		createConfigExtraArgs(packageinfo, config);

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		javax.xml.bind.Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8"); // NOI18N
		marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(packageinfo, os);

		outPath = new Path("config.xml");
		addResourceToProject(project, outPath, new ByteArrayInputStream(os.toByteArray()), monitor);
	}

	protected void createConfigExtraArgs(Packageinfo packageinfo, ObjectFactory config) {
		boolean autostart = false;
		// TODO: Ask the question to set auto start
		List<JAXBElement<? extends Serializable>> targetConfig = packageinfo.getTargetconfig().getMemOrSupportbgOrUserfont();
		targetConfig.add(config.createPackageinfoTargetconfigAutostart(autostart ? "checked" : "unchecked"));
	}

	protected void addResourceToProject(IProgressMonitor monitor, IProject project, IPath srcPath, IPath outPath) throws CoreException, IOException {
		addResourceToProject(monitor, project, srcPath, outPath, null);
	}

	protected IProject getProject() {
		return project;
	}

	// public void setGccLocation(String gccLocation) {
	// this.gccLocation = gccLocation;
	// }
	//
	// public String getGccLocation() {
	// return gccLocation;
	// }

	protected BigInteger getOutputType() {
		return BigInteger.valueOf(0);
	}

}
