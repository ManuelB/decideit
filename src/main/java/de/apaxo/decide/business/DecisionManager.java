package de.apaxo.decide.business;

import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.faces.context.FacesContext;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.JdkLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.generic.AlternatorTool;
import org.apache.velocity.tools.generic.ConversionTool;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.DisplayTool;
import org.apache.velocity.tools.generic.EscapeTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.apache.velocity.tools.generic.SortTool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.apaxo.decide.entities.Decision;
import de.apaxo.decide.entities.DecisionStatus;
import de.apaxo.decide.entities.Decision_;
import de.apaxo.decide.entities.Email2Id;
import de.apaxo.decide.entities.Email2Id_;
import de.apaxo.decide.ui.DecisionForm;

/**
 * This class implements the business logic to save a decision and send the
 * necessary reminder to the person who should take the decision.
 * 
 * @author Manuel Blechschmidt <blechschmidt@apaxo.de>
 * 
 */
@Stateless
public class DecisionManager {

	private VelocityEngine velocityEngine;

	private static final Logger log = Logger.getLogger(DecisionManager.class
			.getName());

	@PersistenceContext
	EntityManager em;

	@Resource
	TimerService timerService;

	@Resource(name = "Mail")
	Session mailSession;

	/**
	 * Init the template engine
	 */
	@PostConstruct
	public void initTemplatEngine() {
		/*
		 * create a new instance of the engine
		 */

		velocityEngine = new VelocityEngine();

		// EasyFactoryConfiguration config = new EasyFactoryConfiguration();

		/*
		 * configure the engine. In this case, we are using ourselves as a
		 * logger (see logging examples..)
		 */

		velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM,
				new JdkLogChute());

		/*
		 * This will allow that macros which are already defined in an template
		 * will replace existing ones.
		 */
		velocityEngine.setProperty(
				RuntimeConstants.VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL, true);

