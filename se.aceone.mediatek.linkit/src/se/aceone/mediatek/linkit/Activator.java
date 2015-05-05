package se.aceone.mediatek.linkit;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin implements BundleActivator {

	private static BundleContext context;
	private static AbstractUIPlugin plugin;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext bundleContext) throws Exception {
		// super.start(context);
		plugin = this;
	}

	public static AbstractUIPlugin getDefault() {
		return plugin;
	}

	public static final String ID = "se.aceone.mediatek.linkit";
	public static final String CPU_16PX = "cpu_16px";
	public static final String CPU_32PX = "cpu_32px";
	public static final String CPU_64PX = "cpu_64px";

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		super.initializeImageRegistry(registry);
		Bundle bundle = Platform.getBundle(ID);

		ImageDescriptor myImage = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/16px-Nuvola_devices_ksim_cpu.png"), null));
		registry.put(CPU_16PX, myImage);
		myImage = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/32px-Nuvola_devices_ksim_cpu.png"), null));
		registry.put(CPU_32PX, myImage);
		myImage = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/64px-Nuvola_devices_ksim_cpu.png"), null));
		registry.put(CPU_64PX, myImage);
	}

	// AbstractUIPlugin plugin = Activator.getDefault();
	// ImageRegistry imageRegistry = plugin.getImageRegistry();
	// Image myImage = imageRegistry.get(Activator.MY_IMAGE_ID);
}
