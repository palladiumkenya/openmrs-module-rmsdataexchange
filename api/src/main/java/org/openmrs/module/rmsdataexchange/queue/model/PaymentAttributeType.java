package org.openmrs.module.rmsdataexchange.queue.model;

import java.util.Date;

public class PaymentAttributeType {
	
	private Integer paymentAttributeTypeId;
	
	private String name;
	
	private String description;
	
	private String format;
	
	private Integer foreignKey;
	
	private Boolean searchable;
	
	private Integer creator;
	
	private Date dateCreated;
	
	private Integer changedBy;
	
	private Date dateChanged;
	
	private Boolean retired;
	
	private Integer retiredBy;
	
	private Date dateRetired;
	
	private String retireReason;
	
	private String editPrivilege;
	
	private Double sortWeight;
	
	private String uuid;
	
	// No-arg constructor required by Hibernate
	public PaymentAttributeType() {
	}
	
	// Constructor with required fields
	public PaymentAttributeType(String name, String description, String format, Integer creator, Boolean searchable) {
		this.name = name;
		this.description = description;
		this.format = format;
		this.creator = creator;
		this.searchable = searchable;
		this.dateCreated = new Date();
		this.retired = false;
	}
	
	// Getters and Setters
	public Integer getPaymentAttributeTypeId() {
		return paymentAttributeTypeId;
	}
	
	public void setPaymentAttributeTypeId(Integer paymentAttributeTypeId) {
		this.paymentAttributeTypeId = paymentAttributeTypeId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getFormat() {
		return format;
	}
	
	public void setFormat(String format) {
		this.format = format;
	}
	
	public Integer getForeignKey() {
		return foreignKey;
	}
	
	public void setForeignKey(Integer foreignKey) {
		this.foreignKey = foreignKey;
	}
	
	public Boolean getSearchable() {
		return searchable;
	}
	
	public void setSearchable(Boolean searchable) {
		this.searchable = searchable;
	}
	
	public Integer getCreator() {
		return creator;
	}
	
	public void setCreator(Integer creator) {
		this.creator = creator;
	}
	
	public Date getDateCreated() {
		return dateCreated;
	}
	
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	public Integer getChangedBy() {
		return changedBy;
	}
	
	public void setChangedBy(Integer changedBy) {
		this.changedBy = changedBy;
	}
	
	public Date getDateChanged() {
		return dateChanged;
	}
	
	public void setDateChanged(Date dateChanged) {
		this.dateChanged = dateChanged;
	}
	
	public Boolean getRetired() {
		return retired;
	}
	
	public void setRetired(Boolean retired) {
		this.retired = retired;
	}
	
	public Integer getRetiredBy() {
		return retiredBy;
	}
	
	public void setRetiredBy(Integer retiredBy) {
		this.retiredBy = retiredBy;
	}
	
	public Date getDateRetired() {
		return dateRetired;
	}
	
	public void setDateRetired(Date dateRetired) {
		this.dateRetired = dateRetired;
	}
	
	public String getRetireReason() {
		return retireReason;
	}
	
	public void setRetireReason(String retireReason) {
		this.retireReason = retireReason;
	}
	
	public String getEditPrivilege() {
		return editPrivilege;
	}
	
	public void setEditPrivilege(String editPrivilege) {
		this.editPrivilege = editPrivilege;
	}
	
	public Double getSortWeight() {
		return sortWeight;
	}
	
	public void setSortWeight(Double sortWeight) {
		this.sortWeight = sortWeight;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	@Override
	public String toString() {
		return "CashierPaymentAttributeType{" + "paymentAttributeTypeId=" + paymentAttributeTypeId + ", name='" + name
		        + '\'' + ", description='" + description + '\'' + ", format='" + format + '\'' + ", foreignKey="
		        + foreignKey + ", searchable=" + searchable + ", creator=" + creator + ", dateCreated=" + dateCreated
		        + ", changedBy=" + changedBy + ", dateChanged=" + dateChanged + ", retired=" + retired + ", retiredBy="
		        + retiredBy + ", dateRetired=" + dateRetired + ", retireReason='" + retireReason + '\''
		        + ", editPrivilege='" + editPrivilege + '\'' + ", sortWeight=" + sortWeight + ", uuid='" + uuid + '\'' + '}';
	}
}
