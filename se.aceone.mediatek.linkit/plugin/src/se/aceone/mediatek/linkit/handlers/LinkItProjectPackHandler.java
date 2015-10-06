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
