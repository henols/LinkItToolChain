package se.aceone.mediatek.linkit.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class LinkIt10HelperRVTC extends LinkIt10HelperGCC {

	static final String LINK_IT_SDK10_CAMMEL_CASE = "LinkItSDK10";
	public static final String LINK_IT_SDK10 = LINK_IT_SDK10_CAMMEL_CASE.toUpperCase();

	public static final String COMPILER_IT_SDK10_RTVC = "RVCT31BIN";

	public LinkIt10HelperRVTC(IProject project) {
		super(project);
	}

	public String getEnvironmentPath() {
		String linkitEnv = System.getenv().get(LINK_IT_SDK10_CAMMEL_CASE);
		if (linkitEnv == null) {
			linkitEnv = System.getenv().get(LINK_IT_SDK10);
		}
		if (linkitEnv == null) {
			linkitEnv = "C:\\Program Files (x86)\\LinkIt SDK V1.0.00";
		}
		return linkitEnv;
	}

	public String getCompilerPath() {
		String compiler = System.getenv().get(COMPILER_IT_SDK10_RTVC);
		if (compiler == null) {
			compiler = "C:/Program Files/ARM/RVCT";
		} else {
			compiler = new Path(compiler).removeLastSegments(4).toPortableString();
		}
		return compiler;
	}

	public void setEnvironmentVariables(ICResourceDescription resourceDescription, String devBoard) throws FileNotFoundException, IOException {
		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		ICConfigurationDescription configuration = resourceDescription.getConfiguration();

		contribEnv.addVariable(new EnvironmentVariable(DEV_BOARD, devBoard), configuration);
		contribEnv.addVariable(new EnvironmentVariable(SIZETOOL, ARM_NONE_EABI_SIZE), configuration);

		contribEnv.addVariable(new EnvironmentVariable(COMPILER_TOOL_PATH, new Path(COMPILER_TOOL_PATH_RVTC).toPortableString()), configuration);

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

		for (String key : envVars.keySet()) {
			if (!key.startsWith("GCC") && !key.startsWith("ADS") && !key.startsWith("VTP")) {
				String value = envVars.get(key);
				System.out.println(key + "=" + value);
//				if(key.startsWith("RVCT")){
//					key = "GCC" + key.substring(4);
//				}else if(key.equals("CCOMPILER")){
//					key = "GCCCCOMPILER";
//				}else if(key.equals("CPPCOMPILER")){
//					key = "GCCCPPCOMPILER";
//				}else if(key.equals("LINK")){
//					key = "GCCLINKER";
//				}else if(key.equals("FROMELF")){
//					key = "OBJCOPY";
//				}
				contribEnv.addVariable(new EnvironmentVariable(key, value), configuration);
			}
		}
		numberReader.close();
		contribEnv.addVariable(new EnvironmentVariable(COMPILER_PATH, new Path(getCompilerPath()).toPortableString()), configuration);
	}

	protected void setCompilerIncludePaths(ICProjectDescription projectDescriptor, IContributedEnvironment contribEnv, ICConfigurationDescription configuration) {
		IPath compilerLocation = new Path(getCompilerPath());
		IPath rvtcIncl = compilerLocation.append("Data/3.1/569/include/windows");
		addIncludeFolder(projectDescriptor, rvtcIncl);
	}

	public void setMacros(ICProjectDescription projectDescription, String devBoard) {
		addMacro(projectDescription, "__COMPILER_RVCT__", null, ICSettingEntry.BUILTIN);
		addMacro(projectDescription, "_FOR_WNC", null, ICSettingEntry.BUILTIN);
		addMacro(projectDescription, "_NOUNIX_", null, ICSettingEntry.BUILTIN);
		addMacro(projectDescription, "_USE_MINIGUIENTRY", null, ICSettingEntry.BUILTIN);
		addMacro(projectDescription, "_MINIGUI_LIB_", null, ICSettingEntry.BUILTIN);
		addMacro(projectDescription, devBoard, null);
	}

	protected String getToolChainId() {
		return LINKIT_DEFAULT_TOOL_CHAIN_RVCT;
	}

	protected String getIncludeVar() {
		return "RVCTINCLUDE";
	}

}
