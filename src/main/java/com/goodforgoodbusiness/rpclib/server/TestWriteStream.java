package com.goodforgoodbusiness.rpclib.server;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

public class TestWriteStream implements WriteStream<Buffer> {
	private static class ReadingBuffer {
		private final Buffer buffer;
		private int pos = 0;
		
		private ReadingBuffer(Buffer buffer) {
			this.buffer = buffer;
		}
		
		private int read() {
			if (pos < buffer.length()) {
				return buffer.getByte(pos);
			}
			else {
				return -1;
			}
		}
		
		private boolean done() {
			return pos >= buffer.length();
		}
	}
	
	private final List<ReadingBuffer> incoming = new LinkedList<>();
	private final BlockingQueue<Object> unconsumed = new LinkedBlockingQueue<>();
	
	private boolean closed = false;
	
	private Handler<Throwable> exceptionHandler = null;
	private Handler<Void> drainHandler = null;

	@Override
	public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
		this.exceptionHandler = handler;
		return this;
	}
	
	@Override
	public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
		this.drainHandler = handler;
		
//   * Set a drain handler on the stream. If the write queue is full, then the handler will be called when the write
//   * queue is ready to accept buffers again. See {@link Pump} for an example of this being used.
//   * <p/>
//   * The stream implementation defines when the drain handler, for example it could be when the queue size has been
//   * reduced to {@code maxSize / 2}.
		
		return this;
	}
	
	@Override
	public boolean writeQueueFull() {
		return !unconsumed.isEmpty();
	}

	@Override
	public WriteStream<Buffer> write(Buffer data) {
		incoming.add(new ReadingBuffer(data));
		
		
		
		if (tryConsume()) {
			
		}
		
		currentBuffer.
		
		System.out.println("write");
		return this;
	}

	@Override
	public WriteStream<Buffer> write(Buffer data, Handler<AsyncResult<Void>> handler) {
		write(data);
		
		// generic void success
		handler.handle(new AsyncResult<Void> () {
			@Override public Void result() { return null; }
			@Override public Throwable cause() { return null; }
			@Override public boolean succeeded() { return true; }
			@Override public boolean failed() { return false; }
		});
		
		return this;
	}
	
	@Override
	public void end() {
		this.closed = true;
		this.unconsumed.add(null); 
	}
	
	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		end();
		
		// generic void success
		handler.handle(new AsyncResult<Void> () {
			@Override public Void result() { return null; }
			@Override public Throwable cause() { return null; }
			@Override public boolean succeeded() { return true; }
			@Override public boolean failed() { return false; }
		});
	}
	
	public Object consume() {
		Object next = null;
		
		while (!closed && (next == null)) {
			try {
				// probably switch to a poison pill approach here?
				next = unconsumed.poll(100, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				// continue loop
			}
		}
		
		if (next == null) {
			throw new NoSuchElementException();
		}
		
		return next;
	}
}
