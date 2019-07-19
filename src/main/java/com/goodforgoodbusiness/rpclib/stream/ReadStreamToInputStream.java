package com.goodforgoodbusiness.rpclib.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

/**
 * Wraps Vert.x asynchronous input in an InputStream that can be consumed by other blocking ops
 */
public class ReadStreamToInputStream extends InputStream {
	private static interface Consumer {
		public int read() throws IOException;
		
		public default boolean isDone() {
			return false;
		}
	}
	
	private final AtomicInteger avail = new AtomicInteger(0);
    private final AtomicBoolean closed = new AtomicBoolean(false);
	
	private final BlockingQueue<Consumer> queue = new LinkedBlockingQueue<>();
	private Consumer current = null;
    
    public ReadStreamToInputStream(final ReadStream<Buffer> input) {
		input
            .handler(this::handleBuffer)
            .exceptionHandler(ex -> queue.add(() -> { throw new IOException(ex); }))
			.endHandler(none -> queue.add(() -> -1)) // return -1 forever
		;
    }
    
    /**
     * When a new buffer arrives, place that buffer on to the queue & consume
     */
    private void handleBuffer(Buffer buffer) {
    	if (buffer.length() == 0) {
    		return;
    	}
    	
    	avail.getAndAdd(buffer.length());
    	queue.add(new Consumer() {
           	private int pos = 0;
            		
			@Override
			public int read() {
				// to unsigned byte
				final int b = buffer.getByte(pos++) & 0xFF;
				avail.decrementAndGet();
				return b;
            }

			@Override
			public boolean isDone() {
				return pos == buffer.length();
			}
		});
    }

    @Override
    public int available() throws IOException {
        return avail.get();
    }
    
    @Override
    public void close() throws IOException {
        closed.set(true);
    }

    private void checkClosed() throws IOException {
    	if (closed.get()) {
            throw new IOException("InputReadStream is closed");
        }
    }

    @Override
    public int read() throws IOException {
    	checkClosed();
    	
    	// wait for new op on the queue
        while (current == null || current.isDone()) {
        	try {
        		current = queue.take();
	        } 
	        catch (InterruptedException e) {
	        	checkClosed(); // recheck
	        }
        }
    	
        if (current != null) {
        	return current.read();
        }
        else {
        	return -1;
        }
    }

    @Override
    public boolean markSupported() {
        return false;
    }
    
    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }
}