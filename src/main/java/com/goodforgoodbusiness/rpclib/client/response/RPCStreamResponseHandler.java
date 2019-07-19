package com.goodforgoodbusiness.rpclib.client.response;

import static java.util.stream.StreamSupport.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import com.goodforgoodbusiness.rpclib.client.RPCClientException;
import com.goodforgoodbusiness.rpclib.stream.ReadStreamToInputStream;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.reactivestreams.ReactiveReadStream;
import io.vertx.ext.reactivestreams.ReactiveWriteStream;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.codec.BodyCodec;

/**
 * Translates what comes back from an RPC call in to a stream of response objects.
 * NOTE: this should not be used from the Vert.x event loop thread.
 */
public class RPCStreamResponseHandler<T extends Message> implements RPCResponseHandler<Void> {
	private final Class<T> clazz;
	private final Future<Stream<T>> future;

	private final WriteStream<Buffer> writeStream;
	private final ReadStream<Buffer> readStream;
	
	private final InputStream inputStream;
	
	public RPCStreamResponseHandler(Vertx vertx, Class<T> clazz, Future<Stream<T>> future) {
		this.clazz = clazz;
		this.future = future;
		
		this.writeStream = ReactiveWriteStream.writeStream(vertx);
		this.readStream = ReactiveReadStream.readStream();
		this.inputStream = new ReadStreamToInputStream(readStream);
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
				// start a pump
				var pump = Pump.pump(readStream, writeStream);
				pump.start();
				
				Iterable<T> iter = (() -> new Iterator<>() {
					@Override
					public boolean hasNext() {
						try {
							return inputStream.read() >= 0;
						}
						catch (IOException e) {
							return false;
						}
					}

					@Override
					public T next() {
						try {
							var obj = Any.parseDelimitedFrom(inputStream);
							return obj.unpack(clazz);
						} 
						catch (IOException e) {
							throw new NoSuchElementException();
						}
					}
				});
				
				future.complete(
					stream(iter.spliterator(), false)
				);
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
