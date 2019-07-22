package com.goodforgoodbusiness.rpclib.client;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static io.netty.buffer.Unpooled.directBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.goodforgoodbusiness.rpclib.client.response.RPCResponseHandler;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import io.netty.buffer.ByteBufOutputStream;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.WebClient;

/**
 * Basic bridging code to use protobufs over Vert.x as an RPC
 */
public class RPCClient {
	private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
	
	private final WebClient client;
	private final URI rpcURI;
	
	public RPCClient(WebClient client, URI rpcURI) {
		this.client = client;
		this.rpcURI = rpcURI;
	}
	
	/**
	 * Send a message, collecting replies as an {@link InputStream}
	 */
	public void send(Message m, RPCResponseHandler handler) {
		var any = Any.pack(m);
		var bos = new ByteBufOutputStream(directBuffer(any.getSerializedSize()));
		
		try {
			any.writeTo(bos);
			bos.close();
		}
		catch (IOException e) {
			handler.preFail(e);
		}
		
		client.postAbs(rpcURI.toString())
			.putHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM)
			.as(handler.getBodyCodec())
			.sendBuffer(Buffer.buffer(bos.buffer()), handler)
		;
	}
}
