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

import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.UserService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.fhir2.api.translators.PatientTranslator;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeService;
import org.openmrs.module.rmsdataexchange.api.dao.RmsdataexchangeDao;
import org.springframework.transaction.annotation.Transactional;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import java.util.Set;

public class RmsdataexchangeServiceImpl extends BaseOpenmrsService implements RmsdataexchangeService {
	
	RmsdataexchangeDao dao;
	
	UserService userService;
	
	private PatientTranslator patientTranslator;
	
	public PatientTranslator getPatientTranslator() {
		return patientTranslator;
	}
	
	public void setPatientTranslator(PatientTranslator patientTranslator) {
		this.patientTranslator = patientTranslator;
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
	
	public org.hl7.fhir.r4.model.Patient convertPatientToFhirResource(Patient patient) {
		return (patientTranslator.toFhirResource(patient));
	}
	
	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	@Transactional(readOnly = true)
	public Set<Payment> getPaymentsByBillId(Integer billId) {
		Set<Payment> payments = dao.getPaymentsByBillId(billId);
		return payments;
	}
}
