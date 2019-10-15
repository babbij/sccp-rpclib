package com.colabriq.rpclib.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import com.colabriq.rpclib.RPCCommon;
import com.colabriq.rpclib.server.receiver.RPCReceiver;
import com.google.protobuf.Any;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

/**
 * Handles RPC requests sent in as protocol buffers and dispatches them to the 
 * logic that sits behind each.
 */
public class RPCHandler implements Handler<RoutingContext> {
	private final ExecutorService executorService;
	private final Set<RPCReceiver> rpcs;
	
	public RPCHandler(ExecutorService executorService, Set<RPCReceiver> rpcs) {
		this.rpcs = rpcs;
		this.executorService = executorService;
	}
	
	@Override
	public void handle(RoutingContext ctx) {
		ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, RPCCommon.PROTOBUF_CONTENT_TYPE);
		ctx.response().setChunked(true);
		
		// XXX should move to using non-blocking streams all the way here
		// but these are not yet stable, so just read the request in for now
		
		var data = ctx.getBody().getBytes();
		
		// still do this in another thread as it could take some time to perform an RPC call
		
		executorService.execute(() -> {
			try {
				var any = Any.parseFrom(data);
				
				var rpc = rpcs.stream()
					.filter(r -> r.test(any))
					.findFirst()
				;
				
				if (rpc.isPresent()) {
					// XXX temporarily do this with buffered streams 
					var os = new ByteArrayOutputStream();
					rpc.get().exec(any, os);
					ctx.response().end(Buffer.buffer(os.toByteArray()));
				}
				else {
					ctx.fail(new RPCExecutionException("Did not find inbound handler for " + any.getTypeUrl()));
				}
			}
			catch (RPCExecutionException e) {
				ctx.fail(e);
			}
			catch (IOException e) {
				ctx.fail(new RPCExecutionException("I/O Exception occurred", e));
			}
		});
	}
}
