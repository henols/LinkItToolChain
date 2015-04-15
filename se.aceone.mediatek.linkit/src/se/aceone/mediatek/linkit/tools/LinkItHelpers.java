package se.aceone.mediatek.linkit.tools;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.language.settings.providers.ScannerDiscoveryLegacySupport;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
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
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
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

	private static final String BUILD_PATH_SYSCALLS_SAM3 = "\"{build.path}/syscalls_sam3.c.o\"";
	private static final String BUILD_PATH_ARDUINO_SYSCALLS_SAM3 = "\"{build.path}/arduino/syscalls_sam3.c.o\"";
	private static final String BUILD_PATH_SYSCALLS_MTK = "\"{build.path}/syscalls_mtk.c.o\"";
	private static final String BUILD_PATH_ARDUINO_SYSCALLS_MTK = "\"{build.path}/arduino/syscalls_mtk.c.o\"";

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
	private static void addIncludeFolder(ICConfigurationDescription configurationDescription, IPath includePath) {
		// find all languages
		ICFolderDescription folderDescription = configurationDescription.getRootFolderDescription();
		ICLanguageSetting[] settings = folderDescription.getLanguageSettings();

		// Add include path to all languages
		for(ICLanguageSetting setting: settings) {
			String langId = setting.getLanguageId();
			if (langId != null && langId.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
				List<ICLanguageSettingEntry> includes = new ArrayList<ICLanguageSettingEntry>();
				includes.addAll(setting.getSettingEntriesList(ICSettingEntry.INCLUDE_PATH));
				includes.add(new CIncludePathEntry(includePath, ICSettingEntry.LOCAL));
				setting.setSettingEntries(ICSettingEntry.INCLUDE_PATH, includes);

//					ICLanguageSettingEntry[] orgIncludeEntries = setting.getSettingEntries(ICSettingEntry.INCLUDE_PATH);
//					ICLanguageSettingEntry[] includeEntries = new ICLanguageSettingEntry[orgIncludeEntries.length + 1];
//					System.arraycopy(orgIncludeEntries, 0, includeEntries, 0, orgIncludeEntries.length);
//					includeEntries[orgIncludeEntries.length] = CDataUtil.getPooledEntry(new CIncludePathEntry(includePath, ICSettingEntry.VALUE_WORKSPACE_PATH));
////					includeEntries[orgIncludeEntries.length] = new CIncludePathEntry(includePath, ICSettingEntry.VALUE_WORKSPACE_PATH); // (location.toString());
//					setting.setSettingEntries(ICSettingEntry.INCLUDE_PATH, includeEntries);
			}
		}
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
	public static void addIncludeFolder(IProject project, IPath includePath) {
		// find all languages
//		ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
		ICProjectDescription projectDescription = CoreModel.getDefault().getProjectDescription(project, true);
		ICConfigurationDescription configurationDescription = projectDescription.getDefaultSettingConfiguration();
		addIncludeFolder(configurationDescription, includePath);

		projectDescription.setActiveConfiguration(configurationDescription);
		projectDescription.setCdtProjectCreated();
		try {
			CoreModel.getDefault().setProjectDescription(project, projectDescription, true, null);
		} catch (CoreException e) {
			Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Could not add folder " + includePath.toOSString() + " to includepoth in project" + project.getName(), e));
		}

	}

	public static void addCodeFolder(IProject project, String PathVarName, String SubFolder, String LinkName, ICConfigurationDescription configurationDescriptions[]) throws CoreException {
		for(ICConfigurationDescription curConfig : configurationDescriptions) {
			LinkItHelpers.addCodeFolder(project, PathVarName, SubFolder, LinkName, curConfig);
		}

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
	 * This method creates a link folder in the project and add the folder as a
	 * source path to the project it also adds the path to the include folder
	 * if the includepath parameter points to a path that contains a subfolder
	 * named "utility" this subfolder will be added to the include path as
	 * well <br/>
	 * Forget about this. Arduino made this all so complicated I don't know
	 * anymore what needs to be added to what<br/>
	 * <br/>
	 * note Arduino has these subfolders in the libraries that need to be
	 * include.<br/>
	 * <br/>
	 * note that in the current eclipse version, there is no need to add the
	 * subfolder as a code folder. This may change in the future as it looks
	 * like a bug to me.<br/>
	 * 
	 * @param project
	 * @param Path
	 * @throws CoreException
	 * @see addLibraryDependency
	 *      {@link #addLibraryDependency(IProject, IProject)}
	 */
	public static void addCodeFolder(IProject project, String PathVarName, String SubFolder, String LinkName, ICConfigurationDescription configurationDescription) throws CoreException {
		IFolder link = project.getFolder(LinkName);

		LinkFolderToFolder(project, new Path(PathVarName).append(SubFolder), new Path(LinkName));

		// Now the folder has been created we need to make sure the special folders are added to the path
		addIncludeFolder(configurationDescription, link.getFullPath());

		IPathVariableManager pathMan = project.getPathVariableManager();

		String possibleIncludeFolder = "utility";
		File file = new File(new Path(pathMan.resolveURI(pathMan.getURIValue(PathVarName)).getPath()).append(SubFolder).append(possibleIncludeFolder).toString());
		if (file.exists()) {
			addIncludeFolder(configurationDescription, link.getFullPath().append(possibleIncludeFolder));
		}

		possibleIncludeFolder = "src";
		file = new File(new Path(pathMan.resolveURI(pathMan.getURIValue(PathVarName)).getPath()).append(SubFolder).append(possibleIncludeFolder).toString());
		if (file.exists()) {
			addIncludeFolder(configurationDescription, link.getFullPath().append(possibleIncludeFolder));
		}

		possibleIncludeFolder = "arch";
		file = new File(new Path(pathMan.resolveURI(pathMan.getURIValue(PathVarName)).getPath()).append(SubFolder).append(possibleIncludeFolder).toString());
		if (file.exists()) {
			addIncludeFolder(configurationDescription, link.getFullPath().append(possibleIncludeFolder).append(makeEnvironmentVar(ENV_KEY_ARCHITECTURE)));
		}
	}

	/**
	 * This method creates a link folder in the project and adds the folder as a
	 * source path to the project it also adds the path to the include
	 * folder if the includepath parameter points to a path that contains a
	 * subfolder named "utility" this subfolder will be added to the include
	 * path
	 * as well <br/>
	 * <br/>
	 * note Arduino has these subfolders in the libraries that need to be
	 * include.<br/>
	 * <br/>
	 * note that in the current eclipse version, there is no need to add the
	 * subfolder as a code folder. This may change in the future as it looks
	 * like a bug to me.<br/>
	 * 
	 * @param project
	 * @param Path
	 * @throws CoreException
	 * @see addLibraryDependency
	 *      {@link #addLibraryDependency(IProject, IProject)}
	 */
	public static void addCodeFolder(IProject project, IPath Path, ICConfigurationDescription configurationDescription) throws CoreException {

		// create a link to the path
		String NiceName = Path.lastSegment();
		String PathName = project.getName() + NiceName;
		URI ShortPath = URIUtil.toURI(Path.removeTrailingSeparator().removeLastSegments(1));

		IWorkspace workspace = project.getWorkspace();
		IPathVariableManager pathMan = workspace.getPathVariableManager();

		pathMan.setURIValue(PathName, ShortPath);

		addCodeFolder(project, PathName, NiceName, NiceName, configurationDescription);

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

	/**
	 * This method sets the eclipse path variables to contain the 3 important
	 * Arduino hardware folders (code wise that is)
	 * Core path (used when referencing Arduino Code) The Arduino Pin Path (used
	 * from Arduino 1.0 to reference the arduino pin variants) The libraries
	 * path (used to find libraries)
	 * Paths are given relative to the arduino folder to avoid conflict when a
	 * version control system is being used (these values are in the .project
	 * file) As the arduino folder location is in the workspace all values in
	 * the .project file become relative avoiding conflict.
	 * 
	 * @param project
	 */
	public static void setProjectPathVariables(IProject project, IPath platformPath) {
		IPath PinPath = platformPath.append(LinkItConst.VARIANTS_FOLDER);
		IPath arduinoHardwareLibraryPath = platformPath.append(LinkItConst.LIBRARY_PATH_SUFFIX);
		IPathVariableManager pathMan = project.getPathVariableManager();
		try {
			// TODO the code below was changed for issue #34 (better multiple user support) but fails on Mac
			// So i split it to use the old code on mac.
			// but they should be merged
			// then it turned out nothing worked anymore. So I reverted to the old code
			// Path arduinoPath = new Path(pathMan.getURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO).getRawPath());
			// if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			pathMan.setURIValue(LinkItConst.WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB, URIUtil.toURI(arduinoHardwareLibraryPath));
			pathMan.setURIValue(LinkItConst.PATH_VARIABLE_NAME_ARDUINO_PLATFORM, URIUtil.toURI(platformPath));
			pathMan.setURIValue(LinkItConst.PATH_VARIABLE_NAME_ARDUINO_PINS, URIUtil.toURI(PinPath));
			// } else {
			//
			// String prefix = "${" + ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO + "}/";
			// String test = prefix + arduinoHardwareLibraryPath.makeRelativeTo(arduinoPath).toString();
			// // URI uriTest = URIUtil.toURI(test, false);
			//
			// pathMan.setURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB, pathMan.getURIValue(test));
			// test = prefix + platformPath.makeRelativeTo(arduinoPath).toString();
			// // uriTest = URIUtil.toURI(test, false);
			// pathMan.setURIValue(ArduinoConst.PATH_VARIABLE_NAME_ARDUINO_PLATFORM, pathMan.getURIValue(test));
			// test = prefix + PinPath.makeRelativeTo(arduinoPath).toString();
			// URI uriTest = URIUtil.toURI(pathMan.convertFromUserEditableFormat(test, true));
			// pathMan.setURIValue(ArduinoConst.PATH_VARIABLE_NAME_ARDUINO_PINS, uriTest);
			// }
		} catch (CoreException e) {
			Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Failed to create the path variable variables. The setup will not work properly", e));
			e.printStackTrace();
		}
	}

	private static void searchFiles(File folder, HashSet<String> Hardwarelists, String Filename, int depth) {
		if (depth > 0) {
			File[] a = folder.listFiles();
			if (a == null) {
				Common.log(new Status(IStatus.INFO, LinkItConst.CORE_PLUGIN_ID, "The folder " + folder + " does not contain any files.", null));
				return;
			}
			for(File f : a) {
				if (f.isDirectory()) {
					searchFiles(f, Hardwarelists, Filename, depth - 1);
				} else if (f.getName().equals(Filename)) {
					try {
						Hardwarelists.add(f.getCanonicalPath());
					} catch (IOException e) {
						// e.printStackTrace();
					}
				}
			}
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
	 * This method adds the Arduino code in a subfolder named Arduino. 2 linked
	 * subfolders named core and variant link to the real Arduino code note
	 * if your arduino ide version is from before 1.0 only 1 folder is created
	 * 
	 * @param project
	 *            The project to add the arduino code to
	 * @param ProjectProperties
	 *            The properties to use to add the core folder
	 * @throws CoreException
	 */
	public static void addArduinoCodeToProject(IProject project, ICConfigurationDescription configurationDescription) throws CoreException {

		String boardVariant = getBuildEnvironmentVariable(configurationDescription, ENV_KEY_build_variant, "");
		String buildCoreFolder = getBuildEnvironmentVariable(configurationDescription, ENV_KEY_build_core_folder, "");
		if (buildCoreFolder.contains(":")) {
			String sections[] = buildCoreFolder.split(":");
			if (sections.length != 2) {
				Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "the value for key " + ENV_KEY_build_core_folder + " in boards.txt is invalid:" + buildCoreFolder, null));
			} else {
				String architecture = getBuildEnvironmentVariable(configurationDescription, ENV_KEY_ARCHITECTURE, "");
				addCodeFolder(project, WORKSPACE_PATH_VARIABLE_NAME_ARDUINO, ARDUINO_HARDWARE_FOLDER_NAME + "/" + sections[1] + "/" + architecture + "/" + ARDUINO_CORE_FOLDER_NAME + "/" + sections[1], "arduino/core", configurationDescription);
			}
		} else {
			addCodeFolder(project, PATH_VARIABLE_NAME_ARDUINO_PLATFORM, ARDUINO_CORE_FOLDER_NAME + "/" + buildCoreFolder, "arduino/core", configurationDescription);
		}
		if (!boardVariant.equals("")) {
			LinkItHelpers.addCodeFolder(project, PATH_VARIABLE_NAME_ARDUINO_PINS, boardVariant, "arduino/variant", configurationDescription);
		} else {// this is Arduino version 1.0
			IFolder variantFolder = project.getFolder("arduino/variant");
			if (variantFolder.exists()) {
				try {
					variantFolder.delete(true, null);
				} catch (CoreException e) {
					Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "failed to delete the variant folder", e));
				}
			}
		}

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
	 * Remove all the arduino environment variables.
	 * 
	 * @param contribEnv
	 * @param confDesc
	 */
	private static void RemoveAllArduinoEnvironmentVariables(IContributedEnvironment contribEnv, ICConfigurationDescription confDesc) {

		IEnvironmentVariable[] CurVariables = contribEnv.getVariables(confDesc);
		for(int i = (CurVariables.length - 1); i > 0; i--) {
			if (CurVariables[i].getName().startsWith(LinkItConst.ENV_KEY_ARDUINO_START)) {
				contribEnv.removeVariable(CurVariables[i].getName(), confDesc);
			}
		}
	}

	/**
	 * Sets the default values. Basically some settings are not set in the
	 * platform.txt file. Here I set these values. This method should be called
	 * as
	 * first. This way the values in platform.txt and boards.txt will take
	 * precedence of the default values declared here
	 * 
	 * @param contribEnv
	 * @param confDesc
	 * @param platformFile
	 *            Used to define the hardware as different settings are needed
	 *            for avr and sam
	 */
	private static void setTheEnvironmentVariablesSetTheDefaults(IContributedEnvironment contribEnv, ICConfigurationDescription confDesc, IPath platformFile) {
		// Set some default values because the platform.txt does not contain
		// them
//		IEnvironmentVariable var = new EnvironmentVariable(ENV_KEY_ARDUINO_PATH, getArduinoPath().toString());
//		contribEnv.addVariable(var, confDesc);
//
//		// from 1.5.3 onwards 3 more environment variables need to be added
//		var = new EnvironmentVariable(ENV_KEY_ARCHITECTURE, platformFile.removeLastSegments(1).lastSegment());
//		contribEnv.addVariable(var, confDesc);
//		var = new EnvironmentVariable(ENV_KEY_BUILD_ARCH, platformFile.removeLastSegments(1).lastSegment().toUpperCase());
//		contribEnv.addVariable(var, confDesc);
//		var = new EnvironmentVariable(ENV_KEY_HARDWARE_PATH, platformFile.removeLastSegments(3).toString());
//		contribEnv.addVariable(var, confDesc);
//
//		// from 1.5.8 onward 1 more environment variable is needed
//		var = new EnvironmentVariable(ENV_KEY_PLATFORM_PATH, platformFile.removeLastSegments(1).toString());
//		contribEnv.addVariable(var, confDesc);
//		// Teensy uses build.core.path
//		var = new EnvironmentVariable(ENV_KEY_build_core_path, "${" + ENV_KEY_PLATFORM_PATH + "}/cores/${" + ENV_KEY_build_core_folder + "}");
//		contribEnv.addVariable(var, confDesc);
//
//		String buildVariantPath = "${" + ENV_KEY_PLATFORM_PATH + "}/variants/${" + ArduinoConst.ENV_KEY_build_variant + "}";
//		var = new EnvironmentVariable(ENV_KEY_build_variant_path, buildVariantPath);
//		contribEnv.addVariable(var, confDesc);
//
//		// I'm not sure why but till now arduino refused to put this in the platform.txt file
//		// I won't call them idiots for this but it is getting close
//		var = new EnvironmentVariable(ENV_KEY_SOFTWARE, "ARDUINO");
//		contribEnv.addVariable(var, confDesc);
//		var = new EnvironmentVariable(ENV_KEY_runtime_ide_version, GetARDUINODefineValue());
//		contribEnv.addVariable(var, confDesc);
//		// for the due from arduino IDE 1.6.1 onwards link the due bin builder to the hex binder
//		var = new EnvironmentVariable("A.RECIPE.OBJCOPY.HEX.PATTERN", "${A.RECIPE.OBJCOPY.BIN.PATTERN}");
//		contribEnv.addVariable(var, confDesc);
//		// End of section permitting denigrating remarks on arduino software development team
//
//		// For Teensy I added a flag that allows to compile everything in one
//		// project not using the archiving functionality
//		// I set the default value to: use the archiver
//		var = new EnvironmentVariable(ENV_KEY_use_archiver, "true");
//		contribEnv.addVariable(var, confDesc);
//
//		// Build Time
//		Date d = new Date();
//		GregorianCalendar cal = new GregorianCalendar();
//		long current = d.getTime() / 1000;
//		long timezone = cal.get(Calendar.ZONE_OFFSET) / 1000;
//		long daylight = cal.get(Calendar.DST_OFFSET) / 1000;
//		// p.put("extra.time.utc", Long.toString(current));
//		var = new EnvironmentVariable("A.EXTRA.TIME.UTC", Long.toString(current));
//		contribEnv.addVariable(var, confDesc);
//		// p.put("extra.time.local", Long.toString(current + timezone + daylight));
//		var = new EnvironmentVariable("A.EXTRA.TIME.LOCAL", Long.toString(current + timezone + daylight));
//		contribEnv.addVariable(var, confDesc);
//		// p.put("extra.time.zone", Long.toString(timezone));
//		var = new EnvironmentVariable("A.EXTRA.TIME.ZONE", Long.toString(timezone));
//		contribEnv.addVariable(var, confDesc);
//		// p.put("extra.time.dst", Long.toString(daylight));
//		var = new EnvironmentVariable("A.EXTRA.TIME.DTS", Long.toString(daylight));
//		contribEnv.addVariable(var, confDesc);
//		// End of Teensy specific settings
//
//		if (platformFile.segment(platformFile.segmentCount() - 2).equals("avr")) {
//			var = new EnvironmentVariable(ENV_KEY_compiler_path, makeEnvironmentVar("A.RUNTIME.IDE.PATH") + "/hardware/tools/avr/bin/");
//			contribEnv.addVariable(var, confDesc);
//		} else if (platformFile.segment(platformFile.segmentCount() - 2).equals("sam")) {
//			var = new EnvironmentVariable(ENV_KEY_build_system_path, makeEnvironmentVar("A.RUNTIME.IDE.PATH") + "/hardware/arduino/sam/system");
//			contribEnv.addVariable(var, confDesc);
//			var = new EnvironmentVariable(ENV_KEY_build_generic_path, makeEnvironmentVar("A.RUNTIME.IDE.PATH") + "/hardware/tools/g++_arm_none_eabi/arm-none-eabi/bin");
//			contribEnv.addVariable(var, confDesc);
//		} else if (platformFile.segment(platformFile.segmentCount() - 2).equals("mtk")) {
//			var = new EnvironmentVariable(ENV_KEY_build_system_path, makeEnvironmentVar("A.RUNTIME.IDE.PATH") + "/hardware/arduino/mtk/system");
//			contribEnv.addVariable(var, confDesc);
//		}
//
//		// some glue to make it work
//		String extraPathForOS = "";
//		if (Platform.getWS().equals(Platform.WS_WIN32)) {
//			extraPathForOS = "${PathDelimiter}${" + ENV_KEY_ARDUINO_PATH + "}/hardware/tools/avr/utils/bin${PathDelimiter}${" + ENV_KEY_ARDUINO_PATH + "}";
//		}
//		var = new EnvironmentVariable("PATH", "${A.COMPILER.PATH}${PathDelimiter}${" + ENV_KEY_build_generic_path + "}" + extraPathForOS + "${PathDelimiter}${PATH}");
//		contribEnv.addVariable(var, confDesc);
//
//		var = new EnvironmentVariable(ENV_KEY_build_path, "${ProjDirPath}/${ConfigName}");
//		contribEnv.addVariable(var, confDesc);
//
//		var = new EnvironmentVariable(ENV_KEY_build_project_name, makeEnvironmentVar("ProjName"));
//		contribEnv.addVariable(var, confDesc);
//
//		// if (firstTime) {
//		if (getBuildEnvironmentVariable(confDesc, ENV_KEY_JANTJE_SIZE_SWITCH, "").isEmpty()) {
//			var = new EnvironmentVariable(ENV_KEY_JANTJE_SIZE_SWITCH, makeEnvironmentVar(ENV_KEY_recipe_size_pattern));
//			contribEnv.addVariable(var, confDesc);
//		}
//		if (getBuildEnvironmentVariable(confDesc, ENV_KEY_JANTJE_SIZE_COMMAND, "").isEmpty()) {
//			var = new EnvironmentVariable(ENV_KEY_JANTJE_SIZE_COMMAND, JANTJE_SIZE_COMMAND);
//			contribEnv.addVariable(var, confDesc);
//		}
//
//		// Set the warning level default off like arduino does
//		if (getBuildEnvironmentVariable(confDesc, ENV_KEY_JANTJE_WARNING_LEVEL, "").isEmpty()) {
//			var = new EnvironmentVariable(ENV_KEY_JANTJE_WARNING_LEVEL, ENV_KEY_WARNING_LEVEL_OFF);
//			contribEnv.addVariable(var, confDesc);
//		}
//
//		var = new EnvironmentVariable(ENV_KEY_archive_file, "arduino.ar");
//		contribEnv.addVariable(var, confDesc);

	}





	/**
	 * When parsing boards.txt and platform.txt some processing needs to be done
	 * to get "acceptable environment variable values" This method does the
	 * parsing
	 * 
	 * @param inputString
	 *            the value string as read from the file
	 * @return the string to be stored as value for the environment variable
	 */
	public static String makeEnvironmentString(String inputString) {
		// String ret = inputString.replaceAll("-o \"\\{object_file}\"",
		// "").replaceAll("\"\\{object_file}\"",
		// "").replaceAll("\"\\{source_file}\"", "")
		// .replaceAll("\\{", "\\${" + ArduinoConst.ENV_KEY_START);
		String ret = inputString.replaceAll("\\{(?!\\{)", "\\${" + LinkItConst.ENV_KEY_ARDUINO_START);
		StringBuilder sb = new StringBuilder(ret);
		String regex = "\\{[^}]*\\}";
		Pattern p = Pattern.compile(regex); // Create the pattern.
		Matcher matcher = p.matcher(sb); // Create the matcher.
		while (matcher.find()) {
			String buf = sb.substring(matcher.start(), matcher.end()).toUpperCase();
			sb.replace(matcher.start(), matcher.end(), buf);
		}
		return sb.toString();
	}

	/**
	 * When parsing boards.txt and platform.txt some processing needs to be done
	 * to get "acceptable environment variable keys" This method does the
	 * parsing
	 * 
	 * @param inputString
	 *            the key string as read from the file
	 * @return the string to be used as key for the environment variable
	 */
	static String osString = null;

	private static String makeKeyString(String string) {
		if (osString == null) {
			if (Platform.getOS().equals(Platform.OS_LINUX)) {
				osString = "\\.LINUX";
			} else if (Platform.getOS().equals(Platform.OS_WIN32)) {
				osString = "\\.WINDOWS";
			} else {
				osString = "\\.\\.";
			}
		}
		return LinkItConst.ENV_KEY_ARDUINO_START + string.toUpperCase().replaceAll(osString, "");
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

	/**
	 * Given a source file calculates the base of the output file. this method
	 * may not be needed if I can used the eclipse default behavior. However
	 * the eclipse default behavior is different from the arduino default
	 * behavior. So I keep it for now and we'll see how it goes The eclipse
	 * default
	 * behavior is (starting from the project folder [configuration]/Source The
	 * Arduino default behavior is all in 1 location (so no subfolders)
	 * 
	 * @param Source
	 *            The source file to find the
	 * @return The base file name for the ouput if Source is "file.cpp" the
	 *         output is "file.cpp"
	 */
	public static IPath GetOutputName(IPath Source) {
		IPath outputName;
		if (Source.toString().startsWith("arduino")) {
			outputName = new Path("arduino").append(Source.lastSegment());
		} else {
			outputName = Source;
		}
		return outputName;
	}


	/**
	 * Reads the version number from the lib/version.txt file
	 * 
	 * @return the version number if found if no version number found the error
	 *         returned by the file read method
	 */
	static public String GetIDEVersion(IPath arduinoIDEPath) {

		File file = arduinoIDEPath.append(LinkItConst.LIB_VERSION_FILE).toFile();
		try {
			// Open the file that is the first
			// command line parameter
			FileInputStream fstream = new FileInputStream(file);
			// Get the object of DataInputStream
			try (DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));) {

				String strLine = br.readLine();
				in.close();
				return strLine;
			}
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
			return e.getMessage();
		}
	}

	private static String makeEnvironmentVar(String string) {
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
		IToolChain tcs = ManagedBuildManager.getExtensionToolChain(toolChainId);
		String childId = ManagedBuildManager.calculateChildId(toolChainId, null);
		IConfiguration cfg = new Configuration(mProj, (ToolChain)tcs, childId, name);
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

}
