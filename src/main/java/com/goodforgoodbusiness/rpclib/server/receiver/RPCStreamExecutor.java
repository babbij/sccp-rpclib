package com.goodforgoodbusiness.rpclib.server.receiver;

import java.util.stream.Stream;

import com.google.protobuf.Message;

/**
 * Represents the function to be performed to transform input to output.
 */
public interface RPCStreamExecutor<U extends Message, T extends Message> {
	public Stream<T> exec(U u) throws Exception;
}
