package com.goodforgoodbusiness.rpclib.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import com.goodforgoodbusiness.rpclib.server.receiver.RPCReceiver;
import com.goodforgoodbusiness.rpclib.stream.InputWriteStream;
import com.goodforgoodbusiness.rpclib.stream.WriteOutputStream;
import com.google.protobuf.Any;

import io.vertx.core.Handler;
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
		this.executorService = executorService;
		this.rpcs = rpcs;
	}
	
	@Override
	public void handle(RoutingContext ctx) {
		ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/x-protobuf");
		ctx.response().setChunked(true);
		
		InputWriteStream ws;
		
		try {
			ws = new InputWriteStream();
		}
		catch (IOException e) {
			ctx.fail(e);
			return;
		}
		
		InputStream is = ws.getInputStream();
		OutputStream os = new WriteOutputStream(ctx.response());
		
		// must be done in a separate thread so the blocking code in the protobuf
		// parser/encoder does not block the Vert.x thread.
		executorService.execute(() -> {
			try {
				var any = Any.parseFrom(is);
				
				var rpc = rpcs.stream()
					.filter(r -> r.test(any))
					.findFirst()
				;
				
				if (rpc.isPresent()) {
					rpc.get().exec(any, os);
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
		
		var pipe = ctx.request().pipe();
		pipe.to(ws);
	}
}
