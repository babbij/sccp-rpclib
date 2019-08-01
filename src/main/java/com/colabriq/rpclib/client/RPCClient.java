package com.colabriq.rpclib.client;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.colabriq.rpclib.RPCCommon;
import com.colabriq.rpclib.client.response.RPCResponseHandler;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.WebClient;

/**
 * Basic bridging code to use protobufs over Vert.x as an RPC
 */
public class RPCClient {
	private final WebClient client;
	private final URI rpcURI;
	
	public RPCClient(Vertx vertx, WebClient client, URI rpcURI) {
		this.client = client;
		this.rpcURI = rpcURI;
	}
	
	/**
	 * Send a message, collecting replies as an {@link InputStream}
	 */
	public void send(Message m, RPCResponseHandler handler) {
		var any = Any.pack(m);
		var bos = new ByteArrayOutputStream(any.getSerializedSize());
		
		try {
			any.writeTo(bos);
			bos.close();
		}
		catch (IOException e) {
			handler.preFail(e);
		}
		
		client.postAbs(rpcURI.toString())
			.putHeader(CONTENT_TYPE, RPCCommon.PROTOBUF_CONTENT_TYPE)
			.as(handler.getBodyCodec())
			.sendBuffer(Buffer.buffer(bos.toByteArray()), handler);
//			.sendBuffer(Buffer.buffer(bos.buffer()), handler)
			
		;
	}
}
