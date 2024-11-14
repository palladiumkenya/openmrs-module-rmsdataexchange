package org.openmrs.module.rmsdataexchange.advice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.validation.constraints.NotNull;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.rmsdataexchange.api.CashierBillService;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeService;
import org.openmrs.module.rmsdataexchange.api.util.AdviceUtils;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.openmrs.module.rmsdataexchange.api.util.SimpleObject;
import org.openmrs.module.rmsdataexchange.api.util.WarningThrowable;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.BeforeAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.openmrs.module.rmsdataexchange.api.CashierBillService;

public class PaymentInterceptor implements MethodBeforeAdvice {
	
	private Boolean debugMode = false;
	
	private CashierBillService billService;
	
	public CashierBillService getBillService() {
		return billService;
	}
	
	public void setBillService(CashierBillService billService) {
		this.billService = billService;
	}
	
	@Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		
		debugMode = AdviceUtils.isRMSLoggingEnabled();
		if (AdviceUtils.isRMSIntegrationEnabled()) {
			// Check if the method name is "save" and there is at least one argument
			if (method.getName().equals("save") && args.length > 0 && args[0] instanceof Bill) {
				Bill bill = (Bill) args[0];
				
				if (bill == null) {
					return;
				}
				
				if (debugMode)
					System.out.println("rmsdataexchange Module: Intercepted save call for Bill: " + bill.getUuid());
				
				try {
					// billService.save(bill);
					if (billService == null) {
						billService = Context.getService(CashierBillService.class);
					}
					if (billService != null) {
						Integer billID = bill.getId();
						if (debugMode)
							System.out.println("rmsdataexchange Module: Bill ID is: " + billID);
						Set<Payment> paymentsBefore = billService.getPaymentsByBillId(billID);
						Set<Payment> paymentsAfter = bill.getPayments();
						if (debugMode)
							System.out.println("rmsdataexchange Module: Payments before: " + paymentsBefore.size());
						for (Payment payment : paymentsBefore) {
							if (debugMode)
							System.out.println("rmsdataexchange Module: Payment before tendered Each: "
							        + payment.getAmountTendered());
						}
						if (debugMode)
							System.out.println("rmsdataexchange Module: Payments after: " + paymentsAfter.size());
						for (Payment payment : paymentsAfter) {
							if (debugMode)
							System.out.println("rmsdataexchange Module: Payment after tendered Each: "
							        + payment.getAmountTendered());
						}
						// Prevent the save bill from proceeding
						throw new WarningThrowable("rmsdataexchange Module: Redirecting to check if there are payments");
					} else {
						if (debugMode)
							System.out.println("rmsdataexchange Module: Payments Error: failed to load bill service");
					}
				}
				catch (Exception ex) {
					if (debugMode)
						System.out.println("rmsdataexchange Module: Payments Error: " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		}
	}
}
