package org.openmrs.module.rmsdataexchange.api.impl;

import org.hibernate.SessionFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.rmsdataexchange.api.BillAttributeService;
import org.openmrs.module.rmsdataexchange.queue.model.BillAttribute;
import org.openmrs.module.rmsdataexchange.queue.model.BillAttributeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service("billAttributeService")
public class BillAttributeServiceImpl implements BillAttributeService {
	
	private SessionFactory sessionFactory;
	
	@Autowired
	public BillAttributeServiceImpl(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public BillAttribute saveBillAttribute(BillAttribute billAttribute) {
		sessionFactory.getCurrentSession().saveOrUpdate(billAttribute);
		return billAttribute;
	}
	
	@Override
	public BillAttribute getBillAttribute(Integer billAttributeId) {
		return sessionFactory.getCurrentSession().get(BillAttribute.class, billAttributeId);
	}
	
	@Override
	public void deleteBillAttribute(BillAttribute billAttribute) {
		sessionFactory.getCurrentSession().delete(billAttribute);
	}
	
	@Override
	public List<BillAttribute> getBillAttributesByBillId(Integer billId) {
		return sessionFactory.getCurrentSession()
		        .createQuery("from CashierBillAttribute where billId = :billId", BillAttribute.class)
		        .setParameter("billId", billId).getResultList();
	}
	
	@Override
	public List<BillAttribute> getBillAttributesByBillUuid(String billUuid) {
		return sessionFactory
		        .getCurrentSession()
		        .createQuery(
		            "from CashierBillAttribute ba " + "join CashierBill b on ba.billId = b.billId "
		                    + "where b.uuid = :billUuid", BillAttribute.class).setParameter("billUuid", billUuid)
		        .getResultList();
	}
	
	@Override
	public List<BillAttribute> getBillAttributesByTypeId(Integer billAttributeTypeId) {
		return sessionFactory.getCurrentSession()
		        .createQuery("from CashierBillAttribute where paymentAttributeTypeId = :typeId", BillAttribute.class)
		        .setParameter("typeId", billAttributeTypeId).getResultList();
	}
	
	@Override
	public List<BillAttribute> getAllBillAttributes(Boolean includeVoided) {
		String query = "from CashierBillAttribute";
		if (!includeVoided) {
			query += " where voided = false";
		}
		return sessionFactory.getCurrentSession().createQuery(query, BillAttribute.class).getResultList();
	}
	
	@Override
	public BillAttributeType saveBillAttributeType(BillAttributeType billAttributeType) {
		sessionFactory.getCurrentSession().saveOrUpdate(billAttributeType);
		return billAttributeType;
	}
	
	@Override
	public BillAttributeType getBillAttributeType(Integer billAttributeTypeId) {
		return sessionFactory.getCurrentSession().get(BillAttributeType.class, billAttributeTypeId);
	}
	
	@Override
	public List<BillAttributeType> getAllBillAttributeTypes(Boolean includeRetired) {
		String query = "from CashierBillAttributeType";
		if (!includeRetired) {
			query += " where retired = false";
		}
		return sessionFactory.getCurrentSession().createQuery(query, BillAttributeType.class).getResultList();
	}
	
	@Override
	public void voidBillAttribute(BillAttribute billAttribute, String reason, Integer voidedBy) {
		billAttribute.setVoided(true);
		billAttribute.setVoidedBy(voidedBy);
		billAttribute.setDateVoided(new Date());
		billAttribute.setVoidReason(reason);
		saveBillAttribute(billAttribute);
	}
	
	@Override
	public void unvoidBillAttribute(BillAttribute billAttribute) {
		billAttribute.setVoided(false);
		billAttribute.setVoidedBy(null);
		billAttribute.setDateVoided(null);
		billAttribute.setVoidReason(null);
		saveBillAttribute(billAttribute);
	}
	
	@Override
	public void retireBillAttributeType(BillAttributeType billAttributeType, String reason, Integer retiredBy) {
		billAttributeType.setRetired(true);
		billAttributeType.setRetiredBy(retiredBy);
		billAttributeType.setDateRetired(new Date());
		billAttributeType.setRetireReason(reason);
		saveBillAttributeType(billAttributeType);
	}
	
	@Override
	public void unretireBillAttributeType(BillAttributeType billAttributeType) {
		billAttributeType.setRetired(false);
		billAttributeType.setRetiredBy(null);
		billAttributeType.setDateRetired(null);
		billAttributeType.setRetireReason(null);
		saveBillAttributeType(billAttributeType);
	}
}
