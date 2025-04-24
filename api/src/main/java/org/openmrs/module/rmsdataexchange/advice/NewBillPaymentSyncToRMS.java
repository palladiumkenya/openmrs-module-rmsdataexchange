package org.openmrs.module.rmsdataexchange.advice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentMode;
import org.openmrs.module.rmsdataexchange.api.util.AdviceUtils;
import org.openmrs.module.rmsdataexchange.api.util.SimpleObject;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeService;

/**
 * Detects when a new payment has been made to a bill and syncs to RMS Financial System
 */
public class NewBillPaymentSyncToRMS implements MethodInterceptor {
	
	private Boolean debugMode = false;
	
	private RmsdataexchangeService billService;
	
	public RmsdataexchangeService getBillService() {
		return billService;
	}
	
	public void setBillService(RmsdataexchangeService billService) {
		this.billService = billService;
	}
	
	@Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        Object result = null;
        try {
			debugMode = AdviceUtils.isRMSLoggingEnabled();
            if(AdviceUtils.isRMSIntegrationEnabled()) {
                String methodName = invocation.getMethod().getName();
                if(debugMode) System.out.println("rmsdataexchange Module: method intercepted: " + methodName);
				Bill oldBill = new Bill();
            
                if ("save".equalsIgnoreCase(methodName)) {
                    if(debugMode) System.out.println("rmsdataexchange Module: Intercepting save bill method");

                    Object[] args = invocation.getArguments();
					Set<Payment> oldPayments = new HashSet<>();
                    
                    if (args.length > 0 && args[0] instanceof Bill) {
                        oldBill = (Bill) args[0];
                        
						Integer oldBillId = oldBill.getId();
						oldPayments = billService.getPaymentsByBillId(oldBillId);
                    }
                    
                    // Proceed with the original method

                    try {
						result = invocation.proceed();
					} catch(Exception et) {}

                    try {
                        Bill newBill = (Bill) result;

						if(result != null && newBill != null) {
							Set<Payment> newPayments = newBill.getPayments();

							if(debugMode) System.out.println("rmsdataexchange Module: Got a bill edit. checking if it is a payment. OldPayments: " + oldPayments.size() + " NewPayments: " + newPayments.size());

							if(newPayments.size() > oldPayments.size()) {
								if(debugMode) System.out.println("rmsdataexchange Module: New bill payment detected");

								Set<Payment> payments = AdviceUtils.symmetricPaymentDifference(oldPayments, newPayments);
								if(debugMode) System.out.println("rmsdataexchange Module: New bill payments made: " + payments.size());

								for(Payment payment : payments) {
									// Use a thread to send the data. This frees up the frontend to proceed
									syncPaymentRunnable runner = new syncPaymentRunnable(payment);
									Thread thread = new Thread(runner);
									thread.start();
								}
							}
						}
                    } catch(Exception ex) {
                        if(debugMode) System.err.println("rmsdataexchange Module: Error checking for pre bill payments: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                } else {
                    if(debugMode) System.out.println("rmsdataexchange Module: This is not the save method. We ignore.");
                    try {
						result = invocation.proceed();
					} catch(Exception et) {}
                }
            }
        } catch(Exception ex) {
            if(debugMode) System.err.println("rmsdataexchange Module: Error checking for post bill payments: " + ex.getMessage());
            ex.printStackTrace();
			// Any failure in RMS should not cause the payment to fail so we always proceed the invocation
			try {
            	result = invocation.proceed();
			} catch(Exception et) {}
        }
        
        return (result);
    }
	
	/**
	 * Send payment to remote RMS system
	 * 
	 * @param paymentsBefore payments in the bill before saving
	 * @param paymentsAfter payments in the bill after saving
	 */
	public static void checkPaymentsAndSendToRMS(Set<Payment> oldPayments, Set<Payment> newPayments) {
		if (newPayments.size() > oldPayments.size()) {
			Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
			
			if (debugMode)
				System.out.println("rmsdataexchange Module: New bill payment detected");
			
			Set<Payment> payments = AdviceUtils.symmetricPaymentDifference(oldPayments, newPayments);
			if (debugMode)
				System.out.println("rmsdataexchange Module: New bill payments made: " + payments.size());
			
			for (Payment payment : payments) {
				// Use a thread to send the data. This frees up the frontend to proceed
				syncPaymentRunnable runner = new syncPaymentRunnable(payment);
				Thread thread = new Thread(runner);
				thread.start();
			}
		}
	}
	
	/**
	 * Prepare the payment payload
	 * 
	 * @param bill
	 * @return
	 */
	public static String prepareBillPaymentRMSPayload(@NotNull Payment payment) {
		String ret = "";
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		
		if (payment != null) {
			if (debugMode)
				System.out.println("rmsdataexchange Module: New bill payment created: UUID: " + payment.getUuid()
				        + ", Amount Tendered: " + payment.getAmountTendered());
			SimpleObject payloadPrep = new SimpleObject();
			payloadPrep.put("bill_reference", payment.getBill().getUuid());
			payloadPrep.put("amount_paid", payment.getAmountTendered());
			PaymentMode paymentMode = payment.getInstanceType();
			payloadPrep.put("payment_method_id", paymentMode != null ? paymentMode.getId() : 1);
			
			ret = payloadPrep.toJson();
			if (debugMode)
				System.out.println("rmsdataexchange Module: Got payment details: " + ret);
		} else {
			if (debugMode)
				System.out.println("rmsdataexchange Module: payment is null");
		}
		return (ret);
	}
	
	/**
	 * Send the new payment payload to RMS
	 * 
	 * @param patient
	 * @return
	 */
	public static Boolean sendRMSNewPayment(@NotNull Payment payment) {
		Boolean ret = false;
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		
		String payload = prepareBillPaymentRMSPayload(payment);
		
		HttpsURLConnection con = null;
		HttpsURLConnection connection = null;
		try {
			if (debugMode)
				System.out.println("rmsdataexchange Module: using payment payload: " + payload);
			
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
							System.out
							        .println("rmsdataexchange Module: We got the Auth token. Now sending the new bill payment details. Payload: "
							                + payload);
						String finalUrl = baseURL + "/bill-payment";
						if (debugMode)
							System.out.println("rmsdataexchange Module: Final Create Payment URL: " + finalUrl);
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
								System.out.println("rmsdataexchange Module: Got New Payment Response as: "
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
									System.out.println("rmsdataexchange Module: Got New Payment final response: success: "
									        + success + " message: " + message);
							}
							catch (Exception e) {
								if (debugMode)
									System.err.println("rmsdataexchange Module: Error getting New Payment final response: "
									        + e.getMessage());
								e.printStackTrace();
							}
							
							if (success != null && success == true) {
								ret = true;
							}
							
						} else {
							if (debugMode)
								System.err.println("rmsdataexchange Module: Failed to send New Payment to RMS: Error Code: "
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
								        .println("rmsdataexchange Module: Failed to send New Payment to RMS: Error Response: "
								                + errorResponse.toString());
							}
							catch (Exception et) {}
						}
					}
					catch (Exception em) {
						if (debugMode)
							System.err
							        .println("rmsdataexchange Module: Error. Failed to send the New Payment final payload: "
							                + em.getMessage());
						em.printStackTrace();
					}
				}
			} else {
				if (debugMode)
					System.err.println("rmsdataexchange Module: Bill Payment Failed to get auth: " + responseCode);
			}
			
		}
		catch (Exception ex) {
			if (debugMode)
				System.err.println("rmsdataexchange Module: Error. Bill Payment Failed to get auth token: "
				        + ex.getMessage());
			ex.printStackTrace();
		}
		
		return (ret);
	}
	
	/**
	 * A thread to free up the frontend
	 */
	private static class syncPaymentRunnable implements Runnable {
		
		Payment payment = new Payment();
		
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		
		public syncPaymentRunnable(@NotNull Payment payment) {
			this.payment = payment;
		}
		
		@Override
		public void run() {
			// Run the thread
			
			try {
				if (debugMode)
					System.out.println("rmsdataexchange Module: Start sending payment to RMS");
				
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
				
				// If the patient doesnt exist, send the patient to RMS
				if (debugMode)
					System.out.println("RMS Sync RMSDataExchange Module Bill Payment: Send the patient first");
				Boolean testPatientSending = NewPatientRegistrationSyncToRMS.sendRMSPatientRegistration(payment.getBill()
				        .getPatient());
				
				if (testPatientSending) {
					if (debugMode)
						System.out.println("rmsdataexchange Module: Finished sending Patient to RMS");
				} else {
					if (debugMode)
						System.out.println("rmsdataexchange Module: Failed to send Patient to RMS");
				}
				
				sleepTime = AdviceUtils.getRandomInt(5000, 10000);
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
				
				// If the bill doesnt exist, send the bill to RMS
				if (debugMode)
					System.out.println("RMS Sync RMSDataExchange Module Bill Payment: Send the bill next");
				Boolean testBillSending = NewBillCreationSyncToRMS.sendRMSNewBill(payment.getBill());
				
				if (testBillSending) {
					if (debugMode)
						System.out.println("rmsdataexchange Module: Finished sending Bill to RMS");
				} else {
					if (debugMode)
						System.out.println("rmsdataexchange Module: Failed to send Bill to RMS");
				}
				
				sleepTime = AdviceUtils.getRandomInt(5000, 10000);
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
				
				// Now we can send the bill payment
				Boolean testPaymentSending = sendRMSNewPayment(payment);
				
				if (testPaymentSending) {
					if (debugMode)
						System.out.println("rmsdataexchange Module: Successfully Finished sending payment to RMS");
				} else {
					if (debugMode)
						System.out.println("rmsdataexchange Module: Failed to send payment to RMS");
				}
			}
			catch (Exception ex) {
				if (debugMode)
					System.err.println("rmsdataexchange Module: Error. Failed to send payment to RMS: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
}
