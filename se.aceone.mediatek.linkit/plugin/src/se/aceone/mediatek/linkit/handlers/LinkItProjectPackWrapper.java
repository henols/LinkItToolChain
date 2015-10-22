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
package se.aceone.mediatek.linkit.handlers;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

import se.aceone.mediatek.linkit.common.ExternalCommandLauncher;
import se.aceone.mediatek.linkit.common.LinkItConst;
import se.aceone.mediatek.linkit.tools.Common;
import se.aceone.mediatek.linkit.xml.config.Packageinfo;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.APIAuth;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Namelist;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Operationinfo;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Output;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Resolution;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Targetconfig;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Userinfo;
import se.aceone.mediatek.linkit.xml.config.Packageinfo.Vxp;

public class LinkItProjectPackWrapper {

	static LinkItProjectPackWrapper PACK_WRAPPER = null;
	MessageConsole console = null;

	private LinkItProjectPackWrapper() {
		// no constructor needed
	}

	static private LinkItProjectPackWrapper getProjectPackWrapper() {
		if (PACK_WRAPPER == null) {
			PACK_WRAPPER = new LinkItProjectPackWrapper();
		}
		return PACK_WRAPPER;
	}

	static public void pack(IProject project, String cConf) {
		getProjectPackWrapper().internalPack(project, cConf);
	}

