package com.goodforgoodbusiness.rpclib.client.response;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.goodforgoodbusiness.rpclib.client.RPCClientException;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

/**
 * Pulls messages of the specified type off of an InputStream
 */
class RPCStreamIterator<T extends Message> implements Iterator<RPCResponse<T>>, AutoCloseable {
	private final Logger log = Logger.getLogger(RPCStreamIterator.class);
	
	private final InputStream inputStream;
	private final Class<T> clazz;

	RPCStreamIterator(Class<T> clazz, InputStream is) {
		this.clazz = clazz;
		this.inputStream = new BufferedInputStream(is);
	}
	
	@Override
	public boolean hasNext() {
		inputStream.mark(1);
		
		try {
			try {
				var result = inputStream.read() >= 0;
				if (!result) {
					close();
				}
				
				return result;
			}
			finally {
				inputStream.reset();
			}
		}
		catch (IOException e) {
			close();
			return false;
		}
	}

	@Override
	public RPCResponse<T> next() {
		if (hasNext()) {
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
					hasNext(); // will close the inputStream if no further elements
				}
			};
		}
		else {
			throw new NoSuchElementException();
		}
	}

	@Override
	public void close() {
		try {
			inputStream.close();
		}
		catch (IOException e) {
			log.error("Could not close InputStream", e);
		}
	}
}
