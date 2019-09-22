package com.colabriq.rpclib.client.response;

import java.io.IOException;
import java.io.InputStream;

import com.colabriq.rpclib.client.RPCClientException;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import io.vertx.core.Future;

/**
 * Translates what comes back from an RPC call in to a single response.
 */
public class RPCSingleResponseHandler<T extends Message> implements RPCResponseHandler {
	private final Class<T> clazz;
	private final Future<RPCResponse<T>> future;

	public RPCSingleResponseHandler(Class<T> clazz, Future<RPCResponse<T>> future) {
		this.clazz = clazz;
		this.future = future;
	}
	
	@Override
	public void failure(Exception e) {
		future.fail(e);
	}
	
	@Override
	public void success(InputStream is) {
		// use the supplier encapsulation to get work done in the original caller thread
		future.complete(() -> {
			try {
				var obj = Any.parseDelimitedFrom(is);
				if (obj.is(clazz)) {
					return obj.unpack(clazz);
				}
				else {
					throw new RPCClientException("Unexpected class returned: " + clazz.getName());
				}
			}
			catch (IOException e) {
				throw new RPCClientException(e);
			}
			finally {
				try {
					is.close();
				}
				catch (IOException e) {
					// do nothing
				}
			}
		});
	}
}
