package org.openmrs.module.rmsdataexchange.api.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.VisitAttributeType;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.rmsdataexchange.api.RMSBillAttributeService;
import org.openmrs.module.rmsdataexchange.api.RMSPaymentAttributeService;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeService;
import org.openmrs.module.rmsdataexchange.queue.model.RMSBillAttribute;
import org.openmrs.module.rmsdataexchange.queue.model.RMSBillAttributeType;
import org.openmrs.module.rmsdataexchange.queue.model.RMSPaymentAttribute;
import org.openmrs.module.rmsdataexchange.queue.model.RMSPaymentAttributeType;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueue;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueueSystem;
import org.openmrs.util.PrivilegeConstants;

import java.util.concurrent.ThreadLocalRandom;

public class AdviceUtils {
	
	/**
	 * Checks if a bill/patient is in create mode or edit mode (using dateCreated) CREATE MODE =
	 * true, EDIT MODE = false
	 * 
	 * @param date
	 * @return
	 */
	public static boolean checkIfCreateModetOrEditMode(Date date) {
		// Get the current time in milliseconds
		long now = System.currentTimeMillis();
		
		// Get the time of the provided date in milliseconds
		long timeOfDate = date.getTime();
		
		// Calculate the difference in milliseconds
		long diffInMillis = now - timeOfDate;
		
		// Check if the difference is positive (date is before now) and less than 60 seconds (60,000 ms)
		return diffInMillis >= 0 && diffInMillis < 60 * 1000;
	}
	
	/**
	 * Check if there are any new payments
	 * 
	 * @param oldSet
	 * @param newSet
	 * @return
	 */
	public static Set<Payment> symmetricPaymentDifference(Set<Payment> oldSet, Set<Payment> newSet) {
        Set<Payment> result = new HashSet<>(newSet);
        Boolean debugMode = isRMSLoggingEnabled();

        // Add elements from newSet that are not in oldSet based on amount comparison
        for (Payment item1 : oldSet) {
            for (Payment item2 : newSet) {
                if(debugMode) System.out.println("rmsdataexchange Module: Payments comparison: Oldset comparing item uuid " + item2.getAmountTendered() + " with Newset: " + item1.getAmountTendered());
                // BigDecimal behaves different. You cannot use ==
                if (item1.getAmountTendered().compareTo(item2.getAmountTendered()) == 0) {
                    if(debugMode) System.out.println("rmsdataexchange Module: Payments comparison: Found a match: " + item2.getAmountTendered()+ " and: " + item1.getAmountTendered());
                    if(debugMode) System.out.println("rmsdataexchange Module: Payments comparison: Removing item amount " + item2.getAmountTendered() + " size before: " + result.size());
                    // result.remove(item2);
                    for(Payment test : result) {
                        if (item2.getAmountTendered().compareTo(test.getAmountTendered()) == 0) {
                            result.remove(test);
                            break;
                        }
                    }
                    if(debugMode) System.out.println("rmsdataexchange Module: Payments comparison: Removing item: size after: " + result.size());
                    break;
                }
            }
        }

        if(debugMode) System.out.println("rmsdataexchange Module: Payments comparison: " + result.size());

        return result;
    }
	
	/**
	 * Checks whether RMS Logging is enabled
	 * 
	 * @return true (Enabled) and false (Disabled)
	 */
	public static Boolean isRMSLoggingEnabled() {
		Boolean ret = false;
		
		GlobalProperty globalRMSEnabled = Context.getAdministrationService().getGlobalPropertyObject(
		    RMSModuleConstants.RMS_LOGGING_ENABLED);
		String isRMSLoggingEnabled = globalRMSEnabled.getPropertyValue();
		
		if (isRMSLoggingEnabled != null && isRMSLoggingEnabled.trim().equalsIgnoreCase("true")) {
			ret = true;
		}
		
		return (ret);
	}
	
