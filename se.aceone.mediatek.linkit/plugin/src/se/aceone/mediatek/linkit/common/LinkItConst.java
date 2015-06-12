package se.aceone.mediatek.linkit.common;


public interface LinkItConst {

	static final String LINK_IT_SDK = "LINK_IT_SDK";

	static final String INCLUDE = "INICLUDE";
	static final String LIBRARY = "LIBRARY";
	static final String GCCLOCATION = "GCCLOCATION";
	static final String COMPILER_TOOL_PATH = "COMPILERTOOLPATH";
	static final String COMPILER_TOOL_PATH_GCC = "bin";
	static final String COMPILER_TOOL_PATH_RVTC = "Programs/3.1/569/win_32-pentium";

	
	static final String SIZETOOL = "SIZETOOL";
	static final String ARM_NONE_EABI_SIZE = "arm-none-eabi-size";

	static final String LINKIT10 = "LINKIT10";
	static final String TOOL_PATH = "TOOLPATH";
	static final String COMPILER_PATH = "COMPILERPATH";

	static final String ARM_NONE_EABI_THUMB = "ARMNONEEABITHUMB";

	static final String DEV_BOARD = "DEV_BOARD";
	static final String HDK_LINKIT_ONE_V1 = "__HDK_LINKIT_ONE_V1__";
	static final String HDK_MT2502A_DEV_BOARD = "__HDK_MT2502A_DEV_BOARD__";
	static final String HDK_MT2502D_DEV_BOARD = "__HDK_MT2502D_DEV_BOARD__";

	static final String[] DEV_BOARDS_1_0 = { HDK_LINKIT_ONE_V1, HDK_MT2502A_DEV_BOARD, HDK_MT2502D_DEV_BOARD };
	static final String[] DEV_BOARDS_2_0 = { HDK_LINKIT_ONE_V1, HDK_MT2502A_DEV_BOARD, HDK_MT2502D_DEV_BOARD };

	static final String LAST_USED_BOARD_SDK_1_0 = "last used board sdk 1.0";
	static final String LAST_USED_BOARD_SDK_2_0 = "last used board sdk 2.0";

	// General stuff
	public static final String PluginStart = "se.aceone.mediatek.linkit.";
	public static final String CORE_PLUGIN_ID = PluginStart + "core";
	public static final String LINKIT_NODE = PluginStart + LINK_IT_SDK;

	// build configurations
	static final String LINKIT_CONFIGURATION = "se.aceone.mediatek.linkit.configuration";
	static final String LINKIT_CONFIGURATION_NAME = "Default";
	static final String LINKIT_DEFAULT_TOOL_CHAIN_GCC = "se.aceone.mediatek.linkit.toolChain.default.gcc";
	static final String LINKIT_DEFAULT_TOOL_CHAIN_RVCT = "se.aceone.mediatek.linkit.toolChain.default.rvct";

	// natures
	public static final String Cnatureid = "org.eclipse.cdt.core.cnature";
	public static final String CCnatureid = "org.eclipse.cdt.core.ccnature";
	public static final String Buildnatureid = "org.eclipse.cdt.managedbuilder.core.managedBuildNature";
	public static final String Scannernatureid = "org.eclipse.cdt.managedbuilder.core.ScannerConfigNature";
	public static final String LinkItNatureID = PluginStart + "linkitnature";


	// Porperties
	
	static final String DEVELOPER= "Developer";
	static final String APP_NAME ="App name";
	static final String APP_VERSION ="App version";
	static final String APP_ID = "app id";
	static final String  DEFAULT_LIB_LIST="Defaultliblist";

	
}
