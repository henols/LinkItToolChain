package se.aceone.mediatek.linkit.commad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;

import se.aceone.mediatek.linkit.xml.proj.ObjectFactory;
import se.aceone.mediatek.linkit.xml.proj.VisualStudioProject;
import se.aceone.mediatek.linkit.xml.proj.VisualStudioProject.Files;
import se.aceone.mediatek.linkit.xml.proj.VisualStudioProject.Files.Filter;
import se.aceone.mediatek.linkit.xml.proj.VisualStudioProject.Files.Filter.File;

public class UpdateProjectFile extends AbstractHandler {

	private IFile file;
	private IProject project;

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		extractProjectAndFileFromInitiatingEvent(event);
		Job job = new Job("My Job Name"){
	        @Override
	        protected IStatus run(IProgressMonitor monitor){
	        	try {
					updateProjectFile(monitor);
				} catch (JAXBException e) {
					e.printStackTrace();
	                MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Error reading project file", e.getMessage());
				} catch (CoreException e) {
					e.printStackTrace();
					MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Error", e.getMessage());
				} catch (IOException e) {
					e.printStackTrace();
					MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Error", e.getMessage());
				}
	            monitor.done();
	            return Status.OK_STATUS;
	        }
	    };
	    job.setUser(true);
	    job.schedule();
		
		return null;
	}

	private void updateProjectFile(IProgressMonitor monitor) throws JAXBException, CoreException, IOException {
		JAXBContext 
			context = JAXBContext.newInstance(VisualStudioProject.class);
			Unmarshaller nmarshaller = context.createUnmarshaller();
			VisualStudioProject visualStudioProject = (VisualStudioProject)nmarshaller.unmarshal(file.getContents());
			Files files = visualStudioProject.getFiles();
			ObjectFactory ox = new ObjectFactory();
			
			for(Filter f : files.getFilter()) {
				String filter = f.getFilter();
				List<File> fileList = f.getFile();
				fileList.clear();
				System.out.println(f.getName() + " " + filter);
				if(f.getName().startsWith("Resource")){
					filter += ";xml";
				}
				List<String> ext = Arrays.asList(filter.split(";"));
				List<IPath> findResourcesByExtension = findResourcesByExtension(ext);
				for(IPath iPath : findResourcesByExtension) {
					String osString =iPath.toOSString();
					if(!osString.startsWith(".") && !osString.equals("config.xml")){
						File e = ox.createVisualStudioProjectFilesFilterFile();
						e.setRelativePath(osString);
						fileList.add(e );
						System.out.println(iPath);
					}
				}
			}
			Marshaller marshaller = context.createMarshaller();
			
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			marshaller.marshal(visualStudioProject, out);

			InputStream stream = new ByteArrayInputStream(out.toByteArray());

			
			if (file.exists()) {
				file.setContents(stream, true, true, monitor);
			} else {
				file.create(stream, true, monitor);
			}
			stream.close();

	}

	private List<IPath> findResourcesByExtension(final List<String> extensions) {
		final List<IPath> foundFiles = new ArrayList<IPath>();
		try {
			project.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) throws CoreException {
					if (resource.isLinked()) {
						return false;
					}
					if (resource.getType() == IResource.FILE) {
						String rfe = resource.getFileExtension();
						if (rfe != null) {
							if (extensions.contains(rfe)) {
								foundFiles.add(resource.getFullPath().makeRelativeTo(project.getFullPath()));
							}
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return foundFiles;
	}

	private boolean extractProjectAndFileFromInitiatingEvent(ExecutionEvent event) {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		// Get the active WorkbenchPage
		IWorkbenchPage activePage = window.getActivePage();

		// Get the Selection from the active WorkbenchPage page
		ISelection selection = activePage.getSelection();
		if (selection instanceof ITreeSelection) {
			TreeSelection treeSelection = (TreeSelection)selection;
			TreePath[] treePaths = treeSelection.getPaths();
			TreePath treePath = treePaths[0];

			// The TreePath contains a series of segments in our usage:
			// o The first segment is usually a project
			// o The last segment generally refers to the file

			// The first segment should be a IProject
			Object firstSegmentObj = treePath.getFirstSegment();
			project = (IProject)((IAdaptable)firstSegmentObj).getAdapter(IProject.class);
			if (project == null) {
				MessageDialog.openError(window.getShell(), "Navigator Popup", getClassHierarchyAsMsg("Expected the first segment to be IAdapatable to an IProject.\nBut got the following class hierarchy instead.", "Make sure to directly select a file.", firstSegmentObj));
				return false;
			}

			// The last segment should be an IResource
			Object lastSegmentObj = treePath.getLastSegment();
			IResource theResource = (IResource)((IAdaptable)lastSegmentObj).getAdapter(IResource.class);
			if (theResource == null) {
				MessageDialog.openError(window.getShell(), "Navigator Popup", getClassHierarchyAsMsg("Expected the last segment to be IAdapatable to an IResource.\nBut got the following class hierarchy instead.", "Make sure to directly select a file.", firstSegmentObj));
				return false;
			}

			// As the last segment is an IResource we should be able to get an IFile reference from it
			file = (IFile)((IAdaptable)lastSegmentObj).getAdapter(IFile.class);

			return true;
		} else {
			String selectionClass = selection.getClass().getSimpleName();
			MessageDialog.openError(window.getShell(), "Unexpected Selection Class", String.format("Expected a TreeSelection but got a %s instead.\nProcessing Terminated.", selectionClass));
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	private static String getClassHierarchyAsMsg(String msgHeader, String msgTrailer, Object theObj) {
		String msg = msgHeader + "\n\n";

		Class theClass = theObj.getClass();
		while (theClass != null) {
			msg = msg + String.format("Class=%s\n", theClass.getName());
			Class[] interfaces = theClass.getInterfaces();
			for(Class theInterface : interfaces) {
				msg = msg + String.format("    Interface=%s\n", theInterface.getName());
			}
			theClass = theClass.getSuperclass();
		}

		msg = msg + "\n" + msgTrailer;

		return msg;
	}
}
