package com.goodforgoodbusiness.rpclib.client.response;

import static java.util.stream.StreamSupport.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.stream.Stream;

import com.goodforgoodbusiness.rpclib.client.RPCClientException;
import com.goodforgoodbusiness.rpclib.stream.InputWriteStream;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.codec.BodyCodec;

/**
 * Translates what comes back from an RPC call in to a stream of response objects.
 * NOTE: this should not be used from the Vert.x event loop thread.
 */
public class RPCStreamResponseHandler<T extends Message> implements RPCResponseHandler {
	private final Class<T> clazz;
	private final Future<Stream<RPCResponse<T>>> future;

	private final InputWriteStream writeStream;
	private final InputStream inputStream;
	
	public RPCStreamResponseHandler(Vertx vertx, Class<T> clazz, Future<Stream<RPCResponse<T>>> future) {
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
				// build an iterator that pulls Message one by one from an iterator
				var iterator = new Iterator<RPCResponse<T>>() {
					@Override
					public boolean hasNext() {
						try {
							return inputStream.available() > 0;
						} 
						catch (IOException e) {
							return false;
						}
					}

					@Override
					public RPCResponse<T> next() {
						return () -> {
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
									if (inputStream.available() < 0) {
										inputStream.close();
									}
								}
								catch (IOException e) {
									// do nothing
								}
							}
						};
					}
				};
				
				Iterable<RPCResponse<T>> iterable = () -> iterator;
				future.complete(stream(iterable.spliterator(), false));
			}
			else {
				var e = new RPCClientException("RPC returned " + result.result().statusCode());
				e.fillInStackTrace();
				future.fail(e);
			}
		}
		else {
			future.fail(result.cause());
		}
	}
}
