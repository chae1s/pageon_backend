package com.pageon.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Configuration
public class CloudFrontConfig {

    @Value("${cdn.cloudfront.keypair-id}")
    private String keyPairId;

    @Value("${cdn.cloudfront.private-key-path}")
    private Resource privateKeyPathResource;


    @Bean
    public CloudFrontUtilities cloudFrontUtilities() {
        return CloudFrontUtilities.create();
    }

    @Bean
    public PrivateKey cloudFrontPrivateKey() throws Exception{
        String privateKeyPEM = new String(privateKeyPathResource.getInputStream().readAllBytes())
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePrivate(keySpec);
    }

    @Bean
    public String keyPairId() {
        return keyPairId;
    }

}
