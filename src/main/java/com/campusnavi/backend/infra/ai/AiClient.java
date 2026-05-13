package com.campusnavi.backend.infra.ai;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class AiClient {

    private final RestClient restClient;

    public AiClient(AiClientProperties props) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(of(props.connectTimeout()))
                .build();

        PoolingHttpClientConnectionManager connectionManager
                = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultConnectionConfig(connectionConfig);
        connectionManager.setMaxTotal(props.maxTotal());
        connectionManager.setDefaultMaxPerRoute(props.maxPerRoute());

        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(of(props.responseTimeout()))
                .setConnectionRequestTimeout(of(props.connectionRequestTimeout()))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        this.restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }

    public <Req, Res> Res post(String path, Req request, Class<Res> responseClass) {
        return restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(responseClass);
    }

    public AiHealthResponse healthCheck() {
        return restClient.get()
                .uri("/health")
                .retrieve()
                .body(AiHealthResponse.class);
    }

    private Timeout of(Duration duration) {
        return Timeout.ofMilliseconds(duration.toMillis());
    }
}
