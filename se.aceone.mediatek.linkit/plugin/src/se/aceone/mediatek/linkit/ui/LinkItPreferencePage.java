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
package se.aceone.mediatek.linkit.ui;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import se.aceone.mediatek.linkit.common.LinkItConst;

public class LinkItPreferencePage extends FieldEditorPreferencePage implements LinkItConst, IWorkbenchPreferencePage {

	private boolean mIsDirty = false;
	private StringFieldEditor developer;
	private StringFieldEditor appName;
	private StringFieldEditor appVersion;
	private StringFieldEditor libList;
	private IntegerFieldEditor appId;

	/**
	 * PropertyChange set the flag mIsDirty to false. <br/>
	 * This is needed because the default PerformOK saves all fields in the
	 * object store. Therefore I set the mIsDirty flag to true as soon as a
	 * field gets change. Then I use this flag in the PerformOK to decide to
	 * call the super performOK or not.
	 * 
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		mIsDirty = true;
		testStatus();
	}

	public LinkItPreferencePage() {
		super(org.eclipse.jface.preference.FieldEditorPreferencePage.GRID);
		setDescription("Linkit Settings for a new project");
		setPreferenceStore(new ScopedPreferenceStore(ConfigurationScope.INSTANCE, LinkItConst.LINKIT_NODE));
	}

	@Override
	public boolean isValid() {
		return testStatus();
	}

	@Override
	public boolean okToLeave() {
		return testStatus();
	}

	/**
	 * PerformOK is done when the end users presses OK on a preference page. The
	 * order of the execution of the performOK is undefined. This method saves
	 * the path variables based on the settings and removes the last used
	 * setting.<br/>
	 * 
	 * @see propertyChange
	 * 
	 * @see createFieldEditors
	 * 
	 */
	@Override
	public boolean performOk() {
		if (!testStatus())
			return false;
		if (!mIsDirty)
			return true;

		super.performOk();
		setWorkSpacePathVariables();
		return true;
	}

	/**
	 * This method sets the eclipse path variables to contain the important
	 * Arduino folders (code wise that is)
	 * 
	 * 
	 * The arduino library location in the root folder (used when importing
	 * arduino libraries) The Private library path (used when importing private
	 * libraries) The Arduino IDE root folder
	 * 
	 * 
	 */
	private void setWorkSpacePathVariables() {

//		IWorkspace workspace = ResourcesPlugin.getWorkspace();
//		IPathVariableManager pathMan = workspace.getPathVariableManager();

//		try {
//			IPath ArduinoIDEPath = Common.getArduinoIDEPathFromUserSelection(mArduinoIdePath.getStringValue());
//			pathMan.setURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB,
//					URIUtil.toURI(ArduinoIDEPath.append(ArduinoConst.LIBRARY_PATH_SUFFIX).toString()));
//			pathMan.setURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB, URIUtil.toURI(mArduinoPrivateLibPath.getStringValue()));
//			pathMan.setURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO, URIUtil.toURI(ArduinoIDEPath));
//		} catch (CoreException e) {
//			Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
//					"Failed to create the workspace path variables. The setup will not work properly", e));
//			e.printStackTrace();
//		}
	}

	@Override
	public void init(IWorkbench workbench) {
		// nothing to do
	}

