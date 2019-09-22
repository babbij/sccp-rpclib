package com.colabriq.rpclib.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;

import com.colabriq.rpclib.RPCCommon;
import com.colabriq.rpclib.client.response.RPCResponseHandler;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

/**
 * Basic bridging code to use protobufs over Vert.x as an RPC
 */
public class RPCClient {
	/**
	 * Check if an HTTP response is 2xx
	 */
	public static boolean httpOK(int status) {
		return status >= 200 && status < 300;
	}
	
	private final URI rpcURI;
	
	public RPCClient(URI rpcURI) {
		this.rpcURI = rpcURI;
	}
	
	/**
	 * Send a message, collecting replies as an {@link InputStream}
	 */
	public void send(Message m, RPCResponseHandler handler) {
		try {
			// write out bytes for outgoing message
			var os = new ByteArrayOutputStream();
			Any.pack(m).writeTo(os);
			os.close();
			
			HttpRequest request = HttpRequest.newBuilder()
				.version(Version.HTTP_2)
			    .uri(rpcURI)
			    .headers("Content-Type", RPCCommon.PROTOBUF_CONTENT_TYPE)
			    .POST(HttpRequest.BodyPublishers.ofByteArray(os.toByteArray()))
			    .build();
			
			var future = HttpClient.newBuilder()
				.build()
				.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
			
			var response = future.get();
			
			if (httpOK(response.statusCode())) {
				handler.success(response.body());
			}
			else {
				handler.failure(new RPCClientException("Status returned: " + response.statusCode()));
				
				var is = response.body();
				if (is != null) {
					try {
						is.close();
					}
					catch (IOException e) {
					}
				}
				
			}
		}
		catch (InterruptedException | ExecutionException | IOException e) {
			handler.failure(e);
		}
	}
}
