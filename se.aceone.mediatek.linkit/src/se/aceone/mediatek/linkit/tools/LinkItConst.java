package se.aceone.mediatek.linkit.tools;

public interface LinkItConst {

	static final String LINK_IT_SDK20 = "LinkItSDK20".toUpperCase();
	static final String INCLUDE = "INICLUDE";
	static final String LIBRARY = "LIBRARY";
	static final String GCCLOCATION = "GCCLOCATION";

	static final String SIZETOOL = "SIZETOOL";
	static final String ARM_NONE_EABI_SIZE = "arm-none-eabi-size";

	static final String LINKIT10 = "LINKIT10";
	static final String TOOL_PATH = "TOOLPATH";

	static final String ARM_NONE_EABI_THUMB = "ARMNONEEABITHUMB";

	static final String DEV_BOARD = "DEV_BOARD";
	static final String HDK_LINKIT_ONE_V1 = "__HDK_LINKIT_ONE_V1__";
	static final String HDK_MT2502A_DEV_BOARD = "__HDK_MT2502A_DEV_BOARD__";
	static final String HDK_MT2502D_DEV_BOARD = "__HDK_MT2502D_DEV_BOARD__";

	static final String[] DEV_BOARDS = { HDK_LINKIT_ONE_V1, HDK_MT2502A_DEV_BOARD, HDK_MT2502D_DEV_BOARD };

	// General stuff
	public static final String PluginStart = "se.aceone.mediatek.linkit.";
	public static final String CORE_PLUGIN_ID = PluginStart + "core";

	// build configurations
	static final String LINKIT_CONFIGURATION = "se.aceone.mediatek.linkit.configuration";
	static final String LINKIT_CONFIGURATION_NAME = "Default";
	static final String LINKIT_DEFAULT_TOOL_CHAIN = "se.aceone.mediatek.linkit.toolChain.default";

	// natures
	public static final String Cnatureid = "org.eclipse.cdt.core.cnature";
	public static final String CCnatureid = "org.eclipse.cdt.core.ccnature";
	public static final String Buildnatureid = "org.eclipse.cdt.managedbuilder.core.managedBuildNature";
	public static final String Scannernatureid = "org.eclipse.cdt.managedbuilder.core.ScannerConfigNature";
	public static final String LinkItNatureID = PluginStart + "linkitnature";

}
