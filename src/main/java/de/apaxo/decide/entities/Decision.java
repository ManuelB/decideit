package de.apaxo.decide.entities;

import java.util.Calendar;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;

import de.apaxo.decide.validation.Email;

@Entity
public class Decision {
	
	@Id
	private String id;
	@Size(min=1, max=140)
	private String what;
	@Email
	private String who;
	@Email
	private String from;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Calendar creationDate = Calendar.getInstance();
	
	@Temporal(TemporalType.TIMESTAMP)
	private Calendar decisionDate;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Calendar lastReminderDate = Calendar.getInstance();
	
	@Temporal(TemporalType.TIMESTAMP)
	private Calendar nextReminderDate = Calendar.getInstance();

	private DecisionStatus status = DecisionStatus.Pending;
		
	public String getWhat() {
		return what;
	}


	public void setWhat(String what) {
		this.what = what;
	}


	public String getWho() {
		return who;
	}


	public void setWho(String who) {
		this.who = who;
	}


	public String getFrom() {
		return from;
	}


	public void setFrom(String from) {
		this.from = from;
	}


	public DecisionStatus getStatus() {
		return status;
	}


	public void setStatus(DecisionStatus status) {
		this.status = status;
	}


	public Calendar getCreationDate() {
		return creationDate;
	}


	public void setCreationDate(Calendar creationDate) {
		this.creationDate = creationDate;
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public Calendar getDecisionDate() {
		return decisionDate;
	}


	public void setDecisionDate(Calendar decisionDate) {
		this.decisionDate = decisionDate;
	}


	public Calendar getLastReminderDate() {
		return lastReminderDate;
	}


	public void setLastReminderDate(Calendar lastReminderDate) {
		this.lastReminderDate = lastReminderDate;
	}


	public Calendar getNextReminderDate() {
		return nextReminderDate;
	}


	public void setNextReminderDate(Calendar nextReminderDate) {
		this.nextReminderDate = nextReminderDate;
	}
}
