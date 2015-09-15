package se.aceone.mediatek.linkit.toolchain;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineGenerator;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacro;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import se.aceone.mediatek.linkit.common.LinkItConst;
import se.aceone.mediatek.linkit.tools.Common;
import se.aceone.mediatek.linkit.xml.config.Packageinfo;
import se.aceone.mediatek.linkit.xml.proj.VisualStudioProject;
import se.aceone.mediatek.linkit.xml.proj.VisualStudioProject.Files;

@SuppressWarnings("restriction")
public class GCCLinkerCommadLineGenerator implements IManagedCommandLineGenerator, LinkItConst {
	public final String AT = "@"; //$NON-NLS-1$
	public final String COLON = ":"; //$NON-NLS-1$
	public final String DOT = "."; //$NON-NLS-1$
	public final String ECHO = "echo"; //$NON-NLS-1$
	public final String IN_MACRO = "$<"; //$NON-NLS-1$
	public final String LINEBREAK = "\\\n"; //$NON-NLS-1$
	public final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
	public final String OUT_MACRO = "$@"; //$NON-NLS-1$
	public final String SEPARATOR = "/"; //$NON-NLS-1$
	public final String SINGLE_QUOTE = "'"; //$NON-NLS-1$
	public final String DOUBLE_QUOTE = "\""; //$NON-NLS-1$
	public final String TAB = "\t"; //$NON-NLS-1$
	public final String WHITESPACE = " "; //$NON-NLS-1$
	public final String WILDCARD = "%"; //$NON-NLS-1$
	public final String UNDERLINE = "_"; //$NON-NLS-1$
	public final String EMPTY = ""; //$NON-NLS-1$

	public final String VAR_FIRST_CHAR = "$"; //$NON-NLS-1$
	public final char VAR_SECOND_CHAR = '{';
	public final String VAR_FINAL_CHAR = "}"; //$NON-NLS-1$
	public final String CLASS_PROPERTY_PREFIX = "get"; //$NON-NLS-1$

	public final String CMD_LINE_PRM_NAME = "COMMAND"; //$NON-NLS-1$
	public final String FLAGS_PRM_NAME = "FLAGS"; //$NON-NLS-1$
	public final String OUTPUT_FLAG_PRM_NAME = "OUTPUT_FLAG"; //$NON-NLS-1$
	public final String OUTPUT_PREFIX_PRM_NAME = "OUTPUT_PREFIX"; //$NON-NLS-1$
	public final String OUTPUT_PRM_NAME = "OUTPUT"; //$NON-NLS-1$
	public final String INPUTS_PRM_NAME = "INPUTS"; //$NON-NLS-1$

	public GCCLinkerCommadLineGenerator() {
	}

