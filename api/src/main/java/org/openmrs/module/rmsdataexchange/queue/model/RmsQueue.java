package org.openmrs.module.rmsdataexchange.queue.model;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Order;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class RmsQueue extends BaseOpenmrsData implements Serializable {
	
	private Integer id;
	
	private String payload;
	
	private Integer retries;
	
	private RmsQueueSystem system;
	
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
	
	public RmsQueueSystem getSystem() {
		return system;
	}
	
	public void setSystem(RmsQueueSystem system) {
		this.system = system;
	}
	
	@Override
	public String toString() {
		return "RmsQueue{" + "id=" + id + ", payload='" + payload + '\'' + ", retries=" + retries + ", system=" + system
		        + '}';
	}
	
}
