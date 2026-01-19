/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.rmsdataexchange.api.impl;

import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.DataException;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.UserService;
import org.openmrs.api.db.DAOException;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.fhir2.api.translators.PatientTranslator;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeDao;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeService;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueue;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueueSystem;
import org.springframework.transaction.annotation.Transactional;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;

import java.util.List;
import java.util.Set;

public class RmsdataexchangeServiceImpl extends BaseOpenmrsService implements RmsdataexchangeService {
	
	RmsdataexchangeDao dao;
	
	UserService userService;
	
	private PatientTranslator patientTranslator;
	
	private EncounterTranslator<org.openmrs.Encounter> encounterTranslator;
	
	private ObservationTranslator observationTranslator;
	
	public PatientTranslator getPatientTranslator() {
		return patientTranslator;
	}
	
	public void setPatientTranslator(PatientTranslator patientTranslator) {
		this.patientTranslator = patientTranslator;
	}
	
	public EncounterTranslator<org.openmrs.Encounter> getEncounterTranslator() {
		return encounterTranslator;
	}
	
	public void setEncounterTranslator(EncounterTranslator<org.openmrs.Encounter> encounterTranslator) {
		this.encounterTranslator = encounterTranslator;
	}
	
	public ObservationTranslator getObservationTranslator() {
		return observationTranslator;
	}
	
	public void setObservationTranslator(ObservationTranslator observationTranslator) {
		this.observationTranslator = observationTranslator;
	}
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setDao(RmsdataexchangeDao dao) {
		this.dao = dao;
	}
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setUserService(UserService userService) {
		this.userService = userService;
	}
	
	@Override
	public org.hl7.fhir.r4.model.Patient convertPatientToFhirResource(Patient patient) {
		return (patientTranslator.toFhirResource(patient));
	}
	
	@Override
	public org.hl7.fhir.r4.model.Encounter convertEncounterToFhirResource(Encounter encounter) {
		return (encounterTranslator.toFhirResource(encounter));
	}
	
	@Override
	public org.hl7.fhir.r4.model.Observation convertObservationToFhirResource(Obs obs) {
		return (observationTranslator.toFhirResource(obs));
	}
	
	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	@Transactional(readOnly = true)
	public Set<Payment> getPaymentsByBillId(Integer billId) {
		Set<Payment> payments = dao.getPaymentsByBillId(billId);
		return payments;
	}
	
	@Override
	public List<RMSQueue> getQueueItems() {
		return (dao.getQueueItems());
	}
	
	@Override
	public RMSQueue saveQueueItem(RMSQueue queue) {
		return (dao.saveQueueItem(queue));
	}
	
	@Override
	public RMSQueue getQueueItemByUUID(String queueUUID) throws DataException {
		return (dao.getQueueItemByUUID(queueUUID));
	}
	
	@Override
	public RMSQueue getQueueItemByID(Integer queueID) throws DataException {
		return (dao.getQueueItemByID(queueID));
	}
	
	@Override
	public RMSQueue removeQueueItem(RMSQueue queue) throws DAOException {
		return (dao.removeQueueItem(queue));
	}
	
	@Override
	public RMSQueueSystem getQueueSystemByUUID(String queueSystemUUID) throws DataException {
		return (dao.getQueueSystemByUUID(queueSystemUUID));
	}
	
	@Override
	public RMSQueueSystem getQueueSystemByID(Integer queueSystemID) throws DataException {
		return (dao.getQueueSystemByID(queueSystemID));
	}
	
	@Override
	public Concept getLatestObsConcept(Patient patient, String conceptIdentifier) {
		return (dao.getLatestObsConcept(patient, conceptIdentifier));
	}
}
