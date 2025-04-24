package org.openmrs.module.rmsdataexchange.api;

import org.openmrs.module.rmsdataexchange.queue.model.PaymentAttribute;
import org.openmrs.module.rmsdataexchange.queue.model.PaymentAttributeType;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface PaymentAttributeService {
	
	// CRUD Operations
	PaymentAttribute savePaymentAttribute(PaymentAttribute paymentAttribute);
	
	PaymentAttribute getPaymentAttribute(Integer paymentAttributeId);
	
	void deletePaymentAttribute(PaymentAttribute paymentAttribute);
	
	List<PaymentAttribute> getPaymentAttributesByPaymentUuid(String paymentUuid);
	
	// Query Operations
	List<PaymentAttribute> getPaymentAttributesByPaymentId(Integer paymentId);
	
	List<PaymentAttribute> getPaymentAttributesByTypeId(Integer paymentAttributeTypeId);
	
	List<PaymentAttribute> getAllPaymentAttributes(Boolean includeVoided);
	
	// Type Operations
	PaymentAttributeType savePaymentAttributeType(PaymentAttributeType paymentAttributeType);
	
	PaymentAttributeType getPaymentAttributeType(Integer paymentAttributeTypeId);
	
	List<PaymentAttributeType> getAllPaymentAttributeTypes(Boolean includeRetired);
	
	// Utility Operations
	void voidPaymentAttribute(PaymentAttribute paymentAttribute, String reason, Integer voidedBy);
	
	void unvoidPaymentAttribute(PaymentAttribute paymentAttribute);
	
	void retirePaymentAttributeType(PaymentAttributeType paymentAttributeType, String reason, Integer retiredBy);
	
	void unretirePaymentAttributeType(PaymentAttributeType paymentAttributeType);
	
}