		/**
		 * Load templates from classpath
		 */
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER,
				"classpath");
		velocityEngine.setProperty("classpath.resource.loader.class",
				ClasspathResourceLoader.class.getName());

		velocityEngine.init();
	}

	/**
	 * Process this decision by sending it to the receiver.
	 * @param decision
	 * @throws MessagingException
	 */
	public void processDecision(Decision decision) throws MessagingException {
		log.fine("Sending decision: " + decision.getWhat() + " from "
				+ decision.getFrom() + " to " + decision.getWho());
		decision.setId(UUID.randomUUID().toString());
		incrementAndScheduleReminder(decision);

		em.persist(decision);

		sendDecisionEmail(decision, decision.getFrom()
				+ " wants you to take a decision", false);

	}

	/**
	 * Sends the decision email to the receiver (who) and the requester (from).
	 * 
	 * @param decision
	 * @param subject
	 * @throws MessagingException
	 */
	private void sendDecisionEmail(Decision decision, String subject,
			boolean reminder) throws MessagingException {
		InternetAddress from = new InternetAddress();
		from.setAddress(decision.getFrom());

		String fromEmailId = getEmailIdFromAddress(decision.getFrom());

		InternetAddress who = new InternetAddress();
		who.setAddress(decision.getWho());

		Map<String, Object> params = new HashMap<String, Object>();
		DecisionForm decisionForm = new DecisionForm();
		decisionForm.setDecision(decision);
		decisionForm.setFromEmailId(fromEmailId);
		params.put("decisionForm", decisionForm);

		sendMailTo(subject, who, params, "decide-email");

		sendMailTo((reminder ? "Reminder" : "Decision") + " was send to "
				+ decision.getWho(), from, params, "decide-email-to-from");

	}

	/**
	 * Extracts the server url from the given faces context.
	 * 
	 * @return
	 */
	private String extractServerUrl() {
		FacesContext facesContext = getOrCreateCurrentFacesContext();
		String url;
		if (facesContext != null
				&& facesContext.getExternalContext().getRequest() != null) {
			url = facesContext.getExternalContext().getRequest().toString();
		} else {

			try {
				String hostname = InetAddress.getLocalHost().getHostName();
				log.warning("Could not find facesContext. Setting url to: http://"
						+ hostname + ":8080/decideit/");
				url = "http://" + hostname + ":8080/decideit/";
			} catch (UnknownHostException e) {
				log.warning("Could not find facesContext. Setting url to: http://localhost:8080/decideit/");
				url = "http://localhost:8080/decideit/";
			}
		}
		// get everything before the last /
		// e.g. http://www.example.com/foo/bar.jsf
		// matches 1. http://www.example.com/foo/
		Pattern everythingWithoutLast = Pattern.compile("^(.*?)[^/]*$");
		Matcher everythingWithoutLastMatcher = everythingWithoutLast
				.matcher(url);
		everythingWithoutLastMatcher.matches();
		String serverUrl = everythingWithoutLastMatcher.group(1);
		return serverUrl;
	}

	/**
	 * Return or creates a faces contest
	 * 
	 * @return
	 */
	private FacesContext getOrCreateCurrentFacesContext() {
		FacesContext context = FacesContext.getCurrentInstance();
		return context;
	}

	/**
	 * Send the given JSF page with the parameters to the given email address.
	 * 
	 * @param subject
	 *            the subject for the email
	 * @param to
	 *            the to address for the person
	 * @param params
	 *            the params to pass to the JSF engine
	 * @param template
	 *            the template to process for the JSF engine
	 * @throws MessagingException
	 */
	private void sendMailTo(String subject, InternetAddress to,
			Map<String, Object> params, String template)
			throws MessagingException {
		if(!params.containsKey("title")) {
			params.put("title", subject);
		}
		params.put("serverUrl", extractServerUrl());
		MimeMultipart mp;
		String htmlContent;
		String noHTMLString;
		MimeMessage mail = new MimeMessage(mailSession);
		mail.addFrom(new Address[] { new InternetAddress("decide-it@apaxo.de") });
		mail.addRecipient(javax.mail.Message.RecipientType.TO, to);
		// fromMail.addRecipient(RecipientType.CC, from);

		mail.setSubject(subject, "UTF-8");

		mp = new MimeMultipart("alternative");

		BodyPart fromMailTextPart = new MimeBodyPart();

		htmlContent = capture(template, params);

		htmlContent = htmlContent.replaceAll("\"//", "\"http://");

		// Fill the multi part with plain content
		noHTMLString = getPlainTextFromHtml(htmlContent);

		fromMailTextPart.setContent(noHTMLString, "text/plain");
		fromMailTextPart.setHeader("MIME-Version", "1.0");
		fromMailTextPart.setHeader("Content-Type", "text/plain");
		mp.addBodyPart(fromMailTextPart);

		// Create the fromMail part
		BodyPart fromMailHtmlPart = new MimeBodyPart();

		// Fill the multi part with html content
		fromMailHtmlPart.setContent(htmlContent, "text/html; charset=UTF-8");
		fromMailHtmlPart.setHeader("MIME-Version", "1.0");
		fromMailHtmlPart.setHeader("Content-Type", "text/html; charset=UTF-8");
		mp.addBodyPart(fromMailHtmlPart);

		// set the multi part for the fromMail
		mail.setContent(mp);

		mail.setHeader("MIME-Version", "1.0");
		mail.setHeader("Content-Type", mp.getContentType());
		mail.setHeader("X-Mailer", "decide-it Apaxo GmbH");

		mail.setSentDate(new Date());

		mail.saveChanges();

		Transport.send(mail);
	}

	/**
	 * Encodes the address as an id. If there is already an id return id.
	 * Otherwise create a new one.
	 * 
	 * @param address
	 * @return
	 */
	private String getEmailIdFromAddress(String address) {
		String id;
		// Try to find the id for the email, if not found create it
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Email2Id> cq = cb.createQuery(Email2Id.class);
		Root<Email2Id> mail2IdRoot = cq.from(Email2Id.class);
		cq.where(cb.equal(mail2IdRoot.get(Email2Id_.address), address));

		cq.select(mail2IdRoot);
		TypedQuery<Email2Id> q = em.createQuery(cq);
		List<Email2Id> email2Id = q.getResultList();
		if (email2Id.size() == 0) {
			Email2Id newEmail2Id = new Email2Id();
			newEmail2Id.setId(UUID.randomUUID().toString());
			newEmail2Id.setAddress(address);
			em.persist(newEmail2Id);
			id = newEmail2Id.getId();
		} else {
			id = email2Id.get(0).getId();
		}
		return id;
	}

	/**
	 * Schedules the reminder to the next day.
	 * 
	 * @param decision
	 */
	private void incrementAndScheduleReminder(Decision decision) {
		Calendar nextReminderDate = decision.getNextReminderDate();
		// send next reminder tomorrow
		nextReminderDate.add(Calendar.DAY_OF_YEAR, 1);
		decision.setNextReminderDate(nextReminderDate);
		scheduleReminder(decision);
	}

	/**
	 * Schedules a reminder for a decision.
	 * 
	 * @param decision
	 */
	public void scheduleReminder(Decision decision) {
		Calendar nextReminderDate = decision.getNextReminderDate();
		if (nextReminderDate != null) {
			Date date = nextReminderDate.getTime();
			TimerConfig timerConfig = new TimerConfig();
			timerConfig.setInfo(decision.getId());
			timerService.createSingleActionTimer(date, timerConfig);
		}
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
		String noHTMLString = "";
        StringBuilder output = new StringBuilder();
        try {

            Document doc = Jsoup.parse(htmlContent);
            Elements elements = doc.body().select("*");

            int countImageLinks = 0;

            for (Element el : elements) {
                if (el.tag().getName().equals("a")) {
                    String linkHref = el.attr("href");
                    String linkText = el.text();
                    Element img = el.select("img").first();
                    // add spaces in front and after every parenthesis ()
                    // for making sure that mail clients create
                    // a correct link: SEMRECSYS-1066
                    if (img != null) {
                        String imgAlt = "\"" + img.attr("alt") + "\"";
                        if (imgAlt.equals("\"\""))
                            imgAlt = "";
                        String imgSrc = img.attr("src");
                        countImageLinks++;
                        output.append("Image Link " + countImageLinks + " "
                                + linkText + "\n" + imgAlt + "\n" + "( "
                                + imgSrc + " )\n( " + linkHref + " )\n\n");
                    } else {
                        output.append(linkText + " ( " + linkHref + " )\n\n");
                    }
                } else {
                    output.append(el.ownText() + "\n");
                }
            }
        } catch (Exception ex) {
            log.log(Level.WARNING,
                    "An error occurred while converting html mime content '"
                            + htmlContent + "' to text/plain output: ", ex);
        }

        noHTMLString = output.toString();
        return noHTMLString;
	}

	/**
	 * Parses a template with the volicity template engine.
	 * 
	 * @param template
	 * @param params
	 * @return
	 */
	String capture(String templateName, Map<String, Object> params) {
		String errorMessage = "";
		try {
			VelocityContext context = new VelocityContext(params);
			addVelocityTools(context);
			StringWriter out = new StringWriter();
			log.fine("Generating filling template with Object: " + params);
			Template template = velocityEngine.getTemplate("email-templates/"
					+ templateName + ".html");
			template.merge(context, out);
			return out.getBuffer().toString();
		} catch (ResourceNotFoundException rnfe) {
			// A referenced resource could not be loaded
			log.log(Level.WARNING,
					"Template Error: A referenced resource could not be loaded",
					rnfe);
			errorMessage = rnfe.getLocalizedMessage();
		} catch (ParseErrorException pee) {
			// syntax error: problem parsing the template
			log.log(Level.WARNING,
					"syntax error: problem parsing the template", pee);
			errorMessage = pee.getLocalizedMessage();
		} catch (MethodInvocationException mie) {
			// something invoked in the template
			// threw an exception
			log.log(Level.WARNING,
					"something invoked in the template threw an exception", mie);
			errorMessage = mie.getLocalizedMessage();
		} catch (Exception e) {
			log.log(Level.WARNING, "Error during Template parsing", e);
			errorMessage = e.getLocalizedMessage();
		}
		return "Error parsing Template [" + errorMessage
				+ "]. Call the administrator to see the log file.";
	}

	/**
	 * This function adds the following tools to the template context: number -
	 * http
	 * ://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/
	 * tools/generic/NumberTool.html convert -
	 * http://velocity.apache.org/tools/releases
	 * /2.0/javadoc/org/apache/velocity/tools/generic/ConversionTool.html date -
	 * http
	 * ://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/
	 * tools/generic/DateTool.html display -
	 * http://velocity.apache.org/tools/releases
	 * /2.0/javadoc/org/apache/velocity/tools/generic/DisplayTool.html esc -
	 * http
	 * ://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/
	 * tools/generic/EscapeTool.html sorter -
	 * http://velocity.apache.org/tools/releases
	 * /2.0/javadoc/org/apache/velocity/tools/generic/SortTool.html
	 * 
	 * @param context
	 */
	private void addVelocityTools(VelocityContext context) {

		HashMap<String, Object> localeMap = new HashMap<String, Object>();
		localeMap.put(ToolContext.LOCALE_KEY, context.get("locale"));

		// Locale dependend:
		// * ConversionTool
		// * DisplayTool
		// * BrowserTool
		// * DateTool
		// * MathTool
		// * NumberTool
		// * ResourceTool
		NumberTool numberTool = new NumberTool();
		context.put("number", numberTool);
		numberTool.configure(localeMap);

		ConversionTool conversionTool = new ConversionTool();
		context.put("convert", conversionTool);
		conversionTool.configure(localeMap);

		DateTool dateTool = new DateTool();
		context.put("date", dateTool);
		dateTool.configure(localeMap);

		MathTool math = new MathTool();
		context.put("math", math);
		math.configure(localeMap);

		DisplayTool displayTool = new DisplayTool();
		context.put("display", displayTool);
		displayTool.configure(localeMap);

		EscapeTool escapeTool = new EscapeTool();
		context.put("esc", escapeTool);
		escapeTool.configure(localeMap);

		SortTool sortTool = new SortTool();
		context.put("sorter", sortTool);

		AlternatorTool alternatorTool = new AlternatorTool();
		context.put("alternator", alternatorTool);

		context.put("en_US", Locale.US);

	}

	/**
	 * TODO: This function only works when the request was initiated by a web
	 * request but we need a function that also runs in unit tests and scheduled
	 * executions
	 * 
	 * Render a template in memory and return the content as a string. The
	 * request parameter 'emailClient' is set to true during rendering. This
	 * method relies on a FacesContext for Facelets templating so it only works
	 * when the app is deployed.
	 * http://www.ninthavenue.com.au/how-to-create-email-from-jsf-templates
	 * 
	 * private String capture(String template, Map<String, String> params) {
	 * 
	 * // setup a response catcher FacesContext faces =
	 * getOrCreateCurrentFacesContext(); if (faces == null) {
	 * log.warning("Faces could not be found returning empty string."); return
	 * ""; } ExternalContext context = faces.getExternalContext();
	 * ServletRequest request = (ServletRequest) faces.getExternalContext()
	 * .getRequest(); HttpServletResponse response = (HttpServletResponse)
	 * context .getResponse(); ResponseCatcher catcher = new
	 * ResponseCatcher(response);
	 * 
	 * // hack the request state UIViewRoot oldView = faces.getViewRoot();
	 * Map<String, Object> oldAttributes = null; if (params != null) {
	 * oldAttributes = new HashMap<String, Object>(params.size() * 2); // with
	 * // buffer for (String key : params.keySet()) { oldAttributes.put(key,
	 * request.getAttribute(key)); request.setAttribute(key, params.get(key)); }
	 * } if (request != null) { request.setAttribute("emailClient", true); }
	 * context.setResponse(catcher);
	 * 
	 * try { // build a JSF view for the template and render it ViewHandler
	 * views = faces.getApplication().getViewHandler(); UIViewRoot view =
	 * views.createView(faces, template); faces.setViewRoot(view);
	 * views.getViewDeclarationLanguage(faces, template).buildView(faces, view);
	 * views.renderView(faces, view); } catch (IOException ioe) { String msg =
	 * "Failed to render " + template; faces.addMessage(null, new FacesMessage(
	 * FacesMessage.SEVERITY_ERROR, msg, msg)); return null; } finally {
	 * 
	 * // restore the request state if (oldAttributes != null) { for (String key
	 * : oldAttributes.keySet()) { request.setAttribute(key,
	 * oldAttributes.get(key)); } } if (request != null) {
	 * request.setAttribute("emailClient", null); }
	 * context.setResponse(response); faces.setViewRoot(oldView); } return
	 * catcher.toString(); }
	 */

	/**
	 * Answer the decision with no.
	 * 
	 * @param decision
	 * @throws MessagingException
	 */
	public void yes(Decision decision) throws MessagingException {
		decide(decision, DecisionStatus.Yes);
	}

	/**
	 * Answer the decision with yes.
	 * 
	 * @param decision
	 * @throws MessagingException
	 */
	public void no(Decision decision) throws MessagingException {
		decide(decision, DecisionStatus.No);
	}

	/**
	 * 
	 * @param decision
	 * @param status
	 * @throws MessagingException
	 */
	private void decide(Decision decision, DecisionStatus status)
			throws MessagingException {
		Decision persistedDecision = em.find(Decision.class, decision.getId());
		if (persistedDecision == null) {
			throw new RuntimeException("Decision was deleted.");
		} else if (persistedDecision.getStatus() != DecisionStatus.Pending) {
			throw new IllegalStateException("Decision was already taken.");
		}
		persistedDecision.setStatus(status);
		persistedDecision.setDecisionDate(Calendar.getInstance());
		persistedDecision.setNextReminderDate(null);

		Map<String,Object> params = new HashMap<String, Object>();
		DecisionForm decisionForm = new DecisionForm();
		decisionForm.setDecision(persistedDecision);
		params.put("decisionForm", decisionForm);
		
		InternetAddress to = new InternetAddress();
		to.setAddress(persistedDecision.getFrom());
		// This creates problems when recreating the state
		// it seams that the buffer gets full
		sendMailTo(
				persistedDecision.getStatus() + " was decided for "
						+ persistedDecision.getWhat(), to,
				params, "decision-email-taken");
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
			incrementAndScheduleReminder(decision);
			try {
				sendDecisionEmail(decision, "Reminder: " + decision.getFrom()
						+ " wants you to take a decision", true);
			} catch (MessagingException e) {
				log.log(Level.WARNING,
						"Could not send decision mail. Won't try again", e);
			}
		}
	}
}