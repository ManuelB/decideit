/**
 * 
 */
package de.incentergy.decide.business;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.junit.Test;

import de.incentergy.decide.business.DecisionManager;
import de.incentergy.decide.entities.Decision;
import de.incentergy.decide.entities.DecisionStatus;
import de.incentergy.decide.entities.Email2Id;

/**
 * @author manuel
 * 
 */
public class DecisionManagerTest {

	/**
	 * Test method for
	 * {@link de.incentergy.decide.business.DecisionManager#processDecision(de.incentergy.decide.entities.Decision)}
	 * .
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testProcessDecision() throws MessagingException {
		Decision decision = getTestDecision();
		DecisionManager decisionManager = getMockedDecisionManager();
		CriteriaQuery<Email2Id> cq = mock(CriteriaQuery.class);
		when(cq.from(any(Class.class))).thenReturn(mock(Root.class));
		when(
				decisionManager.em.getCriteriaBuilder().createQuery(
						Email2Id.class)).thenReturn(cq);
		TypedQuery<Email2Id> tq = mock(TypedQuery.class);
		when(tq.getResultList()).thenReturn(new ArrayList<Email2Id>());
		when(decisionManager.em.createQuery(cq)).thenReturn(tq);
		decisionManager.processDecision(decision);
	}

	private Decision getTestDecision() {
		Decision decision = new Decision();
		decision.setWhat("Do I get vacations");
		decision.setWho("boss@incentergy.de");
		decision.setFrom("manuel.blechschmidt@incentergy.de");
		return decision;
	}

	/**
	 * Test method for
	 * {@link de.incentergy.decide.business.DecisionManager#get(java.lang.String)}.
	 */
	@Test
	public void testGet() {
		DecisionManager decisionManager = getMockedDecisionManager();
		when(
				decisionManager.em.find(Decision.class,
						"8DC262E4-2812-4DA9-89F1-A64FBD281E8B")).thenReturn(
				getTestDecision());
		Decision decision = decisionManager
				.get("8DC262E4-2812-4DA9-89F1-A64FBD281E8B");
		assertEquals("Do I get vacations", decision.getWhat());
		assertEquals("boss@incentergy.de", decision.getWho());
		assertEquals("manuel.blechschmidt@incentergy.de", decision.getFrom());
	}

	/**
	 * Test method for
	 * {@link de.incentergy.decide.business.DecisionManager#yes(de.incentergy.decide.entities.Decision)}
	 * .
	 * @throws MessagingException 
	 */
	@Test
	public void testYes() throws MessagingException {
		DecisionManager decisionManager = getMockedDecisionManager();
		Decision testDecision = getTestDecision();
		when(
				decisionManager.em.find(Decision.class,
						"8DC262E4-2812-4DA9-89F1-A64FBD281E8B")).thenReturn(
				testDecision);
		testDecision.setId("8DC262E4-2812-4DA9-89F1-A64FBD281E8B");
		decisionManager.yes(testDecision);
		assertEquals(DecisionStatus.Yes, testDecision.getStatus());

	}

	/**
	 * Test method for
	 * {@link de.incentergy.decide.business.DecisionManager#no(de.incentergy.decide.entities.Decision)}
	 * .
	 * @throws MessagingException 
	 */
	@Test
	public void testNo() throws MessagingException {
		DecisionManager decisionManager = getMockedDecisionManager();
		Decision testDecision = getTestDecision();
		when(
				decisionManager.em.find(Decision.class,
						"8DC262E4-2812-4DA9-89F1-A64FBD281E8B")).thenReturn(
				testDecision);
		testDecision.setId("8DC262E4-2812-4DA9-89F1-A64FBD281E8B");
		decisionManager.no(testDecision);
		assertEquals(DecisionStatus.No, testDecision.getStatus());
	}

