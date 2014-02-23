package de.apaxo.decide.business;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.faces.application.FacesMessage;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;

import de.apaxo.decide.entities.Decision;
import de.apaxo.decide.entities.DecisionStatus;
import de.apaxo.decide.entities.Decision_;
import de.apaxo.decide.entities.Email2Id;
import de.apaxo.decide.entities.Email2Id_;
import de.apaxo.decide.ui.faces.ResponseCatcher;

/**
 * This class implements the business logic to save a decision and send the
 * necessary reminder to the person who should take the decision.
 * 
 * @author Manuel Blechschmidt <blechschmidt@apaxo.de>
 * 
 */
@Stateless
public class DecisionManager {

	private static final Logger log = Logger.getLogger(DecisionManager.class
			.getName());

	@PersistenceContext
	EntityManager em;

	@Resource
	TimerService timerService;

	@Resource(name = "Mail")
	Session mailSession;

	public void processDecision(Decision decision) throws MessagingException {
		log.fine("Sending decision: " + decision.getWhat() + " from "
				+ decision.getFrom() + " to " + decision.getWho());
		decision.setId(UUID.randomUUID().toString());
		scheduleReminder(decision);

		em.persist(decision);

		sendDecisionEmail(decision, decision.getFrom()
				+ " wants you to take a decision");

	}

	/**
	 * Sends the decision email to the receiver.
	 * 
	 * @param decision
	 * @param subject
	 * @throws MessagingException
	 */
	private void sendDecisionEmail(Decision decision, String subject)
			throws MessagingException {
		MimeMessage whoMail = new MimeMessage(mailSession);

		InternetAddress from = new InternetAddress();
		from.setAddress(decision.getFrom());

		// Try to find the id for the email, if not found create it
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Email2Id> cq = cb.createQuery(Email2Id.class);
		Root<Email2Id> mail2IdRoot = cq.from(Email2Id.class);
		cq.where(cb.equal(mail2IdRoot.get(Email2Id_.address),
				decision.getFrom()));

		cq.select(mail2IdRoot);
		TypedQuery<Email2Id> q = em.createQuery(cq);
		List<Email2Id> email2Id = q.getResultList();
		if (email2Id.size() == 0) {
			Email2Id newEmail2Id = new Email2Id();
			newEmail2Id.setId(UUID.randomUUID().toString());
			newEmail2Id.setAddress(decision.getFrom());
			em.persist(newEmail2Id);
		}

		InternetAddress who = new InternetAddress();
		who.setAddress(decision.getWho());
		whoMail.addFrom(new Address[] { from });
		whoMail.addRecipient(RecipientType.TO, who);
		// whoMail.addRecipient(RecipientType.CC, from);

		whoMail.setSubject(subject, "UTF-8");

		MimeMultipart mp = new MimeMultipart("alternative");

		BodyPart whoMailTextPart = new MimeBodyPart();

		Map<String, String> params = new HashMap<String, String>();
		params.put("decisionId", decision.getId());

		String htmlContent = capture("/decide-email.xhtml", params);

		String url = FacesContext.getCurrentInstance().getExternalContext()
				.getRequest().toString();
		// get everything before the last /
		// e.g. http://www.example.com/foo/bar.jsf
		// matches 1. http://www.example.com/foo/
		Pattern everythingWithoutLast = Pattern.compile("^(.*?)[^/]*$");
		Matcher everythingWithoutLastMatcher = everythingWithoutLast
				.matcher(url);
		everythingWithoutLastMatcher.matches();
		String serverUrl = everythingWithoutLastMatcher.group(1);

		htmlContent = htmlContent.replaceAll("\"//", "\"http://");
		htmlContent = htmlContent.replaceAll("__SERVERURL__", serverUrl);

		// Fill the multi part with plain content
		String noHTMLString = getPlainTextFromHtml(htmlContent);

		whoMailTextPart.setContent(noHTMLString, "text/plain");
		whoMailTextPart.setHeader("MIME-Version", "1.0");
		whoMailTextPart.setHeader("Content-Type", "text/plain");
		mp.addBodyPart(whoMailTextPart);

		// Create the whoMail part
		BodyPart whoMailHtmlPart = new MimeBodyPart();

		// Fill the multi part with html content
		whoMailHtmlPart.setContent(htmlContent, "text/html; charset=UTF-8");
		whoMailHtmlPart.setHeader("MIME-Version", "1.0");
		whoMailHtmlPart.setHeader("Content-Type", "text/html; charset=UTF-8");
		mp.addBodyPart(whoMailHtmlPart);

		// set the multi part for the whoMail
		whoMail.setContent(mp);

		whoMail.setHeader("MIME-Version", "1.0");
		whoMail.setHeader("Content-Type", mp.getContentType());
		whoMail.setHeader("X-Mailer", "semRecSys Apaxo GmbH");

		whoMail.setSentDate(new Date());

		whoMail.saveChanges();

		Transport.send(whoMail);
	}

