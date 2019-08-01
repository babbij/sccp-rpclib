package com.goodforgoodbusiness.rpclib.client;

public class RPCClientException extends Exception {
	public RPCClientException(String message) {
		super(message);
	}
	
	public RPCClientException(String message, Throwable cause) {
		super(message, cause);
	}

	public RPCClientException(Throwable cause) {
		super(cause);
	}
}
