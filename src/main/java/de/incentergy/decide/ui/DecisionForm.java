package de.incentergy.decide.ui;

import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Model;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.mail.MessagingException;

import de.incentergy.decide.business.DecisionManager;
import de.incentergy.decide.entities.Decision;
import de.incentergy.decide.entities.DecisionStatus;

@Model
public class DecisionForm {

	@Inject
	DecisionManager decisionManager;

	private Decision decision = new Decision();

	private String answer;

	private String fromEmailId;

	private static final Logger log = Logger.getLogger(DecisionForm.class
			.getName());

	@PostConstruct
	public void findDecisionById() {
		FacesContext context = FacesContext.getCurrentInstance();
		Map<String, String> paramMap = context.getExternalContext()
				.getRequestParameterMap();
		String decisionId = paramMap.get("decisionId");
		if (decisionId != null && !decisionId.equals("")) {
			decision = decisionManager.get(decisionId);
		}
	}

	public String processAnswer() throws MessagingException {
		// this function is called twice don't know why
		if (answer != null && decision != null
				&& decision.getStatus() == DecisionStatus.Pending) {
			if (answer.equals("yes")) {
				yes();
				return "/decision-yes.jsf";
			} else if (answer.equals("no")) {
				no();
				return "/decision-no.jsf";
			}
		}
		return null;
	}

	public Decision getDecision() {
		return decision;
	}

	public void setDecision(Decision decision) {
		this.decision = decision;
	}

	public String submit() throws MessagingException {
		log.fine("Process new decision: "
				+ (decision != null ? decision.getWhat() : null));
		decisionManager.processDecision(decision);
		return "decision-send";
	}

	public String yes() throws MessagingException {
		decisionManager.yes(decision);
		return "decision-yes";
	}

	public String no() throws MessagingException {
		decisionManager.no(decision);
		return "decision-no";
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public String getFromEmailId() {
		return fromEmailId;
	}

	public void setFromEmailId(String fromEmailId) {
		this.fromEmailId = fromEmailId;
	}
}
