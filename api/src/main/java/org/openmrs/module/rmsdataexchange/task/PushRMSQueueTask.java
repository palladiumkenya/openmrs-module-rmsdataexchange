package org.openmrs.module.rmsdataexchange.task;

import java.util.List;

import org.openmrs.api.context.Context;
import org.openmrs.module.rmsdataexchange.advice.NewBillCreationSyncToRMS;
import org.openmrs.module.rmsdataexchange.advice.NewBillPaymentSyncToRMS;
import org.openmrs.module.rmsdataexchange.advice.NewPatientRegistrationSyncToRMS;
import org.openmrs.module.rmsdataexchange.advice.NewPatientVisitSyncToWonderHealth;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeService;
import org.openmrs.module.rmsdataexchange.api.util.AdviceUtils;
import org.openmrs.module.rmsdataexchange.api.util.RMSModuleConstants;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueue;
import org.openmrs.module.rmsdataexchange.queue.model.RMSQueueSystem;
import org.openmrs.scheduler.tasks.AbstractTask;

public class PushRMSQueueTask extends AbstractTask {
	
	private Boolean debugMode = false;
	
	@Override
	public void execute() {
		debugMode = AdviceUtils.isRMSLoggingEnabled();
		if (debugMode)
			System.err.println("rmsdataexchange module: starting the RMS queue processing scheduled task");
		// Get a list of all queue items
		RmsdataexchangeService rmsdataexchangeService = Context.getService(RmsdataexchangeService.class);
		List<RMSQueue> rmsQueueItems = rmsdataexchangeService.getQueueItems();
		if (rmsQueueItems != null && rmsQueueItems.size() > 0) {
			if (debugMode)
				System.err.println("rmsdataexchange module: Queue Processing: There are some items in the queue");
			for (RMSQueue item : rmsQueueItems) {
				if (!item.getVoided()) {
					// Get the payload
					String payload = item.getPayload();
					// Check if item needs mods
					Boolean modified = false;
					// Get the system used
					RMSQueueSystem system = item.getRmsSystem();
					if (system.getUuid() == RMSModuleConstants.RMS_SYSTEM_PATIENT) {
						// This is a payload for RMS system Patient
						Integer sleepTime = AdviceUtils.getRandomInt(5000, 10000);
						// Delay
						try {
							//Delay for random seconds
							if (debugMode)
								System.out.println("rmsdataexchange Module: Queue Processing: Sleep for milliseconds: "
								        + sleepTime);
							Thread.sleep(sleepTime);
						}
						catch (Exception ie) {
							Thread.currentThread().interrupt();
						}
						
						Boolean sendPatientToRMSResult = NewPatientRegistrationSyncToRMS.sendRMSPatientRegistration(payload);
						
						if (sendPatientToRMSResult == false) {
							// Failed to send the payload. We update the retries
							if (debugMode)
								System.err
								        .println("rmsdataexchange Module: Queue Processing: Failed to send patient to RMS");
							item.setRetries(item.getRetries() + 1);
						} else {
							// Sent the payload. We remove the queue entry
							if (debugMode)
								System.out
								        .println("rmsdataexchange Module: Queue Processing: Finished sending patient to RMS");
							item.setRetries(item.getRetries() + 1);
							item.setVoided(true);
							item.setVoidReason("Payload Sent");
						}
						modified = true;
					} else if (system.getUuid() == RMSModuleConstants.RMS_SYSTEM_BILL) {
						// This is a payload for RMS system Bill
						Integer sleepTime = AdviceUtils.getRandomInt(5000, 10000);
						// Delay
						try {
							//Delay for random seconds
							if (debugMode)
								System.out.println("rmsdataexchange Module: Queue Processing: Sleep for milliseconds: "
								        + sleepTime);
							Thread.sleep(sleepTime);
						}
						catch (Exception ie) {
							Thread.currentThread().interrupt();
						}
						
						Boolean sendBillToRMSResult = NewBillCreationSyncToRMS.sendRMSNewBill(payload);
						
						if (sendBillToRMSResult == false) {
							// Failed to send the payload. We update the retries
							if (debugMode)
								System.err.println("rmsdataexchange Module: Queue Processing: Failed to send bill to RMS");
							item.setRetries(item.getRetries() + 1);
						} else {
							// Sent the payload. We remove the queue entry
							if (debugMode)
								System.out.println("rmsdataexchange Module: Queue Processing: Finished sending bill to RMS");
							item.setRetries(item.getRetries() + 1);
							item.setVoided(true);
							item.setVoidReason("Payload Sent");
						}
						modified = true;
					} else if (system.getUuid() == RMSModuleConstants.RMS_SYSTEM_PAYMENT) {
						// This is a payload for RMS system Bill Payment
						Integer sleepTime = AdviceUtils.getRandomInt(5000, 10000);
						// Delay
						try {
							//Delay for random seconds
							if (debugMode)
								System.out.println("rmsdataexchange Module: Queue Processing: Sleep for milliseconds: "
								        + sleepTime);
							Thread.sleep(sleepTime);
						}
						catch (Exception ie) {
							Thread.currentThread().interrupt();
						}
						
						Boolean sendBillPaymentToRMSResult = NewBillPaymentSyncToRMS.sendRMSNewPayment(payload);
						
						if (sendBillPaymentToRMSResult == false) {
							// Failed to send the payload. We update the retries
							if (debugMode)
								System.err
								        .println("rmsdataexchange Module: Queue Processing: Failed to send bill payment to RMS");
							item.setRetries(item.getRetries() + 1);
						} else {
							// Sent the payload. We remove the queue entry
							if (debugMode)
								System.out
								        .println("rmsdataexchange Module: Queue Processing: Finished sending bill payment to RMS");
							item.setRetries(item.getRetries() + 1);
							item.setVoided(true);
							item.setVoidReason("Payload Sent");
						}
						modified = true;
					} else if (system.getUuid() == RMSModuleConstants.WONDER_HEALTH_SYSTEM_PATIENT) {
						// This is a payload for Wonder Health system Patient
						Integer sleepTime = AdviceUtils.getRandomInt(5000, 10000);
						// Delay
						try {
							//Delay for random seconds
							if (debugMode)
								System.out.println("rmsdataexchange Module: Queue Processing: Sleep for milliseconds: "
								        + sleepTime);
							Thread.sleep(sleepTime);
						}
						catch (Exception ie) {
							Thread.currentThread().interrupt();
						}
						
						Boolean sendWonderHealthResult = NewPatientVisitSyncToWonderHealth
						        .sendWonderHealthPatientRegistration(payload);
						if (sendWonderHealthResult == false) {
							// Failed to send the payload. We update the retries
							if (debugMode)
								System.err
								        .println("rmsdataexchange Module: Queue Processing: Failed to send patient to Wonder Health");
							item.setRetries(item.getRetries() + 1);
						} else {
							// Sent the payload. We remove the queue entry
							if (debugMode)
								System.out
								        .println("rmsdataexchange Module: Queue Processing: Finished sending patient to Wonder Health");
							item.setRetries(item.getRetries() + 1);
							item.setVoided(true);
							item.setVoidReason("Payload Sent");
						}
						modified = true;
					}
					
					if (modified) {
						rmsdataexchangeService.saveQueueItem(item);
					}
				}
			}
		} else {
			if (debugMode)
				System.err.println("rmsdataexchange module: Queue Processing: There are NO items in the queue. Exiting");
		}
	}
}
