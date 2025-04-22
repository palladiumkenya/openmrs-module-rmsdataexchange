package org.openmrs.module.rmsdataexchange.task;

import java.util.List;

import org.openmrs.api.context.Context;
import org.openmrs.module.rmsdataexchange.advice.NewPatientRegistrationSyncToWonderHealth;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeService;
import org.openmrs.module.rmsdataexchange.api.util.AdviceUtils;
import org.openmrs.module.rmsdataexchange.api.util.RMSModuleConstants;
import org.openmrs.module.rmsdataexchange.queue.model.RmsQueue;
import org.openmrs.module.rmsdataexchange.queue.model.RmsQueueSystem;
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
		List<RmsQueue> rmsQueueItems = rmsdataexchangeService.getQueueItems();
		if(rmsQueueItems != null && rmsQueueItems.size() > 0) {
			if (debugMode)
				System.err.println("rmsdataexchange module: Queue Processing: There are some items in the queue");
			for(RmsQueue item : rmsQueueItems) {
				// Get the payload
				String payload = item.getPayload();
				// Get the system used
				RmsQueueSystem system = item.getSystem();
				if(system.getUuid() == RMSModuleConstants.RMS_SYSTEM_PATIENT) {
					// This is a payload for RMS system Patient
				} else if(system.getUuid() == RMSModuleConstants.RMS_SYSTEM_BILL) {
						// This is a payload for RMS system Bill
				} else if(system.getUuid() == RMSModuleConstants.RMS_SYSTEM_PAYMENT) {
						// This is a payload for RMS system Bill Payment
				} else if(system.getUuid() == RMSModuleConstants.WONDER_HEALTH_SYSTEM_PATIENT) {
					// This is a payload for Wonder Health system Patient
					Integer sleepTime = AdviceUtils.getRandomInt(5000, 10000);
					// Delay
					try {
						//Delay for random seconds
						if (debugMode)
							System.out.println("rmsdataexchange Module: Queue Processing: Sleep for milliseconds: " + sleepTime);
						Thread.sleep(sleepTime);
					}
					catch (Exception ie) {
						Thread.currentThread().interrupt();
					}
					
					NewPatientRegistrationSyncToWonderHealth newPatientRegistrationSyncToWonderHealth = new NewPatientRegistrationSyncToWonderHealth();
					Boolean sendWonderHealthResult = newPatientRegistrationSyncToWonderHealth.sendWonderHealthPatientRegistration(payload);
					if (sendWonderHealthResult == false) {
						// Failed to send the payload. We put it in the queue
						if (debugMode)
							System.err
									.println("rmsdataexchange Module: Queue Processing: Failed to send patient to Wonder Health");
					} else {
						if (debugMode)
							System.out.println("rmsdataexchange Module: Queue Processing: Finished sending patient to Wonder Health");
					}
				}
			}
		} else {
			if (debugMode)
				System.err.println("rmsdataexchange module: Queue Processing: There are NO items in the queue. Exiting");
		}
	}
}
