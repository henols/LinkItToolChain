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
package se.aceone.mediatek.linkit.toolchain;

import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyInfo;
import org.eclipse.cdt.managedbuilder.makegen.gnu.DefaultGCCDependencyCalculator2;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

public class LinkItDependencyCalculator extends DefaultGCCDependencyCalculator2 {
	
	
	@Override
	public int getCalculatorType() {
		return TYPE_NODEPENDENCIES;
	}
	
	@Override
	public IManagedDependencyInfo getDependencySourceInfo(IPath source, IResource resource, IBuildObject buildContext, ITool tool, IPath topBuildDirectory) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator2#getDependencySourceInfo(org.eclipse.core.runtime.IPath, org.eclipse.cdt.managedbuilder.core.IBuildObject, org.eclipse.cdt.managedbuilder.core.ITool, org.eclipse.core.runtime.IPath)
	 */
	@Override
	public IManagedDependencyInfo getDependencySourceInfo(IPath source, IBuildObject buildContext, ITool tool, IPath topBuildDirectory) {
		return null;
	}


}
