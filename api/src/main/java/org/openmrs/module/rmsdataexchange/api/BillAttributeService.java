package org.openmrs.module.rmsdataexchange.api;

import org.openmrs.module.rmsdataexchange.queue.model.BillAttribute;
import org.openmrs.module.rmsdataexchange.queue.model.BillAttributeType;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface BillAttributeService {
	
	// CRUD Operations
	BillAttribute saveBillAttribute(BillAttribute billAttribute);
	
	BillAttribute getBillAttribute(Integer billAttributeId);
	
	void deleteBillAttribute(BillAttribute billAttribute);
	
	// Query Operations
	List<BillAttribute> getBillAttributesByBillId(Integer billId);
	
	List<BillAttribute> getBillAttributesByBillUuid(String billUuid);
	
	List<BillAttribute> getBillAttributesByTypeId(Integer billAttributeTypeId);
	
	List<BillAttribute> getAllBillAttributes(Boolean includeVoided);
	
	// Type Operations
	BillAttributeType saveBillAttributeType(BillAttributeType billAttributeType);
	
	BillAttributeType getBillAttributeType(Integer billAttributeTypeId);
	
	List<BillAttributeType> getAllBillAttributeTypes(Boolean includeRetired);
	
	// Utility Operations
	void voidBillAttribute(BillAttribute billAttribute, String reason, Integer voidedBy);
	
	void unvoidBillAttribute(BillAttribute billAttribute);
	
	void retireBillAttributeType(BillAttributeType billAttributeType, String reason, Integer retiredBy);
	
	void unretireBillAttributeType(BillAttributeType billAttributeType);
}
