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

package se.aceone.mediatek.linkit;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin implements BundleActivator {

	// private static BundleContext context;
	private static AbstractUIPlugin plugin;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext )
	 */
	public void start(BundleContext bundleContext) throws Exception {
		// super.start(context);
		plugin = this;
	}

	public static AbstractUIPlugin getDefault() {
		return plugin;
	}

	public static final String ID = "se.aceone.mediatek.linkit.toolchain";
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
