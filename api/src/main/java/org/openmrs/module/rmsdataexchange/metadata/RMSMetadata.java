/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.rmsdataexchange.metadata;

// import org.openmrs.Form;
// import org.openmrs.PatientIdentifierType.LocationBehavior;
// import org.openmrs.api.AdministrationService;
// import org.openmrs.api.context.Context;
// import org.openmrs.PersonAttributeType;
// import org.openmrs.customdatatype.CustomDatatype;
// import org.openmrs.customdatatype.datatype.DateDatatype;
// import org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle;
import org.springframework.stereotype.Component;

// import org.openmrs.customdatatype.datatype.FreeTextDatatype;
// import org.openmrs.customdatatype.datatype.ConceptDatatype;

// import java.util.Date;

// import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.encounterType;
// import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.form;
// import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.globalProperty;
// import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.patientIdentifierType;
// import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.personAttributeType;
// import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.providerAttributeType;
// import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.relationshipType;
// import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.visitAttributeType;
// import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.visitType;

/**
 * RMS metadata bundle
 */
@Component
// public class RMSMetadata extends AbstractMetadataBundle {
public class RMSMetadata {
	
	public static final class _PersonAttributeType {
		
		public static final String RMS_PATIENT_SYNCHRONIZED = "5007a8d2-d136-4e4e-86fc-eaff284df906";
		
	}
	
	/**
	 * @see org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle#install()
	 */
	// @Override
	public void install() {
		
		// install(personAttributeType("Is RMS Synchronized", "Is RMS Synchronized (true or false)", String.class, null, true, 1.0, _PersonAttributeType.RMS_PATIENT_SYNCHRONIZED));
		
	}
}
