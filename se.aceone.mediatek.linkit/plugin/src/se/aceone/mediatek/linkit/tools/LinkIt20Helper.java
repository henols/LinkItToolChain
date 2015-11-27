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
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Vxp;

public class LinkIt20Helper extends LinkItHelper {

	static final String LINK_IT_SDK20_CAMMEL_CASE = "LinkItSDK20";
	public static final String LINK_IT_SDK20 = LINK_IT_SDK20_CAMMEL_CASE.toUpperCase();

	public LinkIt20Helper(IProject project) {
		super(project);
	}
	
	public String getEnvironmentPath() {
		String linkitEnv = System.getenv().get(LINK_IT_SDK20_CAMMEL_CASE);
		if (linkitEnv == null) {
			linkitEnv = System.getenv().get(LINK_IT_SDK20);
		}
		return linkitEnv;
	}
	
	@Override
	public String getCompilerPath() {
		return new Path(getEnvironmentPath()).append(getGccLocation()).toPortableString();
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

}
