package de.incentergy.decide.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
@Entity
public class Email2Id {
	@Id
	private String id;
	private String address;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
}
