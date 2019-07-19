package com.goodforgoodbusiness.rpclib.client;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Build a {@link WebClient} for use wherever you need it.
 */
public class RPCWebClientCreator {
	public static WebClient create(Vertx vertx) {
		var options = new WebClientOptions();
		
		options.setKeepAlive(true);
		options.setProtocolVersion(HttpVersion.HTTP_2);
		options.setHttp2ClearTextUpgrade(true);
		
		return WebClient.create(vertx, options);
	}
}
