package org.openmrs.module.rmsdataexchange.advice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;
import org.openmrs.module.fhir2.api.translators.LocationTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.fhir2.api.translators.PatientTranslator;
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
			if (AdviceUtils.isHIEMCHIntegrationEnabled()) {
				
				if (method.getName().equals("saveVisit") && args.length > 0 && args[0] instanceof Visit) {
					
					Visit visit = (Visit) args[0];
					
					// check visit info and only process the end of pregnant visits
					if (visit != null && visit.getStopDatetime() != null) {
						// Check if visit is already being/been processed (using user property) NB: When checking in a patient, a strange thing happens
						// If you select to add to the queue, the method "saveVisit" is called twice. This fixes the anomourous behavior
						String syncCheck = Context.getAuthenticatedUser()
						        .getUserProperty("hie-mch-visit-" + visit.getUuid());
						if (debugMode)
							System.out.println("rmsdataexchange Module: Kisumu HIE Maternal Profile: Sync check is: "
							        + syncCheck);
						
						if (syncCheck == null || syncCheck.trim().equalsIgnoreCase("0") || syncCheck.isEmpty()
						        || syncCheck.trim().equalsIgnoreCase("")) {
							if (debugMode)
								System.out
								        .println("rmsdataexchange Module: Kisumu HIE Maternal Profile: Visit not processed yet. Now processing");
							Context.getAuthenticatedUser().setUserProperty("hie-mch-visit-" + visit.getUuid(), "1");
							
							if (debugMode)
								System.out.println("rmsdataexchange Module: Kisumu HIE Maternal Profile: Visit End Date: "
								        + visit.getStopDatetime());
							if (debugMode)
								System.out.println("rmsdataexchange Module: Kisumu HIE Maternal Profile: Visit UUID: "
								        + visit.getUuid());
							if (debugMode)
								System.out
								        .println("rmsdataexchange Module: Kisumu HIE Maternal Profile: Visit Date Changed: "
								                + visit.getDateChanged());
							Patient patient = visit.getPatient();
							
							if (patient != null) {
								// Ensure the patient is female and pregnant
								if (patient.getGender().equalsIgnoreCase("F") && AdviceUtils.isPatientPregnant(patient)) {
									if (debugMode)
										System.out.println("rmsdataexchange Module: Kisumu HIE Maternal Profile: Patient is being checked out");
									if (debugMode)
										System.out.println("rmsdataexchange Module: Kisumu HIE Maternal Profile: Patient Name: "
										        + patient.getPersonName().getFullName());
									if (debugMode)
										System.out.println("rmsdataexchange Module: Kisumu HIE Maternal Profile: Patient DOB: " + patient.getBirthdate());
									if (debugMode)
										System.out.println("rmsdataexchange Module: Kisumu HIE Maternal Profile: Patient Age: " + patient.getAge());
									
									String payload = prepareMaternalProfilePayload(visit);
									// Use a thread to send the data. This frees up the frontend to proceed
									sendMaternalProfileRunnable runner = new sendMaternalProfileRunnable(payload, patient);
									Daemon.runInDaemonThread(runner, RmsdataexchangeActivator.getDaemonToken());
								} else {
									if (debugMode)
										System.out
										        .println("rmsdataexchange Module: Kisumu HIE Maternal Profile: The patient is either not female and/or not pregnant");
								}
							} else {
								if (debugMode)
									System.out
									        .println("rmsdataexchange Module: Kisumu HIE Maternal Profile: Error: No patient attached to the visit");
							}
						} else {
							if (debugMode)
								System.out
								        .println("rmsdataexchange Module: Kisumu HIE Maternal Profile: Visit already processed. We ignore.");
						}
						
					} else {
						if (debugMode)
							System.out
							        .println("rmsdataexchange Module: Kisumu HIE Maternal Profile: This is a new visit. We ignore.");
					}
				}
			}
		}
		catch (Exception ex) {
			if (debugMode)
				System.err.println("rmsdataexchange Module: Error getting maternal profile: " + ex.getMessage());
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
				System.out.println("rmsdataexchange Module: We have an open session");
				Context.addProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
				Context.addProxyPrivilege(PrivilegeConstants.GET_RELATIONSHIPS);
				Context.addProxyPrivilege(PrivilegeConstants.GET_RELATIONSHIP_TYPES);
				Context.addProxyPrivilege(PrivilegeConstants.GET_PATIENTS);
				Context.addProxyPrivilege(PrivilegeConstants.GET_PERSONS);
				Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			} else {
				System.out.println("rmsdataexchange Module: Error: We have NO open session");
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
				
				RmsdataexchangeService rmsdataexchangeService = Context.getService(RmsdataexchangeService.class);
				
				if (encounterTranslator == null) {
					if (debugMode)
						System.out.println("rmsdataexchange Module: Encounter translator is null we call it manually");
					try {
						encounterTranslator = Context.getRegisteredComponent("encounterTranslatorImpl",
						    EncounterTranslator.class);
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
						observationTranslator = Context.getRegisteredComponent("observationTranslatorImpl",
						    ObservationTranslator.class);
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
				
				for (Encounter enc : encounters) {
					org.hl7.fhir.r4.model.Encounter encounterResource = new org.hl7.fhir.r4.model.Encounter();
					if (encounterTranslator != null) {
						if (debugMode)
							System.out.println("rmsdataexchange Module: Using encounter translator to get the payload");
						try {
							encounterResource = encounterTranslator.toFhirResource(enc);
						}
						catch (Exception ex) {
							if (debugMode)
								System.out.println("rmsdataexchange Module: encounter translator error: " + ex.getMessage());
							ex.printStackTrace();
							if (debugMode)
								System.out.println("rmsdataexchange Module: Using the service to convert to FHIR");
							encounterResource = rmsdataexchangeService.convertEncounterToFhirResource(enc);
						}
					} else {
						if (debugMode)
							System.out.println("rmsdataexchange Module: HIE kisumu Manually constructing the payload");
						
						encounterResource.setId(enc.getUuid());
						
					}
					
					// Add encounter to bundle
					bundle.addEntry().setFullUrl(FhirConstants.PATIENT + "/" + encounterResource.getIdElement().getIdPart())
					        .setResource(encounterResource);
					
					Set<Obs> allObs = enc.getAllObs();
					
					for (Obs obs : allObs) {
						org.hl7.fhir.r4.model.Observation observationResource = new org.hl7.fhir.r4.model.Observation();
						if (observationTranslator != null) {
							if (debugMode)
								System.out.println("rmsdataexchange Module: Using observation translator to get the payload");
							try {
								observationResource = observationTranslator.toFhirResource(obs);
							}
							catch (Exception ex) {
								if (debugMode)
									System.out.println("rmsdataexchange Module: observation translator error: "
									        + ex.getMessage());
								ex.printStackTrace();
								if (debugMode)
									System.out.println("rmsdataexchange Module: Using the service to convert to FHIR");
								observationResource = rmsdataexchangeService.convertObservationToFhirResource(obs);
							}
						} else {
							if (debugMode)
								System.out.println("rmsdataexchange Module: HIE kisumu Manually constructing the payload");
							
							observationResource.setId(obs.getUuid());
						}
						
						// Add encounter to bundle
						bundle.addEntry()
						        .setFullUrl(FhirConstants.PATIENT + "/" + observationResource.getIdElement().getIdPart())
						        .setResource(observationResource);
					}
				}
				
				if (debugMode)
					System.out.println("rmsdataexchange Module: Creating FHIR payload for maternal profile: "
					        + visit.getPatient().getUuid());
				
				FhirContext fhirContext = FhirContext.forR4();
				ret = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
				if (debugMode)
					System.out.println("rmsdataexchange Module: Got FHIR maternal profile details: " + ret);
				
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
	 * Send the maternal profile payload to Kisumu HIE
	 * 
	 * @param load
	 * @return
	 */
	public static Boolean sendHIEMaternalProfile(@NotNull String load) {
		Boolean ret = false;
		String payload = load;
		Boolean debugMode = false;
		
		// HttpsURLConnection con = null;
		HttpURLConnection connection = null;

		String authUsername = AdviceUtils.getKHIEAuthUserName();
		String authPassword = AdviceUtils.getKHIEAuthPassword();
		if (!StringUtils.isEmpty(authUsername) || !StringUtils.isEmpty(authPassword)) {
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
				
				try {
					
					// We send the payload to Kisumu HIE
					String auth = authUsername + ":" + authPassword;
					if (debugMode)
						System.err
						        .println("rmsdataexchange Module: Kisumu HIE We got the Auth token. Now sending the patient registration details. Token: "
						                + auth);
					String kisumuHIEUrl = AdviceUtils.getKHIEEndpointURL();
					if (debugMode)
						System.out
						        .println("rmsdataexchange Module: Wonder health patient registration URL: " + kisumuHIEUrl);
					URL url = new URL(kisumuHIEUrl);
					
					byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes("UTF-8"));
					if (debugMode)
						System.err.println("rmsdataexchange Module: Kisumu HIE Encoded Auth " + auth);
					String authHeader = "Basic " + new String(encodedAuth);
					
					// Debug TODO: remove in production
					AdviceUtils.trustAllCerts();
					
					if (url.getProtocol().equalsIgnoreCase("https")) {
						connection = (HttpsURLConnection) url.openConnection();
					} else if (url.getProtocol().equalsIgnoreCase("http")) {
						connection = (HttpURLConnection) url.openConnection();
					}
					
					connection.setRequestMethod("POST");
					connection.setDoOutput(true);
					connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
					connection.setRequestProperty("Accept", "application/json");
					connection.setRequestProperty(HttpHeaders.AUTHORIZATION, authHeader);
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
							System.out.println("rmsdataexchange Module: Kisumu HIE Got patient registration Response as: "
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
		}
		catch (Exception ex) {
			if (debugMode)
				System.err.println("rmsdataexchange Module: Kisumu HIE Error. Username or Password not update: ");
			
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
	private class sendMaternalProfileRunnable implements Runnable {
		
		String payload = "";
		
		Patient patient = null;
		
		Boolean debugMode = false;
		
		public sendMaternalProfileRunnable(@NotNull String payload, @NotNull Patient patient) {
			this.payload = payload;
			this.patient = patient;
		}
		
		@Override
		public void run() {
			
			try {
				if (Context.isSessionOpen()) {
					System.out.println("rmsdataexchange Module: We have an open session");
					Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
					Context.addProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
				} else {
					System.out.println("rmsdataexchange Module: Error: We have NO open session");
					Context.openSession();
					Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
					Context.addProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
				}
				debugMode = AdviceUtils.isRMSLoggingEnabled();
				
				if (debugMode)
					System.out.println("rmsdataexchange Module: Start sending Maternal Profile to Kisumu HIE");
				
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
				
				Boolean sendHIEResult = sendHIEMaternalProfile(payload);
				
				// TODO: Put the message in the queue in case of failure to send
				
				// if (sendHIEResult == false) {
				// 	// Failed to send the payload. We put it in the queue
				// 	if (debugMode)
				// 		System.err
				// 		        .println("rmsdataexchange Module: Failed to send Maternal Profile to Kisumu HIE. Adding to queue");
				// 	RmsdataexchangeService rmsdataexchangeService = Context.getService(RmsdataexchangeService.class);
				// 	RMSQueueSystem rmsQueueSystem = rmsdataexchangeService
				// 	        .getQueueSystemByUUID(RMSModuleConstants.WONDER_HEALTH_SYSTEM_PATIENT);
				// 	Boolean addToQueue = AdviceUtils.addSyncPayloadToQueue(payload, rmsQueueSystem);
				// 	if (addToQueue) {
				// 		if (debugMode)
				// 			System.out
				// 			        .println("rmsdataexchange Module: Finished adding Maternal Profile to Kisumu HIE Queue");
				// 		// Mark sent using person attribute
				// 		AdviceUtils.setPersonAttributeValueByTypeUuid(patient,
				// 		    RMSModuleConstants.PERSON_ATTRIBUTE_WONDER_HEALTH_SYNCHRONIZED_UUID, "1");
				// 	} else {
				// 		if (debugMode)
				// 			System.err
				// 			        .println("rmsdataexchange Module: Error: Failed to add Maternal Profile to Kisumu HIE Queue");
				// 		// Mark NOT sent using person attribute
				// 		AdviceUtils.setPersonAttributeValueByTypeUuid(patient,
				// 		    RMSModuleConstants.PERSON_ATTRIBUTE_WONDER_HEALTH_SYNCHRONIZED_UUID, "0");
				// 	}
				// } else {
				// 	// Success sending the Maternal Profile
				// 	if (debugMode)
				// 		System.out.println("rmsdataexchange Module: Finished sending Maternal Profile to Kisumu HIE");
				// 	// Mark sent using person attribute
				// 	AdviceUtils.setPersonAttributeValueByTypeUuid(patient,
				// 	    RMSModuleConstants.PERSON_ATTRIBUTE_WONDER_HEALTH_SYNCHRONIZED_UUID, "1");
				// }
				
			}
			catch (Exception ex) {
				if (debugMode)
					System.err
					        .println("rmsdataexchange Module: Error. Failed to send Maternal Profile to Kisumu HIE Maternal Profile: "
					                + ex.getMessage());
				ex.printStackTrace();
			}
			finally {
				// Context.closeSession();
			}
		}
	}
	
}
