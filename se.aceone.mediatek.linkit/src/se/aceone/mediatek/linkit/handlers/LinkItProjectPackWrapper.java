package se.aceone.mediatek.linkit.handlers;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import se.aceone.mediatek.linkit.common.ExternalCommandLauncher;
import se.aceone.mediatek.linkit.common.LinkItConst;
import se.aceone.mediatek.linkit.tools.Common;

public class LinkItProjectPackWrapper {

	static LinkItProjectPackWrapper PACK_WRAPPER = null;
	MessageConsole console = null;

	private LinkItProjectPackWrapper() {
		// no constructor needed
	}

	static private LinkItProjectPackWrapper getUploadSketchWrapper() {
		if (PACK_WRAPPER == null) {
			PACK_WRAPPER = new LinkItProjectPackWrapper();
		}
		return PACK_WRAPPER;
	}

	static public void upload(IProject project, String cConf) {
		getUploadSketchWrapper().internalUpload(project, cConf);
	}

	public static MessageConsole findConsole(String name) {
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

	public void internalUpload(final IProject project, String cConf) {

		// Check that we have a AVR Project
		// try {
		if (project == null /*
							 * || !Project.hasNature(LinkItConst.ArduinoNatureID
							 * )
							 */) {
			Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "The current selected project is not an arduino sketch", null));
			return;
		}
		// } catch (CoreException e) {
		// // Log the Exception
		// Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID,
		// "Can't access project nature", e));
		// }

		// String UpLoadTool = Common.getBuildEnvironmentVariable(project,
		// cConf, LinkItConst.ENV_KEY_upload_tool, "");
		// String MComPort = Common.getBuildEnvironmentVariable(project, cConf,
		// LinkItConst.ENV_KEY_JANTJE_COM_PORT, "");
		console = findConsole("LinkIt tool console");
//		console.clearConsole();
		console.activate();
		MessageConsoleStream messageStream = console.newMessageStream();
		messageStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
		MessageConsoleStream outStream = console.newMessageStream();
		outStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
		MessageConsoleStream errorStream = console.newMessageStream();
		errorStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));

		messageStream.println();
		messageStream.println("Starting packing");

		final String command = "pwd";

		Job uploadjob = new Job("Pack command") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					runConsoledCommand(console, command, monitor, project);
				} catch (IOException e) {
					Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Problem while executing command.", e));
				}
				return Status.OK_STATUS;

			}
		};
		// new UploadJobWrapper(uploadJobName, Project, cConf, realUploader);
		uploadjob.setRule(null);
		uploadjob.setPriority(Job.LONG);
		uploadjob.setUser(true);
		uploadjob.schedule();

		Job job = new Job("pluginStartInitiator") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.DECORATE);
		job.schedule();

	}

	/**
	 * UploadJobWrapper stops the serial port and restarts the serial port as
	 * needed. in between it calls the real uploader IUploader
	 * 
	 * @author jan
	 * 
	 */

	protected void runConsoledCommand(MessageConsole console, String command, IProgressMonitor monitor, IProject project) throws IOException {
		ExternalCommandLauncher launcher = new ExternalCommandLauncher(command, project);
		launcher.setConsole(console);
		launcher.redirectErrorStream(true);
		launcher.launch(monitor);
	}

}
