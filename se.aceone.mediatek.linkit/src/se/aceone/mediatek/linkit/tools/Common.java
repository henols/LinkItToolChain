package se.aceone.mediatek.linkit.tools;

import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import se.aceone.mediatek.linkit.Activator;

public class Common implements LinkItConst {

	public static String makeNameCompileSafe(String name) {
		return name.trim().replace(" ", "_").replace("/", "_").replace("\\", "_").replace("(", "_").replace(")", "_").replace("*", "_").replace("?", "_").replace("%", "_").replace(".", "_").replace(":", "_").replace("|", "_").replace("<", "_").replace(">", "_").replace(",", "_").replace("\"", "_").replace("-", "_");
	}

	/**
	 * Gets a persistent project property
	 * 
	 * @param project
	 *            The project for which the property is needed
	 * @param tag
	 *            The tag identifying the property to read
	 * @return returns the property when found. When not found returns an empty
	 *         string
	 */
	public static String getPersistentProperty(IProject project, String tag) {
		try {
			String sret = project.getPersistentProperty(new QualifiedName(CORE_PLUGIN_ID, tag));
			if (sret == null) {
				sret = project.getPersistentProperty(new QualifiedName("", tag)); // for
				// downwards
				// compatibility
				if (sret == null) sret = "";
			}
			return sret;
		} catch (CoreException e) {
			log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Failed to read persistent setting " + tag, e));
			// e.printStackTrace();
			return "";
		}
	}

