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

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.WriteAccessException;
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
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Vxp;

public class LinkIt10HelperRVTCLib extends LinkIt10HelperRVTC {

	public LinkIt10HelperRVTCLib(IProject project) {
		super(project);
	}

	@Override
	public void setIncludePaths(ICProjectDescription projectDescriptor, ICResourceDescription resourceDescription) {
		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		ICConfigurationDescription configuration = resourceDescription.getConfiguration();

		ICConfigurationDescription configurationDescription = projectDescriptor.getDefaultSettingConfiguration();

		IPath toolPath = new Path(getBuildEnvironmentVariable(configurationDescription, TOOL_PATH, null));
		IPath envInclude = toolPath.append(getBuildEnvironmentVariable(configurationDescription, INCLUDE, null));
		addIncludeFolder(projectDescriptor, envInclude);

		IPath serviceInclude = envInclude.append("service");
		addIncludeFolder(projectDescriptor, serviceInclude);

		IPath custInclude = toolPath.append("..\\custom\\mediatek\\inc");
		addIncludeFolder(projectDescriptor, custInclude);

		// "C:\Program Files (x86)\LinkIt SDK V1.0.00\include"
		// "C:\Program Files (x86)\LinkIt SDK V1.0.00\include\service"
		// "C:\Program Files (x86)\LinkIt SDK V1.0.00\custom\mediatek\inc"

		setCompilerIncludePaths(projectDescriptor, contribEnv, configuration);
	}

	public void copyProjectResources(ICProjectDescription projectDescriptor, IProgressMonitor monitor) throws CoreException, IOException, JAXBException {
		ICConfigurationDescription configurationDescription = projectDescriptor.getDefaultSettingConfiguration();
		IPath toolPath = new Path(getBuildEnvironmentVariable(configurationDescription, TOOL_PATH, null));
		IProject project = projectDescriptor.getProject();

		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("LINKITWIZARDVS2008LIB", project.getName());
		replacements.put("LINKIT_APP_WIZARDTEMPLATE", project.getName().toUpperCase() + "_H");
		replacements.put("__INCLUDE_PATH__", ".\\\\include;.\\\\include\\\\service;.\\\\include\\\\component;.\\\\src\\\\");
		replacements.put("__LIB_PATH__", "odbc32.lib odbccp32.lib msimg32.lib linkitwin32.lib");
		replacements.put("__VRE_WIZARD_SOURCE_LIST__", "<File RelativePath=\"src\\\\" + project.getName() + ".c\"/>");

		replacements.put("__INCLUDE_HEAD_FILE__", " Includes\r\n#include <vmatcmd.h>\r\n#include <vmkeypad.h>\r\n#include <vmlog.h>\r\n"
				+ "#include <vmpromng.h>\r\n#include <vmsys.h>\r\n");

		IPath wiz = toolPath.append("Wizard").append("LINKIT_SDK_WIZARD_LIB");
		IPath srcPath = wiz.append("LINKITWIZARDVS2008LIB.vcproj");
		IPath outPath = new Path(project.getName() + ".vcproj");
		addResourceToProject(monitor, project, srcPath, outPath, replacements);

		IPath projType = wiz;

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

		Vxp vxp = packageinfo.getVxp();
		vxp.setVenus(BigInteger.valueOf(1));
		vxp.setSdkversion(BigInteger.valueOf(10));
		vxp.setIotWearable(BigInteger.valueOf(2));

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		javax.xml.bind.Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8"); // NOI18N
		marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(packageinfo, os);

		outPath = new Path("config.xml");
		addResourceToProject(project, outPath, new ByteArrayInputStream(os.toByteArray()), monitor);
	}

	public void setMacros(ICProjectDescription projectDescription, String devBoard) {
		addMacro(projectDescription, "__COMPILER_RVCT__", null, ICSettingEntry.BUILTIN);
		addMacro(projectDescription, "_LIB", null, ICSettingEntry.BUILTIN);
		addMacro(projectDescription, devBoard, null);
	}

	public void setSourcePaths(ICProjectDescription projectDescriptor, boolean useSrcFoleder) throws WriteAccessException, CoreException {
		IProject project = projectDescriptor.getProject();
		ICConfigurationDescription configurationDescription = projectDescriptor.getDefaultSettingConfiguration();
		ICConfigurationDescription configuration = configurationDescription.getConfiguration();

		if (useSrcFoleder) {
			addSourceFolder(configuration, project.getFolder("src").getFullPath());
		} else {
			IPath[] ex = { new Path("arm"),new Path("config") };
			addSourceFolder(configuration, project.getFullPath(), ex);
		}
	}
	
	protected String getToolChainId() {
		return LINKIT_DEFAULT_TOOL_CHAIN_STATIC_RVCT;
	}

	protected String getIncludeVar() {
		return null;
	}
	 protected BigInteger getOutputType() {
		return BigInteger.valueOf(3);
	}

}
