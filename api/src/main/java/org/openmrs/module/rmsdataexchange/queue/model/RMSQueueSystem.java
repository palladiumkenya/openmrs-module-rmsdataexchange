package org.openmrs.module.rmsdataexchange.queue.model;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Order;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class RMSQueueSystem extends BaseOpenmrsData implements Serializable {
	
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
		return "RMSQueueSystem [id=" + id + ", description=" + description + ", creator=" + creator + ", getChangedBy()="
		        + getChangedBy() + ", getDateChanged()=" + getDateChanged() + ", getDateCreated()=" + getDateCreated()
		        + ", getDateVoided()=" + getDateVoided() + ", getVoidReason()=" + getVoidReason() + ", getVoided()="
		        + getVoided() + ", getVoidedBy()=" + getVoidedBy() + ", getUuid()=" + getUuid() + "]";
	}
	
}
