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

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeService;
import org.openmrs.module.rmsdataexchange.api.util.AdviceUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.interceptor.TransactionalProxy;
import org.openmrs.module.rmsdataexchange.advice.NewBillPaymentSyncToRMS;

/**
 * Contains module's config.
 */
@Component("rmsdataexchange.RmsdataexchangeConfig")
public class RmsdataexchangeConfig {
	
	public final static String MODULE_PRIVILEGE = "Rmsdataexchange Privilege";
	
}
