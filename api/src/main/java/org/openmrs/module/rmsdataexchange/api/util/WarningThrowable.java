package org.openmrs.module.rmsdataexchange.api.util;

public class WarningThrowable extends Throwable {
	
	public WarningThrowable(String message) {
		super(message);
	}
	
	@Override
	public synchronized Throwable fillInStackTrace() {
		// Prevent stack trace from being populated to make it lightweight
		return this;
	}
}
