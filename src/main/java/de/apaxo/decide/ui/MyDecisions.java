package de.apaxo.decide.ui;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Model;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;

import de.apaxo.decide.business.DecisionManager;
import de.apaxo.decide.entities.Decision;
import de.apaxo.decide.entities.DecisionStatus;

@Model
public class MyDecisions {
	private List<Decision> decisions;
	private String mail;
	@Inject
	DecisionManager decisionManager;

	@PostConstruct
	public void findDecisionByEmail() {
		FacesContext context = FacesContext.getCurrentInstance();
		Map<String, String> paramMap = context.getExternalContext()
				.getRequestParameterMap();
		String emailId = paramMap.get("emailId");
		if (emailId != null && !emailId.equals("")) {
			setDecisions(decisionManager.getRequestedDecisions(emailId));
		}
	}

	public List<Decision> getDecisions() {
		return decisions;
	}

	public void setDecisions(List<Decision> decisions) {
		this.decisions = decisions;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getRowClasses() {
		String rowClasses = "";
		if (getDecisions() != null) {
			rowClasses = Joiner.on(",").join(FluentIterable.from(getDecisions())

			.transform(new Function<Decision, String>() {
				public String apply(Decision d) {
					return d.getStatus() == DecisionStatus.Yes ? "success" : (d
							.getStatus() == DecisionStatus.No ? "danger" : "active");
				}
			}));
		}
		return rowClasses;
	}
}
