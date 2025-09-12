/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.rmsdataexchange.api.dao;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.DataException;
import org.hibernate.query.NativeQuery;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentMode;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeDao;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeService;
import org.openmrs.module.rmsdataexchange.api.util.AdviceUtils;
import org.openmrs.module.rmsdataexchange.queue.model.RMSBillAttribute;
import org.openmrs.module.rmsdataexchange.queue.model.RMSBillAttributeType;
import org.openmrs.module.rmsdataexchange.queue.model.RMSPaymentAttribute;
import org.openmrs.module.rmsdataexchange.queue.model.RMSPaymentAttributeType;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueue;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueueSystem;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.hibernate.Criteria;
import org.hibernate.SQLQuery;

public class RmsdataexchangeDaoImpl implements RmsdataexchangeDao {
	
	private SessionFactory sessionFactory;
	
	// private Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
	// private Boolean debugMode = false;
	
	@Override
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Set<Payment> getPaymentsByBillId(Integer billId) {
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();

		// Get the current Hibernate session from DbSessionFactory
		Session session = sessionFactory.getCurrentSession();
        
        // Ensure no caching is used by ignoring the cache
        session.setCacheMode(CacheMode.IGNORE);

		String sqlQuery = "SELECT distinct cbp.bill_payment_id, cbp.uuid, cbp.bill_id, cbp.payment_mode_id, cbp.amount_tendered, cbp.amount FROM cashier_bill cb inner join cashier_bill_payment cbp on cbp.bill_id = cb.bill_id and cb.bill_id =:billId";

		// Execute the query and fetch the result
        List<Object[]> resultList = session.createSQLQuery(sqlQuery)
                                           .setParameter("billId", billId)
                                           .list();
		
		if(debugMode) System.out.println("rmsdataexchange Module: Payments got SQL payments: " + resultList.size());
										   
		// Create a Set to hold the resulting Payment objects
        Set<Payment> payments = new HashSet<>();

        // Iterate through the results and map them to Payment objects
        for (Object[] row : resultList) {
            Payment payment = new Payment();
            payment.setId((Integer) row[0]);  // payment_id
            payment.setUuid((String) row[1]); // payment uuid
			Bill newBill = new Bill();
			newBill.setId(billId);
			payment.setBill(newBill); // bill
			PaymentMode newPaymentMode = new PaymentMode();
			newPaymentMode.setId((Integer) row[3]);
			payment.setInstanceType(newPaymentMode); // payment mode
			payment.setAmountTendered((BigDecimal) row[4]); //Amount Tendered
			payment.setAmount((BigDecimal) row[5]); //Total Amount
            payments.add(payment);
        }

		return(payments);
	}
	
