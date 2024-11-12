package org.openmrs.module.rmsdataexchange.api.impl;

import org.openmrs.annotation.Authorized;
import org.openmrs.module.kenyaemr.cashier.api.impl.BillServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.rmsdataexchange.api.CashierBillService;
import org.openmrs.module.rmsdataexchange.api.dao.RmsdataexchangeDao;
import org.springframework.transaction.annotation.Transactional;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import java.util.Set;

public class CashierBillServiceImpl extends BillServiceImpl implements CashierBillService {
	
	RmsdataexchangeDao dao;
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setDao(RmsdataexchangeDao dao) {
		this.dao = dao;
	}
	
	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	@Transactional(readOnly = true)
	public Set<Payment> getPaymentsByBillId(Integer billId) {
		Set<Payment> payments = dao.getPaymentsByBillId(billId);
		return payments;
	}
}
