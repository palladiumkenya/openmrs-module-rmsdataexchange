package org.openmrs.module.rmsdataexchange.api.impl;

import org.openmrs.module.rmsdataexchange.api.PaymentAttributeService;
import org.hibernate.SessionFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.rmsdataexchange.queue.model.PaymentAttribute;
import org.openmrs.module.rmsdataexchange.queue.model.PaymentAttributeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service("paymentAttributeService")
public class PaymentAttributeServiceImpl implements PaymentAttributeService {
	
	private SessionFactory sessionFactory;
	
	@Autowired
	public PaymentAttributeServiceImpl(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public PaymentAttribute savePaymentAttribute(PaymentAttribute paymentAttribute) {
		sessionFactory.getCurrentSession().saveOrUpdate(paymentAttribute);
		return paymentAttribute;
	}
	
	@Override
	public PaymentAttribute getPaymentAttribute(Integer paymentAttributeId) {
		return sessionFactory.getCurrentSession().get(PaymentAttribute.class, paymentAttributeId);
	}
	
	@Override
	public void deletePaymentAttribute(PaymentAttribute paymentAttribute) {
		sessionFactory.getCurrentSession().delete(paymentAttribute);
	}
	
	@Override
	public List<PaymentAttribute> getPaymentAttributesByPaymentId(Integer paymentId) {
		return sessionFactory.getCurrentSession()
		        .createQuery("from PaymentAttribute where billPaymentId = :paymentId", PaymentAttribute.class)
		        .setParameter("paymentId", paymentId).getResultList();
	}
	
	@Override
	public List<PaymentAttribute> getPaymentAttributesByTypeId(Integer paymentAttributeTypeId) {
		return sessionFactory.getCurrentSession()
		        .createQuery("from PaymentAttribute where paymentAttributeTypeId = :typeId", PaymentAttribute.class)
		        .setParameter("typeId", paymentAttributeTypeId).getResultList();
	}
	
	@Override
	public List<PaymentAttribute> getAllPaymentAttributes(Boolean includeVoided) {
		String query = "from PaymentAttribute";
		if (!includeVoided) {
			query += " where voided = false";
		}
		return sessionFactory.getCurrentSession().createQuery(query, PaymentAttribute.class).getResultList();
	}
	
	@Override
	public PaymentAttributeType savePaymentAttributeType(PaymentAttributeType paymentAttributeType) {
		sessionFactory.getCurrentSession().saveOrUpdate(paymentAttributeType);
		return paymentAttributeType;
	}
	
	@Override
	public PaymentAttributeType getPaymentAttributeType(Integer paymentAttributeTypeId) {
		return sessionFactory.getCurrentSession().get(PaymentAttributeType.class, paymentAttributeTypeId);
	}
	
	@Override
	public List<PaymentAttributeType> getAllPaymentAttributeTypes(Boolean includeRetired) {
		String query = "from PaymentAttributeType";
		if (!includeRetired) {
			query += " where retired = false";
		}
		return sessionFactory.getCurrentSession().createQuery(query, PaymentAttributeType.class).getResultList();
	}
	
	@Override
	public void voidPaymentAttribute(PaymentAttribute paymentAttribute, String reason, Integer voidedBy) {
		paymentAttribute.setVoided(true);
		paymentAttribute.setVoidedBy(voidedBy);
		paymentAttribute.setDateVoided(new Date());
		paymentAttribute.setVoidReason(reason);
		savePaymentAttribute(paymentAttribute);
	}
	
	@Override
	public void unvoidPaymentAttribute(PaymentAttribute paymentAttribute) {
		paymentAttribute.setVoided(false);
		paymentAttribute.setVoidedBy(null);
		paymentAttribute.setDateVoided(null);
		paymentAttribute.setVoidReason(null);
		savePaymentAttribute(paymentAttribute);
	}
	
	@Override
	public void retirePaymentAttributeType(PaymentAttributeType paymentAttributeType, String reason, Integer retiredBy) {
		paymentAttributeType.setRetired(true);
		paymentAttributeType.setRetiredBy(retiredBy);
		paymentAttributeType.setDateRetired(new Date());
		paymentAttributeType.setRetireReason(reason);
		savePaymentAttributeType(paymentAttributeType);
	}
	
	@Override
	public void unretirePaymentAttributeType(PaymentAttributeType paymentAttributeType) {
		paymentAttributeType.setRetired(false);
		paymentAttributeType.setRetiredBy(null);
		paymentAttributeType.setDateRetired(null);
		paymentAttributeType.setRetireReason(null);
		savePaymentAttributeType(paymentAttributeType);
	}
	
	@Override
	public List<PaymentAttribute> getPaymentAttributesByPaymentUuid(String paymentUuid) {
		return sessionFactory
		        .getCurrentSession()
		        .createQuery(
		            "from PaymentAttribute pa " + "join CashierBillPayment bp on pa.billPaymentId = bp.billPaymentId "
		                    + "where bp.uuid = :paymentUuid", PaymentAttribute.class)
		        .setParameter("paymentUuid", paymentUuid).getResultList();
	}
}
