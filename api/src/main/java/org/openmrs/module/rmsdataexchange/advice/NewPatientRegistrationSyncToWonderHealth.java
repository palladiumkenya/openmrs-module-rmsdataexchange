package org.openmrs.module.rmsdataexchange.advice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeService;
import org.openmrs.module.rmsdataexchange.api.util.AdviceUtils;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.openmrs.module.rmsdataexchange.api.util.SimpleObject;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;

import org.openmrs.api.VisitService;
import org.openmrs.module.fhir2.api.FhirPatientService;
import org.openmrs.module.fhir2.api.translators.PatientTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerTranslator;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.openmrs.PersonName;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.HumanName;
import java.util.UUID;

import org.openmrs.Patient;
import org.openmrs.PersonName;
import org.openmrs.PersonAddress;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.context.Context;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;

/**
 * Detects when a new visit has started and syncs patient data to Wonder Health
 */
@Component("rmsdataexchange.NewPatientRegistrationSyncToWonderHealth")
public class NewPatientRegistrationSyncToWonderHealth implements AfterReturningAdvice {
	
	private Boolean debugMode = false;
	
	private static final Integer MAX_CHILD_AGE = 5;
	
	private PatientTranslator patientTranslator;
	
	public PatientTranslator getPatientTranslator() {
		return patientTranslator;
	}
	
