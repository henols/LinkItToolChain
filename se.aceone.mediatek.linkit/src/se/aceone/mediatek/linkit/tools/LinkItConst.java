package se.aceone.mediatek.linkit.tools;

/**
 * ArduinoConst only contains global strings used in the eclipse plugin.
 * 
 * @author Jan Baeyens
 */
public interface LinkItConst {

	public static final String LINK_IT_SDK20 = "LinkItSDK20".toUpperCase();
	static final String LINKIT10 = "LINKIT10";

	static final String ARM_NONE_EABI_THUMB = "ARMNONEEABITHUMB";

	static final String DEV_BOARD = "DEV_BOARD";
	static final String HDK_LINKIT_ONE_V1 = "__HDK_LINKIT_ONE_V1__";
	static final String HDK_MT2502A_DEV_BOARD = "__HDK_MT2502A_DEV_BOARD__";
	static final String HDK_MT2502D_DEV_BOARD = "__HDK_MT2502D_DEV_BOARD__";
	
	static final String[] DEV_BOARDS = { HDK_LINKIT_ONE_V1, HDK_MT2502A_DEV_BOARD, HDK_MT2502D_DEV_BOARD };
	
	// General stuff
	public static final String PluginStart = "se.aceone.mediatek.linkit.";
	public static final String CORE_PLUGIN_ID = PluginStart + "core";

	
	// natures
	public static final String Cnatureid = "org.eclipse.cdt.core.cnature";
	public static final String CCnatureid = "org.eclipse.cdt.core.ccnature";
	public static final String Buildnatureid = "org.eclipse.cdt.managedbuilder.core.managedBuildNature";
	public static final String Scannernatureid = "org.eclipse.cdt.managedbuilder.core.ScannerConfigNature";
	public static final String LinkItNatureID = PluginStart + "linkitnature";

	// preference nodes
	public static final String NODE_ARDUINO = PluginStart + "arduino";

	// preference keys
	public static final String KEY_ARDUINO_IDE_VERSION = "Arduino IDE Version";
	public static final String KEY_ARDUINOPATH = "Arduino Path";
	public static final String KEY_PRIVATE_LIBRARY_PATH = "Private Library Path";
	public static final String KEY_PRIVATE_HARDWARE_PATH = "Private hardware Path";

	// properties keys
	public static final String KEY_LAST_USED_ARDUINOBOARD = "Arduino Board";
	public static final String KEY_LAST_USED_COM_PORT = "Arduino Port";
	public static final String KEY_LAST_USED_PROGRAMMER = "Arduino Programmer";
	public static final String KEY_LAST_USED_ARDUINO_BOARDS_FILE = "Arduino boards file";
	public static final String KEY_LAST_USED_ARDUINO_MENU_OPTIONS = "Arduino Custom Option Selections";
	public static final String KEY_LAST_USED_SCOPE_FILTER_MENU_OPTION = "Arduino scope filter on off";

	// Folder Information
	public static final String LIBRARY_PATH_SUFFIX = "libraries";
	public static final String ARDUINO_HARDWARE_FOLDER_NAME = "hardware";
	public static final String ARDUINO_CORE_FOLDER_NAME = "cores";
	public static final String DEFAULT = "Default";
	public static final String BOARDS_FILE_NAME = "boards.txt";
	public static final String PLATFORM_FILE_NAME = "platform.txt";
	public static final String LIB_VERSION_FILE = "lib/version.txt";
	public static final String VARIANTS_FOLDER = "variants";

	public static final String WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB = "ArduinoLibPath";
	public static final String WORKSPACE_PATH_VARIABLE_NAME_ARDUINO = "ArduinoPath";
	public static final String WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB = "ArduinoPivateLibPath";
	public static final String WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB = "ArduinoHardwareLibPath";
	public static final String PATH_VARIABLE_NAME_ARDUINO_PINS = "ArduinoPinPath";
	public static final String PATH_VARIABLE_NAME_ARDUINO_PLATFORM = "ArduinoPlatformPath";

	// tags to interpret the arduino input files
	public static final String BoardNameKeyTAG = "name";
	public static final String UploadToolTeensy = "teensy_reboot";
	public static final String Upload_ssh = "ssh upload";

}
