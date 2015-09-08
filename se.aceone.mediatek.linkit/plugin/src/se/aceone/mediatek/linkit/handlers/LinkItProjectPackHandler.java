package se.aceone.mediatek.linkit.handlers;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import se.aceone.mediatek.linkit.common.LinkItConst;
import se.aceone.mediatek.linkit.tools.Common;

public class LinkItProjectPackHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject selectedProjects[] = se.aceone.mediatek.linkit.tools.Common.getSelectedProjects();
		switch (selectedProjects.length) {
		case 0:
			Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "No project found to pack"));
			break;
		case 1:
			final IProject buildProject = selectedProjects[0];
			Job mBuildJob = new Job("") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							LinkItProjectPackWrapper.pack(buildProject, CoreModel.getDefault().getProjectDescription(buildProject).getActiveConfiguration()
									.getName());
						}
					});
					return Status.OK_STATUS;
				}

			};
			mBuildJob.setPriority(Job.INTERACTIVE);
			mBuildJob.schedule();
			break;
		default:
			Common.log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Only 1 project should be seleted: found "
					+ Integer.toString(selectedProjects.length) + " the names are :" + selectedProjects.toString()));

		}
		return null;
	}

	class PackJobHandler extends Job {
		IProject myBuildProject = null;

		public PackJobHandler(IProject buildProject) {
			super("Pack the code of project " + buildProject.getName());
			myBuildProject = buildProject;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {

			return Status.OK_STATUS;
		}
	}
}
