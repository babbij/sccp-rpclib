package com.goodforgoodbusiness.rpclib.server.receiver;

import java.io.OutputStream;

import com.goodforgoodbusiness.rpclib.server.RPCExecutionException;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

public interface RPCReceiver {
	/**
	 * Try to parse incoming {@link Message} as {@link Any}, to type this {@link RPCReceiverSingleResponse} wants.
	 */
	public boolean test(Any any);
	
	/** 
	 * Do the work of the RPC call, against the object produced by {@link #tryHandle(Any)}.
	 */
	public void exec(Any any, OutputStream os) throws RPCExecutionException;
}
