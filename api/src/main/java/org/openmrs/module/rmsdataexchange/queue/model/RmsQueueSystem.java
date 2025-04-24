package org.openmrs.module.rmsdataexchange.queue.model;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Order;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class RmsQueueSystem extends BaseOpenmrsData implements Serializable {
	
	private Integer id;
	
	private String description;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String toString() {
		return "RmsQueueSystem{" + "id=" + id + ", description='" + description + '\'' + '}';
	}
}
