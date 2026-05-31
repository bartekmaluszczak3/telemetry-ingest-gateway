package org.example.gateway.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;


@Configuration
public class SslConfig {

    @Value("${server.ssl.key-store}")
    private String keyStorePath;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${server.ssl.key-store-type}")
    private String keyStoreType;

    @Value("${server.ssl.trust-store}")
    private String trustStorePath;

    @Value("${server.ssl.trust-store-password}")
    private String trustStorePassword;

    @Bean(name = "mtlsRestTemplate")
    public RestTemplate mtlsRestTemplate() throws Exception {
        SSLContext sslContext = buildSslContext();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setSSLSocketFactory(
                                        SSLConnectionSocketFactoryBuilder.create()
                                                .setSslContext(sslContext)
                                                .build()
                                )
                                .build()
                )
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(3000);
        factory.setConnectionRequestTimeout(3000);

        return new RestTemplate(factory);
    }

    @Bean
    public SSLContext sslContext() throws Exception {
        return buildSslContext();
    }

    private SSLContext buildSslContext() throws Exception {
        KeyStore keyStore   = loadKeyStore(keyStorePath, keyStorePassword, keyStoreType);
        KeyStore trustStore = loadKeyStore(trustStorePath, trustStorePassword, "PKCS12");

        return SSLContextBuilder.create()
                .loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
                .loadTrustMaterial(trustStore, null)
                .setProtocol("TLSv1.3")
                .build();
    }


    private KeyStore loadKeyStore(String path, String password, String type) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type);
        String cleanPath = path.replace("classpath:", "");

        try (InputStream is = getResourceAsStream(cleanPath)) {
            if (is == null) {
                throw new IllegalStateException("Keystore not found");
            }
            keyStore.load(is, password.toCharArray());
        }

        return keyStore;
    }

    private InputStream getResourceAsStream(String path) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        if (is != null) return is;
        try {
            return new java.io.FileInputStream(path);
        } catch (java.io.FileNotFoundException e) {
            return null;
        }
    }
}
