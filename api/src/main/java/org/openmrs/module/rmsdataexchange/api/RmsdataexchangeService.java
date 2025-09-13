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

import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueue;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueueSystem;
import org.springframework.transaction.annotation.Transactional;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.Person;

/**
 * The main service of this module, which is exposed for other modules. See
 * moduleApplicationContext.xml on how it is wired up.
 */
public interface RmsdataexchangeService extends OpenmrsService {
	
	@Transactional(readOnly = true)
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	Set<Payment> getPaymentsByBillId(Integer billId);
	
	@Transactional(readOnly = true)
	org.hl7.fhir.r4.model.Patient convertPatientToFhirResource(Patient patient);
	
	@Transactional(readOnly = true)
	List<RMSQueue> getQueueItems();
	
	@Transactional
	RMSQueue saveQueueItem(RMSQueue queue);
	
	@Transactional(readOnly = true)
	RMSQueue getQueueItemByUUID(String queueUUID);
	
	@Transactional(readOnly = true)
	RMSQueue getQueueItemByID(Integer queueID);
	
	@Transactional
	RMSQueue removeQueueItem(RMSQueue queue);
	
	@Transactional
	Concept getLatestObsConcept(Patient patient, String conceptIdentifier);
	
	@Transactional(readOnly = true)
	RMSQueueSystem getQueueSystemByUUID(String queueSystemUUID);
	
	@Transactional(readOnly = true)
	RMSQueueSystem getQueueSystemByID(Integer queueSystemID);
	
	// Boolean getBillAttribute(Bill bill, String attributeUUID);
	
	// Boolean getPaymentAttribute(Payment payment, String attributeUUID);
	
	// Boolean getPersonAttribute(Person person, String attributeUUID);
}
