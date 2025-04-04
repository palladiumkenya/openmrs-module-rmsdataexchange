package org.openmrs.module.rmsdataexchange.api.impl;

import org.openmrs.module.rmsdataexchange.api.RMSPaymentAttributeService;
import org.openmrs.module.rmsdataexchange.api.RmsdataexchangeDao;
import org.hibernate.SessionFactory;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.rmsdataexchange.queue.model.RMSPaymentAttribute;
import org.openmrs.module.rmsdataexchange.queue.model.RMSPaymentAttributeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.openmrs.api.impl.BaseOpenmrsService;

import java.util.Date;
import java.util.List;

public class RMSPaymentAttributeServiceImpl extends BaseOpenmrsService implements RMSPaymentAttributeService {
	
	RmsdataexchangeDao dao;
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setDao(RmsdataexchangeDao dao) {
		this.dao = dao;
	}
	
	@Override
	public RMSPaymentAttribute savePaymentAttribute(RMSPaymentAttribute paymentAttribute) {
		return dao.savePaymentAttribute(paymentAttribute);
	}
	
	@Override
	public RMSPaymentAttribute getPaymentAttribute(Integer paymentAttributeId) {
		return dao.getPaymentAttribute(paymentAttributeId);
	}
	
	@Override
	public void deletePaymentAttribute(RMSPaymentAttribute paymentAttribute) {
		dao.deletePaymentAttribute(paymentAttribute);
	}
	
	@Override
	public List<RMSPaymentAttribute> getPaymentAttributesByPaymentId(Integer paymentId) {
		return dao.getPaymentAttributesByPaymentId(paymentId);
	}
	
	@Override
	public List<RMSPaymentAttribute> getPaymentAttributesByTypeId(Integer paymentAttributeTypeId) {
		return dao.getPaymentAttributesByTypeId(paymentAttributeTypeId);
	}
	
	@Override
	public List<RMSPaymentAttribute> getAllPaymentAttributes(Boolean includeVoided) {
		return dao.getAllPaymentAttributes(includeVoided);
	}
	
	@Override
	public List<RMSPaymentAttribute> getAllPaymentAttributesByPaymentId(Integer paymentId, Boolean includeVoided) {
		return dao.getAllPaymentAttributesByPaymentId(paymentId, includeVoided);
	}
	
	@Override
	public RMSPaymentAttributeType savePaymentAttributeType(RMSPaymentAttributeType paymentAttributeType) {
		return dao.savePaymentAttributeType(paymentAttributeType);
	}
	
	@Override
	public RMSPaymentAttributeType getPaymentAttributeType(Integer paymentAttributeTypeId) {
		return dao.getPaymentAttributeType(paymentAttributeTypeId);
	}
	
	@Override
	public RMSPaymentAttributeType getPaymentAttributeTypeByUuid(String typeUuid) {
		return dao.getPaymentAttributeTypeByUuid(typeUuid);
	}
	
	@Override
	public List<RMSPaymentAttributeType> getAllPaymentAttributeTypes(Boolean includeRetired) {
		return dao.getAllPaymentAttributeTypes(includeRetired);
	}
	
	@Override
	public void voidPaymentAttribute(RMSPaymentAttribute paymentAttribute, String reason, User voidedBy) {
		dao.voidPaymentAttribute(paymentAttribute, reason, voidedBy);
	}
	
	@Override
	public void unvoidPaymentAttribute(RMSPaymentAttribute paymentAttribute) {
		dao.unvoidPaymentAttribute(paymentAttribute);
	}
	
	@Override
	public void retirePaymentAttributeType(RMSPaymentAttributeType paymentAttributeType, String reason, User retiredBy) {
		dao.retirePaymentAttributeType(paymentAttributeType, reason, retiredBy);
	}
	
	@Override
	public void unretirePaymentAttributeType(RMSPaymentAttributeType paymentAttributeType) {
		dao.unretirePaymentAttributeType(paymentAttributeType);
	}
	
	@Override
	public List<RMSPaymentAttribute> getPaymentAttributesByPaymentUuid(String paymentUuid) {
		return dao.getPaymentAttributesByPaymentUuid(paymentUuid);
	}
}
