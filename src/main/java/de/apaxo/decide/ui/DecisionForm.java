package de.apaxo.decide.ui;

import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Model;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.mail.MessagingException;

import de.apaxo.decide.business.DecisionManager;
import de.apaxo.decide.entities.Decision;

@Model
public class DecisionForm {

	@Inject
	DecisionManager decisionManager;

	private Decision decision = new Decision();

	private String answer;

	@ManagedProperty("#{param.fromEmailId}")
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
		log.fine("Process answer");
		if (answer != null) {
			if (answer.equals("yes")) {
				return "/" + yes() + ".jsf";
			} else if (answer.equals("no")) {
				return "/" + no() + ".jsf";
			}
			return "/index.jsf";
		}
		return "/decide.jsf";

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
