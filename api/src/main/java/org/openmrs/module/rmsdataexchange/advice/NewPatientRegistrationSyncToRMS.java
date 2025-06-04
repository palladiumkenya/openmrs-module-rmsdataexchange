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

import javax.net.ssl.HttpsURLConnection;
import javax.validation.constraints.NotNull;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.Hibernate;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.rmsdataexchange.RmsdataexchangeActivator;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeService;
import org.openmrs.module.rmsdataexchange.api.util.AdviceUtils;
import org.openmrs.module.rmsdataexchange.api.util.RMSModuleConstants;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.openmrs.module.rmsdataexchange.api.util.SimpleObject;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueueSystem;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.aop.AfterReturningAdvice;

/**
 * Detects when a new patient has been registered and syncs to RMS Financial System
 */
public class NewPatientRegistrationSyncToRMS implements AfterReturningAdvice {
	
	private Boolean debugMode = false;
	
	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		try {
			debugMode = AdviceUtils.isRMSLoggingEnabled();
			if (AdviceUtils.isRMSIntegrationEnabled()) {
				// Check if the method is "savePatient"
				if (method.getName().equals("savePatient") && args.length > 0 && args[0] instanceof Patient) {
					Patient patient = (Patient) args[0];
					
					// Log patient info
					if (patient != null) {
						
						// Check if the patient has already been synced (using patient attribute)
						String attrCheck = AdviceUtils.getPersonAttributeValueByTypeUuid(patient,
						    RMSModuleConstants.PERSON_ATTRIBUTE_RMS_SYNCHRONIZED_UUID);
						if (debugMode)
							System.out.println("rmsdataexchange Module: RMS: Attribute check is: " + attrCheck);
						if (attrCheck == null || attrCheck.trim().equalsIgnoreCase("0") || attrCheck.isEmpty()
						        || attrCheck.trim().equalsIgnoreCase("")) {
							Date patientCreationDate = patient.getDateCreated();
							if (debugMode)
								System.out.println("rmsdataexchange Module: RMS Patient Date Changed: "
								        + patient.getDateChanged());
							if (debugMode)
								System.out.println("rmsdataexchange Module: patient was created on: " + patientCreationDate);
							
							if (patientCreationDate != null && AdviceUtils.checkIfCreateModetOrEditMode(patientCreationDate)) {
								// CREATE MODE
								if (debugMode)
									System.out.println("rmsdataexchange Module: New patient registered:");
								if (debugMode)
									System.out.println("rmsdataexchange Module: Name: "
									        + patient.getPersonName().getFullName());
								if (debugMode)
									System.out.println("rmsdataexchange Module: DOB: " + patient.getBirthdate());
								if (debugMode)
									System.out.println("rmsdataexchange Module: Age: " + patient.getAge());
								
								// Use a thread to send the data. This frees up the frontend to proceed
								Hibernate.initialize(patient.getIdentifiers());
								Integer ids = patient.getIdentifiers().size();
								if (debugMode)
									System.out.println("rmsdataexchange Module: patient identifiers: " + ids);
								String payload = preparePatientRMSPayload(patient);
								syncPatientRunnable runner = new syncPatientRunnable(payload, patient);
								Daemon.runInDaemonThread(runner, RmsdataexchangeActivator.getDaemonToken());
							} else {
								// EDIT MODE
								if (debugMode)
									System.out.println("rmsdataexchange Module: patient in edit mode. we ignore");
							}
						} else {
							if (debugMode)
								System.out.println("rmsdataexchange Module: RMS: Patient already sent to remote. we ignore");
						}
					} else {
						if (debugMode)
							System.out.println("rmsdataexchange Module: Attempted to save a null patient.");
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
	 * Prepare the JSON payload for patient registration
	 * 
	 * @param patient
	 * @return
	 */
	private static String preparePatientRMSPayload(@NotNull Patient patient) {
		String ret = "";
		
		try {
			if (Context.isSessionOpen()) {
				System.out.println("rmsdataexchange Module: We have an open session F");
				Context.addProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
				Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			} else {
				System.out.println("rmsdataexchange Module: Error: We have NO open session F");
				Context.openSession();
				Context.addProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
				Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			}
			Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
			
			if (patient != null) {
				Hibernate.initialize(patient.getIdentifiers());
				Integer ids = patient.getIdentifiers().size();
				if (debugMode)
					System.out.println("rmsdataexchange Module: patient identifiers: " + ids);
				if (debugMode)
					System.out.println("rmsdataexchange Module: New patient created: "
					        + patient.getPersonName().getFullName() + ", Age: " + patient.getAge());
				SimpleObject payloadPrep = new SimpleObject();
				payloadPrep.put("first_name", patient.getPersonName().getGivenName());
				payloadPrep.put("middle_name", patient.getPersonName().getMiddleName());
				payloadPrep.put("patient_unique_id", patient.getUuid());
				payloadPrep.put("last_name", patient.getPersonName().getFamilyName());
				PatientIdentifierType nationalIDIdentifierType = Context.getPatientService().getPatientIdentifierTypeByUuid(
				    "49af6cdc-7968-4abb-bf46-de10d7f4859f");
				String natID = "";
				if (nationalIDIdentifierType != null) {
					natID = getPatientIdentifier(patient, nationalIDIdentifierType);
				}
				payloadPrep.put("id_number", natID);
				String phoneNumber = patient.getAttribute("Telephone contact") != null ? patient.getAttribute(
				    "Telephone contact").getValue() : "";
				payloadPrep.put("phone", phoneNumber);
				payloadPrep.put("hospital_code", Utils.getDefaultLocationMflCode(null));
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
				payloadPrep.put("dob", formatter.format(patient.getBirthdate()));
				payloadPrep.put("gender", patient.getGender() != null ? (patient.getGender().equalsIgnoreCase("M") ? "Male"
				        : (patient.getGender().equalsIgnoreCase("F") ? "Female" : "")) : "");
				ret = payloadPrep.toJson();
				if (debugMode)
					System.out.println("rmsdataexchange Module: Got patient registration details: " + ret);
			} else {
				if (debugMode)
					System.out.println("rmsdataexchange Module: patient is null");
			}
		}
		catch (Exception ex) {
			Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
			if (debugMode)
				System.err.println("rmsdataexchange Module: Error getting new patient payload: " + ex.getMessage());
			ex.printStackTrace();
		}
		finally {
			// Context.closeSession();
		}
		
		return (ret);
	}
	
	/**
	 * Send the patient registration payload to RMS
	 * 
	 * @param Patient patient
	 * @return
	 */
	public static Boolean sendRMSPatientRegistration(@NotNull Patient patient) {
		return (sendRMSPatientRegistration(preparePatientRMSPayload(patient)));
	}
	
	/**
	 * Send the patient registration payload to RMS
	 * 
	 * @param String patient
	 * @return
	 */
	public static Boolean sendRMSPatientRegistration(@NotNull String patient) {
		Boolean ret = false;
		String payload = patient;
		Boolean debugMode = false;
		
		HttpsURLConnection con = null;
		HttpsURLConnection connection = null;
		try {
			if (Context.isSessionOpen()) {
				System.out.println("rmsdataexchange Module: We have an open session 3");
				Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			} else {
				System.out.println("rmsdataexchange Module: Error: We have NO open session 3");
				Context.openSession();
				Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			}
			User current = Daemon.getDaemonThreadUser();
			System.out.println("rmsdataexchange Module: Current user in session 3: "
			        + (current != null ? current.getUsername() : ""));
			
			debugMode = AdviceUtils.isRMSLoggingEnabled();
			if (debugMode)
				System.out.println("rmsdataexchange Module: using payload: " + payload);
			
			// Create URL
			String baseURL = AdviceUtils.getRMSEndpointURL();
			String completeURL = baseURL + "/login";
			if (debugMode)
				System.out.println("rmsdataexchange Module: Auth URL: " + completeURL);
			URL url = new URL(completeURL);
			String rmsUser = AdviceUtils.getRMSAuthUserName();
			String rmsPassword = AdviceUtils.getRMSAuthPassword();
			SimpleObject authPayloadCreator = SimpleObject.create("email", rmsUser != null ? rmsUser : "", "password",
			    rmsPassword != null ? rmsPassword : "");
			String authPayload = authPayloadCreator.toJson();
			
			// Get token
			con = (HttpsURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			con.setRequestProperty("Accept", "application/json");
			con.setConnectTimeout(10000); // set timeout to 10 seconds
			
			PrintStream os = new PrintStream(con.getOutputStream());
			os.print(authPayload);
			os.close();
			
			int responseCode = con.getResponseCode();
			
			if (responseCode == HttpURLConnection.HTTP_OK) { //success
				BufferedReader in = null;
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				
				String input;
				StringBuffer response = new StringBuffer();
				
				while ((input = in.readLine()) != null) {
					response.append(input);
				}
				in.close();
				
				String returnResponse = response.toString();
				if (debugMode)
					System.out.println("rmsdataexchange Module: Got Auth Response as: " + returnResponse);
				
				// Extract the token and token expiry date
				ObjectMapper mapper = new ObjectMapper();
				JsonNode jsonNode = null;
				String token = "";
				String expires_at = "";
				SimpleObject authObj = new SimpleObject();
				
				try {
					jsonNode = mapper.readTree(returnResponse);
					if (jsonNode != null) {
						token = jsonNode.get("token") == null ? "" : jsonNode.get("token").getTextValue();
						authObj.put("token", token);
						expires_at = jsonNode.get("expires_at") == null ? "" : jsonNode.get("expires_at").getTextValue();
						authObj.put("expires_at", expires_at);
					}
				}
				catch (Exception e) {
					if (debugMode)
						System.err.println("rmsdataexchange Module: Error getting auth token: " + e.getMessage());
					e.printStackTrace();
				}
				
				if (!token.isEmpty()) {
					try {
						// We send the payload to RMS
						if (debugMode)
							System.err
							        .println("rmsdataexchange Module: We got the Auth token. Now sending the patient registration details. Payload: "
							                + payload);
						String finalUrl = baseURL + "/create-patient-profile";
						if (debugMode)
							System.out.println("rmsdataexchange Module: Final patient registration URL: " + finalUrl);
						URL finUrl = new URL(finalUrl);
						
						connection = (HttpsURLConnection) finUrl.openConnection();
						connection.setRequestMethod("POST");
						connection.setDoOutput(true);
						connection.setRequestProperty("Authorization", "Bearer " + token);
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
							in.close();
							
							String finalReturnResponse = finalResponse.toString();
							if (debugMode)
								System.out.println("rmsdataexchange Module: Got patient registration Response as: "
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
									        .println("rmsdataexchange Module: Got patient registration final response: success: "
									                + success + " message: " + message);
							}
							catch (Exception e) {
								if (debugMode)
									System.err
									        .println("rmsdataexchange Module: Error getting patient registration final response: "
									                + e.getMessage());
								e.printStackTrace();
							}
							
							if (success != null && success == true) {
								ret = true;
							}
							
						} else {
							if (debugMode)
								System.err.println("rmsdataexchange Module: Failed to send patient to RMS: Error Code: "
								        + finalResponseCode);
							try {
								// Use getErrorStream() to get the error message content
								BufferedReader reader = new BufferedReader(
								        new InputStreamReader(connection.getErrorStream()));
								String line;
								StringBuilder errorResponse = new StringBuilder();
								while ((line = reader.readLine()) != null) {
									errorResponse.append(line);
								}
								reader.close();
								
								// Output the error message/content
								System.out
								        .println("rmsdataexchange Module: Failed to send New Patient to RMS: Error Response: "
								                + errorResponse.toString());
							}
							catch (Exception et) {}
						}
					}
					catch (Exception em) {
						if (debugMode)
							System.err.println("rmsdataexchange Module: Error. Failed to send the final payload: "
							        + em.getMessage());
						em.printStackTrace();
					}
				}
			} else {
				if (debugMode)
					System.err.println("rmsdataexchange Module: Failed to get auth: " + responseCode);
			}
			
		}
		catch (Exception ex) {
			if (debugMode)
				System.err.println("rmsdataexchange Module: Error. Failed to get auth token: " + ex.getMessage());
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
	private static String getPatientIdentifier(Patient patient, PatientIdentifierType patientIdentifierType) {
		String ret = "";
		if (Context.isSessionOpen()) {
			System.out.println("rmsdataexchange Module: We have an open session G");
			Context.addProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
			Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
		} else {
			System.out.println("rmsdataexchange Module: Error: We have NO open session G");
			Context.openSession();
			Context.addProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
			Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
		}
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		Integer patId = patient.getId();
		Patient localPatient = Context.getPatientService().getPatient(patId);
		
		Integer ids = localPatient.getIdentifiers().size();
		if (debugMode)
			System.out.println("rmsdataexchange Module: patient identifiers: " + ids);
		
		if (patientIdentifierType != null && localPatient != null) {
			try {
				Set<PatientIdentifier> identifiers = localPatient.getIdentifiers();
				
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
					System.err.println("rmsdataexchange Module: Error Getting the identifier: " + ex.getMessage());
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
			// Run the thread
			
			try {
				if (Context.isSessionOpen()) {
					System.out.println("rmsdataexchange Module: We have an open session H");
					Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
					Context.addProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
				} else {
					System.out.println("rmsdataexchange Module: Error: We have NO open session H");
					Context.openSession();
					Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
					Context.addProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
				}
				debugMode = AdviceUtils.isRMSLoggingEnabled();
				
				if (debugMode)
					System.out.println("rmsdataexchange Module: Start sending patient to RMS");
				
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
				
				Boolean testPatientSending = sendRMSPatientRegistration(payload);
				
				if (!testPatientSending) {
					
					if (debugMode)
						System.out.println("rmsdataexchange Module: Failed to send patient to RMS");
					RmsdataexchangeService rmsdataexchangeService = Context.getService(RmsdataexchangeService.class);
					RMSQueueSystem rmsQueueSystem = rmsdataexchangeService
					        .getQueueSystemByUUID(RMSModuleConstants.RMS_SYSTEM_PATIENT);
					Boolean addToQueue = AdviceUtils.addSyncPayloadToQueue(payload, rmsQueueSystem);
					if (addToQueue) {
						if (debugMode)
							System.out.println("rmsdataexchange Module: Finished adding patient to RMS Patient Queue");
						// Mark sent using person attribute
						AdviceUtils.setPersonAttributeValueByTypeUuid(patient,
						    RMSModuleConstants.PERSON_ATTRIBUTE_RMS_SYNCHRONIZED_UUID, "1");
					} else {
						if (debugMode)
							System.err.println("rmsdataexchange Module: Error: Failed to add patient to RMS Patient Queue");
						// Mark NOT sent using person attribute
						AdviceUtils.setPersonAttributeValueByTypeUuid(patient,
						    RMSModuleConstants.PERSON_ATTRIBUTE_RMS_SYNCHRONIZED_UUID, "0");
					}
				} else {
					// Success sending the patient
					if (debugMode)
						System.out.println("rmsdataexchange Module: Finished sending patient to RMS");
					
					// Mark sent using person attribute
					AdviceUtils.setPersonAttributeValueByTypeUuid(patient,
					    RMSModuleConstants.PERSON_ATTRIBUTE_RMS_SYNCHRONIZED_UUID, "1");
				}
			}
			catch (Exception ex) {
				if (debugMode)
					System.err.println("rmsdataexchange Module: Error. Failed to send patient to RMS: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
}