	/**
	 * Checks whether RMS Integration is enabled
	 * 
	 * @return true (Enabled) and false (Disabled)
	 */
	public static Boolean isRMSIntegrationEnabled() {
		Boolean ret = false;
		
		GlobalProperty globalRMSEnabled = Context.getAdministrationService().getGlobalPropertyObject(
		    RMSModuleConstants.RMS_SYNC_ENABLED);
		String isRMSLoggingEnabled = globalRMSEnabled.getPropertyValue();
		
		if (isRMSLoggingEnabled != null && isRMSLoggingEnabled.trim().equalsIgnoreCase("true")) {
			ret = true;
		}
		
		return (ret);
	}
	
	/**
	 * Gets the RMS endpoint URL
	 * 
	 * @return
	 */
	public static String getRMSEndpointURL() {
		String ret = "";
		
		GlobalProperty globalPostUrl = Context.getAdministrationService().getGlobalPropertyObject(
		    RMSModuleConstants.RMS_ENDPOINT_URL);
		String baseURL = globalPostUrl.getPropertyValue();
		
		if (baseURL == null || baseURL.trim().isEmpty()) {
			baseURL = "https://siaya.tsconect.com/api";
		}
		ret = baseURL.trim();
		
		return (ret);
	}
	
	/**
	 * Gets the Wonder Health auth URL
	 * 
	 * @return
	 */
	public static String getWonderHealthAuthURL() {
		String ret = "";
		
		GlobalProperty globalPostUrl = Context.getAdministrationService().getGlobalPropertyObject(
		    RMSModuleConstants.WONDER_HEALTH_AUTH_URL);
		String baseURL = globalPostUrl.getPropertyValue();
		
		if (baseURL == null || baseURL.trim().isEmpty()) {
			baseURL = " https://kenyafhirtest.iwonderpro.com/FHIRAPI/create/login";
		}
		ret = baseURL.trim();
		
		return (ret);
	}
	
	/**
	 * Gets the Wonder Health auth Token
	 * 
	 * @return
	 */
	public static String getWonderHealthAuthToken() {
		String ret = "";
		
		GlobalProperty globalPostUrl = Context.getAdministrationService().getGlobalPropertyObject(
		    RMSModuleConstants.WONDER_HEALTH_AUTH_TOKEN);
		String token = globalPostUrl.getPropertyValue();
		
		if (!StringUtils.isEmpty(token)) {
			ret = token;
		}
		
		return (ret);
	}
	
	/**
	 * Gets the wonder health endpoint URL
	 * 
	 * @return
	 */
	public static String getWonderHealthEndpointURL() {
		String ret = "";
		
		GlobalProperty globalPostUrl = Context.getAdministrationService().getGlobalPropertyObject(
		    RMSModuleConstants.WONDERHEALTH_ENDPOINT_URL);
		String baseURL = globalPostUrl.getPropertyValue();
		
		if (baseURL == null || baseURL.trim().isEmpty()) {
			baseURL = "https://kenyafhirtest.iwonderpro.com/FHIRAPI/create";
		}
		ret = baseURL.trim();
		
		return (ret);
	}
	
	/**
	 * Gets the RMS Auth Username
	 * 
	 * @return
	 */
	public static String getRMSAuthUserName() {
		String ret = "";
		
		GlobalProperty rmsUserGP = Context.getAdministrationService().getGlobalPropertyObject(
		    RMSModuleConstants.RMS_USERNAME);
		String rmsUser = rmsUserGP.getPropertyValue();
		
		ret = (rmsUser == null || rmsUser.trim().isEmpty()) ? "" : rmsUser.trim();
		
		return (ret);
	}
	
	/**
	 * Gets the RMS Auth Password
	 * 
	 * @return
	 */
	public static String getRMSAuthPassword() {
		String ret = "";
		
		GlobalProperty rmsPasswordGP = Context.getAdministrationService().getGlobalPropertyObject(
		    RMSModuleConstants.RMS_PASSWORD);
		String rmsPassword = rmsPasswordGP.getPropertyValue();
		
		ret = (rmsPassword == null || rmsPassword.trim().isEmpty()) ? "" : rmsPassword.trim();
		
		return (ret);
	}
	
