package com.goodforgoodbusiness.rpclib.stream;

import io.vertx.core.file.AsyncFile;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.WriteStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrap a Vert.x {@link WriteStream} in to an {@link OutputStream}.
 */
public class OutputStreamToWriteStream extends OutputStream {
	private final WriteStream<Buffer> response;
	
    private final byte [] buffer;
    private int counter = 0;
	
    public OutputStreamToWriteStream(final WriteStream<Buffer> _response) {
        this.response = _response;
        this.buffer = new byte[1024];
    }
    
    @Override
    public synchronized void write(final int b) throws IOException {
        buffer[counter++] = (byte) b;
        if (counter >= buffer.length) {
            flush();
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        super.flush();

        if (counter > 0) {
            byte[] remaining = buffer;
            if (counter < buffer.length) {
                remaining = new byte[counter];
                System.arraycopy(buffer, 0, remaining, 0, counter);
            }

            response.write(Buffer.buffer(remaining));
            counter = 0;
        }
    }

    @Override
    public void close() throws IOException {
        flush();
        super.close();

        if (response instanceof HttpServerResponse) {
            try {
                response.end();
            }
            catch (final IllegalStateException ignore) {
            }
        }
        else if (response instanceof AsyncFile) {
            ((AsyncFile) response).close();
        }
    }
}