	private String makeVariable(String variableName) {
		return "${" + variableName + "}"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public IManagedCommandLineInfo generateCommandLineInfo(ITool tool, String commandName, String[] flags, String outputFlag, String outputPrefix,
			String outputName, String[] inputResources, String commandLinePattern) {
		// if (commandLinePattern == null || commandLinePattern.length() <= 0)
		// commandLinePattern = Tool.DEFAULT_PATTERN;

		// tool.getCommandLinePattern()

		IToolChain parent = (IToolChain) tool.getParent();
		IConfiguration configuration = parent.getParent();
		IManagedProject managedProject = configuration.getManagedProject();
		IProject project = (IProject) managedProject.getOwner();
		File configFile = new File(project.getFile("config.xml").getLocationURI());

		String aCommand = "";
		JAXBContext jaxbContext;
		try {
			IBuildMacroProvider macroProvider = ManagedBuildManager.getBuildMacroProvider();

			jaxbContext = JAXBContext.newInstance(Packageinfo.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			Packageinfo packageinfo = (Packageinfo) jaxbUnmarshaller.unmarshal(configFile);
			for (String mn : packageinfo.getAPIAuth().getCategory()) {
				String macroName = mn.toUpperCase();
				IBuildMacro macro = macroProvider.getMacro(macroName, IBuildMacroProvider.CONTEXT_CONFIGURATION, configuration, true);
				if (macro != null) {
					System.out.println(macroName + " " + macro);
					commandLinePattern += WHITESPACE + DOUBLE_QUOTE + makeVariable(LINKIT10) + "/" + makeVariable("GCCLIB") + "/" + makeVariable(macroName)
							+ DOUBLE_QUOTE;
				}
			}

			System.out.println(aCommand);
		} catch (JAXBException e) {
			IStatus status = new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Error reading config.xml file", e);
			Common.log(status);
			return null;
		}

		List<IPath> vcprojs = getFilesWithExt(project, "vcproj");
		if (vcprojs.isEmpty()) {
			IStatus status = new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Did not find *.vcproj file");
			Common.log(status);
			return null;
		}

		File vcprojFile = new File(project.getFile(vcprojs.get(0)).getLocationURI());
		try {
			jaxbContext = JAXBContext.newInstance(VisualStudioProject.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			VisualStudioProject vsProject = (VisualStudioProject) jaxbUnmarshaller.unmarshal(vcprojFile);
			Files files = vsProject.getFiles();
			for (VisualStudioProject.Files.File projfile : files.getFile()) {
				String libFile = projfile.getRelativePath();
				IFile file = project.getFile(libFile);
				String path = file.getRawLocation().toOSString();

				System.out.println(path);
				if (libFile.endsWith(".a") && file.exists()) {
					commandLinePattern += WHITESPACE + DOUBLE_QUOTE + path + DOUBLE_QUOTE;
				}
			}

		} catch (JAXBException e) {
			IStatus status = new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Error reading *.vcproj file", e);
			Common.log(status);
			return null;
		}

		IFolder lib = project.getFolder("lib");
		if (lib.exists()) {
			try {
				IResource[] members = lib.members();
				for (IResource member : members) {
					if (member.exists() && member instanceof IFile) {
						if (member.getName().endsWith(".a")) {
							String path = member.getLocation().toOSString();
							commandLinePattern += WHITESPACE + DOUBLE_QUOTE + path + DOUBLE_QUOTE;
						}
					}

				}
			} catch (CoreException e) {
			}
		}

		// if the output name isn't a variable then quote it
		if (outputName.length() > 0 && outputName.indexOf("$(") != 0) //$NON-NLS-1$
			outputName = DOUBLE_QUOTE + outputName + DOUBLE_QUOTE;

		String inputsStr = ""; //$NON-NLS-1$
		if (inputResources != null) {
			for (String inp : inputResources) {
				if (inp != null && inp.length() > 0) {
					// if the input resource isn't a variable then quote it
					if (inp.indexOf("$(") != 0) { //$NON-NLS-1$
						inp = DOUBLE_QUOTE + inp + DOUBLE_QUOTE;
					}
					inputsStr = inputsStr + inp + WHITESPACE;
				}
			}
			inputsStr = inputsStr.trim();
		}

		String flagsStr = stringArrayToString(flags);

		String command = commandLinePattern;

		command = command.replace(makeVariable(CMD_LINE_PRM_NAME), commandName);
		command = command.replace(makeVariable(FLAGS_PRM_NAME), flagsStr);
		command = command.replace(makeVariable(OUTPUT_FLAG_PRM_NAME), outputFlag);
		command = command.replace(makeVariable(OUTPUT_PREFIX_PRM_NAME), outputPrefix);
		command = command.replace(makeVariable(OUTPUT_PRM_NAME), outputName);
		command = command.replace(makeVariable(INPUTS_PRM_NAME), inputsStr);

		command = command.replace(makeVariable(CMD_LINE_PRM_NAME.toLowerCase()), commandName);
		command = command.replace(makeVariable(FLAGS_PRM_NAME.toLowerCase()), flagsStr);
		command = command.replace(makeVariable(OUTPUT_FLAG_PRM_NAME.toLowerCase()), outputFlag);
		command = command.replace(makeVariable(OUTPUT_PREFIX_PRM_NAME.toLowerCase()), outputPrefix);
		command = command.replace(makeVariable(OUTPUT_PRM_NAME.toLowerCase()), outputName);
		command = command.replace(makeVariable(INPUTS_PRM_NAME.toLowerCase()), inputsStr);

		return new ManagedCommandLineInfo(command.trim(), commandLinePattern, commandName, stringArrayToString(flags), outputFlag, outputPrefix, outputName,
				stringArrayToString(inputResources));
		// return null;
	}

	private String stringArrayToString(String[] array) {
		if (array == null || array.length <= 0)
			return new String();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < array.length; i++)
			sb.append(array[i] + WHITESPACE);
		return sb.toString().trim();
	}

	protected List<IPath> getFilesWithExt(final IProject project, final String ext) {
		final List<IPath> files = new ArrayList<IPath>();
		try {
			project.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) {
					if (resource.getType() == IResource.FILE && resource.getName().endsWith(ext)) {
						files.add(resource.getProjectRelativePath());
						return true;
					}
					return true;
				}
			}, 1, false);
		} catch (CoreException e) {
		}
		return files;
	}
}
