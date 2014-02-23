package de.apaxo.decide.ui;

import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Model;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.mail.MessagingException;

import de.apaxo.decide.business.DecisionManager;
import de.apaxo.decide.entities.Decision;
import de.apaxo.decide.entities.DecisionStatus;

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
	public void findDecisionById() throws MessagingException {
		FacesContext context = FacesContext.getCurrentInstance();
		Map<String, String> paramMap = context.getExternalContext()
				.getRequestParameterMap();
		String decisionId = paramMap.get("decisionId");
		if (decisionId != null && !decisionId.equals("")) {
			decision = decisionManager.get(decisionId);
		}

		if (answer != null && decision != null
				&& decision.getStatus() == DecisionStatus.Pending) {
			if (answer.equals("yes")) {
				yes();
			} else if (answer.equals("no")) {
				no();
			}
		}
	}

	/**
	 * Get the correct view for a processed answer
	 * @return
	 * @throws MessagingException
	 */
	public String processAnswer() throws MessagingException {
		log.fine("Process answer");
		// this function is called twice don't know why
		if (answer.equals("yes")) {
			return "decision-yes";
		} else {
			return "decision-no";
		}

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
