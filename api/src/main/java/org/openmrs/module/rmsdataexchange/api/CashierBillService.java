package org.openmrs.module.rmsdataexchange.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.springframework.transaction.annotation.Transactional;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import java.util.Set;

public interface CashierBillService extends IBillService {
	
	@Transactional(readOnly = true)
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	Set<Payment> getPaymentsByBillId(Integer billId);
	
}
