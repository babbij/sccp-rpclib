package com.colabriq.rpclib.client.response;

import java.io.IOException;
import java.io.InputStream;

import com.colabriq.rpclib.client.RPCClientException;
import com.colabriq.vertx.stream.InputWriteStream;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.codec.BodyCodec;

/**
 * Translates what comes back from an RPC call in to a single response.
 */
public class RPCSingleResponseHandler<T extends Message> implements RPCResponseHandler {
	private final Class<T> clazz;
	private final Future<RPCResponse<T>> future;
	
	private final InputWriteStream writeStream;
	private final InputStream inputStream;

	public RPCSingleResponseHandler(Vertx vertx, Class<T> clazz, Future<RPCResponse<T>> future) {
		this.clazz = clazz;
		this.future = future;
		
		InputWriteStream iws = null;
		
		try {
			iws = new InputWriteStream();
		}
		catch (IOException e) {
			future.fail(e);
		}
		
		if (iws != null) {
			this.writeStream = iws;
			this.inputStream = writeStream.getInputStream();
		}
		else {
			this.writeStream = null;
			this.inputStream = null;
		}
	}
	
	@Override
	public void preFail(Exception e) {
		future.fail(e);
	}
	
	@Override
	public BodyCodec<Void> getBodyCodec() {
		return BodyCodec.pipe(writeStream);
	}
	
	@Override
	public void handle(AsyncResult<HttpResponse<Void>> result) {
		if (result.succeeded()) {
			if (httpOK(result.result().statusCode())) {
				// use the supplier encapsulation to get work done in the original caller thread
				future.complete(() -> {
					try {
						var obj = Any.parseDelimitedFrom(inputStream);
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
							inputStream.close();
						}
						catch (IOException e) {
							// do nothing
						}
					}
				});
			}
			else {
				var e = new RPCClientException("RPC returned " + result.result().statusCode());
				e.fillInStackTrace();
				future.fail(e);
				
				try {
					inputStream.close();
				}
				catch (IOException e_) {
					// do nothing
				}
			}
		}
		else {
			future.fail(result.cause());

			try {
				inputStream.close();
			}
			catch (IOException e_) {
				// do nothing
			}
		}
	}
}
