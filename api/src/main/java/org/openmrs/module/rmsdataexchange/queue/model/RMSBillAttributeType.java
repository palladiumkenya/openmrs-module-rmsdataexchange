package org.openmrs.module.rmsdataexchange.queue.model;

import java.io.Serializable;
import java.util.Date;

import org.openmrs.BaseChangeableOpenmrsMetadata;

public class RMSBillAttributeType extends BaseChangeableOpenmrsMetadata implements Serializable {
	
	private Integer billAttributeTypeId;
	
	private String format;
	
	private Integer foreignKey;
	
	private Boolean searchable;
	
	private String editPrivilege;
	
	private Double sortWeight;
	
	// No-arg constructor required by Hibernate
	public RMSBillAttributeType() {
	}
	
	// Constructor with required fields
	public RMSBillAttributeType(String format, Boolean searchable) {
		this.format = format;
		this.searchable = searchable;
	}
	
	// Getters and Setters
	public Integer getBillAttributeTypeId() {
		return billAttributeTypeId;
	}
	
	public void setBillAttributeTypeId(Integer billAttributeTypeId) {
		this.billAttributeTypeId = billAttributeTypeId;
	}
	
	@Override
	public Integer getId() {
		return billAttributeTypeId;
	}
	
	@Override
	public void setId(Integer id) {
		this.billAttributeTypeId = id;
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
	
	@Override
	public String toString() {
		return "RMSBillAttributeType [billAttributeTypeId=" + billAttributeTypeId + ", format=" + format + ", foreignKey="
		        + foreignKey + ", searchable=" + searchable + ", editPrivilege=" + editPrivilege + ", sortWeight="
		        + sortWeight + ", getName()=" + getName() + ", getDescription()=" + getDescription() + ", getChangedBy()="
		        + getChangedBy() + ", getCreator()=" + getCreator() + ", getDateChanged()=" + getDateChanged()
		        + ", getDateCreated()=" + getDateCreated() + ", getDateRetired()=" + getDateRetired()
		        + ", getRetireReason()=" + getRetireReason() + ", getRetired()=" + getRetired() + ", getRetiredBy()="
		        + getRetiredBy() + ", getUuid()=" + getUuid() + "]";
	}
}
