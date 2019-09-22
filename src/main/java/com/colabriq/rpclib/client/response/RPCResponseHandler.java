package com.colabriq.rpclib.client.response;

import java.io.InputStream;

public interface RPCResponseHandler {	
	/**
	 * Handle a success response from the remote server
	 */
	public void success(InputStream is);
	
	/**
	 * Handle a failure response from the remote server
	 */
	public void failure(Exception e);
	
}
