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
package se.aceone.mediatek.linkit.tools;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;

public class LinkIt20Helper extends LinkItHelper {

	static final String LINK_IT_SDK20_CAMMEL_CASE = "LinkItSDK20";
	public static final String LINK_IT_SDK20 = LINK_IT_SDK20_CAMMEL_CASE.toUpperCase();

	public LinkIt20Helper(IProject project) {
		super(project);
	}
	
	public String getEnvironmentPath() {
		String linkitEnv = System.getenv().get(LINK_IT_SDK20_CAMMEL_CASE);
		if (linkitEnv == null) {
			linkitEnv = System.getenv().get(LINK_IT_SDK20);
		}
		return linkitEnv;
	}
	
	@Override
	public String getCompilerPath() {
		return new Path(getEnvironmentPath()).append(getGccLocation()).toPortableString();
	}

}
