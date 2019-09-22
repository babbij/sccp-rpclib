package com.colabriq.rpclib.client.response;

import static java.util.stream.StreamSupport.stream;

import java.io.InputStream;
import java.util.stream.Stream;

import com.google.protobuf.Message;

import io.vertx.core.Future;

/**
 * Translates what comes back from an RPC call in to a stream of response objects.
 * NOTE: this should not be used from the Vert.x event loop thread.
 */
public class RPCStreamResponseHandler<T extends Message> implements RPCResponseHandler {
	private final Class<T> clazz;
	private final Future<Stream<RPCResponse<T>>> future;
	
	public RPCStreamResponseHandler(Class<T> clazz, Future<Stream<RPCResponse<T>>> future) {
		this.clazz = clazz;
		this.future = future;
	}
	
	@Override
	public void success(InputStream is) {		
		// build an iterator that pulls Message one by one from an iterator
		var iterator = new RPCStreamIterator<>(clazz, is);
		Iterable<RPCResponse<T>> iterable = () -> iterator;
		
		var stream = stream(iterable.spliterator(), false);
		stream.onClose(() -> iterator.close());
		
		future.complete(stream);
	}
	
	@Override
	public void failure(Exception e) {
		future.fail(e);
	}
}
