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
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import se.aceone.mediatek.linkit.common.LinkItPreferences;
import se.aceone.mediatek.linkit.xml.config.ObjectFactory;
import se.aceone.mediatek.linkit.xml.config.Packageinfo;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.APIAuth;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Namelist;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Output;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Userinfo;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Vxp;

public class LinkIt10Helper extends LinkItHelper {

	static final String LINK_IT_SDK10_CAMMEL_CASE = "LinkItSDK10";
	public static final String LINK_IT_SDK10 = LINK_IT_SDK10_CAMMEL_CASE.toUpperCase();

	public LinkIt10Helper(IProject project, Compiler compiler) {
		super(project, compiler);
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

	@Override
	public void setIncludePaths(ICProjectDescription projectDescriptor, ICResourceDescription resourceDescription) {
		super.setIncludePaths(projectDescriptor, resourceDescription);

		ICConfigurationDescription configurationDescription = projectDescriptor.getDefaultSettingConfiguration();
		IProject project = projectDescriptor.getProject();

		IPath linkit10 = new Path(getBuildEnvironmentVariable(configurationDescription, LINKIT10, null));
		String gccIncludeVar = getBuildEnvironmentVariable(configurationDescription, getIncludeVar(), null);
		IPath gccInclude = linkit10.append(gccIncludeVar);
		IPath linkit = project.getFolder("LinkIt").getProjectRelativePath();

		linkFileToFolder(project, gccInclude, linkit);
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

		createConfig(project, projType, monitor);

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

	}

	protected String getIncludeVar() {
		if (compiler instanceof CompilerGCC) {
			return "GCCINCLUDE";
		}
		if (compiler instanceof CompilerRVCT) {
			return "RVCTINCLUDE";
		}
		return "NULL_INCLUDE";
	}

}
