package com.colabriq.rpclib.client.response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.codec.BodyCodec;

public interface RPCResponseHandler extends Handler<AsyncResult<HttpResponse<Void>>> {
	/**
	 * Check if an HTTP response is 2xx
	 */
	public default boolean httpOK(int status) {
		return status >= 200 && status < 300;
	}
	
	/**
	 * Handle an error before the response is actually handled.
	 */
	public void preFail(Exception e);
	
	/**
	 * Return the {@link BodyCodec} to use for the handle method.
	 */
	public BodyCodec<Void> getBodyCodec();
	
	/**
	 * Handle a response from the server (redeclared from Handler)
	 */
	@Override
	public void handle(AsyncResult<HttpResponse<Void>> event);
}
