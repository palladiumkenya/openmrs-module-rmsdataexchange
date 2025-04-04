package org.openmrs.module.rmsdataexchange.queue.model;

import java.io.Serializable;
import java.util.Date;

import org.openmrs.BaseChangeableOpenmrsData;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;

public class RMSBillAttribute extends BaseChangeableOpenmrsData implements Serializable {
	
	private Integer billAttributeId;
	
	private Bill bill;
	
	private String value;
	
	private RMSBillAttributeType attributeType;
	
	// No-arg constructor required by Hibernate
	public RMSBillAttribute() {
	}
	
	// Constructor with required fields
	public RMSBillAttribute(Bill bill, String value, RMSBillAttributeType attributeType) {
		this.bill = bill;
		this.value = value;
		this.attributeType = attributeType;
	}
	
	// Getters and Setters
	public Integer getBillAttributeId() {
		return billAttributeId;
	}
	
	public void setBillAttributeId(Integer billAttributeId) {
		this.billAttributeId = billAttributeId;
	}
	
	public Bill getBill() {
		return bill;
	}
	
	public void setBill(Bill bill) {
		this.bill = bill;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public RMSBillAttributeType getAttributeType() {
		return attributeType;
	}
	
	public void setAttributeType(RMSBillAttributeType attributeType) {
		this.attributeType = attributeType;
	}
	
	@Override
	public Integer getId() {
		return billAttributeId;
	}
	
	@Override
	public void setId(Integer id) {
		this.billAttributeId = id;
	}
	
	@Override
	public String toString() {
		return "RMSBillAttribute [billAttributeId=" + billAttributeId + ", bill=" + bill + ", value=" + value
		        + ", attributeType=" + attributeType + ", getId()=" + getId() + ", getChangedBy()=" + getChangedBy()
		        + ", getCreator()=" + getCreator() + ", getDateChanged()=" + getDateChanged() + ", getDateCreated()="
		        + getDateCreated() + ", getDateVoided()=" + getDateVoided() + ", getVoidReason()=" + getVoidReason()
		        + ", getVoided()=" + getVoided() + ", getVoidedBy()=" + getVoidedBy() + ", getUuid()=" + getUuid() + "]";
	}
	
}
