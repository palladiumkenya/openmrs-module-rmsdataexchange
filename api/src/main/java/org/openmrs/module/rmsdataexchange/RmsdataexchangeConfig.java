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
public class RmsdataexchangeConfig implements BeanPostProcessor {
	
	private Boolean processedBean = false;
	
	public final static String MODULE_PRIVILEGE = "Rmsdataexchange Privilege";
	
	@Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		// We only register for one bean instance and make sure it is a transaction proxy NB: the name "cashierBillService" can change in future so it is not safe to rely on it
		if (bean instanceof IBillService && processedBean == false && bean instanceof TransactionalProxy) {
		//  if (bean instanceof IBillService && processedBean == false && beanName.equalsIgnoreCase("cashierBillService")) {
			System.out.println("rmsdataexchange Module: Registering Bean Proxy for: " + beanName);
			processedBean = true;
			printAllSuperClassesAndInterfaces(bean.getClass());
            return Proxy.newProxyInstance(
                bean.getClass().getClassLoader(),
                bean.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    if ("save".equals(method.getName()) && args != null && args.length > 0 && args[0] instanceof Bill) {
						Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
                        if (debugMode) System.out.println("rmsdataexchange Module: Intercepting IBillService save Bill call - Called by: " + Thread.currentThread().getStackTrace()[2]);

						Bill bill = (Bill) args[0];
				
						if (bill == null) {
							if (debugMode) System.out.println("rmsdataexchange Module: Bill is null. No need to continue");
							return method.invoke(bean, args);
						}

						if (debugMode) System.out.println("rmsdataexchange Module: Got the Bill UUID: " + bill.getUuid());

						RmsdataexchangeService billService = Context.getService(RmsdataexchangeService.class);
						Set<Payment> paymentsBefore = billService.getPaymentsByBillId(bill.getId());
						if (debugMode) System.out.println("rmsdataexchange Module: Payments before: " + paymentsBefore.size());

						for(Payment payment : paymentsBefore) {
							if (debugMode) System.out.println("rmsdataexchange Module: One payment before: " + payment.getAmountTendered());
						}

                        // Invoke the original save method
                        Object result = method.invoke(bean, args);

                        if(result != null && result instanceof Bill) {
							Bill newBill = (Bill) result;
							if (newBill != null) {
								Set<Payment> paymentsAfter = newBill.getPayments();
								if (debugMode) System.out.println("rmsdataexchange Module: Payments after: " + paymentsAfter.size());

								for(Payment payment : paymentsAfter) {
									if (debugMode) System.out.println("rmsdataexchange Module: One payment after: " + payment.getAmountTendered());
								}

								if (debugMode) System.out.println("rmsdataexchange Module: Checking if there is need to Send payments to RMS");
								NewBillPaymentSyncToRMS.checkPaymentsAndSendToRMS(paymentsBefore, paymentsAfter);
							}
						}

                        return result;
                    }
                    return method.invoke(bean, args);
                }
            );
        }
        return bean;
    }
	
	public static void printAllSuperClassesAndInterfaces(Class<?> clazz) {
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
		
		if (debugMode)
			System.out.println("rmsdataexchange Module: Class hierarchy for: " + clazz.getName());
		
		// Print all superclasses
		Class<?> currentClass = clazz;
		while (currentClass != null) {
			if (debugMode)
				System.out.println("rmsdataexchange Module: Superclass: " + currentClass.getName());
			currentClass = currentClass.getSuperclass();
		}
		
		// Print all interfaces
		currentClass = clazz;
		while (currentClass != null) {
			Class<?>[] interfaces = currentClass.getInterfaces();
			for (Class<?> iface : interfaces) {
				if (debugMode)
					System.out.println("rmsdataexchange Module: Interface: " + iface.getName());
			}
			currentClass = currentClass.getSuperclass();
		}
	}
	
}