	public static int getPersistentPropertyInt(IProject project, String tag, int defaultValue) {
		try {
			String sret = project.getPersistentProperty(new QualifiedName(CORE_PLUGIN_ID, tag));
			if (sret == null) {
				return defaultValue;
			}
			return Integer.parseInt(sret);
		} catch (CoreException e) {
			log(new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Failed to read persistent setting " + tag, e));
			// e.printStackTrace();
			return defaultValue;
		}
	}

	/**
	 * Sets a persistent project property
	 * 
	 * @param project
	 *            The project for which the property needs to be set
	 * @param tag
	 *            The tag identifying the property to read
	 * @return returns the property when found. When not found returns an empty
	 *         string
	 */
	public static void setPersistentProperty(IProject project, String tag, String value) {
		try {
			project.setPersistentProperty(new QualifiedName(CORE_PLUGIN_ID, tag), value);
			project.setPersistentProperty(new QualifiedName("", tag), value); // for
			// downwards
			// compatibility
		} catch (CoreException e) {
			IStatus status = new Status(IStatus.ERROR, LinkItConst.CORE_PLUGIN_ID, "Failed to write properties", e);
			Common.log(status);

		}
	}

	public static void setPersistentProperty(IProject project, String Tag, int Value) {
		setPersistentProperty(project, Tag, Integer.toString(Value));
	}

	/**
	 * Logs the status information
	 * 
	 * @param status
	 *            the status information to log
	 */
	public static void log(IStatus status) {
		int style = StatusManager.LOG;

		if (status.getSeverity() == IStatus.ERROR) {
			style = StatusManager.LOG | StatusManager.SHOW | StatusManager.BLOCK;
			StatusManager stMan = StatusManager.getManager();
			stMan.handle(status, style);
		} else {
			Activator.getDefault().getLog().log(status);
		}

	}


	/**
	 * ToInt converts a string to a integer in a save way
	 * 
	 * @param number
	 *            is a String that will be converted to an integer. Number can
	 *            be null or empty and can contain leading and trailing white
	 *            space
	 * @return The integer value represented in the string based on parseInt
	 * @see parseInt. After error checking and modifications parseInt is used
	 *      for the conversion
	 **/
	public static int toInt(String number) {
		if (number == null) return 0;
		if (number.equals("")) return 0;
		return Integer.parseInt(number.trim());
	}

	private static ICConfigurationDescription[] getConfigurations(IProject prj) {
		ICProjectDescription prjd = CoreModel.getDefault().getProjectDescription(prj, false);
		if (prjd != null) {
			ICConfigurationDescription[] cfgs = prjd.getConfigurations();
			if (cfgs != null) {
				return cfgs;
			}
		}

		return new ICConfigurationDescription[0];
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}

	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getActivePage();
		}
		return null;
	}

	/**
	 * Class used to efficiently special case the scenario where there's only a
	 * single project in the workspace. See bug 375760
	 */
	private static class ImaginarySelection implements ISelection {
		private IProject fProject;

		ImaginarySelection(IProject project) {
			fProject = project;
		}

		@Override
		public boolean isEmpty() {
			return fProject == null;
		}

		IProject getProject() {
			return fProject;
		}
	}

	static HashSet<IProject> fProjects = new HashSet<IProject>();

	static public IProject[] getSelectedProjects() {
		fProjects.clear();
		getSelectedProjects(getActiveWorkbenchWindow().getSelectionService().getSelection());
		return fProjects.toArray(new IProject[fProjects.size()]);
	}

	static private void getSelectedProjects(ISelection selection) {

		boolean badObject = false;

		if (selection != null) {
			if (selection instanceof IStructuredSelection) {
				if (selection.isEmpty()) {
					// could be a form editor or something. try to get the
					// project from the active part
					IWorkbenchPage page = getActivePage();
					if (page != null) {
						IWorkbenchPart part = page.getActivePart();
						if (part != null) {
							Object o = part.getAdapter(IResource.class);
							if (o != null && o instanceof IResource) {
								fProjects.add(((IResource)o).getProject());
							}
						}
					}
				}
				Iterator<?> iter = ((IStructuredSelection)selection).iterator();
				while (iter.hasNext()) {
					Object selItem = iter.next();
					IProject project = null;
					if (selItem instanceof ICElement) {
						ICProject cproject = ((ICElement)selItem).getCProject();
						if (cproject != null) project = cproject.getProject();
					} else if (selItem instanceof IResource) {
						project = ((IResource)selItem).getProject();
					} else if (selItem instanceof IAdaptable) {
						Object adapter = ((IAdaptable)selItem).getAdapter(IProject.class);
						if (adapter != null && adapter instanceof IProject) {
							project = (IProject)adapter;
						}
					}
					// Check whether the project is CDT project
					if (project != null) {
						if (!CoreModel.getDefault().isNewStyleProject(project))
							project = null;
						else {
							ICConfigurationDescription[] tmp = getConfigurations(project);
							if (tmp.length == 0) project = null;
						}
					}
					if (project != null) {
						fProjects.add(project);
					} else {
						badObject = true;
						break;
					}
				}
			} else if (selection instanceof ITextSelection) {
				// If a text selection check the selected part to see if we can
				// find
				// an editor part that we can adapt to a resource and then
				// back to a project.
				IWorkbenchWindow window = getActiveWorkbenchWindow();
				if (window != null) {
					IWorkbenchPage page = window.getActivePage();
					if (page != null) {
						IWorkbenchPart part = page.getActivePart();
						if (part instanceof IEditorPart) {
							IEditorPart epart = (IEditorPart)part;
							IResource resource = (IResource)epart.getEditorInput().getAdapter(IResource.class);
							if (resource != null) {
								IProject project = resource.getProject();
								badObject = !(project != null && CoreModel.getDefault().isNewStyleProject(project));

								if (!badObject) {
									fProjects.add(project);
								}
							}
						}
					}
				}

			} else if (selection instanceof ImaginarySelection) {
				fProjects.add(((ImaginarySelection)selection).getProject());
			}
		}

		if (!badObject && !fProjects.isEmpty()) {
			Iterator<IProject> iter = fProjects.iterator();
			ICConfigurationDescription[] firstConfigs = getConfigurations(iter.next());
			if (firstConfigs != null) {
				for(ICConfigurationDescription firstConfig : firstConfigs) {
					boolean common = true;
					Iterator<IProject> iter2 = fProjects.iterator();
					while (iter2.hasNext()) {
						ICConfigurationDescription[] currentConfigs = getConfigurations(iter2.next());
						int j = 0;
						for(; j < currentConfigs.length; j++) {
							if (firstConfig.getName().equals(currentConfigs[j].getName())) break;
						}
						if (j == currentConfigs.length) {
							common = false;
							break;
						}
					}
					if (common) {
						break;
					}
				}
			}
		}
		// action.setEnabled(enable);

		// Bug 375760
		// If focus is on a view that doesn't provide a resource/project
		// context. Use the selection in a
		// project/resource view. We support three views. If more than one is
		// open, nevermind. If there's only
		// one project in the workspace and it's a CDT one, use it
		// unconditionally.
		//
		// Note that whatever project we get here is just a candidate; it's
		// tested for suitability when we
		// call ourselves recursively
		//
		if (badObject || fProjects.isEmpty()) {
			// Check for lone CDT project in workspace
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			if (projects != null && projects.length == 1) {
				IProject project = projects[0];
				if (CoreModel.getDefault().isNewStyleProject(project) && (getConfigurations(project).length > 0)) {
					getSelectedProjects(new ImaginarySelection(project));
					return;
				}
			}

			// Check the three supported views
			IWorkbenchPage page = getActivePage();
			int viewCount = 0;
			if (page != null) {
				IViewReference theViewRef = null;
				IViewReference viewRef = null;

				theViewRef = page.findViewReference("org.eclipse.cdt.ui.CView"); //$NON-NLS-1$
				viewCount += (theViewRef != null) ? 1 : 0;

				viewRef = page.findViewReference("org.eclipse.ui.navigator.ProjectExplorer"); //$NON-NLS-1$
				viewCount += (viewRef != null) ? 1 : 0;
				theViewRef = (theViewRef == null) ? viewRef : theViewRef;

				viewRef = page.findViewReference("org.eclipse.ui.views.ResourceNavigator"); //$NON-NLS-1$
				viewCount += (viewRef != null) ? 1 : 0;
				theViewRef = (theViewRef == null) ? viewRef : theViewRef;

				if (theViewRef != null && viewCount >= 1) {
					IViewPart view = theViewRef.getView(false);
					if (view != null) {
						ISelection cdtSelection = view.getSite().getSelectionProvider().getSelection();
						if (cdtSelection != null) {
							if (!cdtSelection.isEmpty()) {
								if (!cdtSelection.equals(selection)) { // avoids
									// infinite
									// recursion
									getSelectedProjects(cdtSelection);
									return;
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Provides the build environment variable based on project and string This
	 * method does not add any knowledge.(like adding A.)
	 * 
	 * @param project
	 *            the project that contains the environment variable
	 * @param envName
	 *            the key that describes the variable
	 * @param defaultvalue
	 *            The return value if the variable is not found.
	 * @return The expanded build environment variable
	 */
	static public String getBuildEnvironmentVariable(IProject project, String configName, String envName, String defaultvalue) {
		ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project);
		return getBuildEnvironmentVariable(prjDesc.getConfigurationByName(configName), envName, defaultvalue);
	}

	/**
	 * Provides the build environment variable based on project and string This
	 * method does not add any knowledge.(like adding A.)
	 * 
	 * @param project
	 *            the project that contains the environment variable
	 * @param envName
	 *            the key that describes the variable
	 * @param defaultvalue
	 *            The return value if the variable is not found.
	 * @return The expanded build environment variable
	 */
	static public String getBuildEnvironmentVariable(ICConfigurationDescription configurationDescription, String envName, String defaultvalue) {
		return getBuildEnvironmentVariable(configurationDescription, envName, defaultvalue, true);
	}

	static public String getBuildEnvironmentVariable(ICConfigurationDescription configurationDescription, String envName, String defaultvalue, boolean expanded) {
		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		try {
			return envManager.getVariable(envName, configurationDescription, expanded).getValue();
		} catch (Exception e) {// ignore all errors and return the default value
		}
		return defaultvalue;
	}
	
}
