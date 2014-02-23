package de.apaxo.decide.business;

import java.util.Calendar;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import de.apaxo.decide.entities.Decision;
import de.apaxo.decide.entities.DecisionStatus;
import de.apaxo.decide.entities.Email2Id;

/**
 * This class loads demo data.
 * 
 * @author Manuel Blechschmidt <blechschmidt@apaxo.de>
 *
 */
@Singleton
@Startup
public class DemoData {
	
	@PersistenceContext
	EntityManager em;
	
	@PostConstruct
	public void loadDemoData() {
		Decision decision = new Decision();
		decision.setId("03B4F3E3-2FC1-4D85-B9C4-F8BEB3A80F3E");
		decision.setWhat("Will I get vacations in 3 weeks?");
		decision.setWho("boss@example.com");
		decision.setFrom("blechschmidt@apaxo.de");
		decision.setCreationDate(Calendar.getInstance());
		decision.setNextReminderDate(Calendar.getInstance());
		decision.setStatus(DecisionStatus.Yes);
		em.persist(decision);
		
		decision = new Decision();
		decision.setId("1F2C51BD-8404-42AC-92B3-4699F08D8232");
		decision.setWhat("Should I paint our living room red?");
		decision.setWho("girlfriend@example.com");
		decision.setFrom("blechschmidt@apaxo.de");
		decision.setCreationDate(Calendar.getInstance());
		decision.setNextReminderDate(Calendar.getInstance());
		decision.setStatus(DecisionStatus.No);
		em.persist(decision);
		
		decision = new Decision();
		decision.setId("A6E6F626-6F97-474D-988C-2FC28A8141BD");
		decision.setWhat("Can I come in 10 days for having a beer?");
		decision.setWho("friend@example.com");
		decision.setFrom("blechschmidt@apaxo.de");
		decision.setCreationDate(Calendar.getInstance());
		decision.setNextReminderDate(Calendar.getInstance());
		decision.setStatus(DecisionStatus.Pending);
		em.persist(decision);
		
		Email2Id email2Id = new Email2Id();
		email2Id.setAddress("blechschmidt@apaxo.de");
		email2Id.setId("134ED57F-CB29-4658-9F16-B407DA1497D1");
		em.persist(email2Id);
	}
}
