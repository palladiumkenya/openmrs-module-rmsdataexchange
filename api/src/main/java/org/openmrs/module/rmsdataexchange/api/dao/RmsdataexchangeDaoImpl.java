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
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.DataException;
import org.openmrs.api.db.DAOException;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentMode;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeDao;
import org.openmrs.module.rmsdataexchange.api.util.AdviceUtils;
import org.openmrs.module.rmsdataexchange.queue.model.RmsQueue;
import org.openmrs.module.rmsdataexchange.queue.model.RmsQueueSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.hibernate.Criteria;

public class RmsdataexchangeDaoImpl implements RmsdataexchangeDao {
	
	private SessionFactory sessionFactory;
	
	private Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
	
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
	
	@Override
	public List<RmsQueue> getQueueItems() throws DataException {
		if (debugMode)
			System.out.println("rmsdataexchange Module: Getting all queued items");
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(RmsQueue.class);
		criteria.add(Restrictions.eq("voided", false));
		criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
		return criteria.list();
	}
	
	@Override
	public RmsQueue saveQueueItem(RmsQueue queue) throws DAOException {
		if (debugMode)
			System.out.println("rmsdataexchange Module: Saving the RMS Queue");
		sessionFactory.getCurrentSession().saveOrUpdate(queue);
		return queue;
	}
	
	@Override
	public RmsQueue getQueueItemByUUID(String queueUUID) throws DataException {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(RmsQueue.class);
		criteria.add(Restrictions.eq("uuid", queueUUID));
		return (RmsQueue) criteria.uniqueResult();
	}
	
	@Override
	public RmsQueue getQueueItemByID(Integer queueID) throws DataException {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(RmsQueue.class);
		criteria.add(Restrictions.eq("id", queueID));
		return (RmsQueue) criteria.uniqueResult();
	}
	
	@Override
	public RmsQueue removeQueueItem(RmsQueue queue) throws DAOException {
		if (debugMode)
			System.out.println("rmsdataexchange Module: Removing RMS Queue Item");
		sessionFactory.getCurrentSession().delete(queue);
		return queue;
	}
	
	@Override
	public RmsQueueSystem getQueueSystemByUUID(String queueSystemUUID) throws DataException {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(RmsQueueSystem.class);
		criteria.add(Restrictions.eq("uuid", queueSystemUUID));
		return (RmsQueueSystem) criteria.uniqueResult();
	}
	
	@Override
	public RmsQueueSystem getQueueSystemByID(Integer queueSystemID) throws DataException {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(RmsQueueSystem.class);
		criteria.add(Restrictions.eq("id", queueSystemID));
		return (RmsQueueSystem) criteria.uniqueResult();
	}
}
