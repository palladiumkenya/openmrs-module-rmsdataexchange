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
import org.openmrs.module.DaemonToken;
import org.openmrs.module.DaemonTokenAware;
import org.openmrs.module.rmsdataexchange.advice.BeanInterceptorConfig;
import org.openmrs.module.rmsdataexchange.api.util.AdviceUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
public class RmsdataexchangeActivator extends BaseModuleActivator implements DaemonTokenAware {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	private static DaemonToken daemonToken;
	
	/**
	 * @see #started()
	 */
	public void started() {
		log.info("Started rmsdataexchange Module");
		System.err.println("rmsdataexchange Module Started: " + AdviceUtils.printCurrentDateTime());
	}
	
	/**
	 * @see #shutdown()
	 */
	public void shutdown() {
		log.info("Shutdown rmsdataexchange Module");
		System.err.println("rmsdataexchange Module Shutdown: " + AdviceUtils.printCurrentDateTime());
	}
	
	@Override
	public void stopped() {
		System.err.println("rmsdataexchange Module stopped: " + AdviceUtils.printCurrentDateTime());
	}
	
	@Override
	public void willRefreshContext() {
		System.err.println("rmsdataexchange Module refreshing context: " + AdviceUtils.printCurrentDateTime());
	}
	
	@Override
	public void willStart() {
		System.err.println("rmsdataexchange Module starting: " + AdviceUtils.printCurrentDateTime());
	}
	
	@Override
	public void willStop() {
		System.err.println("rmsdataexchange Module stopping: " + AdviceUtils.printCurrentDateTime());
	}
	
	@Override
	public void contextRefreshed() {
		System.err.println("rmsdataexchange Module finished refreshing context: " + AdviceUtils.printCurrentDateTime());
	}
	
	@Override
	public void setDaemonToken(DaemonToken token) {
		RmsdataexchangeActivator.daemonToken = token;
		System.err.println("rmsdataexchange Module: Got daemon token as: " + token);
	}
	
	public static DaemonToken getDaemonToken() {
		return daemonToken;
	}
}
