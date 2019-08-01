package com.goodforgoodbusiness.rpclib.server.receiver;

import com.google.protobuf.Message;

/**
 * Represents the function to be performed to transform input to output.
 */
public interface RPCSingleExecutor<U extends Message, T extends Message> {
	public T exec(U u) throws Exception;
}
