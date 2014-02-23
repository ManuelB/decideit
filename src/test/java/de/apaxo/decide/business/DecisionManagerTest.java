/**
 * 
 */
package de.apaxo.decide.business;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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

import de.apaxo.decide.entities.Decision;
import de.apaxo.decide.entities.DecisionStatus;
import de.apaxo.decide.entities.Email2Id;

/**
 * @author manuel
 * 
 */
public class DecisionManagerTest {

	/**
	 * Test method for
	 * {@link de.apaxo.decide.business.DecisionManager#processDecision(de.apaxo.decide.entities.Decision)}
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
		decision.setWho("scharhag@apaxo.de");
		decision.setFrom("blechschmidt@apaxo.de");
		return decision;
	}

	/**
	 * Test method for
	 * {@link de.apaxo.decide.business.DecisionManager#get(java.lang.String)}.
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
		assertEquals("scharhag@apaxo.de", decision.getWho());
		assertEquals("blechschmidt@apaxo.de", decision.getFrom());
	}

	/**
	 * Test method for
	 * {@link de.apaxo.decide.business.DecisionManager#yes(de.apaxo.decide.entities.Decision)}
	 * .
	 */
	@Test
	public void testYes() {
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
	 * {@link de.apaxo.decide.business.DecisionManager#no(de.apaxo.decide.entities.Decision)}
	 * .
	 */
	@Test
	public void testNo() {
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
	 * {@link de.apaxo.decide.business.DecisionManager#getRequestedDecisions(java.lang.String)}
	 * .
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testGetRequestedDecisions() {
		DecisionManager decisionManager = getMockedDecisionManager();
		Email2Id email2Id = new Email2Id();
		email2Id.setId("83024578-BFCF-4626-816D-BF533E35759C");
		email2Id.setAddress("blechschmidt@apaxo.de");
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
	 * {@link de.apaxo.decide.business.DecisionManager#sendReminder(javax.ejb.Timer)}
	 * .
	 */
	@Test
	public void testSendReminder() {
		DecisionManager decisionManager = getMockedDecisionManager();
		Timer timer = mock(Timer.class);
		when(timer.getInfo()).thenReturn("38AB5F2D-BB91-4937-85D5-DD8737870E1");
		decisionManager.sendReminder(timer);
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
			}
		};
	}

}
