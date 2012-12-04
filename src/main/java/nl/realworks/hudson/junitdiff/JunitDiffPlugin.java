package nl.realworks.hudson.junitdiff;

import hudson.Plugin;
import hudson.tasks.BuildStep;

/**
 * @version $Id: JunitDiffPlugin.java 90694 2009-04-22 14:20:39Z ronald $
 */
public class JunitDiffPlugin extends Plugin {

	public static final String DISPLAY_NAME = "JUnit Diff";

    @Override
	public void start() throws Exception {
        BuildStep.PUBLISHERS.addRecorder(JunitDiffPublisher.DESCRIPTOR);
    }

}
