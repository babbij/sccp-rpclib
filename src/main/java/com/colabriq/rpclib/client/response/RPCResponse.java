package com.colabriq.rpclib.client.response;

import com.colabriq.rpclib.client.RPCClientException;

/**
 * Returned by the RPC calls.
 * This allows us to hijack the original caller thread for some processing.
 */
public interface RPCResponse<T> {
	public T get() throws RPCClientException;
}
