package org.openmrs.module.rmsdataexchange.queue.model;

import java.util.Date;

public class PaymentAttribute {
    private Integer paymentAttributeId;
    private Integer billPaymentId;
    private String value;
    private Integer paymentAttributeTypeId;
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
    public CashierPaymentAttribute() {}

    // Constructor with required fields
    public CashierPaymentAttribute(Integer billPaymentId, String value, 
            Integer paymentAttributeTypeId, Integer creator) {
        this.billPaymentId = billPaymentId;
        this.value = value;
        this.paymentAttributeTypeId = paymentAttributeTypeId;
        this.creator = creator;
        this.dateCreated = new Date();
        this.voided = false;
    }

    // Getters and Setters
    public Integer getPaymentAttributeId() {
        return paymentAttributeId;
    }

    public void setPaymentAttributeId(Integer paymentAttributeId) {
        this.paymentAttributeId = paymentAttributeId;
    }

    public Integer getBillPaymentId() {
        return billPaymentId;
    }

    public void setBillPaymentId(Integer billPaymentId) {
        this.billPaymentId = billPaymentId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getPaymentAttributeTypeId() {
        return paymentAttributeTypeId;
    }

    public void setPaymentAttributeTypeId(Integer paymentAttributeTypeId) {
        this.paymentAttributeTypeId = paymentAttributeTypeId;
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
        return "CashierPaymentAttribute{" +
                "paymentAttributeId=" + paymentAttributeId +
                ", billPaymentId=" + billPaymentId +
                ", value='" + value + '\'' +
                ", paymentAttributeTypeId=" + paymentAttributeTypeId +
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
