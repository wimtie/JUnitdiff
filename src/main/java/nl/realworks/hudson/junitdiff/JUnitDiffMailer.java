package nl.realworks.hudson.junitdiff;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.Mailer;
import hudson.tasks.Mailer.UserProperty;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * JUnitDiffMailer
 * 
 * Check de logs van Hudson voor de unit tests en genereert en e-mailt een
 * rapport met de veranderingen in de test resultaten.
 * 
 * @version $Id: JUnitDiffMailer.java 120523 2010-12-08 11:50:09Z ronald $
 */
public final class JUnitDiffMailer {

	public boolean run(AbstractBuild<?, ?> build, BuildListener listener) {
		try {
			return run2(build, listener);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean run2(AbstractBuild<?, ?> build, BuildListener listener) throws IOException {
		final PrintStream logger = listener.getLogger();
		final String project = build.getProject().getName();
		
		AbstractTestResultAction<?> result = build.getTestResultAction();
		if (result == null) {
			logger.println("Build has no test results.");
			return false;
		}
		AbstractBuild<?, ?> prevBuild = getPreviousReallyNotFailedBuild(build);
		if (prevBuild == null) {
			logger.println("There is no previous not failed build.");
			return true;
		}
		AbstractTestResultAction<?> prevResult = prevBuild.getTestResultAction();
		if (prevResult == null) {
			logger.println("Previous build " + prevBuild.getDisplayName()
					+ " has no test results.");
			return false;
		}
		logger.println("Previous build with test results: "
				+ prevBuild.getDisplayName());

		Map<String, CaseResult> currentFailures = getFailures(result.getFailedTests());
		Map<String, CaseResult> oldFailures = getFailures(prevResult.getFailedTests());

		CollectionDiff<String> cDiff = new CollectionDiff<String>(
				oldFailures.keySet(), currentFailures.keySet());
	
		List<String> solved = cDiff.getItemsLeft();
		Collections.sort(solved);
		List<String> failed = cDiff.getItemsRight();
		Collections.sort(failed);
		
		int currentNrTests = result.getTotalCount();
		int addedTests = currentNrTests - prevResult.getTotalCount();
		int nrFailed = currentFailures.size();
		int percFailed = nrFailed * 100 / currentNrTests;

		logger.println("nrFailed == " + nrFailed);
		logger.println("solved.size() == " + solved.size());
		if (failed.isEmpty() && solved.isEmpty()) {
			logger.println("failed.isEmpty && solved.isEmpty(); no e-mail sent");
			return true;
		}

		final int nrSucces = currentNrTests - nrFailed;
		String subject = "Junit;" +
						" Tests: " + currentNrTests + " (" + plusmin(addedTests) + ")" 
						+ " Failed: " + nrFailed + " (" + plusmin(failed.size()) + ")"
						+ " Succes: " + nrSucces + " (" + plusmin(solved.size()) + ")"
						+ " (" + project + ")";

		Appendable ap = new StringBuilder();
		ap.append("http://hudson.base.nl:8080/hudson/job/" + project + "/\n");
		ap.append("Diff between:\n");
		ap.append(build.getId() + " (" + build.getDisplayName() + ") and "
				+ prevBuild.getId() + " (" + prevBuild.getDisplayName() + ")\n");
		ap.append("\n");
		ap.append("Total tests : " + currentNrTests + " (" + plusmin(addedTests) + ")\n");
		ap.append("Success     : " + (currentNrTests - nrFailed) + " (" + plusmin(solved.size()) + ")\n");
		ap.append("Failed      : " + nrFailed+ " (" + percFailed + "%) (" + plusmin(failed.size()) + ")\n");
		
		ap.append("\n");
		ap.append("Tests fixed:\n - ");
		ap.append(glue(solved, "\n - "));
		ap.append("\n\n");
		ap.append("Tests failed:\n - ");
		ap.append(glue(failed, "\n - "));
		ap.append("\n\n\n");
		ap.append("Stacktraces:\n");
		for (String test : failed) {
			ap.append(formatTest(test, currentFailures.get(test)));
			ap.append('\n');
		}

		try {
			final Session session = Session.getInstance(new Properties());
			MimeMessage m = new MimeMessage(session);
			final InternetAddress hudson =
				new InternetAddress(Mailer.DESCRIPTOR.getAdminAddress());
			m.setFrom(hudson);
			Set<User> culprits = build.getCulprits();
			addUpstreamCommitters(build, logger, culprits);
			if (!addRecipients(m, culprits, listener)) {
				m.addRecipient(RecipientType.TO, hudson);
			}
			m.setSubject(subject);
			m.setContent(ap.toString(), "text/plain");
			m.saveChanges();
			logger.println("[" + JunitDiffPlugin.DISPLAY_NAME + "]"
					+" sending e-mail to "
					+ Arrays.deepToString(m.getAllRecipients()));
			Transport transport = session.getTransport("smtp");
			transport.connect();
			transport.sendMessage(m, m.getAllRecipients());
			transport.close();
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

	private static AbstractBuild<?, ?> getPreviousReallyNotFailedBuild(
			AbstractBuild<?, ?> build) {
		AbstractBuild<?, ?> prevBuild = build.getPreviousNotFailedBuild();
		while (prevBuild != null
				&& prevBuild.getResult().isWorseThan(Result.UNSTABLE)) {
			prevBuild = prevBuild.getPreviousNotFailedBuild();
		}
		return prevBuild;
	}

	private void addUpstreamCommitters(AbstractBuild<?, ?> build,
			final PrintStream logger, Set<User> culprits) {
		Map<AbstractProject, Integer> upstreamBuilds = build.getUpstreamBuilds();
		logger.println("Upstream builds: " + upstreamBuilds);
		if (upstreamBuilds.isEmpty()) {
			logger.println("No upstream builds.");
			return;
		}
		AbstractBuild prevBuild = getPreviousReallyNotFailedBuild(build);
		Map<AbstractProject, Integer> prevUpstreamBuilds =
				prevBuild.getUpstreamBuilds();
		logger.println("Previous upstream builds: " + prevUpstreamBuilds);
		final int upstreamBuildNr = upstreamBuilds.values().iterator().next();
		logger.println("Using upstream build: " + upstreamBuildNr);
		final int prevUpstreamBuildNr;
		if (prevUpstreamBuilds.isEmpty()) {
			logger.println("No previous upstream builds.");
			prevUpstreamBuildNr = upstreamBuildNr - 1;
		} else {
			prevUpstreamBuildNr = prevUpstreamBuilds.values().iterator().next();
		}
		logger.println("Using previous upstream build: " + prevUpstreamBuildNr);
		for (int buildNr = prevUpstreamBuildNr + 1;
				buildNr <= upstreamBuildNr; buildNr++) {
			final AbstractProject upstreamProject =
					upstreamBuilds.keySet().iterator().next();
			AbstractBuild b = (AbstractBuild) upstreamProject.getBuildByNumber(buildNr);
			logger.println("Upstream build " + buildNr + " is " + b);
			if (b == null) {
				// Dit is eigenlijk niet nodig, maar dan werkt het tenminste
				// tijdens het debuggen.
				continue;
			}
			ChangeLogSet changeSet = b.getChangeSet();
			for (Object i : changeSet.getItems()) {
				Entry e = (Entry) i;
				final User author = e.getAuthor();
				logger.println("Adding upstream committer: " + author);
				culprits.add(author);
			}
		}
	}

	private boolean addRecipients(MimeMessage m, Set<User> users,
			BuildListener listener)
			throws UnsupportedEncodingException, MessagingException {
		final PrintStream logger = listener.getLogger();
		boolean somethingAdded = false;
		for (User u : users) {
			String fullName = u.getFullName();
			logger.println("Add recipient: " + u.getId() + "/" + fullName);
			UserProperty email = u.getProperty(Mailer.UserProperty.class);
			String address = Util.fixEmpty(email.getAddress());
			logger.println("Recipient address (" + u.getId() + "): " + address);			
			if (address != null) {
				final InternetAddress developer = new InternetAddress(
						address, fullName);
				m.addRecipient(RecipientType.TO, developer);
				somethingAdded = true;
			}
		}
		return somethingAdded;
	}
	
	private static String glue(List<String> stuff, String glue) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String s : stuff) {
			if (first) {
				first = false;
			} else {
				sb.append(glue);
			}
			sb.append(s);
		}
		return sb.toString();
	}

	private static String plusmin(int getal) {
		if (getal < 0) {
			return "" + getal;
		}
		return "+" + getal;
	}

	private static CharSequence formatTest(String test, CaseResult cr) {
		return test + "\n" + cr.getErrorStackTrace();
	}

	static Map<String, CaseResult> getFailures(List<CaseResult> failures) {
		Map<String, CaseResult> result = new HashMap<String, CaseResult>();
		for (CaseResult cr : failures) {
			result.put(cr.getFullName(), cr);
		}
		return result;
	}

}