	public static MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName())) {
				return (MessageConsole) existing[i];
			}
		}
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}

	public void internalPack(final IProject project, String cConf) {

		// Check that we have a AVR Project
		try {
			if (project == null || !(project.hasNature(LinkItConst.Cnatureid) || project.hasNature(LinkItConst.CCnatureid))) {
				Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "The current selected project is not an C/C++ Project", null));
				return;
			}
		} catch (CoreException e) {
			// Log the Exception
			Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Can't access project nature", e));
		}

		// String UpLoadTool = Common.getBuildEnvironmentVariable(project,
		// cConf, LinkItConst.ENV_KEY_upload_tool, "");
		// String MComPort = Common.getBuildEnvironmentVariable(project, cConf,
		// LinkItConst.ENV_KEY_JANTJE_COM_PORT, "");
		console = findConsole("LinkIt pack");
		// console.clearConsole();
		console.activate();

		executCommand(project, "Packing resources");

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

	protected List<String> buildPackDigistCommand(final IProject project) {
		// MessageConsoleStream messageStream = console.newMessageStream();
		// messageStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
		// MessageConsoleStream outStream = console.newMessageStream();
		// outStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
		// MessageConsoleStream errorStream = console.newMessageStream();
		// errorStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
		List<String> command = new ArrayList<String>();

		File file = new File(project.getFile("config.xml").getLocationURI());

		// messageStream.println();
		// messageStream.println("Starting Digist Packer Command Shell");

		JAXBContext jaxbContext;
		Packageinfo packageinfo;
		try {
			jaxbContext = JAXBContext.newInstance(Packageinfo.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			packageinfo = (Packageinfo) jaxbUnmarshaller.unmarshal(file);
		} catch (JAXBException e) {
			// errorStream.println("Error reading config.xml file");
			return null;
		}
		String confName = getConfigurationName(project);
		// String axfFile = project.getFile(new Path(confName).append(project.getName() +
		// ".axf")).getLocation().toOSString();
		// String outFile = project.getFile(new Path(confName).append(project.getName() +
		// ".pkd")).getLocation().toOSString();
		String projFile = project.getFile(new Path(project.getName() + ".vcproj")).getLocation().toOSString();
		String toolPath = getEnvVariable(project, LinkItConst.TOOL_PATH);

		IPath packCmd = new Path(toolPath).append(getEnvVariable(project, LinkItConst.PACK_DIGEST));

		// Digist Packer Command Shell v3.1329.00
		// shell command arg count: 30
		// argv[0]: C:\Program Files (x86)\LinkIt SDK V1.0.00\tools\PackDigist.exe
		// argv[1]: C:\dev\ws\ws-mtk_tracker_v1.3\tracker_tinitell\tracker_tinitell.vcproj
		// argv[2]: Demo
		// argv[3]: 0.1.0
		// argv[4]: Tinitell
		// argv[5]: 500
		// argv[6]: supportBg
		// argv[7]: Not Support rotate
		// argv[8]: Demo
		// argv[9]: Demo
		// argv[10]: Demo
		// argv[11]: 1234567890
		// argv[12]: content
		// argv[13]: Audio Call Camera TCP File HTTP Sensor SIM card Record SMS(person) SMS(SP) BitStream Contact LBS
		// MMS ProMng SMSMng Video XML Sec SysStorage Payment BT PUSH UDP SysFile sso
		// argv[14]: vxp
		// argv[15]: RVCT
		// argv[16]: PKD
		// argv[17]: UnCompress
		// argv[18]: venus
		// argv[19]: Adaptable
		// argv[20]: 8388768
		// argv[21]: UnSysMemAble
		// argv[22]:
		//
		// argv[23]: -1
		// argv[24]: UNPUSH
		// argv[25]: -1
		// argv[26]: -1
		// argv[27]: AutoStart
		// argv[28]: UnTransferImg
		// argv[29]: NoIdleShortcut
		// adaptable = 1
		// Pack digist Done.
		// Prepare copy the file

		// Digist Packer Command Shell v3.1329.00
		// shell command arg count: 31
		// argv[0]: C:\dev\eclipseMTK\LINKIT_ASSIST_SDK\tools\PackDigist.exe
		// argv[1]: C:/dev/eclipseMTK/workspace/sometest\sometest.vcproj
		// argv[2]: Demo
		// argv[3]: 2.0.0
		// argv[4]: MediaTek Inc
		// argv[5]: 500
		// argv[6]: supportBg
		// argv[7]: Not Support rotate
		// argv[8]: Demo
		// argv[9]: Demo
		// argv[10]: Demo
		// argv[11]: 1234567890
		// argv[12]: content
		// argv[13]: Audio Call Camera TCP File HTTP Sensor SIM card Record SMS(person) SMS(SP) BitStream Contact LBS
		// MMS ProMng SMSMng Video XML Sec SysStorage Payment BT PUSH UDP SysFile sso
		// argv[14]: vxp
		// argv[15]: GCC
		// argv[16]: PKD
		// argv[17]: UnCompress
		// argv[18]: venus
		// argv[19]: Adaptable
		// argv[20]: 8388768
		// argv[21]: UnSysMemAble
		// argv[22]: 100
		// argv[23]: prjoectcfg.appid
		// argv[24]: false
		// argv[25]: prjoectcfg.pushappid
		// argv[26]: -1
		// argv[27]: UnAutoStart
		// argv[28]: TransferImg
		// argv[29]: NoIdleShortcut
		// argv[30]: prjoectcfg.CTVvalueHex
		// adaptable = 1
		// Pack tag items done.

		// Commmand
		command.add(packCmd.toOSString());
		// Project file
		command.add(projFile);

		// app english name?
		command.add(setQuotes(project.getName()));

		Userinfo userinfo = packageinfo.getUserinfo();
		command.add(setQuotes(userinfo.getAppversion()));
		command.add(setQuotes((userinfo.getDeveloper() == null ? "" : userinfo.getDeveloper().trim())));

		Targetconfig targetconfig = packageinfo.getTargetconfig();
		List<JAXBElement<? extends Serializable>> configList = targetconfig.getMemOrSupportbgOrUserfont();

		JAXBElement<? extends Serializable> e = getElementByName(configList, "mem");
		if (e != null) {
			command.add(setQuotes(e.getValue() + ""));
		} else {
			command.add("\"-1\"");
		}

		if (isElementChecked(configList, "supportbg")) {
			command.add("\"supportBg\"");
		} else {
			command.add("\"UnSupportBg\"");
		}

		if (isElementChecked(configList, "screenrotate")) {
			command.add("\"support rotate\"");
		} else {
			command.add("\"Not Support rotate\"");
		}

		Namelist namelist = packageinfo.getNamelist();
		command.add(setQuotes(namelist.getEnglish()));
		command.add(setQuotes(namelist.getChinese()));
		command.add(setQuotes(namelist.getCht()));

		Operationinfo operationinfo = packageinfo.getOperationinfo();
		command.add(setQuotes(operationinfo.getImsi() + ""));
		command.add(setQuotes(operationinfo.getContent()));

		String cats = "";
		APIAuth apiAuth = packageinfo.getAPIAuth();
		for (String category : apiAuth.getCategory()) {
			cats += category + " ";
		}
		command.add(setQuotes(cats.trim()));

		switch (packageinfo.getOutput().getType().intValue()) {
		case 0: // VXP(0),
			command.add("\"vxp\"");
			break;
		case 1: // VSM(1),
			command.add("\"vsm\"");
			break;
		case 2: // VSO(2),
			command.add("\"vso\"");
			break;
		case 3:// LIB(3),
			break;
		case 4:// VSP(4),
			command.add("\"vxp\"");
			break;
		case 5:// VTP(5),
			break;
		case 7:// VXPC(7),
			command.add("\"vcp\"");
			break;
		case 8: // VXPD(8);
			command.add("\"vpd\"");
			break;
		}

		// compiler type
		command.add("\"RVCT\""); // TODO check for GCC compiler

		Vxp vxp = packageinfo.getVxp();
		if ((vxp.getSdkversion() != null && vxp.getSdkversion().intValue() == 10) || (vxp.getMreversion() != null && vxp.getMreversion().intValue() == 40)) {
			command.add("\"PKD\"");
		} else {
			command.add("\"VXP\"");
		}

		command.add("\"UnCompress\"");

		// TODO deal with if its set or not
		if (vxp.getVenus().intValue() > 0) {
			command.add("\"venus\"");
		} else {
			command.add("\"novenus\"");
		}

		command.add("\"Adaptable\"");

		// build resulotion
		Resolution res = packageinfo.getResolution();
		int resulotion = (res.getWidth() != null ? res.getWidth().intValue() : 0) << 16 | (res.getHeight() != null ? res.getHeight().intValue() : 0);
		command.add(resulotion + "");

		if (isElementChecked(configList, "usesysmemory")) {
			command.add("\"SysMemAble\"");
		} else {
			command.add("\"UnSysMemAble\"");
		}
		// sysmemSize
		command.add("\"\"");

		// app id
		command.add("\"-1\"");
		command.add("\"UNPUSH\"");

		// pushappid
		command.add("\"-1\"");
		command.add("\"-1\"");

		if (isElementChecked(configList, "autostart")) {
			command.add("\"AutoStart\"");
		} else {
			command.add("\"UnAutoStart\"");
		}

		if (isElementChecked(configList, "TransferImg")) {
			command.add("\"TransferImg\"");
		} else {
			command.add("\"UnTransferImg\"");
		}

		command.add("\"NoIdleShortcut\"");
		command.add(setQuotes(project.getFile(new Path(confName)).getLocation().toOSString()));

		return command;
	}

	private boolean isElementChecked(List<JAXBElement<? extends Serializable>> configList, String name) {
		JAXBElement<? extends Serializable> elementByName = getElementByName(configList, name);
		if (elementByName == null) {
			return false;
		}
		return "checked".equals(elementByName.getValue());
	}

	private JAXBElement<? extends Serializable> getElementByName(List<JAXBElement<? extends Serializable>> configList, String name) {
		for (JAXBElement<? extends Serializable> jaxbElement : configList) {
			if (name.equals(jaxbElement.getName().getLocalPart())) {
				return jaxbElement;
			}
		}
		return null;
	}

	protected List<String> buildPackResourceCommand(final IProject project) {
		// MessageConsoleStream messageStream = console.newMessageStream();
		// messageStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
		// MessageConsoleStream outStream = console.newMessageStream();
		// outStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
		// MessageConsoleStream errorStream = console.newMessageStream();
		// errorStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
		// MessageConsoleStream warnStream = console.newMessageStream();
		// warnStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_MAGENTA));
		List<String> command = new ArrayList<String>();

		// messageStream.println();
		// messageStream.println("Starting packing resources");

		Packageinfo packageinfo = getConfig(project);
		Resolution res = packageinfo.getResolution();
		String resulotion = (res.getWidth() != null ? res.getWidth() : "0") + "x" + (res.getHeight() != null ? res.getHeight() : "0");
		String confName = getConfigurationName(project);
		// String axfFile = project.getFile(new Path(confName).append(project.getName() +
		// ".axf")).getLocation().toOSString();
		String axfFile = project.getFile(new Path(/* confName).append( */project.getName() + ".axf")).getLocation().toOSString();
		String outFile = project.getFile(new Path(project.getName() + ".pkd")).getLocation().toOSString();

		String endsWith = ".vcproj";
		List<IPath> files = getFilesWithExt(project, endsWith);
		if (files.isEmpty()) {
			// errorStream.println("No *.vcproj file in project " + project.getName());
			return null;
		} else if (files.size() > 1) {
			// warnStream.println("More then one *.vcproj file in project: " + project.getName() + " using: " +
			// files.get(0).toOSString());
		}

		String projFile = project.getFile(files.get(0)).getLocation().toOSString();
		String toolPath = getEnvVariable(project, LinkItConst.TOOL_PATH);

		IPath packCmd = new Path(toolPath).append(getEnvVariable(project, LinkItConst.RES_EDITOR));
		// [pack, -silent, -resolution, 128x160, -o, "C:/dev/ws-LinkIt2_0/Henrik\Henrik.pkd", -e, AXF, -vom,
		// "C:/dev/ws-LinkIt2_0/Henrik\Henrik.proj", "C:/dev/ws-LinkIt2_0/Henrik\Henrik.axf"]
		// pack -resolution 128x160 -o "C:\dev\ws\runtime-EclipseApplication\test\Default\test.pkd" -e AXF -vom
		// "C:\dev\ws\runtime-EclipseApplication\test\test.vcproj"
		// "C:\dev\ws\runtime-EclipseApplication\test\Default\test.axf"
		command.add(setQuotes(packCmd.toOSString()));
		command.add("pack");
		command.add("-silent");
		command.add("-resolution");
		command.add(resulotion);
		command.add("-o");
		command.add(setQuotes(outFile));
		command.add("-e");
		command.add("AXF");
		command.add("-vom");
		command.add(setQuotes(projFile));
		command.add(setQuotes(axfFile));
		return command;
	}

	protected Packageinfo getConfig(final IProject project) {
		JAXBContext jaxbContext;
		Packageinfo packageinfo;
		File file = new File(project.getFile("config.xml").getLocationURI());
		try {
			jaxbContext = JAXBContext.newInstance(Packageinfo.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			packageinfo = (Packageinfo) jaxbUnmarshaller.unmarshal(file);
		} catch (JAXBException e) {
			// errorStream.println("Error reading config.xml file in project: " + project.getName());
			return null;
		}
		return packageinfo;
	}

	protected String setQuotes(String s) {
		return '"' + s + '"';
	}

	protected void executCommand(final IProject project, final String jobName) {
		// final MessageConsole console = this.console;
		Job job = new Job(jobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					project.refreshLocal(1, monitor);

					if (isStaticLib(project)) {
						try {
							project.refreshLocal(1, monitor);
							IFile staticLib = project.getFile(new Path(project.getName()+".a"));
							if (staticLib.exists()) {
								staticLib.delete(true, monitor);
								project.refreshLocal(1, monitor);
							}
							IFile defaultLib = project.getFile(new Path("Default").append(project.getName() ));
							if (defaultLib.exists()) {
								defaultLib.copy(staticLib.getFullPath(), true, monitor);

							}
							project.refreshLocal(2, monitor);
						} catch (CoreException e) {
							Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, jobName + ", Failed to rename file " + project.getName() + ".vxp", e));
						}

						return Status.OK_STATUS;
					}

					String confName = getConfigurationName(project);
					IFile axfFileOrg = project.getFile(new Path(confName).append(project.getName() + ".axf"));
					IFile axfFile = project.getFile(new Path(project.getName() + ".axf"));
					if (axfFile.exists()) {
						axfFile.delete(true, monitor);
					}
					if (axfFileOrg.exists()) {
						axfFileOrg.copy(axfFile.getFullPath(), true, monitor);
					} else {
						Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, jobName + ", Failed to find file " + axfFile.getFullPath()));
						return Status.OK_STATUS;
					}

					IFolder armFolder = project.getFolder("arm");
					boolean createdArm = false;
					if(!armFolder.exists()){
						armFolder.create(true, true, monitor);
						createdArm =true;
					}
					List<String> command = buildPackResourceCommand(project);
					int ret = runConsoledCommand(console, command, monitor, project);
					if (axfFile.exists()) {
						axfFile.delete(true, monitor);
					}
					if(createdArm){
						armFolder.delete(true, monitor);
					}
					if (ret != 0) {
						Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Pack Resource, Command returned an error code: " + ret));
					} else {
						command = buildPackDigistCommand(project);
						ret = runConsoledCommand(console, command, monitor, project);
						if (ret != 0) {
							Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Digist Packer Command Shell, Command returned an error code: "
									+ ret));
						}
					}
					// if (armDir.exists()) {
					// armDir.delete(true, monitor);
					// }
				} catch (IOException e) {
					Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, jobName + ", Problem while executing command.", e));
					return Status.OK_STATUS;
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, jobName + ", Failed to copy file " + project.getName() + ".axf", e));
					return Status.OK_STATUS;
				}
				try {
					project.refreshLocal(1, monitor);
					IFile defaultVxp = project.getFile(new Path("Default.vxp"));
					if (defaultVxp.exists()) {
						defaultVxp.delete(true, monitor);
						project.refreshLocal(1, monitor);
					}
					IFile projectVxp = project.getFile(new Path(project.getName() + ".vxp"));
					if (projectVxp.exists()) {
						projectVxp.move(defaultVxp.getFullPath(), true, monitor);

					}
					project.refreshLocal(2, monitor);
				} catch (CoreException e) {
					Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, jobName + ", Failed to rename file " + project.getName() + ".vxp", e));
				}

				return Status.OK_STATUS;

			}

		};
		job.setRule(null);
		job.setPriority(Job.LONG);
		job.setUser(true);
		job.schedule();
	}

	private boolean isStaticLib(IProject project) {
		Packageinfo packageinfo = getConfig(project);
		if (packageinfo == null) {
			return false;
		}
		Output output = packageinfo.getOutput();
		if (output == null) {
			return false;
		}
		BigInteger type = output.getType();
		if (type == null) {
			return false;
		}
		return type.intValue() == 3;
	}

	protected int runConsoledCommand(MessageConsole console, List<String> command, IProgressMonitor monitor, IProject project) throws IOException {
		ExternalCommandLauncher launcher = new ExternalCommandLauncher(command, project);
		launcher.setConsole(console);
		launcher.redirectErrorStream(true);
		return launcher.launch(monitor);
	}

	protected String getEnvVariable(final IProject project, String varName) {
		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		ICProjectDescription projectDescription = CCorePlugin.getDefault().getProjectDescriptionManager().getProjectDescription(project);

		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		ICConfigurationDescription defaultConfigDescription = projectDescription.getDefaultSettingConfiguration();

		IEnvironmentVariable variable = contribEnv.getVariable(varName, defaultConfigDescription);
		return variable.getValue();
	}

	protected String getConfigurationName(final IProject project) {
		ICProjectDescription projectDescription = CCorePlugin.getDefault().getProjectDescriptionManager().getProjectDescription(project);
		return projectDescription.getActiveConfiguration().getName();
	}

}
