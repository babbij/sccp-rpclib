package com.goodforgoodbusiness.rpclib.client.response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.goodforgoodbusiness.rpclib.client.RPCClientException;
import com.goodforgoodbusiness.rpclib.stream.ReadStreamToInputStream;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.reactivestreams.ReactiveReadStream;
import io.vertx.ext.reactivestreams.ReactiveWriteStream;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.codec.BodyCodec;

/**
 * Translates what comes back from an RPC call in to a single response.
 */
public class RPCSingleResponseHandler<T extends Message> implements RPCResponseHandler<String> {
	private final Class<T> clazz;
	private final Future<T> future;
	
	private final WriteStream<Buffer> writeStream;
	private final ReadStream<Buffer> readStream;
	
	private final InputStream inputStream;

	public RPCSingleResponseHandler(Vertx vertx, Class<T> clazz, Future<T> future) {
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
	public BodyCodec<String> getBodyCodec() {
		return BodyCodec.string();
		
//		return BodyCodec.buffer();
	}
		
	@Override
	public void handle(AsyncResult<HttpResponse<String>> result) {
		if (result.succeeded()) {
			if (httpOK(result.result().statusCode())) {
				try {
					var bis = new ByteArrayInputStream(result.result().body().getBytes());
					var obj = Any.parseDelimitedFrom(bis);
					
					if (obj.is(clazz)) {
						future.complete(obj.unpack(clazz));
					}
					else {
						var e = new ClassCastException(clazz.getName());
						e.fillInStackTrace();
						future.fail(e);
					}
				}
				catch (IOException e) {
					future.fail(e);
				}
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
