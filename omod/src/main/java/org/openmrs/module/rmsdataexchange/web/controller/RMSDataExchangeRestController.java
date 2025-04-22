package org.openmrs.module.rmsdataexchange.web.controller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.rmsdataexchange.api.util.AdviceUtils;
import org.openmrs.module.rmsdataexchange.api.util.SimpleObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.openmrs.module.webservices.rest.web.RestConstants;

// @Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/rmsdataexchange/api")
public class RMSDataExchangeRestController extends BaseRestController {
	
	/**
	 * Send RMS MPESA STK Push
	 * 
	 * @param request
	 * @return response proxy
	 */
	@CrossOrigin(origins = "*", methods = { RequestMethod.POST, RequestMethod.OPTIONS })
	@Authorized
	@RequestMapping(method = RequestMethod.POST, value = "/rmsstkpush")
	@ResponseBody
	public Object rmsSTKPush(HttpServletRequest request) {
		String ret = "{\n" + //
		        "    \"message\": \"Error. Failed to forward STK Push\",\n" + //
		        "    \"success\": false,\n" + //
		        "    \"requestId\": \"\"\n" + //
		        "}";
		
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		
		if (AdviceUtils.isRMSIntegrationEnabled()) {
			try {
				String requestBody = "";
				BufferedReader requestReader = request.getReader();
				
				for (String output = ""; (output = requestReader.readLine()) != null; requestBody = requestBody
						+ output) {}
				System.out.println("RMS Sync RMSDataExchange Module: Received STK push details: " + requestBody);
				
				// Login first
				// HttpsURLConnection con = null;
				HttpURLConnection con = null;
				HttpsURLConnection connection = null;
				
				// Create URL
				String baseURL = AdviceUtils.getRMSEndpointURL();
				String completeURL = baseURL + "/login";
				if (debugMode)
					System.out.println("RMS Sync RMSDataExchange Module: STK push Auth URL: " + completeURL);
				URL url = new URL(completeURL);
				String rmsUser = AdviceUtils.getRMSAuthUserName();
				String rmsPassword = AdviceUtils.getRMSAuthPassword();
				SimpleObject authPayloadCreator = SimpleObject.create("email", rmsUser != null ? rmsUser : "", "password",
				    rmsPassword != null ? rmsPassword : "");
				String authPayload = authPayloadCreator.toJson();
				if (debugMode)
					System.out.println("RMS Sync RMSDataExchange Module: STK push Auth Payload: " + authPayload);
				
				// Get token
				if (url.getProtocol().equalsIgnoreCase("https")) {
					con = (HttpsURLConnection) url.openConnection();
				} else if (url.getProtocol().equalsIgnoreCase("http")) {
					con = (HttpURLConnection) url.openConnection();
				}
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
						System.out.println("RMS Sync RMSDataExchange Module: Got STK push Auth Response as: " + returnResponse);
					
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
							System.err.println("RMS Sync RMSDataExchange Module: Error getting STK push auth token: "
							        + e.getMessage());
						e.printStackTrace();
					}
					
					if (!token.isEmpty()) {
						// Send Request
						try {
							// We send the payload to RMS
							if (debugMode)
								System.err
								        .println("RMS Sync RMSDataExchange Module: We got the Auth token. Now sending the STK push details. Token: "
								                + token);
							String finalUrl = baseURL + "/stk-push";
							if (debugMode)
								System.out.println("RMS Sync RMSDataExchange Module: Final STK push URL: " + finalUrl);
							URL finUrl = new URL(finalUrl);
							
							connection = (HttpsURLConnection) finUrl.openConnection();
							connection.setRequestMethod("POST");
							connection.setDoOutput(true);
							connection.setRequestProperty("Authorization", "Bearer " + token);
							connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
							connection.setRequestProperty("Accept", "application/json");
							connection.setConnectTimeout(10000);
							
							// Repost the request
							if (debugMode)
								System.out.println("RMS Sync RMSDataExchange Module: Sending STK push to remote: " + requestBody);
							
							PrintStream pos = new PrintStream(connection.getOutputStream());
							pos.print(requestBody);
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
									System.out.println("RMS Sync RMSDataExchange Module: Got STK push Response as: "
									        + finalReturnResponse);
								
								// forward the responce
								HttpHeaders headers = new HttpHeaders();
								headers.setContentType(MediaType.APPLICATION_JSON);
								return ResponseEntity.ok().headers(headers).body(finalReturnResponse);
								
							} else {
								if (debugMode)
									System.err.println("RMS Sync RMSDataExchange Module: Failed to forward STK push final payload: "
									        + finalResponseCode);
								
								InputStream errorStream = connection.getErrorStream();
								// Read the error response body
								BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
								StringBuilder errorResponse = new StringBuilder();
								String line;
								while ((line = errorReader.readLine()) != null) {
									errorResponse.append(line);
								}
								
								// Close the reader and the error stream
								errorReader.close();
								errorStream.close();
								
								// Handle or log the error response
								String errorBody = errorResponse.toString();
								if (debugMode)
									System.err
									        .println("RMS Sync RMSDataExchange Module: STK Push Error response body: " + errorBody);
								
								HttpHeaders headers = new HttpHeaders();
								String contentType = con.getHeaderField("Content-Type");
								if (contentType != null && contentType.toLowerCase().contains("json")) {
									headers.setContentType(MediaType.APPLICATION_JSON);
								} else {
									headers.setContentType(MediaType.TEXT_PLAIN);
								}
								
								return ResponseEntity.status(finalResponseCode).headers(headers).body(errorBody);
							}
						}
						catch (Exception em) {
							if (debugMode)
								System.err
								        .println("RMS Sync RMSDataExchange Module: Error. Failed to forward STK push final payload: "
								                + em.getMessage());
							em.printStackTrace();
						}
					}
				} else {
					if (debugMode)
						System.err.println("RMS Sync RMSDataExchange Module: STK Push Failed to get auth: " + responseCode);
					try {
						HttpStatus status = HttpStatus.resolve(responseCode);
						return new ResponseEntity<>(ret, status);
					} catch(Exception ec) {}
				}
			}
			catch (Exception ex) {
				if (debugMode)
					System.err.println("RMS Sync RMSDataExchange Module: STK Push Error: " + ex.getMessage());
				ex.printStackTrace();
			}
			
		} else {
			if (debugMode)
				System.err.println("RMS Sync RMSDataExchange Module: STK Push Failed: RMS integration is disabled");
		}
		
		return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(ret);
	}
	
	/**
	 * Send RMS MPESA STK Check
	 * 
	 * @param request
	 * @return response proxy
	 */
	@CrossOrigin(origins = "*", methods = { RequestMethod.POST, RequestMethod.OPTIONS })
	@Authorized
	@RequestMapping(method = RequestMethod.POST, value = "/rmsstkcheck")
	@ResponseBody
	public Object rmsSTKCheck(HttpServletRequest request) {
		String ret = "{\n" + //
		        "    \"success\": false,\n" + //
		        "    \"status\": \"FAILED\",\n" + //
		        "    \"message\": \"Error. Failed to forward STK check status check \",\n" + //
		        "    \"referenceCode\": null\n" + //
		        "}";
		
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		
		if (AdviceUtils.isRMSIntegrationEnabled()) {
			try {
				System.out.println("New NUPI: Received STK check details: " + request.getQueryString());
				
				// Login first
				HttpsURLConnection con = null;
				HttpsURLConnection connection = null;
				
				// Create URL
				String baseURL = AdviceUtils.getRMSEndpointURL();
				String completeURL = baseURL + "/login";
				if (debugMode)
					System.out.println("RMS Sync RMSDataExchange Module: STK check Auth URL: " + completeURL);
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
						System.out.println("RMS Sync RMSDataExchange Module: Got STK check Auth Response as: "
						        + returnResponse);
					
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
							System.err.println("RMS Sync RMSDataExchange Module: Error getting STK check auth token: "
							        + e.getMessage());
						e.printStackTrace();
					}
					
					if (!token.isEmpty()) {
						// Send Request
						try {
							// We send the payload to RMS
							if (debugMode)
								System.err
								        .println("RMS Sync RMSDataExchange Module: We got the Auth token. Now sending the STK check details. Token: "
								                + token);
							String finalUrl = baseURL + "/stk-push-query";
							if (debugMode)
								System.out.println("RMS Sync RMSDataExchange Module: Final STK check URL: " + finalUrl);
							URL finUrl = new URL(finalUrl);
							
							connection = (HttpsURLConnection) finUrl.openConnection();
							connection.setRequestMethod("POST");
							connection.setDoOutput(true);
							connection.setRequestProperty("Authorization", "Bearer " + token);
							connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
							connection.setRequestProperty("Accept", "application/json");
							connection.setConnectTimeout(10000);
							
							// Repost the request
							String requestBody = "";
							BufferedReader requestReader = request.getReader();
							
							for (String output = ""; (output = requestReader.readLine()) != null; requestBody = requestBody
							        + output) {}
							if (debugMode)
								System.out.println("RMS Sync RMSDataExchange Module: Sending STK check to remote: "
								        + requestBody);
							
							PrintStream pos = new PrintStream(connection.getOutputStream());
							pos.print(requestBody);
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
									System.out.println("RMS Sync RMSDataExchange Module: Got STK check Response as: "
									        + finalReturnResponse);
								
								// forward the responce
								HttpHeaders headers = new HttpHeaders();
								headers.setContentType(MediaType.APPLICATION_JSON);
								return ResponseEntity.ok().headers(headers).body(finalReturnResponse);
								
							} else {
								if (debugMode)
									System.err
									        .println("RMS Sync RMSDataExchange Module: Failed to forward STK check final payload: "
									                + finalResponseCode);
								
								InputStream errorStream = connection.getErrorStream();
								// Read the error response body
								BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
								StringBuilder errorResponse = new StringBuilder();
								String line;
								while ((line = errorReader.readLine()) != null) {
									errorResponse.append(line);
								}
								
								// Close the reader and the error stream
								errorReader.close();
								errorStream.close();
								
								// Handle or log the error response
								String errorBody = errorResponse.toString();
								if (debugMode)
									System.err.println("RMS Sync RMSDataExchange Module: STK check Error response body: "
									        + errorBody);
								
								HttpHeaders headers = new HttpHeaders();
								String contentType = con.getHeaderField("Content-Type");
								if (contentType != null && contentType.toLowerCase().contains("json")) {
									headers.setContentType(MediaType.APPLICATION_JSON);
								} else {
									headers.setContentType(MediaType.TEXT_PLAIN);
								}
								
								return ResponseEntity.status(finalResponseCode).headers(headers).body(errorBody);
							}
						}
						catch (Exception em) {
							if (debugMode)
								System.err
								        .println("RMS Sync RMSDataExchange Module: Error. Failed to forward STK check final payload: "
								                + em.getMessage());
							em.printStackTrace();
						}
					}
				} else {
					if (debugMode)
						System.err.println("RMS Sync RMSDataExchange Module: STK check Failed to get auth: " + responseCode);
				}
			}
			catch (Exception ex) {
				if (debugMode)
					System.err.println("RMS Sync RMSDataExchange Module: STK check Error: " + ex.getMessage());
				ex.printStackTrace();
			}
			
		} else {
			if (debugMode)
				System.err.println("RMS Sync RMSDataExchange Module: STK check Failed: RMS integration is disabled");
		}
		
		return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(ret);
	}
	
	/**
	 * Send RMS MPESA TRANSACTION CHECKER
	 * 
	 * @param request
	 * @param transactionId
	 * @return response proxy
	 */
	@CrossOrigin(origins = "*", methods = { RequestMethod.POST, RequestMethod.OPTIONS })
	@Authorized
	@RequestMapping(method = RequestMethod.GET, value = "/rmsmpesachecker")
	@ResponseBody
	public Object rmsMPESAChecker(HttpServletRequest request, @RequestParam("transactionId") String transactionId) {
		String ret = "{\n" + //
		        "    \"message\": \"Error. Failed to check mpesa transaction\",\n" + //
		        "    \"success\": false,\n" + //
		        "    \"requestId\": \"\"\n" + //
		        "}";
		
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		
		if (AdviceUtils.isRMSIntegrationEnabled()) {
			try {
				String requestBody = "";
				System.out.println("RMS Sync RMSDataExchange Module: MPESA transaction check Received transaction Id: " + transactionId);
				
				// Login first
				// HttpsURLConnection con = null;
				HttpURLConnection con = null;
				HttpsURLConnection connection = null;
				
				// Create URL
				String baseURL = AdviceUtils.getRMSEndpointURL();
				String completeURL = baseURL + "/login";
				if (debugMode)
					System.out.println("RMS Sync RMSDataExchange Module: MPESA Trans Check Auth URL: " + completeURL);
				URL url = new URL(completeURL);
				String rmsUser = AdviceUtils.getRMSAuthUserName();
				String rmsPassword = AdviceUtils.getRMSAuthPassword();
				SimpleObject authPayloadCreator = SimpleObject.create("email", rmsUser != null ? rmsUser : "", "password",
				    rmsPassword != null ? rmsPassword : "");
				String authPayload = authPayloadCreator.toJson();
				if (debugMode)
					System.out.println("RMS Sync RMSDataExchange Module: MPESA Trans Check Auth Payload: " + authPayload);
				
				// Get token
				if (url.getProtocol().equalsIgnoreCase("https")) {
					con = (HttpsURLConnection) url.openConnection();
				} else if (url.getProtocol().equalsIgnoreCase("http")) {
					con = (HttpURLConnection) url.openConnection();
				}
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
						System.out.println("RMS Sync RMSDataExchange Module: Got MPESA Trans Check Auth Response as: " + returnResponse);
					
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
							System.err.println("RMS Sync RMSDataExchange Module: Error getting MPESA Trans Check auth token: "
							        + e.getMessage());
						e.printStackTrace();
					}
					
					if (!token.isEmpty()) {
						// Send Request
						try {
							// We send the payload to RMS
							if (debugMode)
								System.err
								        .println("RMS Sync RMSDataExchange Module: We got the Auth token. Now sending the MPESA Trans Check details. Token: "
								                + token);
							String finalUrl = baseURL + "/validate-payment" + "?TransID=" + transactionId;
							if (debugMode)
								System.out.println("RMS Sync RMSDataExchange Module: Final MPESA Trans Check URL: " + finalUrl);
							URL finUrl = new URL(finalUrl);
							
							connection = (HttpsURLConnection) finUrl.openConnection();
							connection.setRequestMethod("POST");
							connection.setDoOutput(true);
							connection.setRequestProperty("Authorization", "Bearer " + token);
							connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
							connection.setRequestProperty("Accept", "application/json");
							connection.setConnectTimeout(10000);
							
							// post the request TODO: Note, this is bad use of the POST method i.e this should be a GET request since we are sending the parameter as part of the URL
							if (debugMode)
								System.out.println("RMS Sync RMSDataExchange Module: Sending MPESA Trans Check to remote: " + requestBody);
							
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
									System.out.println("RMS Sync RMSDataExchange Module: Got MPESA Trans Check Response as: "
									        + finalReturnResponse);
								
								// forward the responce
								HttpHeaders headers = new HttpHeaders();
								headers.setContentType(MediaType.APPLICATION_JSON);
								return ResponseEntity.ok().headers(headers).body(finalReturnResponse);
								
							} else {
								if (debugMode)
									System.err.println("RMS Sync RMSDataExchange Module: Failed to forward MPESA Trans Check final payload: "
									        + finalResponseCode);
								
								InputStream errorStream = connection.getErrorStream();
								// Read the error response body
								BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
								StringBuilder errorResponse = new StringBuilder();
								String line;
								while ((line = errorReader.readLine()) != null) {
									errorResponse.append(line);
								}
								
								// Close the reader and the error stream
								errorReader.close();
								errorStream.close();
								
								// Handle or log the error response
								String errorBody = errorResponse.toString();
								if (debugMode)
									System.err
									        .println("RMS Sync RMSDataExchange Module: MPESA Trans Check Error response body: " + errorBody);
								
								HttpHeaders headers = new HttpHeaders();
								String contentType = con.getHeaderField("Content-Type");
								if (contentType != null && contentType.toLowerCase().contains("json")) {
									headers.setContentType(MediaType.APPLICATION_JSON);
								} else {
									headers.setContentType(MediaType.TEXT_PLAIN);
								}
								
								return ResponseEntity.status(finalResponseCode).headers(headers).body(errorBody);
							}
						}
						catch (Exception em) {
							if (debugMode)
								System.err
								        .println("RMS Sync RMSDataExchange Module: Error. Failed to forward MPESA Trans Check final payload: "
								                + em.getMessage());
							em.printStackTrace();
						}
					}
				} else {
					if (debugMode)
						System.err.println("RMS Sync RMSDataExchange Module: MPESA Trans Check Failed to get auth: " + responseCode);
					try {
						HttpStatus status = HttpStatus.resolve(responseCode);
						return new ResponseEntity<>(ret, status);
					} catch(Exception ec) {}
				}
			}
			catch (Exception ex) {
				if (debugMode)
					System.err.println("RMS Sync RMSDataExchange Module: MPESA Trans Check Error: " + ex.getMessage());
				ex.printStackTrace();
			}
			
		} else {
			if (debugMode)
				System.err.println("RMS Sync RMSDataExchange Module: MPESA Trans Check Failed: RMS integration is disabled");
		}
		
		return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(ret);
	}
}