	/**
	 * Checks whether Wonder Health Integration is enabled
	 * 
	 * @return true (Enabled) and false (Disabled)
	 */
	public static Boolean isWonderHealthIntegrationEnabled() {
		Boolean ret = false;
		
		GlobalProperty globalWONDERHEALTHEnabled = Context.getAdministrationService().getGlobalPropertyObject(
		    RMSModuleConstants.WONDERHEALTH_SYNC_ENABLED);
		String isWONDERHEALTHLoggingEnabled = globalWONDERHEALTHEnabled.getPropertyValue();
		
		if (isWONDERHEALTHLoggingEnabled != null && isWONDERHEALTHLoggingEnabled.trim().equalsIgnoreCase("true")) {
			ret = true;
		}
		
		return (ret);
	}
	
	/**
	 * Gets a random integer between lower and upper
	 * 
	 * @param lower
	 * @param upper
	 * @return
	 */
	public static int getRandomInt(int lower, int upper) {
		if (lower > upper) {
			throw new IllegalArgumentException(
			        "rmsdataexchange Module: getRandomInt Error : Lower limit must be less than or equal to upper limit");
		}
		return ThreadLocalRandom.current().nextInt(lower, upper + 1);
	}
	
	/**
	 * Adds payload to queue for later processing
	 * 
	 * @param payload
	 * @return
	 */
	public static Boolean addSyncPayloadToQueue(String payload, RMSQueueSystem rmsQueueSystem) {
		Boolean ret = false;
		Boolean debugMode = false;
		try {
			if (Context.isSessionOpen()) {
				System.out.println("rmsdataexchange Module: We have an open session M");
				Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			} else {
				System.out.println("rmsdataexchange Module: Error: We have NO open session M");
				Context.openSession();
				Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			}
			debugMode = isRMSLoggingEnabled();
			// get the system
			RmsdataexchangeService rmsdataexchangeService = Context.getService(RmsdataexchangeService.class);
			if (rmsdataexchangeService != null) {
				if (rmsQueueSystem != null) {
					RMSQueue rmsQueue = new RMSQueue();
					rmsQueue.setPayload(payload);
					rmsQueue.setRmsSystem(rmsQueueSystem);
					
					rmsdataexchangeService.saveQueueItem(rmsQueue);
					return (true);
				} else {
					if (debugMode)
						System.err
						        .println("rmsdataexchange Module: Error saving payload to the queue: Failed to get the queue system");
				}
			} else {
				if (debugMode)
					System.err
					        .println("rmsdataexchange Module: Error saving payload to the queue: Failed to load RMS service");
			}
			
		}
		catch (Exception ex) {
			if (debugMode)
				System.err.println("rmsdataexchange Module: Error saving payload to the queue: " + ex.getMessage());
			ex.printStackTrace();
		}
		finally {
			// Context.closeSession();
		}
		
		return (ret);
	}
	
	/**
	 * Print the current date and time
	 * 
	 * @return
	 */
	public static String printCurrentDateTime() {
		// Get the current date and time
		LocalDateTime currentDateTime = LocalDateTime.now();
		
		// Format the date and time for better readability
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		String formattedDateTime = currentDateTime.format(formatter);
		
		return (formattedDateTime);
	}
	
	/**
	 * Get the value of visit attribute
	 * 
	 * @param visit
	 * @param attributeTypeUuid
	 * @return
	 */
	public static String getVisitAttributeValueByTypeUuid(Visit visit, String attributeTypeUuid) {
		if (visit == null || attributeTypeUuid == null) {
			return null;
		}
		
		for (VisitAttribute attribute : visit.getActiveAttributes()) {
			VisitAttributeType type = attribute.getAttributeType();
			if (type != null && attributeTypeUuid.equals(type.getUuid())) {
				return attribute.getValueReference();
			}
		}
		
		return null;
	}
	
	/**
	 * Set visit attribute
	 * 
	 * @param visit
	 * @param attributeTypeUuid
	 * @param value
	 */
	public static void setVisitAttributeValueByTypeUuid(Visit visit, String attributeTypeUuid, Object value) {
		if (visit == null || attributeTypeUuid == null || value == null) {
			return;
		}
		
		VisitAttributeType attributeType = Context.getVisitService().getVisitAttributeTypeByUuid(attributeTypeUuid);
		if (attributeType == null) {
			throw new IllegalArgumentException("rmsdataexchange Module: No VisitAttributeType found for UUID: "
			        + attributeTypeUuid);
		}
		
		VisitAttribute existingAttribute = null;
		
		for (VisitAttribute attr : visit.getAttributes()) {
			if (attributeType.equals(attr.getAttributeType())) {
				existingAttribute = attr;
				break;
			}
		}
		
		if (existingAttribute != null) {
			existingAttribute.setValue(value); // updates existing
		} else {
			VisitAttribute newAttribute = new VisitAttribute();
			newAttribute.setAttributeType(attributeType);
			newAttribute.setValue(value);
			visit.addAttribute(newAttribute); // inserts new
		}
	}
	
