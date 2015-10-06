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
package se.aceone.mediatek.linkit.common;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

public class LinkItPreferences implements LinkItConst {

	public static String getLastUsedBoardNameSDK20() {
		return getGlobalValue(LAST_USED_BOARD_SDK_2_0);
	}

	public static String getLastUsedBoardNameSDK10() {
		return getGlobalValue(LAST_USED_BOARD_SDK_1_0);
	}

	public static void setLastUsedBoardSDK20(String boardName) {
		setGlobalValue(LAST_USED_BOARD_SDK_2_0, boardName);
	}

	public static void setLastUsedBoardSDK10(String boardName) {
		setGlobalValue(LAST_USED_BOARD_SDK_1_0, boardName);
	}

	public static String getGlobalValue(String key, String defaultValue) {
		IEclipsePreferences myScope = getPreferences();
		return myScope.get(key, defaultValue);
	}

	public static String getGlobalValue(String key) {
		return getGlobalValue(key, "");
	}

	protected static boolean getGlobalBoolean(String key) {
		return getGlobalBoolean(key, false);
	}

	protected static boolean getGlobalBoolean(String key, boolean def) {
		IEclipsePreferences myScope = getPreferences();
		return myScope.getBoolean(key, def);
	}

	private static IEclipsePreferences getPreferences() {
		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(LINKIT_NODE);
		return myScope;
	}

	protected static int getGlobalInt(String key) {
		IEclipsePreferences myScope = getPreferences();
		return myScope.getInt(key, 0);
	}

	public static void setGlobalValue(String key, String value) {

		IEclipsePreferences myScope = getPreferences();
		myScope.put(key, value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	protected static void setGlobalInt(String key, int value) {
		IEclipsePreferences myScope = getPreferences();
		myScope.putInt(key, value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	protected static void setGlobalBoolean(String key, boolean value) {
		IEclipsePreferences myScope = getPreferences();
		myScope.putBoolean(key, value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	// DEVELOPER= "Developer";
	// APP_NAME ="App name";
	// APP_VERSION ="App version";
	// APP_ID = "app id";
	// DEFAULT_LIB_LIST="Defaultliblist";

	public static String getAppName() {
		return getGlobalValue(APP_NAME);
	}

	public static void setAppName(String name) {
		setGlobalValue(APP_NAME, name);
	}

	public static int getAppId() {
		return getGlobalInt(APP_ID);
	}

	public static void setAppId(String id) {
		setGlobalValue(APP_ID, id);
	}

	public static String getAppVersion() {
		return getGlobalValue(APP_VERSION);
	}

	public static void setAppVersion(String version) {
		setGlobalValue(APP_VERSION, version);
	}

	public static String getDefaultLibraryList() {
		return getGlobalValue(DEFAULT_LIB_LIST);
	}

	public static void setDefaultLibraryList(String folderName) {
		setGlobalValue(DEFAULT_LIB_LIST, folderName);
	}

	public static String getDeveloper() {
		return getGlobalValue(DEVELOPER);
	}

	public static void setDeveloper(String name) {
		setGlobalValue(DEVELOPER, name);
	}
}