	/**
	 * Test method for
	 * {@link de.incentergy.decide.business.DecisionManager#getRequestedDecisions(java.lang.String)}
	 * .
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testGetRequestedDecisions() {
		DecisionManager decisionManager = getMockedDecisionManager();
		Email2Id email2Id = new Email2Id();
		email2Id.setId("83024578-BFCF-4626-816D-BF533E35759C");
		email2Id.setAddress("manuel.blechschmidt@incentergy.de");
		when(
				decisionManager.em.find(Email2Id.class,
						"83024578-BFCF-4626-816D-BF533E35759C")).thenReturn(
				email2Id);
		CriteriaQuery<Decision> cq = mock(CriteriaQuery.class);
		when(cq.from(any(Class.class))).thenReturn(mock(Root.class));
		when(
				decisionManager.em.getCriteriaBuilder().createQuery(
						Decision.class)).thenReturn(cq);
		TypedQuery<Decision> tq = mock(TypedQuery.class);
		when(tq.getResultList()).thenReturn(new ArrayList<Decision>());
		when(decisionManager.em.createQuery(cq)).thenReturn(tq);

		decisionManager
				.getRequestedDecisions("83024578-BFCF-4626-816D-BF533E35759C");
	}

	/**
	 * Test method for
	 * {@link de.incentergy.decide.business.DecisionManager#sendReminder(javax.ejb.Timer)}
	 * .
	 */
	@Test
	public void testSendReminder() {
		DecisionManager decisionManager = getMockedDecisionManager();
		Timer timer = mock(Timer.class);
		when(timer.getInfo()).thenReturn("38AB5F2D-BB91-4937-85D5-DD8737870E1");
		decisionManager.sendReminder(timer);
	}
	
	@Test
	public void testTestConfig() {
		DecisionManager decisionManager = getMockedDecisionManager();
		assertEquals("https://decide-it.incentergy.de/decideit/", decisionManager.config.getString("webAppUrl"));
	}
	
	@Test
	public void testParseTemplate() {
		DecisionManager decisionManager = getMockedDecisionManager();
		Map<String,Object> params = new HashMap<String, Object>();
		params.put("title", "Test Title");
		String output = decisionManager.capture("decide-email", params);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
				"<!DOCTYPE html>\n" + 
				"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" + 
				"<head>\n" + 
				"	<meta charset=\"utf-8\" />\n" + 
				"	<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\n" + 
				"	<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" + 
				"	<title>Test Title</title>\n" + 
				"\n" + 
				"	<!-- Latest compiled and minified CSS -->\n" + 
				"	<link\n" + 
				"		href=\"//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css\"\n" + 
				"		rel=\"stylesheet\" />\n" + 
				"\n" + 
				"	<!-- Optional theme -->\n" + 
				"	<link rel=\"stylesheet\"\n" + 
				"		href=\"//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap-theme.min.css\" />\n" + 
				"	<link href=\"${serverName}/javax.faces.resource/css/style.css.jsf\" rel=\"stylesheet\" type=\"text/css\" />\n" + 
				"\n" + 
				"	\n" + 
				"</head>\n" + 
				"<body>\n" + 
				"	<div class=\"container-fluid\"><br />\n" + 
				"<h1 class=\"text-center\">${decisionForm.decision.what}</h1>\n" + 
				"<br />\n" + 
				"<br />\n" + 
				"<div class=\"text-center\">\n" + 
				"	<a\n" + 
				"		href=\"${serverUrl}decision/${decisionForm.decision.id}/yes\"\n" + 
				"		class=\"btn btn-large btn-success\">Yes</a> <a\n" + 
				"		href=\"${serverUrl}decision/${decisionForm.decision.id}/no\"\n" + 
				"		class=\"btn btn-large btn-danger\">No</a>\n" + 
				"</div>\n" + 
				"<br />\n" + 
				"<br />\n" + 
				"<br />\n" + 
				"	</div>\n" + 
				"</body>\n" + 
				"</html>", output);
	}

	public DecisionManager getMockedDecisionManager() {
		return new DecisionManager() {
			{
				em = mock(EntityManager.class);
				CriteriaBuilder cb = mock(CriteriaBuilder.class);
				when(em.getCriteriaBuilder()).thenReturn(cb);
				when(
						em.getCriteriaBuilder().equal(any(Expression.class),
								any(Expression.class))).thenReturn(
						mock(Predicate.class));
				mailSession = Session.getDefaultInstance(new Properties());
				timerService = mock(TimerService.class);
				init();
			}
		};
	}

}
