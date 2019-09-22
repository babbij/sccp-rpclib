package com.colabriq.rpclib;

import java.net.URI;
import java.util.stream.Stream;

import com.colabriq.proto.DHTProto.PointerSearchRequest;
import com.colabriq.proto.DHTProto.PointerSearchResponse;
import com.colabriq.rpclib.client.RPCClient;
import com.colabriq.rpclib.client.RPCClientException;
import com.colabriq.rpclib.client.response.RPCResponse;
import com.colabriq.rpclib.client.response.RPCStreamResponseHandler;

import io.vertx.core.Future;

public class TestRPCClient2 {
	public static void main(String [] args) throws Exception {
		var client = new RPCClient(new URI("http://localhost:8080/rpc"));
		
		var messsage = PointerSearchRequest.newBuilder()
			.setPattern("foobar")
			.build()
		;
		
		client.send(
			messsage,
			new RPCStreamResponseHandler<>(
				PointerSearchResponse.class,
				Future.<Stream<RPCResponse<PointerSearchResponse>>>future().setHandler(result -> {
					var it = result.result().iterator();
					
					while (it.hasNext()) {
						try {
							System.out.println(it.next().get().getClass());
						} 
						catch (RPCClientException e) {
							e.printStackTrace();
						}
					}
				})
			)
		);
	}
}
