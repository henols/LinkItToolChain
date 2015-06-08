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
