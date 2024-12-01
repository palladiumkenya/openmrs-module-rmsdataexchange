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
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
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

/**
 * Detects when a new visit has started and syncs patient data to Wonder Health
 */
@Component("rmsdataexchange.NewPatientRegistrationSyncToWonderHealth")
public class NewPatientRegistrationSyncToWonderHealth implements AfterReturningAdvice {
	
	private Boolean debugMode = false;
	
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
			if (patient != null) {
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
				
				if (debugMode)
					System.out.println("rmsdataexchange Module: Creating FHIR payload for patient: " + patient.getUuid());
				
				FhirContext fhirContext = FhirContext.forR4();
				ret = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(patientResource);
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
	 * Send the patient registration payload to RMS
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
							        .println("rmsdataexchange Module: We got the Auth token. Now sending the patient registration details. Token: "
							                + token);
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
							fin.close();
							
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
								System.err.println("rmsdataexchange Module: Failed to send final payload: "
								        + finalResponseCode);
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