	/**
	 * createFieldEditors creates the fields to edit. <br/>
	 */
	@Override
	protected void createFieldEditors() {
		final Composite parent = getFieldEditorParent();

//		mArduinoIdePath = new MyDirectoryFieldEditor(ArduinoConst.KEY_ARDUINOPATH, "Arduino IDE path", parent);
//
//		addField(mArduinoIdePath.getfield());
//
//		String LibPath = LinkItPreferences.getPrivateLibraryPath();
//		if (LibPath.isEmpty()) {
//			String libraryPath = Common.getDefaultPrivateLibraryPath();
//			new File(libraryPath).mkdirs();
//			LinkItPreferences.setPrivateLibraryPath(libraryPath);
//		}
//		mArduinoPrivateLibPath = new DirectoryFieldEditor(ArduinoConst.KEY_PRIVATE_LIBRARY_PATH, "Private Library path", parent);
//		addField(mArduinoPrivateLibPath);
//
//		LibPath = LinkItPreferences.getPrivateHardwarePath();
//		if (LibPath.isEmpty()) {
//			String hardwarePath = Common.getDefaultPrivateHardwarePath();
//			new File(hardwarePath).mkdirs();
//			LinkItPreferences.setPrivateHardwarePath(hardwarePath);
//		}
//		mArduinoPrivateHardwarePath = new DirectoryFieldEditor(ArduinoConst.KEY_PRIVATE_HARDWARE_PATH, "Private hardware path", parent);
//		addField(mArduinoPrivateHardwarePath);

		Dialog.applyDialogFont(parent);
		
		IPreferenceStore preferenceStore = getPreferenceStore();
		preferenceStore.setDefault(DEVELOPER, "aceOne IoT");
		preferenceStore.setDefault(APP_NAME, "Test");
		preferenceStore.setDefault(APP_VERSION, "1.0.0");
		preferenceStore.setDefault(APP_ID, -1);
		preferenceStore.setDefault(DEFAULT_LIB_LIST, "Mediatek; Standard; Memory; Framework; Comman");
		
		developer = new StringFieldEditor(LinkItConst.DEVELOPER, "Developer", parent);
		addField(developer);
		developer.setEnabled(true, parent);
		appName = new StringFieldEditor(LinkItConst.APP_NAME, "Application Name", parent);
		addField(appName);
		appName.setEnabled(true, parent);
		appVersion = new StringFieldEditor(LinkItConst.APP_VERSION, "Application Version", parent);
		addField(appVersion);
		appVersion.setEnabled(true, parent);
		appId= new IntegerFieldEditor(LinkItConst.APP_ID, "Application id", parent);
		appId.setValidRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
		addField(appId);
		appId.setEnabled(true, parent);
		libList= new StringFieldEditor(LinkItConst.DEFAULT_LIB_LIST, "Default libarary list", parent);
		addField(libList);
		libList.setEnabled(true, parent);
//		String[][] buildBeforeUploadOptions = new String[][] { { "Ask every upload", "ASK" }, { "Yes", "YES" }, { "No", "NO" } };
//		mArduinoBuildBeforeUploadOption = new ComboFieldEditor(ArduinoConst.KEY_BUILD_BEFORE_UPLOAD_OPTION, "Build before upload?", buildBeforeUploadOptions,
//				parent);
//		addField(mArduinoBuildBeforeUploadOption);

	}

	/**
	 * testStatus test whether the provided information is OK. Here the code
	 * checks whether there is a hardware\arduino\board.txt file under the
	 * provide path.
	 * 
	 * @return true if the provided info is OK; False if the provided info is
	 *         not OK
	 * 
	 */
	private boolean testStatus() {
		String ErrorMessage = "";
//		String Seperator = "";

		// Validate the arduino path
//		IPath arduinoFolder = Common.getArduinoIDEPathFromUserSelection(mArduinoIdePath.getStringValue());
//		File arduinoBoardFile = arduinoFolder.append(ArduinoConst.LIB_VERSION_FILE).toFile();
//		boolean isArduinoFolderValid = arduinoBoardFile.canRead();
//		if (isArduinoFolderValid) {
//			IPath BoardFile = Common.getArduinoIDEPathFromUserSelection(mArduinoIdePath.getStringValue());
//			if (!BoardFile.equals(mPrefBoardFile)) {
//				mPrefBoardFile = BoardFile;
//				appVersion.setStringValue(ArduinoHelpers.GetIDEVersion(BoardFile));
//			}
//		} else {
//			ErrorMessage += Seperator + "Arduino folder is not correct!";
//			Seperator = "/n";
//		}
//
//		// Validate the private lib path
//		Path PrivateLibFolder = new Path(mArduinoPrivateLibPath.getStringValue());
//		boolean isArduinoPrivateLibFolderValid = PrivateLibFolder.toFile().canRead();
//		if (!isArduinoPrivateLibFolderValid) {
//			ErrorMessage += Seperator + "Private library folder is not correct!";
//			Seperator = "/n";
//		}

		// report status
//		if (isArduinoFolderValid && isArduinoPrivateLibFolderValid) {
//			setErrorMessage(null);
//			setValid(true);
//			return true;
//		}
		setErrorMessage(ErrorMessage);
		setValid(true);
		return true;
	}

}
