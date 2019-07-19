package com.goodforgoodbusiness.rpclib.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import com.goodforgoodbusiness.rpclib.server.receiver.RPCReceiver;
import com.google.protobuf.Any;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.RoutingContext;

/**
 * Handles RPC requests sent in as protocol buffers and dispatches them to the 
 * logic that sits behind each.
 */
public class RPCHandler implements Handler<RoutingContext> {
	private final ExecutorService executorService;
	private final Set<RPCReceiver> rpcs;
	
	public RPCHandler(ExecutorService executorService, Set<RPCReceiver> rpcs) {
		this.executorService = executorService;
		this.rpcs = rpcs;
	}
	
	@Override
	public void handle(RoutingContext ctx) {
		ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/x-protobuf");
		ctx.response().setChunked(true);
		
		var pump = Pump.pump(ctx.request(), new TestWriteStream());
		pump.start();
		
		// must be done in a separate thread so the blocking code in the protobuf
		// parser/encoder does not block the Vert.x thread.
		executorService.execute(() -> {
			try {
				var bin = new byte [] { 10, 45, 116, 121, 112, 101, 46, 103, 111, 111, 103, 108, 101, 97, 112, 105, 115, 46, 99, 111, 109, 47, 68, 72, 84, 46, 80, 111, 105, 110, 116, 101, 114, 80, 117, 98, 108, 105, 115, 104, 82, 101, 113, 117, 101, 115, 116, 18, 10, 10, 3, 97, 98, 99, 18, 3, 1, 2, 3 };
				
//				System.out.println(Arrays.toString(bin));
				
				var any = Any.parseFrom(new ByteArrayInputStream(bin));
				
				
				
				var rpc = rpcs.stream()
					.filter(r -> r.test(any))
					.findFirst()
				;
				
				if (rpc.isPresent()) {
					var os = new ByteArrayOutputStream();
					
					
					
					rpc.get().exec(any, os);

					ctx.response().write(Buffer.buffer(os.toByteArray()));
					ctx.response().end();
				}
				else {
					ctx.fail(400);
				}
			}
			catch (IOException e) {
				ctx.fail(400);
			}
		});
	}
}
