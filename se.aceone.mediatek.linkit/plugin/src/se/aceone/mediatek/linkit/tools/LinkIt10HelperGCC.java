package se.aceone.mediatek.linkit.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import se.aceone.mediatek.linkit.common.LinkItPreferences;
import se.aceone.mediatek.linkit.xml.config.Packageinfo;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.APIAuth;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Namelist;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Output;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Userinfo;

public class LinkIt10HelperGCC extends LinkItHelper {

	static final String LINK_IT_SDK10_CAMMEL_CASE = "LinkItSDK10";
	public static final String LINK_IT_SDK10 = LINK_IT_SDK10_CAMMEL_CASE.toUpperCase();

	public static final String COMPILER_IT_SDK10 = "ARMCOMPILER";

	public LinkIt10HelperGCC(IProject project) {
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
		String compiler = System.getenv().get(COMPILER_IT_SDK10);
		if (compiler == null) {
			compiler = "C:\\dev\\eclipseMTK\\LINKIT_ASSIST_SDK\\tools\\gcc-arm-none-eabi-4_9-2014q4-20141203-win32\\";
		}
		return compiler;
	}

	public void copyProjectResources(ICProjectDescription projectDescriptor, IProgressMonitor monitor) throws CoreException, IOException, JAXBException {
		ICConfigurationDescription configurationDescription = projectDescriptor.getDefaultSettingConfiguration();
		IPath toolPath = new Path(getBuildEnvironmentVariable(configurationDescription, TOOL_PATH, null));
		IProject project = projectDescriptor.getProject();

		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("LINKITWIZARDVS2008", project.getName());
		replacements.put("LINKIT_APP_WIZARDTEMPLATE", project.getName().toUpperCase() + "_H");
		replacements.put("__INCLUDE_PATH__", ".\\\\include;.\\\\include\\\\service;.\\\\include\\\\component;.\\\\ResID;.\\\\;.\\\\src\\\\");
		replacements.put("__LIB_PATH__", "odbc32.lib odbccp32.lib msimg32.lib linkitwin32.lib");
		replacements.put("__VRE_WIZARD_SOURCE_LIST__", "<File RelativePath=\"src\\\\" + project.getName() + ".c\"/>");

		replacements.put("__INCLUDE_HEAD_FILE__", " Includes\r\n#include <vmatcmd.h>\r\n#include <vmkeypad.h>\r\n#include <vmlog.h>\r\n"
				+ "#include <vmpromng.h>\r\n#include <vmsys.h>\r\n");

		IPath wiz = toolPath.append("Wizard").append("LINKIT_SDK_WIZARD_V10_IOT");
		IPath srcPath = wiz.append("LINKITWIZARDVS2008.vcproj");
		IPath outPath = new Path(project.getName() + ".vcproj");
		addResourceToProject(monitor, project, srcPath, outPath, replacements);

		IPath projType = wiz;

		outPath = new Path("src/" + project.getName() + ".c");
		addResourceToProject(monitor, project, projType.append("LINKITWIZARDVS2008.c"), outPath, replacements);

		outPath = new Path("src/" + project.getName() + ".h");
		addResourceToProject(monitor, project, projType.append("LINKITWIZARDVS2008.h"), outPath, replacements);

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

		srcPath = res.append("ref_list_LINKITWIZARDVS2008.txt");
		outPath = new Path("res/ref_list_" + project.getName() + ".txt");
		addResourceToProject(monitor, project, srcPath, outPath, replacements);

		srcPath = res.append("LINKITWIZARDVS2008.res.xml");
		outPath = new Path("res/" + project.getName() + ".res.xml");
		addResourceToProject(monitor, project, srcPath, outPath, replacements);

		IPath resId = projType.append("ResID");

		srcPath = resId.append("ResID.h");
		outPath = new Path("ResID/ResID.h");
		addResourceToProject(monitor, project, srcPath, outPath);

		IPath linkit10 = new Path(getBuildEnvironmentVariable(configurationDescription, LINKIT10, null));
		String gccIncludeVar = getBuildEnvironmentVariable(configurationDescription, getIncludeVar(), null);
		IPath gccInclude = linkit10.append(gccIncludeVar);
		IPath linkit = project.getFolder("LinkIt").getProjectRelativePath();

//		addResourceToProject(monitor, project, gccInclude, outPath);
		linkFileToFolder(project, gccInclude, linkit);
	}

}
