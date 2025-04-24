/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.rmsdataexchange.api;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.DataException;
import org.openmrs.api.db.DAOException;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentMode;
import org.openmrs.module.rmsdataexchange.queue.model.RmsQueue;
import org.openmrs.module.rmsdataexchange.queue.model.RmsQueueSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

public interface RmsdataexchangeDao {
	
	void setSessionFactory(SessionFactory sessionFactory);
	
	SessionFactory getSessionFactory();
	
	Set<Payment> getPaymentsByBillId(Integer billId);
	
	List<RmsQueue> getQueueItems();
	
	RmsQueue saveQueueItem(RmsQueue queue);
	
	RmsQueue getQueueItemByUUID(String queueUUID);
	
	RmsQueue getQueueItemByID(Integer queueID);
	
	RmsQueue removeQueueItem(RmsQueue queue);
	
	RmsQueueSystem getQueueSystemByUUID(String queueSystemUUID);
	
	RmsQueueSystem getQueueSystemByID(Integer queueSystemID);
}