	/**
	 * Schedules the reminder to the next day.
	 * 
	 * @param decision
	 */
	private void scheduleReminder(Decision decision) {
		Calendar nextReminderDate = decision.getNextReminderDate();
		// send next reminder tomorrow
		nextReminderDate.add(Calendar.DAY_OF_YEAR, 1);
		decision.setNextReminderDate(nextReminderDate);

		TimerConfig timerConfig = new TimerConfig();
		timerConfig.setInfo(decision.getId());
		timerService.createSingleActionTimer(nextReminderDate.getTime(),
				timerConfig);
	}

	/**
	 * Returns a decision by id.
	 * 
	 * @param id
	 *            the id to search
	 * @return the decision found with that id
	 */
	public Decision get(String id) {
		return em.find(Decision.class, id);
	}

	/**
	 * Removes the html tags from the text.
	 * 
	 * @param htmlContent
	 * @return
	 */
	private String getPlainTextFromHtml(String htmlContent) {
		return Jsoup.parse(htmlContent).text();
	}

	/**
	 * Render a template in memory and return the content as a string. The
	 * request parameter 'emailClient' is set to true during rendering. This
	 * method relies on a FacesContext for Facelets templating so it only works
	 * when the app is deployed.
	 * http://www.ninthavenue.com.au/how-to-create-email-from-jsf-templates
	 */
	public static String capture(String template, Map<String, String> params) {

		// setup a response catcher
		FacesContext faces = FacesContext.getCurrentInstance();
		ExternalContext context = faces.getExternalContext();
		ServletRequest request = (ServletRequest) faces.getExternalContext()
				.getRequest();
		HttpServletResponse response = (HttpServletResponse) context
				.getResponse();
		ResponseCatcher catcher = new ResponseCatcher(response);

		// hack the request state
		UIViewRoot oldView = faces.getViewRoot();
		Map<String, Object> oldAttributes = null;
		if (params != null) {
			oldAttributes = new HashMap<String, Object>(params.size() * 2); // with
																			// buffer
			for (String key : (Set<String>) params.keySet()) {
				oldAttributes.put(key, request.getAttribute(key));
				request.setAttribute(key, params.get(key));
			}
		}
		request.setAttribute("emailClient", true);
		context.setResponse(catcher);

		try {
			// build a JSF view for the template and render it
			ViewHandler views = faces.getApplication().getViewHandler();
			UIViewRoot view = views.createView(faces, template);
			faces.setViewRoot(view);
			views.getViewDeclarationLanguage(faces, template).buildView(faces,
					view);
			views.renderView(faces, view);
		} catch (IOException ioe) {
			String msg = "Failed to render " + template;
			faces.addMessage(null, new FacesMessage(
					FacesMessage.SEVERITY_ERROR, msg, msg));
			return null;
		} finally {

			// restore the request state
			if (oldAttributes != null) {
				for (String key : (Set<String>) oldAttributes.keySet()) {
					request.setAttribute(key, oldAttributes.get(key));
				}
			}
			request.setAttribute("emailClient", null);
			context.setResponse(response);
			faces.setViewRoot(oldView);
		}
		return catcher.toString();
	}

	/**
	 * Answer the decision with no.
	 * 
	 * @param decision
	 */
	public void yes(Decision decision) {
		decide(decision, DecisionStatus.Yes);
	}

	/**
	 * Answer the decision with yes.
	 * 
	 * @param decision
	 */
	public void no(Decision decision) {
		decide(decision, DecisionStatus.No);
	}

	/**
	 * 
	 * @param decision
	 * @param status
	 */
	private void decide(Decision decision, DecisionStatus status) {
		Decision decision2 = em.find(Decision.class, decision.getId());
		if (decision2 == null) {
			throw new RuntimeException("Deicision was deleted.");
		} else if (decision2.getStatus() != DecisionStatus.Pending) {
			throw new IllegalStateException("Decision was already taken.");
		}
		decision2.setStatus(status);
		decision2.setDecisionDate(Calendar.getInstance());
		decision2.setNextReminderDate(null);
	}

	/**
	 * Get all the decisions for this email address.
	 * 
	 * @param emailId
	 * @return
	 */
	public List<Decision> getRequestedDecisions(String emailId) {
		Email2Id email2Id = em.find(Email2Id.class, emailId);
		// Try to find the id for the email, if not found create it
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Decision> cq = cb.createQuery(Decision.class);
		Root<Decision> decisionRoot = cq.from(Decision.class);
		cq.where(cb.equal(decisionRoot.get(Decision_.from),
				email2Id.getAddress()));
		cq.orderBy(cb.desc(decisionRoot.get(Decision_.creationDate)));
		cq.select(decisionRoot);
		TypedQuery<Decision> q = em.createQuery(cq);
		return q.getResultList();
	}

	/**
	 * Sends a reminder to the person who has not decided yet.
	 * 
	 * @param timer
	 */
	@Timeout
	public void sendReminder(Timer timer) {
		Decision decision = em.find(Decision.class, timer.getInfo());
		if (decision != null && decision.getNextReminderDate() != null
				&& decision.getStatus() == DecisionStatus.Pending) {
			scheduleReminder(decision);
			try {
				sendDecisionEmail(decision, "Reminder: " + decision.getFrom()
						+ " wants you to take a decision");
			} catch (MessagingException e) {
				log.log(Level.WARNING,
						"Could not send decision mail. Won't try again", e);
			}
		}
	}
}