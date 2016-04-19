package com.microsoft.wacommon.applicationinsights;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import com.microsoftopentechnologies.wacommon.Activator;


public class WebPropertyTester {
	public enum ProjExportType {
		WAR, EAR, JAR
	};
	/**
	 * Determines whether a project is a dynamic web project or not.
	 *
	 * @param object
	 *            : variable from the calling test method.
	 * @return true if the project is a dynamic web project else false.
	 * @throws CoreException
	 * @throws WindowsAzureInvalidProjectOperationException
	 */
	public static boolean isWebProj(Object object) throws CoreException {
		boolean retVal = false;
		IProject project = (IProject) object;
		if (project.isOpen()) {
			ProjExportType type = getProjectNature(project);
			if (type != null && type.equals(ProjExportType.WAR)) {
				retVal = true;
			}
		}
		return retVal;
	}

	/**
	 * Method returns nature of project.
	 * 
	 * @param proj
	 * @return
	 */
	public static ProjExportType getProjectNature(IProject proj) {
		ProjExportType type = null;
		try {
			if (proj.hasNature(Messages.natJavaEMF)
					&& proj.hasNature(Messages.natMdCore)
					&& proj.hasNature(Messages.natFctCore)
					&& proj.hasNature(Messages.natJava)
					&& proj.hasNature(Messages.natJs)) {
				type = ProjExportType.WAR;
			} else if (proj.hasNature(Messages.natFctCore)
					&& proj.hasNature(Messages.natMdCore)) {
				if (proj.hasNature(Messages.natJs)
						|| proj.hasNature(Messages.natJava)
						|| proj.hasNature(Messages.natJavaEMF)) {

					type = ProjExportType.JAR;
				} else {
					type = ProjExportType.EAR;
				}
			} else {
				type = ProjExportType.JAR;
			}
		} catch (CoreException e) {
			Activator.getDefault().log(e.getMessage(), e);
		}
		return type;
	}
}
