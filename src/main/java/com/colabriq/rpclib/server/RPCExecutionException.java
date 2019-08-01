package com.colabriq.rpclib.server;

public class RPCExecutionException extends Exception {
	public RPCExecutionException(String message) {
		super(message);
	}

	public RPCExecutionException(String message, Throwable cause) {
		 super(message, cause);
	}
	
	public RPCExecutionException(Throwable cause) {
		 super(cause);
	}
}
