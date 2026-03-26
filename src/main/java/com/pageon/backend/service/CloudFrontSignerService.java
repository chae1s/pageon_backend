package com.pageon.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;

import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudFrontSignerService {

    private final CloudFrontUtilities cloudFrontUtilities;
    private final PrivateKey privateKey;
    private final String keyPairId;

    @Value("${cdn.cloudfront.url-expire-minutes}")
    private int expireMinutes;

    // CloudFront URL에 서명 추가
    public String signUrl(String resourceUrl) {
        Instant expirationDate = Instant.now().plus(expireMinutes, ChronoUnit.MINUTES);

        CannedSignerRequest signerRequest = CannedSignerRequest.builder()
                .resourceUrl(resourceUrl)
                .privateKey(privateKey)
                .keyPairId(keyPairId)
                .expirationDate(expirationDate)
                .build();

        String signedUrl = cloudFrontUtilities.getSignedUrlWithCannedPolicy(signerRequest).url();

        return signedUrl;
    }

    // 여러 URL 한 번에 서명
    public List<String> signUrls(List<String> resourceUrls) {
        return resourceUrls.stream()
                .map(this::signUrl)
                .collect(Collectors.toList());
    }
}
