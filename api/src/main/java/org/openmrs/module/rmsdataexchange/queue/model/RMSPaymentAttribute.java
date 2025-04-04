package org.openmrs.module.rmsdataexchange.queue.model;

import java.io.Serializable;
import java.util.Date;

import org.openmrs.BaseChangeableOpenmrsData;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;

public class RMSPaymentAttribute extends BaseChangeableOpenmrsData implements Serializable {
	
	private Integer paymentAttributeId;
	
	private Payment payment;
	
	private String value;
	
	private RMSPaymentAttributeType attributeType;
	
	// No-arg constructor required by Hibernate
	public RMSPaymentAttribute() {
	}
	
	// Constructor with required fields
	public RMSPaymentAttribute(Payment payment, String value) {
		this.payment = payment;
		this.value = value;
	}
	
	// Getters and Setters
	public Integer getPaymentAttributeId() {
		return paymentAttributeId;
	}
	
	public void setPaymentAttributeId(Integer paymentAttributeId) {
		this.paymentAttributeId = paymentAttributeId;
	}
	
	@Override
	public Integer getId() {
		return paymentAttributeId;
	}
	
	@Override
	public void setId(Integer id) {
		this.paymentAttributeId = id;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public Payment getPayment() {
		return payment;
	}
	
	public void setPayment(Payment payment) {
		this.payment = payment;
	}
	
	public RMSPaymentAttributeType getAttributeType() {
		return attributeType;
	}
	
	public void setAttributeType(RMSPaymentAttributeType attributeType) {
		this.attributeType = attributeType;
	}
	
	@Override
	public String toString() {
		return "RMSPaymentAttribute [paymentAttributeId=" + paymentAttributeId + ", payment=" + payment + ", value=" + value
		        + ", attributeType=" + attributeType + ", getChangedBy()=" + getChangedBy() + ", getCreator()="
		        + getCreator() + ", getDateChanged()=" + getDateChanged() + ", getDateCreated()=" + getDateCreated()
		        + ", getDateVoided()=" + getDateVoided() + ", getVoidReason()=" + getVoidReason() + ", getVoided()="
		        + getVoided() + ", getVoidedBy()=" + getVoidedBy() + ", getUuid()=" + getUuid() + "]";
	}
}
