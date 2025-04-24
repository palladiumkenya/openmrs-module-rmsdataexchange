package org.openmrs.module.rmsdataexchange.queue.model;

import java.util.Date;

public class BillAttribute {
    private Integer billAttributeId;
    private Integer billId;
    private String value;
    private Integer billAttributeTypeId;
    private Integer creator;
    private Date dateCreated;
    private Integer changedBy;
    private Date dateChanged;
    private Boolean voided;
    private Integer voidedBy;
    private Date dateVoided;
    private String voidReason;
    private String uuid;

    // No-arg constructor required by Hibernate
    public CashierBillAttribute() {}

    // Constructor with required fields
    public CashierBillAttribute(Integer billId, String value, Integer billAttributeTypeId, Integer creator) {
        this.billId = billId;
        this.value = value;
        this.billAttributeTypeId = billAttributeTypeId;
        this.creator = creator;
        this.dateCreated = new Date();
        this.voided = false;
    }

    // Getters and Setters
    public Integer getBillAttributeId() {
        return billAttributeId;
    }

    public void setBillAttributeId(Integer billAttributeId) {
        this.billAttributeId = billAttributeId;
    }

    public Integer getBillId() {
        return billId;
    }

    public void setBillId(Integer billId) {
        this.billId = billId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getBillAttributeTypeId() {
        return billAttributeTypeId;
    }

    public void setBillAttributeTypeId(Integer billAttributeTypeId) {
        this.billAttributeTypeId = billAttributeTypeId;
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

    public Boolean getVoided() {
        return voided;
    }

    public void setVoided(Boolean voided) {
        this.voided = voided;
    }

    public Integer getVoidedBy() {
        return voidedBy;
    }

    public void setVoidedBy(Integer voidedBy) {
        this.voidedBy = voidedBy;
    }

    public Date getDateVoided() {
        return dateVoided;
    }

    public void setDateVoided(Date dateVoided) {
        this.dateVoided = dateVoided;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void setVoidReason(String voidReason) {
        this.voidReason = voidReason;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "CashierBillAttribute{" +
                "billAttributeId=" + billAttributeId +
                ", billId=" + billId +
                ", value='" + value + '\'' +
                ", billAttributeTypeId=" + billAttributeTypeId +
                ", creator=" + creator +
                ", dateCreated=" + dateCreated +
                ", changedBy=" + changedBy +
                ", dateChanged=" + dateChanged +
                ", voided=" + voided +
                ", voidedBy=" + voidedBy +
                ", dateVoided=" + dateVoided +
                ", voidReason='" + voidReason + '\'' +
                ", uuid='" + uuid + '\'' +
                '}';
    }
}