	/**
	 * Get the value coded concept of the latest OBS
	 * 
	 * @param patient
	 * @param conceptIdentifier
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Concept getLatestObsConcept(Patient patient, String conceptIdentifier) {
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		Concept concept = AdviceUtils.getConcept(conceptIdentifier);
		if (concept != null) {
			// Get the current Hibernate session from DbSessionFactory
			Session session = sessionFactory.getCurrentSession();
			// Session session = sessionFactory.openSession();
			
			// Ensure no caching is used by ignoring the cache
			session.setCacheMode(CacheMode.IGNORE);
			
			if (debugMode)
				System.out.println("rmsdataexchange Module: HIE CR: Direct SQL: Concept is not null: "
				        + concept.getName().getName());
			Context.addProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
			// SELECT obs_id, person_id, concept_id, value_coded FROM openmrs.obs where person_id=1355 and concept_id=1054 order by obs_id desc limit 1;
			StringBuilder q = new StringBuilder();
			q.append("SELECT obs_id, person_id, concept_id, value_coded FROM openmrs.obs where person_id = :patientId");
			q.append(" and concept_id = :conceptId");
			q.append(" order by obs_id desc limit 1;");
			
			if (debugMode)
				System.out.println("rmsdataexchange Module: HIE CR: Executing SQL query: " + q.toString());
			
			// RmsdataexchangeService rmsdataexchangeService = Context.getService(RmsdataexchangeService.class);
			// rmsdataexchangeService.getQueueItems();
			Context.flushSession();
			Context.clearSession();
			// SessionFactory sf = Context.getRegisteredComponent("sessionFactory", SessionFactory.class);
			// Session session = sf.getCurrentSession();
			// Ensure no caching is used by ignoring the cache
			// Execute the query and fetch the result
			NativeQuery query = session.createSQLQuery(q.toString()).setParameter("patientId", patient.getId()).setParameter("conceptId", concept.getId());
			query.setCacheable(false);
			List<Object[]> resultList = query.list();
			
			if (debugMode)
				System.out.println("rmsdataexchange Module: Latest Obs got SQL obs: " + resultList.size());
			
			if (resultList != null && !resultList.isEmpty()) {
				System.out.println("rmsdataexchange Module: HIE CR: The latest obs was found");
				Object[] latestObs = resultList.get(0);
				Integer obs_id = (Integer) latestObs[0];
				Integer person_id = (Integer) latestObs[1];
				Integer concept_id = (Integer) latestObs[2];
				Integer value_coded = (Integer) latestObs[3];
				
				if (debugMode)
					System.out.println("rmsdataexchange Module: HIE CR: obs_id is: " + obs_id);
				if (debugMode)
					System.out.println("rmsdataexchange Module: HIE CR: person_id is: " + person_id);
				if (debugMode)
					System.out.println("rmsdataexchange Module: HIE CR: concept_id is: " + concept_id);
				if (debugMode)
					System.out.println("rmsdataexchange Module: HIE CR: value_coded is: " + value_coded);
				
				Concept valueCoded = Context.getConceptService().getConcept(value_coded);
				return (valueCoded);
			} else {
				if (debugMode)
					System.out.println("rmsdataexchange Module: HIE CR: no obs found");
			}
			
			Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
			
		} else {
			if (debugMode)
				System.out.println("rmsdataexchange Module: HIE CR: Concept is null");
		}
		return null;
	}
	
	@Override
	public List<RMSQueue> getQueueItems() throws DataException {
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		if (debugMode)
			System.out.println("rmsdataexchange Module: Getting all queued items");
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(RMSQueue.class);
		criteria.add(Restrictions.eq("voided", false));
		criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
		return criteria.list();
	}
	
	@Override
	public RMSQueue saveQueueItem(RMSQueue queue) throws DAOException {
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		if (debugMode)
			System.out.println("rmsdataexchange Module: Saving the RMS Queue");
		// sessionFactory.getCurrentSession().saveOrUpdate(queue);
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.saveOrUpdate(queue);
			tx.commit();
		}
		catch (Exception ex) {
			if (tx != null)
				tx.rollback();
			throw ex;
		}
		finally {
			session.close();
		}
		
		return queue;
	}
	
	@Override
	public RMSQueue getQueueItemByUUID(String queueUUID) throws DataException {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(RMSQueue.class);
		criteria.add(Restrictions.eq("uuid", queueUUID));
		return (RMSQueue) criteria.uniqueResult();
	}
	
	@Override
	public RMSQueue getQueueItemByID(Integer queueID) throws DataException {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(RMSQueue.class);
		criteria.add(Restrictions.eq("id", queueID));
		return (RMSQueue) criteria.uniqueResult();
	}
	
	@Override
	public RMSQueue removeQueueItem(RMSQueue queue) throws DAOException {
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		if (debugMode)
			System.out.println("rmsdataexchange Module: Removing RMS Queue Item");
		sessionFactory.getCurrentSession().delete(queue);
		return queue;
	}
	
	@Override
	public RMSQueueSystem getQueueSystemByUUID(String queueSystemUUID) throws DataException {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(RMSQueueSystem.class);
		criteria.add(Restrictions.eq("uuid", queueSystemUUID));
		return (RMSQueueSystem) criteria.uniqueResult();
	}
	
	@Override
	public RMSQueueSystem getQueueSystemByID(Integer queueSystemID) throws DataException {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(RMSQueueSystem.class);
		criteria.add(Restrictions.eq("id", queueSystemID));
		return (RMSQueueSystem) criteria.uniqueResult();
	}
	
	// Payment Attributes
	
	@Override
	public RMSPaymentAttribute savePaymentAttribute(RMSPaymentAttribute paymentAttribute) {
		sessionFactory.getCurrentSession().saveOrUpdate(paymentAttribute);
		return paymentAttribute;
	}
	
	@Override
	public RMSPaymentAttribute getPaymentAttribute(Integer paymentAttributeId) {
		return sessionFactory.getCurrentSession().get(RMSPaymentAttribute.class, paymentAttributeId);
	}
	
	@Override
	public void deletePaymentAttribute(RMSPaymentAttribute paymentAttribute) {
		sessionFactory.getCurrentSession().delete(paymentAttribute);
	}
	
	@Override
	public List<RMSPaymentAttribute> getPaymentAttributesByPaymentId(Integer paymentId) {
		return sessionFactory.getCurrentSession()
		        .createQuery("from PaymentAttribute where billPaymentId = :paymentId", RMSPaymentAttribute.class)
		        .setParameter("paymentId", paymentId).getResultList();
	}
	
	@Override
	public List<RMSPaymentAttribute> getPaymentAttributesByTypeId(Integer paymentAttributeTypeId) {
		return sessionFactory.getCurrentSession()
		        .createQuery("from PaymentAttribute where paymentAttributeTypeId = :typeId", RMSPaymentAttribute.class)
		        .setParameter("typeId", paymentAttributeTypeId).getResultList();
	}
	
	@Override
	public List<RMSPaymentAttribute> getAllPaymentAttributes(Boolean includeVoided) {
		String query = "from PaymentAttribute";
		if (!includeVoided) {
			query += " where voided = false";
		}
		return sessionFactory.getCurrentSession().createQuery(query, RMSPaymentAttribute.class).getResultList();
	}
	
	@Override
	public List<RMSPaymentAttribute> getAllPaymentAttributesByPaymentId(Integer paymentId, Boolean includeVoided) {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(RMSPaymentAttribute.class, "attr");
		criteria.createAlias("attr.payment", "payment");
		criteria.add(Restrictions.eq("payment.id", paymentId));
		criteria.add(Restrictions.eq("attr.voided", includeVoided));
		criteria.addOrder(org.hibernate.criterion.Order.asc("attr.paymentAttributeId"));
		return criteria.list();
	}
	
	@Override
	public RMSPaymentAttributeType savePaymentAttributeType(RMSPaymentAttributeType paymentAttributeType) {
		sessionFactory.getCurrentSession().saveOrUpdate(paymentAttributeType);
		return paymentAttributeType;
	}
	
	@Override
	public RMSPaymentAttributeType getPaymentAttributeType(Integer paymentAttributeTypeId) {
		return sessionFactory.getCurrentSession().get(RMSPaymentAttributeType.class, paymentAttributeTypeId);
	}
	
	@Override
	public RMSPaymentAttributeType getPaymentAttributeTypeByUuid(String typeUuid) {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(RMSPaymentAttributeType.class);
		criteria.add(Restrictions.eq("uuid", typeUuid));
		return (RMSPaymentAttributeType) criteria.uniqueResult();
	}
	
	@Override
	public List<RMSPaymentAttributeType> getAllPaymentAttributeTypes(Boolean includeRetired) {
		String query = "from PaymentAttributeType";
		if (!includeRetired) {
			query += " where retired = false";
		}
		return sessionFactory.getCurrentSession().createQuery(query, RMSPaymentAttributeType.class).getResultList();
	}
	
	@Override
	public void voidPaymentAttribute(RMSPaymentAttribute paymentAttribute, String reason, User voidedBy) {
		paymentAttribute.setVoided(true);
		paymentAttribute.setVoidedBy(voidedBy);
		paymentAttribute.setDateVoided(new Date());
		paymentAttribute.setVoidReason(reason);
		savePaymentAttribute(paymentAttribute);
	}
	
	@Override
	public void unvoidPaymentAttribute(RMSPaymentAttribute paymentAttribute) {
		paymentAttribute.setVoided(false);
		paymentAttribute.setVoidedBy(null);
		paymentAttribute.setDateVoided(null);
		paymentAttribute.setVoidReason(null);
		savePaymentAttribute(paymentAttribute);
	}
	
	@Override
	public void retirePaymentAttributeType(RMSPaymentAttributeType paymentAttributeType, String reason, User retiredBy) {
		paymentAttributeType.setRetired(true);
		paymentAttributeType.setRetiredBy(retiredBy);
		paymentAttributeType.setDateRetired(new Date());
		paymentAttributeType.setRetireReason(reason);
		savePaymentAttributeType(paymentAttributeType);
	}
	
	@Override
	public void unretirePaymentAttributeType(RMSPaymentAttributeType paymentAttributeType) {
		paymentAttributeType.setRetired(false);
		paymentAttributeType.setRetiredBy(null);
		paymentAttributeType.setDateRetired(null);
		paymentAttributeType.setRetireReason(null);
		savePaymentAttributeType(paymentAttributeType);
	}
	
	@Override
	public List<RMSPaymentAttribute> getPaymentAttributesByPaymentUuid(String paymentUuid) {
		return sessionFactory
		        .getCurrentSession()
		        .createQuery(
		            "from PaymentAttribute pa " + "join CashierBillPayment bp on pa.billPaymentId = bp.billPaymentId "
		                    + "where bp.uuid = :paymentUuid", RMSPaymentAttribute.class)
		        .setParameter("paymentUuid", paymentUuid).getResultList();
	}
	
	// Bill Attributes
	@Override
	public RMSBillAttribute saveBillAttribute(RMSBillAttribute billAttribute) {
		sessionFactory.getCurrentSession().saveOrUpdate(billAttribute);
		return billAttribute;
	}
	
	@Override
	public RMSBillAttribute getBillAttribute(Integer billAttributeId) {
		return sessionFactory.getCurrentSession().get(RMSBillAttribute.class, billAttributeId);
	}
	
	@Override
	public void deleteBillAttribute(RMSBillAttribute billAttribute) {
		sessionFactory.getCurrentSession().delete(billAttribute);
	}
	
	@Override
	public List<RMSBillAttribute> getBillAttributesByBillId(Integer billId) {
		return sessionFactory.getCurrentSession()
		        .createQuery("from CashierBillAttribute where billId = :billId", RMSBillAttribute.class)
		        .setParameter("billId", billId).getResultList();
	}
	
	@Override
	public List<RMSBillAttribute> getBillAttributesByBillUuid(String billUuid) {
		return sessionFactory
		        .getCurrentSession()
		        .createQuery(
		            "from CashierBillAttribute ba " + "join CashierBill b on ba.billId = b.billId "
		                    + "where b.uuid = :billUuid", RMSBillAttribute.class).setParameter("billUuid", billUuid)
		        .getResultList();
	}
	
	@Override
	public List<RMSBillAttribute> getBillAttributesByTypeId(Integer billAttributeTypeId) {
		return sessionFactory.getCurrentSession()
		        .createQuery("from CashierBillAttribute where paymentAttributeTypeId = :typeId", RMSBillAttribute.class)
		        .setParameter("typeId", billAttributeTypeId).getResultList();
	}
	
	@Override
	public List<RMSBillAttribute> getAllBillAttributes(Boolean includeVoided) {
		String query = "from CashierBillAttribute";
		if (!includeVoided) {
			query += " where voided = false";
		}
		return sessionFactory.getCurrentSession().createQuery(query, RMSBillAttribute.class).getResultList();
	}
	
	@Override
	public List<RMSBillAttribute> getAllBillAttributesByBillId(Integer billId, Boolean includeVoided) {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(RMSBillAttribute.class, "attr");
		criteria.createAlias("attr.bill", "bill");
		criteria.add(Restrictions.eq("bill.id", billId));
		criteria.add(Restrictions.eq("attr.voided", includeVoided));
		criteria.addOrder(org.hibernate.criterion.Order.asc("attr.billAttributeId"));
		return criteria.list();
	}
	
	@Override
	public RMSBillAttributeType saveBillAttributeType(RMSBillAttributeType billAttributeType) {
		sessionFactory.getCurrentSession().saveOrUpdate(billAttributeType);
		return billAttributeType;
	}
	
	@Override
	public RMSBillAttributeType getBillAttributeType(Integer billAttributeTypeId) {
		return sessionFactory.getCurrentSession().get(RMSBillAttributeType.class, billAttributeTypeId);
	}
	
	@Override
	public RMSBillAttributeType getBillAttributeTypeByUuid(String typeUuid) {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(RMSBillAttributeType.class);
		criteria.add(Restrictions.eq("uuid", typeUuid));
		return (RMSBillAttributeType) criteria.uniqueResult();
	}
	
	@Override
	public List<RMSBillAttributeType> getAllBillAttributeTypes(Boolean includeRetired) {
		String query = "from CashierBillAttributeType";
		if (!includeRetired) {
			query += " where retired = false";
		}
		return sessionFactory.getCurrentSession().createQuery(query, RMSBillAttributeType.class).getResultList();
	}
	
	@Override
	public void voidBillAttribute(RMSBillAttribute billAttribute, String reason, User voidedBy) {
		billAttribute.setVoided(true);
		billAttribute.setVoidedBy(voidedBy);
		billAttribute.setDateVoided(new Date());
		billAttribute.setVoidReason(reason);
		saveBillAttribute(billAttribute);
	}
	
	@Override
	public void unvoidBillAttribute(RMSBillAttribute billAttribute) {
		billAttribute.setVoided(false);
		billAttribute.setVoidedBy(null);
		billAttribute.setDateVoided(null);
		billAttribute.setVoidReason(null);
		saveBillAttribute(billAttribute);
	}
	
	@Override
	public void retireBillAttributeType(RMSBillAttributeType billAttributeType, String reason, User retiredBy) {
		billAttributeType.setRetired(true);
		billAttributeType.setRetiredBy(retiredBy);
		billAttributeType.setDateRetired(new Date());
		billAttributeType.setRetireReason(reason);
		saveBillAttributeType(billAttributeType);
	}
	
	@Override
	public void unretireBillAttributeType(RMSBillAttributeType billAttributeType) {
		billAttributeType.setRetired(false);
		billAttributeType.setRetiredBy(null);
		billAttributeType.setDateRetired(null);
		billAttributeType.setRetireReason(null);
		saveBillAttributeType(billAttributeType);
	}
}
