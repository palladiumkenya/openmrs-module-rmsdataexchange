package org.openmrs.module.rmsdataexchange.queue.model;

import java.io.Serializable;

import org.openmrs.BaseOpenmrsData;

public class RMSQueue extends BaseOpenmrsData implements Serializable {
	
	private Integer id;
	
	private String payload;
	
	private Integer retries;
	
	private RMSQueueSystem rmsSystem;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getPayload() {
		return payload;
	}
	
	public void setPayload(String payload) {
		this.payload = payload;
	}
	
	public Integer getRetries() {
		return retries;
	}
	
	public void setRetries(Integer retries) {
		this.retries = retries;
	}
	
	public RMSQueueSystem getRmsSystem() {
		return rmsSystem;
	}
	
	public void setRmsSystem(RMSQueueSystem rmsSystem) {
		this.rmsSystem = rmsSystem;
	}
	
	@Override
	public String toString() {
		return "RMSQueue [id=" + id + ", payload=" + payload + ", retries=" + retries + ", system=" + rmsSystem
		        + ", creator=" + creator + ", getId()=" + getId() + ", getPayload()=" + getPayload() + ", getRetries()="
		        + getRetries() + ", getChangedBy()=" + getChangedBy() + ", getCreator()=" + getCreator()
		        + ", getDateChanged()=" + getDateChanged() + ", getDateCreated()=" + getDateCreated() + ", getDateVoided()="
		        + getDateVoided() + ", getVoidReason()=" + getVoidReason() + ", getVoided()=" + getVoided()
		        + ", getVoidedBy()=" + getVoidedBy() + ", getUuid()=" + getUuid() + "]";
	}
	
}
