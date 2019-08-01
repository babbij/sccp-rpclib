package com.goodforgoodbusiness.rpclib.server.receiver;

import java.io.IOException;
import java.io.OutputStream;

import com.goodforgoodbusiness.rpclib.server.RPCExecutionException;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

/**
 * Interface to determine how the actual RPC calls each operate
 */
public class RPCReceiverSingleResponse<T extends Message, U extends Message> implements RPCReceiver {		
	private final Class<T> type;
	private final RPCSingleExecutor<T, U> func;
	
	public RPCReceiverSingleResponse(Class<T> type, RPCSingleExecutor<T, U> func) {
		this.type = type;
		this.func = func;
	}
	
	/**
	 * Try to parse incoming {@link Message} as {@link Any}, to type this {@link RPCReceiverSingleResponse} wants.
	 */
	@Override
	public boolean test(Any any) {
		return any.is(type);
	}
	
	/** 
	 * Do the work of the RPC call, against the object produced by {@link #tryHandle(Any)}.
	 */
	@Override
	public void exec(Any any, OutputStream os) throws RPCExecutionException {
		try {
			var obj = any.unpack(type);
			
			var res = func.exec(obj);
			Any.pack(res).writeDelimitedTo(os);
			
			os.flush();
		}
		catch (Exception e) {
			throw new RPCExecutionException(e);
		}
		finally {
			try {
				os.close();
			}
			catch (IOException e) {
				// don't return this one to client
			}
		}
	}
}
