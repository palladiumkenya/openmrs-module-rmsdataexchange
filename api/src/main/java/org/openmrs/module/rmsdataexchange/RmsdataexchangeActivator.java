/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.rmsdataexchange;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.BaseModuleActivator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
public class RmsdataexchangeActivator extends BaseModuleActivator {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	/**
	 * @see #started()
	 */
	public void started() {
		log.info("Started rmsdataexchange Module");
		System.err.println("rmsdataexchange Module Started: " + printCurrentDateTime());
	}
	
	/**
	 * @see #shutdown()
	 */
	public void shutdown() {
		log.info("Shutdown smsdataexchange Module");
		System.err.println("rmsdataexchange Module Shutdown: " + printCurrentDateTime());
	}
	
	@Override
	public void stopped() {
		System.err.println("rmsdataexchange Module stopped: " + printCurrentDateTime());
	}
	
	@Override
	public void willRefreshContext() {
		System.err.println("rmsdataexchange Module refreshing context: " + printCurrentDateTime());
	}
	
	@Override
	public void willStart() {
		System.err.println("rmsdataexchange Module starting: " + printCurrentDateTime());
	}
	
	@Override
	public void willStop() {
		System.err.println("rmsdataexchange Module stopping: " + printCurrentDateTime());
	}
	
	@Override
	public void contextRefreshed() {
		System.err.println("rmsdataexchange finished refreshing context: " + printCurrentDateTime());
	}
	
	public static String printCurrentDateTime() {
		// Get the current date and time
		LocalDateTime currentDateTime = LocalDateTime.now();
		
		// Format the date and time for better readability
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		String formattedDateTime = currentDateTime.format(formatter);
		
		// Print the formatted current date and time to the console
		// System.out.println("Current Date and Time: " + formattedDateTime);
		
		return (formattedDateTime);
	}
	
}
