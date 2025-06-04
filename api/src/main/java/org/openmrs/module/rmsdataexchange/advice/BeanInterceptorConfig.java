package org.openmrs.module.rmsdataexchange.advice;

import java.lang.reflect.Proxy;
import java.util.Set;

import org.hibernate.Hibernate;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.rmsdataexchange.RmsdataexchangeActivator;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeService;
import org.openmrs.module.rmsdataexchange.api.util.AdviceUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class BeanInterceptorConfig implements BeanPostProcessor {
	
	// private Boolean processedBean = false;
	
	@Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		// System.out.println("rmsdataexchange Module: Registering Bean : " + beanName + " of class: " + bean.getClass().getName());
		// We only register for one bean instance and make sure it is a transaction proxy NB: the name "cashierBillService" can change in future so it is not safe to rely on it
		// if (bean instanceof IBillService && processedBean == false && bean instanceof TransactionalProxy) {
		//  if (bean instanceof IBillService && processedBean == false && beanName.equalsIgnoreCase("cashierBillService")) {
		if (bean instanceof IBillService && beanName.equalsIgnoreCase("cashierBillService")) {
			System.out.println("rmsdataexchange Module: Registering Bean Proxy for: " + beanName);
			// processedBean = true;
			// printAllSuperClassesAndInterfaces(bean.getClass());
            return Proxy.newProxyInstance(
                bean.getClass().getClassLoader(),
                bean.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    if ("save".equals(method.getName()) && args != null && args.length > 0 && args[0] instanceof Bill) {
						try {
							Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();
							if (debugMode) System.out.println("rmsdataexchange Module: Intercepting IBillService save Bill call - Called by: " + Thread.currentThread().getStackTrace()[2]);

							Bill bill = (Bill) args[0];
					
							if (bill == null) {
								if (debugMode) System.out.println("rmsdataexchange Module: Bill is null. No need to continue");
								return method.invoke(bean, args);
							}

							if (debugMode) System.out.println("rmsdataexchange Module: Got the Bill UUID: " + bill.getUuid());
							Hibernate.initialize(bill.getPatient().getIdentifiers());
							Integer ids = bill.getPatient().getIdentifiers().size();
							if (debugMode)
								System.out.println("rmsdataexchange Module: patient identifiers: " + ids);

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
									System.err.println("rmsdataexchange Module: Got daemon token as: " + RmsdataexchangeActivator.getDaemonToken());
									NewBillPaymentSyncToRMS.checkPaymentsAndSendToRMS(paymentsBefore, paymentsAfter);
								}
							}

							return result;
						} catch(Exception ex) {
							Boolean debugMode = false;
							try { debugMode = AdviceUtils.isRMSLoggingEnabled(); } catch(Exception m) {}
							if (debugMode) System.err.println("rmsdataexchange Module: Error sending RMS Bill: " + ex.getMessage());
							ex.printStackTrace();
						}
                    }
                    return method.invoke(bean, args);
                }
            );
        }
        return bean;
    }
	
	/**
	 * Print beans as they are being registered during startup
	 * 
	 * @param clazz
	 */
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
