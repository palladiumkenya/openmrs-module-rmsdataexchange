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

import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.rmsdataexchange.queue.model.RMSBillAttribute;
import org.openmrs.module.rmsdataexchange.queue.model.RMSBillAttributeType;
import org.openmrs.module.rmsdataexchange.queue.model.RMSPaymentAttribute;
import org.openmrs.module.rmsdataexchange.queue.model.RMSPaymentAttributeType;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueue;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueueSystem;

public interface RmsdataexchangeDao {
	
	void setSessionFactory(SessionFactory sessionFactory);
	
	SessionFactory getSessionFactory();
	
	Set<Payment> getPaymentsByBillId(Integer billId);
	
	Concept getLatestObsConcept(Patient patient, String conceptIdentifier);
	
	List<RMSQueue> getQueueItems();
	
	RMSQueue saveQueueItem(RMSQueue queue);
	
	RMSQueue getQueueItemByUUID(String queueUUID);
	
	RMSQueue getQueueItemByID(Integer queueID);
	
	RMSQueue removeQueueItem(RMSQueue queue);
	
	RMSQueueSystem getQueueSystemByUUID(String queueSystemUUID);
	
	RMSQueueSystem getQueueSystemByID(Integer queueSystemID);
	
	// Payment attributes
	// CRUD Operations
	RMSPaymentAttribute savePaymentAttribute(RMSPaymentAttribute paymentAttribute);
	
	RMSPaymentAttribute getPaymentAttribute(Integer paymentAttributeId);
	
	void deletePaymentAttribute(RMSPaymentAttribute paymentAttribute);
	
	List<RMSPaymentAttribute> getPaymentAttributesByPaymentUuid(String paymentUuid);
	
	// Query Operations
	List<RMSPaymentAttribute> getPaymentAttributesByPaymentId(Integer paymentId);
	
	List<RMSPaymentAttribute> getPaymentAttributesByTypeId(Integer paymentAttributeTypeId);
	
	List<RMSPaymentAttribute> getAllPaymentAttributes(Boolean includeVoided);
	
	List<RMSPaymentAttribute> getAllPaymentAttributesByPaymentId(Integer paymentId, Boolean includeVoided);
	
	// Type Operations
	RMSPaymentAttributeType savePaymentAttributeType(RMSPaymentAttributeType paymentAttributeType);
	
	RMSPaymentAttributeType getPaymentAttributeType(Integer paymentAttributeTypeId);
	
	RMSPaymentAttributeType getPaymentAttributeTypeByUuid(String typeUuid);
	
	List<RMSPaymentAttributeType> getAllPaymentAttributeTypes(Boolean includeRetired);
	
	// Utility Operations
	void voidPaymentAttribute(RMSPaymentAttribute paymentAttribute, String reason, User voidedBy);
	
	void unvoidPaymentAttribute(RMSPaymentAttribute paymentAttribute);
	
	void retirePaymentAttributeType(RMSPaymentAttributeType paymentAttributeType, String reason, User retiredBy);
	
	void unretirePaymentAttributeType(RMSPaymentAttributeType paymentAttributeType);
	
	// Bill Attributes
	// CRUD Operations
	RMSBillAttribute saveBillAttribute(RMSBillAttribute billAttribute);
	
	RMSBillAttribute getBillAttribute(Integer billAttributeId);
	
	void deleteBillAttribute(RMSBillAttribute billAttribute);
	
	// Query Operations
	List<RMSBillAttribute> getBillAttributesByBillId(Integer billId);
	
	List<RMSBillAttribute> getBillAttributesByBillUuid(String billUuid);
	
	List<RMSBillAttribute> getBillAttributesByTypeId(Integer billAttributeTypeId);
	
	List<RMSBillAttribute> getAllBillAttributes(Boolean includeVoided);
	
	List<RMSBillAttribute> getAllBillAttributesByBillId(Integer billId, Boolean includeVoided);
	
	// Type Operations
	RMSBillAttributeType saveBillAttributeType(RMSBillAttributeType billAttributeType);
	
	RMSBillAttributeType getBillAttributeType(Integer billAttributeTypeId);
	
	RMSBillAttributeType getBillAttributeTypeByUuid(String typeUuid);
	
	List<RMSBillAttributeType> getAllBillAttributeTypes(Boolean includeRetired);
	
	// Utility Operations
	void voidBillAttribute(RMSBillAttribute billAttribute, String reason, User voidedBy);
	
	void unvoidBillAttribute(RMSBillAttribute billAttribute);
	
	void retireBillAttributeType(RMSBillAttributeType billAttributeType, String reason, User retiredBy);
	
	void unretireBillAttributeType(RMSBillAttributeType billAttributeType);
}
