package org.openmrs.module.rmsdataexchange.advice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;
import org.openmrs.module.fhir2.api.translators.LocationTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.fhir2.api.translators.PatientTranslator;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.openmrs.module.rmsdataexchange.RmsdataexchangeActivator;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeService;
import org.openmrs.module.rmsdataexchange.api.util.AdviceUtils;
import org.openmrs.module.rmsdataexchange.api.util.RMSModuleConstants;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueueSystem;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.aop.AfterReturningAdvice;

import ca.uhn.fhir.context.FhirContext;

/**
 * Detects when a visit has ended and syncs patient data to Kisumu HIE
 */
public class HIEMaternalProfileAdvice implements AfterReturningAdvice {
	
	private Boolean debugMode = false;
	
	private static final Integer MAX_CHILD_AGE = 5;
	
	private PatientTranslator patientTranslator;
	
	private LocationTranslator locationTranslator;

	private EncounterTranslator<org.openmrs.Encounter> encounterTranslator;

	private ObservationTranslator observationTranslator;
	
	public PatientTranslator getPatientTranslator() {
		return patientTranslator;
	}
	
	public void setPatientTranslator(PatientTranslator patientTranslator) {
		this.patientTranslator = patientTranslator;
	}
	
	public LocationTranslator getLocationTranslator() {
		return locationTranslator;
	}
	
