package de.apaxo.decide.business;

import java.util.Calendar;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.logging.Logger;
import java.util.logging.Level;

import de.apaxo.decide.entities.Decision;
import de.apaxo.decide.entities.DecisionStatus;
import de.apaxo.decide.entities.Decision_;
import de.apaxo.decide.entities.Email2Id;

/**
 * This class loads demo data.
 * 
 * @author Manuel Blechschmidt <blechschmidt@apaxo.de>
 * 
 */
@Singleton
@Startup
public class Bootstrap {

    private static final Logger log = Logger
        .getLogger(Bootstrap.class.getName());


	@PersistenceContext
	EntityManager em;
	
	@Inject
	DecisionManager decisionManager;

	@PostConstruct
	public void bootstrap() {
        try {
    		loadDemoData();
		    rescheduleDecisions();
        } catch(Exception ex) {
            log.log(Level.WARNING, "Exception during bootstraping", ex);
        }
	}

	/**
	 * Rescheduled all reminders which were lost during the
	 * last redeploy
	 */
	private void rescheduleDecisions() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Decision> cq = cb.createQuery(Decision.class);
		Root<Decision> decisionRoot = cq.from(Decision.class);
		cq.where(cb.equal(decisionRoot.get(Decision_.status),
				DecisionStatus.Pending));
		cq.orderBy(cb.desc(decisionRoot.get(Decision_.creationDate)));
		cq.select(decisionRoot);
		TypedQuery<Decision> q = em.createQuery(cq);
		for(Decision decision : q.getResultList()) {
			decisionManager.scheduleReminder(decision);
		}
	}

	public void loadDemoData() {
		if (em.find(Decision.class, "03B4F3E3-2FC1-4D85-B9C4-F8BEB3A80F3E") == null) {
			Decision decision = new Decision();
			decision.setId("03B4F3E3-2FC1-4D85-B9C4-F8BEB3A80F3E");
			decision.setWhat("Will I get vacations in 3 weeks?");
			decision.setWho("boss@example.com");
			decision.setFrom("blechschmidt@apaxo.de");
			decision.setCreationDate(Calendar.getInstance());
			decision.setNextReminderDate(null);
			decision.setStatus(DecisionStatus.Yes);
			em.persist(decision);

			decision = new Decision();
			decision.setId("1F2C51BD-8404-42AC-92B3-4699F08D8232");
			decision.setWhat("Should I paint our living room red?");
			decision.setWho("girlfriend@example.com");
			decision.setFrom("blechschmidt@apaxo.de");
			decision.setCreationDate(Calendar.getInstance());
			decision.setNextReminderDate(null);
			decision.setStatus(DecisionStatus.No);
			em.persist(decision);

			decision = new Decision();
			decision.setId("A6E6F626-6F97-474D-988C-2FC28A8141BD");
			decision.setWhat("Can I come in 10 days for having a beer?");
			decision.setWho("friend@example.com");
			decision.setFrom("blechschmidt@apaxo.de");
			decision.setCreationDate(Calendar.getInstance());
			decision.setNextReminderDate(null);
			decision.setStatus(DecisionStatus.Pending);
			em.persist(decision);

			Email2Id email2Id = new Email2Id();
			email2Id.setAddress("blechschmidt@apaxo.de");
			email2Id.setId("134ED57F-CB29-4658-9F16-B407DA1497D1");
			em.persist(email2Id);
		}
	}
}
