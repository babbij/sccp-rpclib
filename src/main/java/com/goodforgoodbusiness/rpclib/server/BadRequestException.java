package com.goodforgoodbusiness.rpclib.server;

public class BadRequestException extends Exception {
	public BadRequestException(String message) {
		super(message);
	}

	public BadRequestException(String message, Throwable cause) {
		 super(message, cause);
	}
}