	public void setLocationTranslator(LocationTranslator locationTranslator) {
		this.locationTranslator = locationTranslator;
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

	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		try {
			debugMode = AdviceUtils.isRMSLoggingEnabled();
			if (AdviceUtils.isWonderHealthIntegrationEnabled()) {
				
				if (method.getName().equals("saveVisit") && args.length > 0 && args[0] instanceof Visit) {
					
					Visit visit = (Visit) args[0];
					
					// check visit info and only process new visits
					if (visit != null && visit.getStopDatetime() == null) {
						// Check if visit is already being/been processed (using user property) NB: When checking in a patient, a strange thing happens
						// If you select to add to the queue, the method "saveVisit" is called twice. This fixes the anomourous behavior
						String syncCheck = Context.getAuthenticatedUser().getUserProperty("visit-" + visit.getUuid());
						if (debugMode)
							System.out.println("rmsdataexchange Module: Kisumu HIE: Sync check is: " + syncCheck);
						
						if (syncCheck == null || syncCheck.trim().equalsIgnoreCase("0") || syncCheck.isEmpty()
						        || syncCheck.trim().equalsIgnoreCase("")) {
							if (debugMode)
								System.out
								        .println("rmsdataexchange Module: Kisumu HIE: Visit not processed yet. Now processing");
							Context.getAuthenticatedUser().setUserProperty("visit-" + visit.getUuid(), "1");
							

							if (debugMode)
								System.out.println("rmsdataexchange Module: Visit End Date: " + visit.getStopDatetime());
							if (debugMode)
								System.out.println("rmsdataexchange Module: Visit UUID: " + visit.getUuid());
							if (debugMode)
								System.out.println("rmsdataexchange Module: Visit Date Changed: "
										+ visit.getDateChanged());
							Patient patient = visit.getPatient();
							
							if (patient != null) {
								// Ensure the patient is female and pregnant
								if (patient.getGender().equalsIgnoreCase("F") && AdviceUtils.isPatientPregnant(patient)) {
									if (debugMode)
										System.out.println("rmsdataexchange Module: New patient checked in");
									if (debugMode)
										System.out.println("rmsdataexchange Module: Patient Name: "
												+ patient.getPersonName().getFullName());
									if (debugMode)
										System.out.println("rmsdataexchange Module: Patient DOB: "
												+ patient.getBirthdate());
									if (debugMode)
										System.out.println("rmsdataexchange Module: Patient Age: " + patient.getAge());
									
									String payload = prepareMaternalProfilePayload(visit);
									// Use a thread to send the data. This frees up the frontend to proceed
									syncPatientRunnable runner = new syncPatientRunnable(payload, patient);
									Daemon.runInDaemonThread(runner, RmsdataexchangeActivator.getDaemonToken());
								} else {
									if (debugMode)
										System.out
												.println("rmsdataexchange Module: Kisumu HIE: The patient is not female and not below 7 years old");
								}
							} else {
								if (debugMode)
									System.out
											.println("rmsdataexchange Module: Kisumu HIE: Error: No patient attached to the visit");
							}
						} else {
							if (debugMode)
								System.out
								        .println("rmsdataexchange Module: Kisumu HIE: Visit already processed. We ignore.");
						}
						
					} else {
						if (debugMode)
							System.out.println("rmsdataexchange Module: Kisumu HIE: Not a new visit. We ignore.");
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
	 * Prepare the FHIR R4 JSON payload for Maternal Profile
	 * 
	 * @param patient
	 * @return
	 */
	private String prepareMaternalProfilePayload(@NotNull Visit visit) {
		String ret = "";
		Boolean debugMode = false;
		
		try {
			if (Context.isSessionOpen()) {
				System.out.println("rmsdataexchange Module: We have an open session J");
				Context.addProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
				Context.addProxyPrivilege(PrivilegeConstants.GET_RELATIONSHIPS);
				Context.addProxyPrivilege(PrivilegeConstants.GET_RELATIONSHIP_TYPES);
				Context.addProxyPrivilege(PrivilegeConstants.GET_PATIENTS);
				Context.addProxyPrivilege(PrivilegeConstants.GET_PERSONS);
				Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			} else {
				System.out.println("rmsdataexchange Module: Error: We have NO open session J");
				Context.openSession();
				Context.addProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
				Context.addProxyPrivilege(PrivilegeConstants.GET_RELATIONSHIPS);
				Context.addProxyPrivilege(PrivilegeConstants.GET_RELATIONSHIP_TYPES);
				Context.addProxyPrivilege(PrivilegeConstants.GET_PATIENTS);
				Context.addProxyPrivilege(PrivilegeConstants.GET_PERSONS);
				Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			}
			debugMode = AdviceUtils.isRMSLoggingEnabled();
			
			if (visit != null) {
				// Create a new FHIR bundle
				Bundle bundle = new Bundle();
				bundle.setType(Bundle.BundleType.TRANSACTION);
				
				org.hl7.fhir.r4.model.Patient patientResource = new org.hl7.fhir.r4.model.Patient();
				org.hl7.fhir.r4.model.Encounter encounterResource = new org.hl7.fhir.r4.model.Encounter();
				org.hl7.fhir.r4.model.Observation observationResource = new org.hl7.fhir.r4.model.Observation();

				RmsdataexchangeService rmsdataexchangeService = Context.getService(RmsdataexchangeService.class);
				
				if (encounterTranslator == null) {
					if (debugMode)
						System.out.println("rmsdataexchange Module: Encounter translator is null we call it manually");
					try {
						encounterTranslator = Context.getRegisteredComponent("encounterTranslatorImpl", EncounterTranslator.class);
						if (debugMode)
							System.out.println("rmsdataexchange Module: Got the Encounter translator");
					}
					catch (Exception ex) {
						if (debugMode)
							System.out
							        .println("rmsdataexchange Module: Completely failed loading the FHIR EncounterTranslator: "
							                + ex.getMessage());
						ex.printStackTrace();
					}
				}

				if (observationTranslator == null) {
					if (debugMode)
						System.out.println("rmsdataexchange Module: Observation translator is null we call it manually");
					try {
						observationTranslator = Context.getRegisteredComponent("observationTranslatorImpl", ObservationTranslator.class);
						if (debugMode)
							System.out.println("rmsdataexchange Module: Got the Observation translator");
					}
					catch (Exception ex) {
						if (debugMode)
							System.out
							        .println("rmsdataexchange Module: Completely failed loading the FHIR ObservationTranslator: "
							                + ex.getMessage());
						ex.printStackTrace();
					}
				}

				Set<Encounter> encounters = visit.getEncounters();

				for(Encounter enc : encounters) {


					Set<Obs> obs = enc.getAllObs();
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

				}
				
				// Add primary patient to bundle
				bundle.addEntry().setFullUrl(FhirConstants.PATIENT + "/" + patientResource.getIdElement().getIdPart())
				        .setResource(patientResource);
				
				if (debugMode)
					System.out.println("rmsdataexchange Module: Creating FHIR payload for patient: " + patient.getUuid());
				
				
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
					System.out.println("rmsdataexchange Module: ERROR: visit is null");
			}
		}
		catch (Exception ex) {
			if (debugMode)
				System.err.println("rmsdataexchange Module: Error getting maternal profile payload: " + ex.getMessage());
			ex.printStackTrace();
		}
		finally {
			// Context.closeSession();
		}
		
		return (ret);
	}
	
	/**
	 * Send the patient registration payload to Kisumu HIE
	 * 
	 * @param patient
	 * @return
	 */
	public static Boolean sendWonderHealthPatientRegistration(@NotNull String patient) {
		Boolean ret = false;
		String payload = patient;
		Boolean debugMode = false;
		
		// HttpsURLConnection con = null;
		HttpURLConnection connection = null;
		try {
			if (Context.isSessionOpen()) {
				System.out.println("rmsdataexchange Module: We have an open session K");
				Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			} else {
				System.out.println("rmsdataexchange Module: Error: We have NO open session K");
				Context.openSession();
				Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			}
			debugMode = AdviceUtils.isRMSLoggingEnabled();
			if (debugMode)
				System.out.println("rmsdataexchange Module: Kisumu HIE using payload: " + payload);
			
			// Get Auth
			String authToken = AdviceUtils.getWonderHealthAuthToken();
			
			if (authToken != null && !StringUtils.isEmpty(authToken) && !authToken.isEmpty()) {
				try {
					// We send the payload to Kisumu HIE
					if (debugMode)
						System.err
						        .println("rmsdataexchange Module: Kisumu HIE We got the Auth token. Now sending the patient registration details. Token: "
						                + authToken);
					String wonderHealthUrl = AdviceUtils.getWonderHealthEndpointURL();
					if (debugMode)
						System.out.println("rmsdataexchange Module: Wonder health patient registration URL: "
						        + wonderHealthUrl);
					URL finWonderHealthUrl = new URL(wonderHealthUrl);
					
					// Debug TODO: remove in production
					AdviceUtils.trustAllCerts();
					
					if (finWonderHealthUrl.getProtocol().equalsIgnoreCase("https")) {
						connection = (HttpsURLConnection) finWonderHealthUrl.openConnection();
					} else if (finWonderHealthUrl.getProtocol().equalsIgnoreCase("http")) {
						connection = (HttpURLConnection) finWonderHealthUrl.openConnection();
					}
					
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
							        .println("rmsdataexchange Module: Kisumu HIE Got patient registration Response as: "
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
								        .println("rmsdataexchange Module: Kisumu HIE  Got patient registration final response: success: "
								                + success + " message: " + message);
						}
						catch (Exception e) {
							if (debugMode)
								System.err
								        .println("rmsdataexchange Module: Kisumu HIE Error getting patient registration final response: "
								                + e.getMessage());
							e.printStackTrace();
						}
						
						if (success != null && success == true) {
							ret = true;
						}
						
					} else {
						if (debugMode)
							System.err.println("rmsdataexchange Module: Kisumu HIE Failed to send final payload: "
							        + finalResponseCode);
					}
				}
				catch (Exception em) {
					if (debugMode)
						System.err.println("rmsdataexchange Module: Kisumu HIE Error. Failed to send the final payload: "
						        + em.getMessage());
					em.printStackTrace();
				}
			} else {
				if (debugMode)
					System.err
					        .println("rmsdataexchange Module: Kisumu HIE Error. Failed to send the final payload: Empty auth token");
			}
			
		}
		catch (Exception ex) {
			if (debugMode)
				System.err.println("rmsdataexchange Module: Kisumu HIE Error. Failed to get auth token: "
				        + ex.getMessage());
			ex.printStackTrace();
		}
		finally {
			// Context.closeSession();
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
		
		String payload = "";
		
		Patient patient = null;
		
		Boolean debugMode = false;
		
		public syncPatientRunnable(@NotNull String payload, @NotNull Patient patient) {
			this.payload = payload;
			this.patient = patient;
		}
		
		@Override
		public void run() {
			
			try {
				if (Context.isSessionOpen()) {
					System.out.println("rmsdataexchange Module: We have an open session L");
					Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
					Context.addProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
				} else {
					System.out.println("rmsdataexchange Module: Error: We have NO open session L");
					Context.openSession();
					Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
					Context.addProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
				}
				debugMode = AdviceUtils.isRMSLoggingEnabled();
				
				if (debugMode)
					System.out.println("rmsdataexchange Module: Start sending patient to Kisumu HIE");
				
				Integer sleepTime = AdviceUtils.getRandomInt(5000, 10000);
				// Delay
				try {
					//Delay for random seconds
					if (debugMode)
						System.out.println("rmsdataexchange Module: Sleep for milliseconds: " + sleepTime);
					Thread.sleep(sleepTime);
				}
				catch (Exception ie) {
					Thread.currentThread().interrupt();
				}
				
				Boolean sendWonderHealthResult = sendWonderHealthPatientRegistration(payload);
				
				if (sendWonderHealthResult == false) {
					// Failed to send the payload. We put it in the queue
					if (debugMode)
						System.err
						        .println("rmsdataexchange Module: Failed to send patient to Kisumu HIE. Adding to queue");
					RmsdataexchangeService rmsdataexchangeService = Context.getService(RmsdataexchangeService.class);
					RMSQueueSystem rmsQueueSystem = rmsdataexchangeService
					        .getQueueSystemByUUID(RMSModuleConstants.WONDER_HEALTH_SYSTEM_PATIENT);
					Boolean addToQueue = AdviceUtils.addSyncPayloadToQueue(payload, rmsQueueSystem);
					if (addToQueue) {
						if (debugMode)
							System.out.println("rmsdataexchange Module: Finished adding patient to Kisumu HIE Queue");
						// Mark sent using person attribute
						AdviceUtils.setPersonAttributeValueByTypeUuid(patient,
						    RMSModuleConstants.PERSON_ATTRIBUTE_WONDER_HEALTH_SYNCHRONIZED_UUID, "1");
					} else {
						if (debugMode)
							System.err
							        .println("rmsdataexchange Module: Error: Failed to add patient to Kisumu HIE Queue");
						// Mark NOT sent using person attribute
						AdviceUtils.setPersonAttributeValueByTypeUuid(patient,
						    RMSModuleConstants.PERSON_ATTRIBUTE_WONDER_HEALTH_SYNCHRONIZED_UUID, "0");
					}
				} else {
					// Success sending the patient
					if (debugMode)
						System.out.println("rmsdataexchange Module: Finished sending patient to Kisumu HIE");
					// Mark sent using person attribute
					AdviceUtils.setPersonAttributeValueByTypeUuid(patient,
					    RMSModuleConstants.PERSON_ATTRIBUTE_WONDER_HEALTH_SYNCHRONIZED_UUID, "1");
				}
				
			}
			catch (Exception ex) {
				if (debugMode)
					System.err.println("rmsdataexchange Module: Error. Failed to send patient to Kisumu HIE: "
					        + ex.getMessage());
				ex.printStackTrace();
			}
			finally {
				// Context.closeSession();
			}
		}
	}
	
}
