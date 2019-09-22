package com.colabriq.rpclib;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import com.colabriq.LogConfigurer;
import com.colabriq.proto.DHTProto.PointerPublishRequest;
import com.colabriq.proto.DHTProto.PointerPublishResponse;
import com.colabriq.proto.DHTProto.PointerSearchRequest;
import com.colabriq.proto.DHTProto.PointerSearchResponse;
import com.colabriq.rpclib.server.RPCHandler;
import com.colabriq.rpclib.server.receiver.RPCReceiver;
import com.colabriq.rpclib.server.receiver.RPCReceiverSingleResponse;
import com.colabriq.rpclib.server.receiver.RPCReceiverStreamResponse;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class TestRPCServer {
	public static void main(String[] args) {
		LogConfigurer.init(TestRPCServer.class, "log4j.debug.properties");
		
		var vertx = Vertx.vertx();
		var router = Router.router(vertx);
		
		router.post("/rpc").handler(new RPCHandler(
			Executors.newSingleThreadExecutor(),
			Set.<RPCReceiver>of(
				new RPCReceiverSingleResponse<PointerPublishRequest, PointerPublishResponse>(
					PointerPublishRequest.class,
					request -> {
						System.out.println("PointerPublishRequest");
						
						return PointerPublishResponse
							.newBuilder()
							.build()
						;
					}
				),
				
				new RPCReceiverStreamResponse<PointerSearchRequest, PointerSearchResponse>(
					PointerSearchRequest.class,
					request -> {
						System.out.println("PointerSearchRequest");
						
						return Stream.of(
							PointerSearchResponse.newBuilder().build(),
							PointerSearchResponse.newBuilder().build(),
							PointerSearchResponse.newBuilder().build()
						);
					}
				)
			)
		));
		
		vertx
			.createHttpServer()
			.requestHandler(router)
			.listen(8080)
		;
	}
}
