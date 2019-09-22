package com.colabriq.rpclib;

import java.net.URI;

import com.colabriq.proto.DHTProto.PointerPublishRequest;
import com.colabriq.proto.DHTProto.PointerPublishResponse;
import com.colabriq.rpclib.client.RPCClient;
import com.colabriq.rpclib.client.RPCClientException;
import com.colabriq.rpclib.client.response.RPCResponse;
import com.colabriq.rpclib.client.response.RPCSingleResponseHandler;
import com.google.protobuf.ByteString;

import io.vertx.core.Future;

public class TestRPCClient {
	public static void main(String [] args) throws Exception {
		var client = new RPCClient(new URI("http://localhost:8080/rpc"));
		
		var messsage = PointerPublishRequest.newBuilder()
			.setPattern("foobar")
			.setData(ByteString.copyFrom(new byte [] { 1, 2, 3, 4, 5 }))
			.build()
		;
		
		client.send(
			messsage,
			new RPCSingleResponseHandler<>(
				PointerPublishResponse.class,
				Future.<RPCResponse<PointerPublishResponse>>future().setHandler(result -> {					
					try {
						System.out.println(result.result().get().getClass());
					} 
					catch (RPCClientException e) {
						e.printStackTrace();
					}
				})
			)
		);
	}
}
