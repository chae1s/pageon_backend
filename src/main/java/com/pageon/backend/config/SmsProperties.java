package com.pageon.backend.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class SmsProperties {
    @Value("${coolsms.api-key}")
    private String apiKey;
    @Value("${coolsms.api-secret}")
    private String apiSecret;
    @Value("${coolsms.domain}")
    private String apiDomain;
    @Value("${coolsms.set-from}")
    private String fromNumber;

}