	/**
	 * Get the value of person attribute
	 * 
	 * @param person
	 * @param attributeTypeUuid
	 * @return
	 */
	public static String getPersonAttributeValueByTypeUuid(Person person, String attributeTypeUuid) {
		if (person == null || attributeTypeUuid == null) {
			return null;
		}
		
		for (PersonAttribute attribute : person.getActiveAttributes()) {
			PersonAttributeType type = attribute.getAttributeType();
			if (type != null && attributeTypeUuid.equals(type.getUuid())) {
				return attribute.getValue();
			}
		}
		
		return null;
	}
	
	/**
	 * Set Person attribute
	 * 
	 * @param Person
	 * @param attributeTypeUuid
	 * @param value
	 */
	public static void setPersonAttributeValueByTypeUuid(Patient patient, String attributeTypeUuid, String value) {
		if (patient == null || attributeTypeUuid == null || value == null) {
			return;
		}
		
		if (Context.isSessionOpen()) {
			System.out.println("rmsdataexchange Module: We have an open session 2");
			Context.addProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
			Context.addProxyPrivilege(PrivilegeConstants.EDIT_PERSONS);
			Context.addProxyPrivilege(PrivilegeConstants.ADD_PERSONS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			Context.addProxyPrivilege(PrivilegeConstants.GET_PATIENTS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_USERS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_PATIENT_IDENTIFIERS);
		} else {
			System.out.println("rmsdataexchange Module: Error: We have NO open session 2");
			Context.openSession();
			Context.addProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
			Context.addProxyPrivilege(PrivilegeConstants.EDIT_PERSONS);
			Context.addProxyPrivilege(PrivilegeConstants.ADD_PERSONS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			Context.addProxyPrivilege(PrivilegeConstants.GET_PATIENTS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_USERS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_PATIENT_IDENTIFIERS);
		}
		User currentUser = Daemon.getDaemonThreadUser();
		System.out.println("rmsdataexchange Module: Current user in session 2: "
		        + (currentUser != null ? currentUser.getUsername() : ""));
		
		PatientService patientService = Context.getPatientService();
		Patient localPatient = patientService.getPatient(patient.getId());
		
		PersonAttributeType attributeType = Context.getPersonService().getPersonAttributeTypeByUuid(attributeTypeUuid);
		if (attributeType == null) {
			throw new IllegalArgumentException("rmsdataexchange Module: No PersonAttributeType found for UUID: "
			        + attributeTypeUuid);
		}
		
		PersonAttribute existingAttribute = null;
		
		for (PersonAttribute attr : localPatient.getAttributes()) {
			if (attributeType.equals(attr.getAttributeType())) {
				existingAttribute = attr;
				break;
			}
		}
		
		for (PatientIdentifier identifier : localPatient.getIdentifiers()) {
			Hibernate.initialize(identifier.getIdentifierType());
			System.err.println("rmsdataexchange Module: Identifier type: " + identifier.getIdentifierType().getName());
		}
		
		if (existingAttribute != null) {
			existingAttribute.setValue(value); // updates existing
			Context.getPatientService().savePatient(localPatient);
		} else {
			PersonAttribute newAttribute = new PersonAttribute();
			newAttribute.setAttributeType(attributeType);
			newAttribute.setValue(value);
			newAttribute.setCreator(Context.getUserService().getUser(1));
			// newAttribute.setCreator(currentUser);
			newAttribute.setDateCreated(new Date());
			localPatient.addAttribute(newAttribute); // inserts new attribute
			Context.getPatientService().savePatient(localPatient);
		}
		// Context.closeSession();
	}
	
	/**
	 * Get the value of bill attribute
	 * 
	 * @param bill
	 * @param attributeTypeUuid
	 * @return
	 */
	public static String getBillAttributeValueByTypeUuid(Bill bill, String attributeTypeUuid) {
		if (bill == null || attributeTypeUuid == null) {
			return null;
		}
		
		RMSBillAttributeService rmsBillAttributeService = Context.getService(RMSBillAttributeService.class);
		List<RMSBillAttribute> billAttributes = rmsBillAttributeService.getAllBillAttributesByBillId(bill.getId(), false);
		
		for (RMSBillAttribute attribute : billAttributes) {
			RMSBillAttributeType type = attribute.getAttributeType();
			if (type != null && attributeTypeUuid.equals(type.getUuid())) {
				return attribute.getValue();
			}
		}
		
		return null;
	}
	
	/**
	 * Set Bill attribute
	 * 
	 * @param Bill
	 * @param attributeTypeUuid
	 * @param value
	 */
	public static void setBillAttributeValueByTypeUuid(Bill bill, String attributeTypeUuid, String value) {
		if (bill == null || attributeTypeUuid == null || value == null) {
			return;
		}
		
		if (Context.isSessionOpen()) {
			System.out.println("rmsdataexchange Module: We have an open session N");
			Context.addProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
			Context.addProxyPrivilege(PrivilegeConstants.EDIT_PERSONS);
			Context.addProxyPrivilege(PrivilegeConstants.ADD_PERSONS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			Context.addProxyPrivilege(PrivilegeConstants.GET_PATIENTS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_USERS);
		} else {
			System.out.println("rmsdataexchange Module: Error: We have NO open session N");
			Context.openSession();
			Context.addProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
			Context.addProxyPrivilege(PrivilegeConstants.EDIT_PERSONS);
			Context.addProxyPrivilege(PrivilegeConstants.ADD_PERSONS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			Context.addProxyPrivilege(PrivilegeConstants.GET_PATIENTS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_USERS);
		}
		RMSBillAttributeService rmsBillAttributeService = Context.getService(RMSBillAttributeService.class);
		RMSBillAttributeType attributeType = rmsBillAttributeService.getBillAttributeTypeByUuid(attributeTypeUuid);
		if (attributeType == null) {
			throw new IllegalArgumentException("No BillAttributeType found for UUID: " + attributeTypeUuid);
		}
		
		RMSBillAttribute existingAttribute = null;
		List<RMSBillAttribute> billAttributes = rmsBillAttributeService.getAllBillAttributesByBillId(bill.getId(), false);
		
		for (RMSBillAttribute attr : billAttributes) {
			if (attributeType.equals(attr.getAttributeType())) {
				existingAttribute = attr;
				break;
			}
		}
		
		if (existingAttribute != null) {
			existingAttribute.setValue(value); // updates existing
			rmsBillAttributeService.saveBillAttribute(existingAttribute);
		} else {
			RMSBillAttribute newAttribute = new RMSBillAttribute();
			newAttribute.setAttributeType(attributeType);
			newAttribute.setValue(value);
			newAttribute.setCreator(Context.getUserService().getUser(1));
			newAttribute.setDateCreated(new Date());
			newAttribute.setBill(bill);
			rmsBillAttributeService.saveBillAttribute(newAttribute);
		}
	}
	
	/**
	 * Get the value of payment attribute
	 * 
	 * @param payment
	 * @param attributeTypeUuid
	 * @return
	 */
	public static String getPaymentAttributeValueByTypeUuid(Payment payment, String attributeTypeUuid) {
		if (payment == null || attributeTypeUuid == null) {
			return null;
		}
		
		RMSPaymentAttributeService rmsPaymentAttributeService = Context.getService(RMSPaymentAttributeService.class);
		List<RMSPaymentAttribute> paymentAttributes = rmsPaymentAttributeService.getAllPaymentAttributesByPaymentId(
		    payment.getId(), false);
		
		for (RMSPaymentAttribute attribute : paymentAttributes) {
			RMSPaymentAttributeType type = attribute.getAttributeType();
			if (type != null && attributeTypeUuid.equals(type.getUuid())) {
				return attribute.getValue();
			}
		}
		
		return null;
	}
	
	/**
	 * Set payment attribute
	 * 
	 * @param Payment
	 * @param attributeTypeUuid
	 * @param value
	 */
	public static void setPaymentAttributeValueByTypeUuid(Payment payment, String attributeTypeUuid, String value) {
		if (payment == null || attributeTypeUuid == null || value == null) {
			return;
		}
		
		RMSPaymentAttributeService rmsPaymentAttributeService = Context.getService(RMSPaymentAttributeService.class);
		RMSPaymentAttributeType attributeType = rmsPaymentAttributeService.getPaymentAttributeTypeByUuid(attributeTypeUuid);
		if (attributeType == null) {
			throw new IllegalArgumentException("No PaymentAttributeType found for UUID: " + attributeTypeUuid);
		}
		
		RMSPaymentAttribute existingAttribute = null;
		List<RMSPaymentAttribute> billAttributes = rmsPaymentAttributeService.getAllPaymentAttributesByPaymentId(
		    payment.getId(), false);
		
		for (RMSPaymentAttribute attr : billAttributes) {
			if (attributeType.equals(attr.getAttributeType())) {
				existingAttribute = attr;
				break;
			}
		}
		
		if (existingAttribute != null) {
			existingAttribute.setValue(value); // updates existing
			rmsPaymentAttributeService.savePaymentAttribute(existingAttribute);
		} else {
			RMSPaymentAttribute newAttribute = new RMSPaymentAttribute();
			newAttribute.setAttributeType(attributeType);
			newAttribute.setValue(value);
			newAttribute.setCreator(Context.getUserService().getUser(1));
			newAttribute.setDateCreated(new Date());
			newAttribute.setPayment(payment);
			rmsPaymentAttributeService.savePaymentAttribute(newAttribute);
		}
	}
	
	/**
	 * Get the status of sync chores
	 * 
	 * @return true - already synced, false - not synced
	 */
	// public static Boolean getRMSSyncStatus() {
	// 	Boolean ret = false;
	
	// 	GlobalProperty rmsPatientSyncStatusGP = Context.getAdministrationService().getGlobalPropertyObject(
	// 	    RMSModuleConstants.RMS_PATIENT_SYNC_STATUS);
	// 	GlobalProperty rmsBillSyncStatusGP = Context.getAdministrationService().getGlobalPropertyObject(
	// 	    RMSModuleConstants.RMS_BILL_SYNC_STATUS);
	// 	String rmsPatientSyncStatus = rmsPatientSyncStatusGP.getPropertyValue();
	// 	String rmsBillSyncStatus = rmsBillSyncStatusGP.getPropertyValue();
	// 	String patientTest = (rmsPatientSyncStatus == null || rmsPatientSyncStatus.trim().isEmpty()) ? ""
	// 	        : rmsPatientSyncStatus.trim();
	// 	String billTest = (rmsBillSyncStatus == null || rmsBillSyncStatus.trim().isEmpty()) ? "" : rmsBillSyncStatus.trim();
	
	// 	if (patientTest.equalsIgnoreCase("true") && billTest.equalsIgnoreCase("true")) {
	// 		return (true);
	// 	}
	
	// 	return (ret);
	// }
	
	/**
	 * Mark the sync chores as done
	 * 
	 * @return
	 */
	// public static void setRMSSyncStatus(Boolean status) {
	
	// 	GlobalProperty rmsPatientSyncStatusGP = Context.getAdministrationService().getGlobalPropertyObject(
	// 	    RMSModuleConstants.RMS_PATIENT_SYNC_STATUS);
	// 	GlobalProperty rmsBillSyncStatusGP = Context.getAdministrationService().getGlobalPropertyObject(
	// 	    RMSModuleConstants.RMS_BILL_SYNC_STATUS);
	// 	if (status) {
	// 		rmsPatientSyncStatusGP.setPropertyValue("true");
	// 		rmsBillSyncStatusGP.setPropertyValue("true");
	// 	} else {
	// 		rmsPatientSyncStatusGP.setPropertyValue("false");
	// 		rmsBillSyncStatusGP.setPropertyValue("false");
	// 	}
	// }
	
}