	public void setPatientTranslator(PatientTranslator patientTranslator) {
		this.patientTranslator = patientTranslator;
	}
	
	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		try {
			debugMode = AdviceUtils.isRMSLoggingEnabled();
			if (AdviceUtils.isWonderHealthIntegrationEnabled()) {
				// Check if the method is "saveVisit"
				if (debugMode)
					System.out.println("rmsdataexchange Module: Wonder Health: Method: " + method.getName());
				if (method.getName().equals("saveVisit") && args.length > 0 && args[0] instanceof Visit) {
					
					for (Object object : args) {
						if (debugMode)
							System.out.println("rmsdataexchange Module: Wonder Health: Object Type: "
							        + object.getClass().getName());
					}
					
					Visit visit = (Visit) args[0];
					
					// check visit info and only process new visits
					if (visit != null && visit.getStopDatetime() == null) {
						if (debugMode)
							System.out.println("rmsdataexchange Module: Visit End Date: " + visit.getStopDatetime());
						Patient patient = visit.getPatient();
						
						if (patient != null) {
							// Check if male or female
							if (patient.getGender().equalsIgnoreCase("F") || patient.getAge() <= 6) {
								if (debugMode)
									System.out.println("rmsdataexchange Module: New patient checked in");
								if (debugMode)
									System.out.println("rmsdataexchange Module: Patient Name: "
									        + patient.getPersonName().getFullName());
								if (debugMode)
									System.out.println("rmsdataexchange Module: Patient DOB: " + patient.getBirthdate());
								if (debugMode)
									System.out.println("rmsdataexchange Module: Patient Age: " + patient.getAge());
								
								String payload = preparePatientPayload(patient);
								// Use a thread to send the data. This frees up the frontend to proceed
								syncPatientRunnable runner = new syncPatientRunnable(payload);
								Thread thread = new Thread(runner);
								thread.start();
							} else {
								if (debugMode)
									System.out
									        .println("rmsdataexchange Module: Wonder Health: The patient is not female and not below 7 years old");
							}
						} else {
							if (debugMode)
								System.out
								        .println("rmsdataexchange Module: Wonder Health: Error: No patient attached to the visit");
						}
						
					} else {
						if (debugMode)
							System.out.println("rmsdataexchange Module: Wonder Health: Error: Not a new visit.");
					}
				}
			}
		}
		catch (Exception ex) {
			if (debugMode)
				System.err.println("rmsdataexchange Module: Error getting new patient: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	/**
	 * Prepare the FHIR R4 JSON payload for patient registration
	 * 
	 * @param patient
	 * @return
	 */
	private String preparePatientPayload(@NotNull Patient patient) {
		String ret = "";
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		
		try {
			Context.openSession();
			Context.addProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
			Context.addProxyPrivilege(PrivilegeConstants.GET_RELATIONSHIPS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_RELATIONSHIP_TYPES);
			Context.addProxyPrivilege(PrivilegeConstants.GET_PATIENTS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_PERSONS);
			if (patient != null) {
				// Create a new FHIR bundle
				Bundle bundle = new Bundle();
				bundle.setType(Bundle.BundleType.COLLECTION);
				
				org.hl7.fhir.r4.model.Patient patientResource = new org.hl7.fhir.r4.model.Patient();
				RmsdataexchangeService rmsdataexchangeService = Context.getService(RmsdataexchangeService.class);
				
				if (patientTranslator == null) {
					if (debugMode)
						System.out.println("rmsdataexchange Module: Patient translator is null we call it manually");
					try {
						patientTranslator = Context.getRegisteredComponent("patientTranslatorImpl", PatientTranslator.class);
						if (debugMode)
							System.out.println("rmsdataexchange Module: Got the Patient translator");
					}
					catch (Exception ex) {
						if (debugMode)
							System.out
							        .println("rmsdataexchange Module: Completely failed loading the FHIR patientTranslator: "
							                + ex.getMessage());
						ex.printStackTrace();
					}
				}
				
				if (patientTranslator != null) {
					if (debugMode)
						System.out.println("rmsdataexchange Module: Using patient translator to get the payload");
					try {
						patientResource = patientTranslator.toFhirResource(patient);
					}
					catch (Exception ex) {
						if (debugMode)
							System.out.println("rmsdataexchange Module: Patient translator error: " + ex.getMessage());
						ex.printStackTrace();
						if (debugMode)
							System.out.println("rmsdataexchange Module: Using the service to convert to FHIR");
						patientResource = rmsdataexchangeService.convertPatientToFhirResource(patient);
					}
				} else {
					if (debugMode)
						System.out.println("rmsdataexchange Module: Manually constructing the payload");
					// Set Patient ID
					patientResource.setId(patient.getUuid());
					
					// Meta info
					// Generate a random UUID (v4)
					UUID uuid = UUID.randomUUID();
					Meta meta = new Meta();
					meta.setVersionId(uuid.toString());
					meta.setLastUpdated(new Date());
					patientResource.setMeta(meta);
					
					// Map Name
					PersonName personName = patient.getPersonName();
					if (personName != null) {
						HumanName name = new HumanName();
						name.setFamily(personName.getFamilyName());
						name.addGiven(personName.getGivenName());
						if (personName.getMiddleName() != null) {
							name.addGiven(personName.getMiddleName());
						}
						patientResource.addName(name);
					}
					
					// Map Identifiers
					for (PatientIdentifier identifier : patient.getActiveIdentifiers()) {
						Identifier fhirIdentifier = new Identifier();
						fhirIdentifier.setSystem("http://fhir.openmrs.org/ext/patient/identifier#system");
						fhirIdentifier.setValue(identifier.getIdentifier());
						fhirIdentifier.setUse(Identifier.IdentifierUse.OFFICIAL);
						patientResource.addIdentifier(fhirIdentifier);
					}
					
					// Map Address
					for (PersonAddress address : patient.getAddresses()) {
						Address fhirAddress = new Address();
						fhirAddress.addLine(address.getAddress1());
						fhirAddress.addLine(address.getAddress2());
						fhirAddress.setCity(address.getCityVillage());
						fhirAddress.setState(address.getStateProvince());
						fhirAddress.setPostalCode(address.getPostalCode());
						fhirAddress.setCountry(address.getCountry());
						fhirAddress.setUse(Address.AddressUse.HOME);
						patientResource.addAddress(fhirAddress);
					}
					
					// Map Gender
					if ("M".equalsIgnoreCase(patient.getGender())) {
						patientResource.setGender(AdministrativeGender.MALE);
					} else if ("F".equalsIgnoreCase(patient.getGender())) {
						patientResource.setGender(AdministrativeGender.FEMALE);
					} else {
						patientResource.setGender(AdministrativeGender.UNKNOWN);
					}
					
					// Map Birthdate
					patientResource.setBirthDate(patient.getBirthdate());
					
					// Map Deceased status
					patientResource.setDeceased(new BooleanType(patient.getDead()));
					
					// Organization
					// patientResource.setManagingOrganization(null);
				}
				
				// Add primary patient to bundle
				bundle.addEntry().setFullUrl(FhirConstants.PATIENT + "/" + patientResource.getIdElement().getIdPart())
				        .setResource(patientResource);
				
				if (debugMode)
					System.out.println("rmsdataexchange Module: Creating FHIR payload for patient: " + patient.getUuid());
				
				//Get any relationships (children under 2yrs age)
				List<Relationship> relationships = Context.getPersonService().getRelationshipsByPerson(patient);
				
				/*+----------------------+--------------------------------------+------------+--------------+
				| relationship_type_id | uuid                                 | a_is_to_b  | b_is_to_a    |
				+----------------------+--------------------------------------+------------+--------------+
				|                    1 | 8d919b58-c2cc-11de-8d13-0010c6dffd0f | Doctor     | Patient      |
				|                    2 | 8d91a01c-c2cc-11de-8d13-0010c6dffd0f | Sibling    | Sibling      |
				|                    3 | 8d91a210-c2cc-11de-8d13-0010c6dffd0f | Parent     | Child        |
				|                    4 | 8d91a3dc-c2cc-11de-8d13-0010c6dffd0f | Aunt/Uncle | Niece/Nephew |
				|                    5 | 5f115f62-68b7-11e3-94ee-6bef9086de92 | Guardian   | Dependant    |
				|                    6 | d6895098-5d8d-11e3-94ee-b35a4132a5e3 | Spouse     | Spouse       |
				|                    7 | 007b765f-6725-4ae9-afee-9966302bace4 | Partner    | Partner      |
				|                    8 | 2ac0d501-eadc-4624-b982-563c70035d46 | Co-wife    | Co-wife      |
				+----------------------+--------------------------------------+------------+--------------+
				*/
				
				// Add related child patients to the bundle
				for (Relationship relationship : relationships) {
					if (relationship.getRelationshipType().getUuid()
					        .equalsIgnoreCase("8d91a210-c2cc-11de-8d13-0010c6dffd0f")) {
						Person relatedPerson = relationship.getPersonB(); // Child
						
						if (relatedPerson != null && relatedPerson.getAge() <= MAX_CHILD_AGE) {
							Patient relatedOpenmrsPatient = Context.getPatientService().getPatientByUuid(
							    relatedPerson.getUuid());
							if (relatedOpenmrsPatient != null) {
								org.hl7.fhir.r4.model.Patient fhirRelatedPatient = patientTranslator
								        .toFhirResource(relatedOpenmrsPatient);
								if (fhirRelatedPatient != null) {
									bundle.addEntry()
									        .setFullUrl(
									            FhirConstants.PATIENT + "/" + fhirRelatedPatient.getIdElement().getIdPart())
									        .setResource(fhirRelatedPatient);
									
									// Add an extension to the primary patient for the relationship
									Extension relationshipExtension = new Extension();
									relationshipExtension
									        .setUrl("http://hl7.org/fhir/StructureDefinition/patient-relationship");
									
									// Add relationship type (e.g., sibling, parent)
									RelationshipType relationshipType = relationship.getRelationshipType();
									if (relationshipType != null) {
										relationshipExtension.addExtension(new Extension("type",
										        new org.hl7.fhir.r4.model.CodeableConcept().setText("Child")));
									}
									
									// Add reference to the related patient
									Reference relatedPatientReference = new Reference();
									relatedPatientReference.setReference(FhirConstants.PATIENT + "/"
									        + fhirRelatedPatient.getIdElement().getIdPart());
									relatedPatientReference.setDisplay(relatedOpenmrsPatient.getPersonName().getFullName());
									relationshipExtension.addExtension(new Extension("relatedPatient",
									        relatedPatientReference));
									
									// Add the extension to the primary patient
									patientResource.addExtension(relationshipExtension);
								}
							}
						}
					}
				}
				
				FhirContext fhirContext = FhirContext.forR4();
				ret = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
				if (debugMode)
					System.out.println("rmsdataexchange Module: Got FHIR patient registration details: " + ret);
				// } else {
				// 	if (debugMode)
				// 		System.out.println("rmsdataexchange Module: ERROR: failed to load FHIR patient service");
				// }
			} else {
				if (debugMode)
					System.out.println("rmsdataexchange Module: ERROR: patient is null");
			}
		}
		catch (Exception ex) {
			if (debugMode)
				System.err.println("rmsdataexchange Module: Error getting new patient payload: " + ex.getMessage());
			ex.printStackTrace();
		}
		finally {
			Context.closeSession();
		}
		
		return (ret);
	}
	
	/**
	 * Send the patient registration payload to Wonder Health
	 * 
	 * @param patient
	 * @return
	 */
	private Boolean sendWonderHealthPatientRegistration(@NotNull String patient) {
		Boolean ret = false;
		String payload = patient;
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		
		HttpsURLConnection con = null;
		HttpsURLConnection connection = null;
		try {
			if (debugMode)
				System.out.println("rmsdataexchange Module: Wonder Health using payload: " + payload);
			
			// Get Auth
			String authToken = AdviceUtils.getWonderHealthAuthToken();
			
			if (!StringUtils.isEmpty(authToken) && !authToken.isEmpty()) {
				try {
					// We send the payload to Wonder Health
					if (debugMode)
						System.err
						        .println("rmsdataexchange Module: Wonder Health We got the Auth token. Now sending the patient registration details. Token: "
						                + authToken);
					String wonderHealthUrl = AdviceUtils.getWonderHealthEndpointURL();
					if (debugMode)
						System.out.println("rmsdataexchange Module: Wonder health patient registration URL: "
						        + wonderHealthUrl);
					URL finWonderHealthUrl = new URL(wonderHealthUrl);
					
					connection = (HttpsURLConnection) finWonderHealthUrl.openConnection();
					connection.setRequestMethod("POST");
					connection.setDoOutput(true);
					connection.setRequestProperty("access-token", authToken);
					connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
					connection.setRequestProperty("Accept", "application/json");
					connection.setConnectTimeout(10000);
					
					PrintStream pos = new PrintStream(connection.getOutputStream());
					pos.print(payload);
					pos.close();
					
					int finalResponseCode = connection.getResponseCode();
					
					if (finalResponseCode == HttpURLConnection.HTTP_OK) { //success
						BufferedReader fin = null;
						fin = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						
						String finalOutput;
						StringBuffer finalResponse = new StringBuffer();
						
						while ((finalOutput = fin.readLine()) != null) {
							finalResponse.append(finalOutput);
						}
						fin.close();
						
						String finalReturnResponse = finalResponse.toString();
						if (debugMode)
							System.out
							        .println("rmsdataexchange Module: Wonder Health Got patient registration Response as: "
							                + finalReturnResponse);
						
						ObjectMapper finalMapper = new ObjectMapper();
						JsonNode finaljsonNode = null;
						Boolean success = false;
						String message = "";
						
						try {
							finaljsonNode = finalMapper.readTree(finalReturnResponse);
							if (finaljsonNode != null) {
								success = finaljsonNode.get("success") == null ? false : finaljsonNode.get("success")
								        .getBooleanValue();
								message = finaljsonNode.get("message") == null ? "" : finaljsonNode.get("message")
								        .getTextValue();
							}
							
							if (debugMode)
								System.err
								        .println("rmsdataexchange Module: Wonder Health  Got patient registration final response: success: "
								                + success + " message: " + message);
						}
						catch (Exception e) {
							if (debugMode)
								System.err
								        .println("rmsdataexchange Module: Wonder Health Error getting patient registration final response: "
								                + e.getMessage());
							e.printStackTrace();
						}
						
						if (success != null && success == true) {
							ret = true;
						}
						
					} else {
						if (debugMode)
							System.err.println("rmsdataexchange Module: Wonder Health Failed to send final payload: "
							        + finalResponseCode);
					}
				}
				catch (Exception em) {
					if (debugMode)
						System.err.println("rmsdataexchange Module: Wonder Health Error. Failed to send the final payload: "
						        + em.getMessage());
					em.printStackTrace();
				}
			}
			
		}
		catch (Exception ex) {
			if (debugMode)
				System.err.println("rmsdataexchange Module: Wonder Health Error. Failed to get auth token: "
				        + ex.getMessage());
			ex.printStackTrace();
		}
		
		return (ret);
	}
	
	/**
	 * Returns the patient identifier
	 * 
	 * @param patient
	 * @param patientIdentifierType
	 * @return
	 */
	private String getPatientIdentifier(Patient patient, PatientIdentifierType patientIdentifierType) {
		String ret = "";
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		
		if (patientIdentifierType != null && patient != null) {
			try {
				Set<PatientIdentifier> identifiers = patient.getIdentifiers();
				
				for (PatientIdentifier patientIdentifier : identifiers) {
					if (!patientIdentifier.getVoided()
					        && patientIdentifier.getIdentifierType().equals(patientIdentifierType)) {
						if (patientIdentifier != null) {
							ret = patientIdentifier.getIdentifier();
							if (debugMode)
								System.err.println("rmsdataexchange Module: Got the identifier as: " + ret);
							break;
						}
					}
				}
			}
			catch (Exception ex) {
				if (debugMode)
					System.err.println("rmsdataexchange Module: Getting the identifier: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
		
		return (ret);
	}
	
	/**
	 * A thread to free up the frontend
	 */
	private class syncPatientRunnable implements Runnable {
		
		String patient = "";
		
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		
		public syncPatientRunnable(@NotNull String patient) {
			this.patient = patient;
		}
		
		@Override
		public void run() {
			// Run the thread
			
			try {
				if (debugMode)
					System.out.println("rmsdataexchange Module: Start sending patient to RMS");
				
				sendWonderHealthPatientRegistration(patient);
				
				if (debugMode)
					System.out.println("rmsdataexchange Module: Finished sending patient to RMS");
			}
			catch (Exception ex) {
				if (debugMode)
					System.err.println("rmsdataexchange Module: Error. Failed to send patient to RMS: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
}
