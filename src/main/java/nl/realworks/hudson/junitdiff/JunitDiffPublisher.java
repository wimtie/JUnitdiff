package nl.realworks.hudson.junitdiff;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.io.PrintStream;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @version $Id: JunitDiffPublisher.java 120522 2010-12-08 11:47:31Z ronald $
 */
public class JunitDiffPublisher extends Notifier {

	public static final BuildStepDescriptor<Publisher> DESCRIPTOR = new DescriptorImpl();

	@DataBoundConstructor
	public JunitDiffPublisher() {
		// Geen idee waarom dit hier moet staan met de @DataBoundConstructor
		// annotatie.
	}

	@Override
	public BuildStepDescriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		final PrintStream logger = listener.getLogger();
		logger.println("build: " + build);
		logger.println("launcher: " + launcher);
		logger.println("listener: " + listener);
		return new JUnitDiffMailer().run(build, listener);
	}

	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			final boolean assignableFrom =
				FreeStyleProject.class.isAssignableFrom(jobType);
			if (!assignableFrom) {
				throw new RuntimeException("jobType is niet assignable met "
						+ FreeStyleProject.class.getName());
			}
			return assignableFrom;
		}

		@Override
		public String getDisplayName() {
			return "E-mail " + JunitDiffPlugin.DISPLAY_NAME;
		}
		
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}
}
