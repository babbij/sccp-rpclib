package com.colabriq.rpclib.client;

public class RPCClientException extends Exception {
	public RPCClientException(String message) {
		super(message);
		fillInStackTrace();
	}
	
	public RPCClientException(String message, Throwable cause) {
		super(message, cause);
		fillInStackTrace();
	}

	public RPCClientException(Throwable cause) {
		super(cause);
		fillInStackTrace();
	}
}
