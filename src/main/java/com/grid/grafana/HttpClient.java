package com.grid.grafana;

import com.grid.grafana.exception.ClientException;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.jboss.logging.Logger;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class HttpClient {
    private final String baseUrl;
    private final String basicAuthValue;
    private final SSLContext sslContext;
    private final Logger logger;

    public HttpClient(String baseUrl, String user, String password, Logger logger) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        this.baseUrl = baseUrl;
        this.basicAuthValue = Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
        this.sslContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
        this.logger = logger;
    }

    public CloseableHttpResponse execute(HttpUriRequest request) throws ClientException, IOException {
        request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + this.basicAuthValue);
        this.logger.debugf("executing request %s", request.getRequestLine());
        try (CloseableHttpClient client = HttpClients.custom().setSSLContext(this.sslContext).build()) {
            return client.execute(request);
        }
    }
}
