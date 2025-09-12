package org.openmrs.module.rmsdataexchange.advice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.validation.constraints.NotNull;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.context.AuthenticationScheme;
import org.openmrs.api.context.Context;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.DaemonTokenAware;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentMode;
import org.openmrs.module.rmsdataexchange.api.util.AdviceUtils;
import org.openmrs.module.rmsdataexchange.api.util.RMSModuleConstants;
import org.openmrs.module.rmsdataexchange.api.util.SimpleObject;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueueSystem;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.aop.AfterReturningAdvice;
import org.openmrs.module.rmsdataexchange.RmsdataexchangeActivator;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeService;
import org.openmrs.api.context.Daemon;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.context.Credentials;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient.ContactComponent;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Concept;
import org.openmrs.Diagnosis;
import org.openmrs.Encounter;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.Visit;
import org.openmrs.VisitAttributeType;
import org.openmrs.api.DiagnosisService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.LocationTranslator;
import org.openmrs.module.fhir2.api.translators.PatientTranslator;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.aop.AfterReturningAdvice;

import ca.uhn.fhir.context.FhirContext;

/**
 * Detects when a new payment has been made to a bill and syncs to RMS Financial System
 */
public class HIEPatientRegistrationAdvice implements AfterReturningAdvice {
	
	private Boolean debugMode = false;
	
	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		debugMode = AdviceUtils.isRMSLoggingEnabled();
		if (AdviceUtils.isHIECRIntegrationEnabled()) {
			if ("savePatient".equals(method.getName()) && returnValue instanceof Patient) {
				Patient saved = (Patient) returnValue;
				
				if (saved.getDateChanged() == null) {
					// New registration -- need to send to CR
					if (debugMode)
						System.out.println("rmsdataexchange Module: HIE CR: New patient registered: "
						        + saved.getPersonName() + " || We send to CR");
					sendPatientToCR(saved);
				} else {
					// Existing patient edited
					if (debugMode)
						System.out.println("rmsdataexchange Module: HIE CR: Existing patient updated: "
						        + saved.getPersonName() + " || We ignore");
				}
			}
		}
	}
	
	/**
	 * Send the patient to the HIE CR
	 * 
	 * @param patient
	 * @return
	 */
	private Boolean sendPatientToCR(Patient patient) {
		Boolean ret = false;
		String payload = preparePatientFHIRPayload(patient);
		
		BufferedReader reader = null;
		HttpsURLConnection connection = null;
		try {
			// Send to HIE CR
			// URL
			GlobalProperty globalHIECRbackEndURL = Context.getAdministrationService().getGlobalPropertyObject(
			    RMSModuleConstants.hieCRURLGlobal);
			String strHIECRbackEndURL = globalHIECRbackEndURL.getPropertyValue();
			strHIECRbackEndURL = strHIECRbackEndURL.trim();
			
			// UserName
			GlobalProperty globalHIECRbackEndUsername = Context.getAdministrationService().getGlobalPropertyObject(
			    RMSModuleConstants.hieCRUsernameGlobal);
			String strHIECRbackEndUsername = globalHIECRbackEndUsername.getPropertyValue();
			strHIECRbackEndUsername = strHIECRbackEndUsername.trim();
			
			// Password
			GlobalProperty globalHIECRbackEndPassword = Context.getAdministrationService().getGlobalPropertyObject(
			    RMSModuleConstants.hieCRPasswordGlobal);
			String strHIECRbackEndPassword = globalHIECRbackEndPassword.getPropertyValue();
			strHIECRbackEndPassword = strHIECRbackEndPassword.trim();
			
			URL url = new URL(strHIECRbackEndURL);
			
			String auth = strHIECRbackEndUsername.trim() + ":" + strHIECRbackEndPassword.trim();
			byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes("UTF-8"));
			String authHeader = "Basic " + new String(encodedAuth);
			
			AdviceUtils.trustAllCerts();
			
			connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty(HttpHeaders.AUTHORIZATION, authHeader);
			connection.setDoOutput(true);
			
			OutputStream outputStream = connection.getOutputStream();
			byte[] output = payload.getBytes("utf-8");
			outputStream.write(output, 0, output.length);
			
			int finalResponseCode = connection.getResponseCode();
			
			if (finalResponseCode == HttpURLConnection.HTTP_OK) { //success
				reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder response = new StringBuilder();
				String responseLine = null;
				while ((responseLine = reader.readLine()) != null) {
					response.append(responseLine.trim());
				}
				String crResponse = response.toString();
				if (debugMode)
					System.out.println("rmsdataexchange Module: HIE CR: Success sending payload to CR: Response: "
					        + crResponse);
			} else {
				reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
				StringBuilder response = new StringBuilder();
				String responseLine = null;
				while ((responseLine = reader.readLine()) != null) {
					response.append(responseLine.trim());
				}
				String crResponse = response.toString();
				if (debugMode)
					System.out.println("rmsdataexchange Module: HIE CR: ERROR: Failed to send payload to CR: Response: "
					        + crResponse);
			}
			
		}
		catch (Exception ex) {
			if (debugMode)
				System.out.println("rmsdataexchange Module: HIE CR: Error sending to remote: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return (ret);
	}
	
	/**
	 * Prepare the patient FHIR payload
	 * 
	 * @param patient
	 * @return
	 */
	private String preparePatientFHIRPayload(Patient patient) {
		String ret = "";
		org.hl7.fhir.r4.model.Patient patientResource = new org.hl7.fhir.r4.model.Patient();
		if (debugMode)
			System.out.println("rmsdataexchange Module: HIE CR: Manually constructing the payload");
		
		// Set Patient ID
		patientResource.setId(patient.getUuid());
		
		// Set Identifiers
		for (PatientIdentifier identifier : patient.getActiveIdentifiers()) {
			Identifier fhirIdentifier = new Identifier();
			fhirIdentifier.setSystem("http://fhir.openmrs.org/ext/patient/identifier#system");
			fhirIdentifier.setValue(identifier.getIdentifier());
			fhirIdentifier.setUse(Identifier.IdentifierUse.OFFICIAL);
			patientResource.addIdentifier(fhirIdentifier);
		}
		
		// Set Patient Name
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
		
		// Map Birthdate
		patientResource.setBirthDate(patient.getBirthdate());
		
		// Map Deceased status
		patientResource.setDeceased(new BooleanType(patient.getDead()));
		
		// Map Gender
		if ("M".equalsIgnoreCase(patient.getGender())) {
			patientResource.setGender(AdministrativeGender.MALE);
		} else if ("F".equalsIgnoreCase(patient.getGender())) {
			patientResource.setGender(AdministrativeGender.FEMALE);
		} else {
			patientResource.setGender(AdministrativeGender.UNKNOWN);
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
		
		// Set marital status
		CodeableConcept ccMaritalStatus = new CodeableConcept();
		Coding codingMaritalStatus = new Coding();
		//marital status
		Obs obsMaritalStatus = AdviceUtils.getLatestObs(patient, RMSModuleConstants.CIVIL_STATUS);
		if (obsMaritalStatus != null) {
			Concept conMaritalStatus = obsMaritalStatus.getValueCoded();
			codingMaritalStatus = AdviceUtils.getMaritalStatusCoding(conMaritalStatus);
			ccMaritalStatus.setCoding(Collections.singletonList(codingMaritalStatus));
			
			patientResource.setMaritalStatus(ccMaritalStatus);
		}
		
		// telecom
		PersonService personService = Context.getPersonService();
		PersonAttributeType personAttributeType = personService
		        .getPersonAttributeTypeByUuid(RMSModuleConstants.TELEPHONE_CONTACT);
		PersonAttributeType personAttributeType2 = personService
		        .getPersonAttributeTypeByUuid(RMSModuleConstants.ALTERNATE_PHONE_CONTACT);
		String personalPhone = "";
		if (personAttributeType != null) {
			PersonAttribute personAttribute = patient.getAttribute(personAttributeType);
			if (personAttribute != null) {
				String primaryPhone = personAttribute.getValue();
				if (primaryPhone != null && !primaryPhone.trim().isEmpty()) {
					if (debugMode)
						System.out.println("rmsdataexchange Module: HIE CR: We got the phone number: " + primaryPhone);
					personalPhone = primaryPhone;
				} else {
					if (debugMode)
						System.out
						        .println("rmsdataexchange Module: HIE CR: failed to get the primary phone number. We try the secondary");
					if (personAttributeType2 != null) {
						PersonAttribute personAttribute2 = patient.getAttribute(personAttributeType2);
						if (personAttribute2 != null) {
							String secondaryPhone = personAttribute2.getValue();
							if (secondaryPhone != null && !secondaryPhone.trim().isEmpty()) {
								if (debugMode)
									System.out.println("rmsdataexchange Module: HIE CR: We got the secondary phone number: "
									        + secondaryPhone);
								personalPhone = secondaryPhone;
							} else {
								if (debugMode)
									System.out
									        .println("rmsdataexchange Module: HIE CR: failed to get the secondary phone number.");
							}
						}
					}
				}
			}
		}
		ContactPoint contactPoint = new ContactPoint();
		contactPoint.setSystem(ContactPoint.ContactPointSystem.PHONE);
		contactPoint.setUse(ContactPoint.ContactPointUse.MOBILE);
		contactPoint.setValue(personalPhone);
		List<ContactPoint> theTelecom = Collections.singletonList(contactPoint);
		patientResource.setTelecom(theTelecom);
		
		// contacts
		ContactComponent nextOfKin = new ContactComponent();
		
		PersonAttributeType nokNameAttributeType = personService
		        .getPersonAttributeTypeByUuid(RMSModuleConstants.NEXT_OF_KIN_NAME);
		PersonAttributeType nokRelationshipAttributeType = personService
		        .getPersonAttributeTypeByUuid(RMSModuleConstants.NEXT_OF_KIN_RELATIONSHIP);
		PersonAttributeType nokPhoneAttributeType = personService
		        .getPersonAttributeTypeByUuid(RMSModuleConstants.NEXT_OF_KIN_CONTACT);
		PersonAttributeType nokAddressAttributeType = personService
		        .getPersonAttributeTypeByUuid(RMSModuleConstants.NEXT_OF_KIN_ADDRESS);
		
		// Contact Name
		if (nokNameAttributeType != null) {
			PersonAttribute nokNameAttribute = patient.getAttribute(nokNameAttributeType);
			if (nokNameAttribute != null) {
				String nokName = nokNameAttribute.getValue();
				if (nokName != null && !nokName.trim().isEmpty()) {
					if (debugMode)
						System.out.println("rmsdataexchange Module: HIE CR: We got the next of kin name: " + nokName);
					nextOfKin.setName(new HumanName().setFamily(nokName).setUse(HumanName.NameUse.OFFICIAL));
				} else {
					
				}
			}
		}
		
		// Contact Relationship - NB: We use the exact match here since kenyaEMR has only one Next Of Kin Option
		if (nokRelationshipAttributeType != null) {
			PersonAttribute nokRelationshipAttribute = patient.getAttribute(nokRelationshipAttributeType);
			if (nokRelationshipAttribute != null) {
				String nokRelationship = nokRelationshipAttribute.getValue();
				if (nokRelationship != null && !nokRelationship.trim().isEmpty()) {
					if (debugMode)
						System.out.println("rmsdataexchange Module: HIE CR: We got the next of kin relationship: "
						        + nokRelationship);
				} else {
					if (debugMode)
						System.out.println("rmsdataexchange Module: HIE CR: next of kin relationship not found");
				}
			}
		}
		
		CodeableConcept ccNokRel = new CodeableConcept();
		Coding codingNokRel = new Coding();
		codingNokRel.setSystem("http://terminology.hl7.org/CodeSystem/v2-0131");
		codingNokRel.setCode("E");
		codingNokRel.setDisplay("Emergency Contact");
		ccNokRel.setCoding(Collections.singletonList(codingNokRel));
		nextOfKin.setRelationship(Collections.singletonList(ccNokRel));
		
		// Contact Telecom
		if (nokPhoneAttributeType != null) {
			PersonAttribute nokPhoneAttribute = patient.getAttribute(nokPhoneAttributeType);
			if (nokPhoneAttribute != null) {
				String nokPhone = nokPhoneAttribute.getValue();
				if (nokPhone != null && !nokPhone.trim().isEmpty()) {
					if (debugMode)
						System.out.println("rmsdataexchange Module: HIE CR: We got the next of kin phone: " + nokPhone);
					ContactPoint nokContactPoint = new ContactPoint();
					nokContactPoint.setSystem(ContactPoint.ContactPointSystem.PHONE);
					nokContactPoint.setUse(ContactPoint.ContactPointUse.MOBILE);
					nokContactPoint.setValue(nokPhone);
					List<ContactPoint> nokTelecom = Collections.singletonList(nokContactPoint);
					nextOfKin.setTelecom(nokTelecom);
				} else {
					if (debugMode)
						System.out.println("rmsdataexchange Module: HIE CR: next of kin phone not found");
				}
			}
		}
		
		// Contact Address
		if (nokAddressAttributeType != null) {
			PersonAttribute nokAddressAttribute = patient.getAttribute(nokAddressAttributeType);
			if (nokAddressAttribute != null) {
				String nokAddress = nokAddressAttribute.getValue();
				if (nokAddress != null && !nokAddress.trim().isEmpty()) {
					if (debugMode)
						System.out.println("rmsdataexchange Module: HIE CR: We got the next of kin address: " + nokAddress);
					Address nokFHIRAddress = new Address().setUse(Address.AddressUse.HOME).setType(Address.AddressType.BOTH)
					        .addLine(nokAddress);
					// .addLine("Apt 4B")
					// .setCity("Springfield")
					// .setState("IL")
					// .setPostalCode("62704")
					// .setCountry("USA");
					nextOfKin.setAddress(nokFHIRAddress);
				} else {
					if (debugMode)
						System.out.println("rmsdataexchange Module: HIE CR: next of kin address not found");
				}
			}
		}
		
		List<ContactComponent> theContact = Collections.singletonList(nextOfKin);
		patientResource.setContact(theContact);
		
		// managing organization
		Reference patientLocation = new Reference();
		String currentLocation = AdviceUtils.getDefaultLocationMflCode(null);
		String facilityName = AdviceUtils.getDefaultLocation().getName();
		String facilityUuid = AdviceUtils.getDefaultLocation().getUuid();
		patientLocation.setId(facilityUuid);
		patientLocation.setReference("Organization/" + currentLocation);
		patientLocation.setDisplay(facilityName);
		patientResource.setManagingOrganization(patientLocation);
		
		// Convert resource object to String
		FhirContext fhirContext = FhirContext.forR4();
		ret = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(patientResource);
		if (debugMode)
			System.out.println("rmsdataexchange Module: HIE CR: Got FHIR patient registration details: " + ret);
		return (ret);
	}
	
}
