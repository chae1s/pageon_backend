package com.pageon.backend.service;

import com.pageon.backend.common.annotation.ExecutionTimer;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.cloudfront-url}")
    private String cloudFrontUrl;

    private final Executor executor;

    public String upload(MultipartFile file, String folder) {
        String originalName = file.getOriginalFilename();
        String fileName = String.format("%s/%s_%s", folder, UUID.randomUUID(), originalName);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(file.getContentType())
                .build();

        try {
            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.S3_UPLOAD_FAILED);
        }

        return cloudFrontUrl + "/" + fileName;

    }

    @ExecutionTimer
    public List<String> uploadMultiple(MultipartFile[] files, String folder) {

        List<String> uploaded = new ArrayList<>();
        List<CompletableFuture<String>> futures = Arrays.stream(files)
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    log.info("[Async] 업로드 시작: {}", file.getOriginalFilename());

                    return upload(file, folder);
                }, executor))
                .toList();

        try {
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            return allOf.thenApply(s -> {
                return futures.stream()
                        .map(CompletableFuture::join)
                        .toList();
            }).join();
        } catch (Exception e) {
            futures.forEach(f -> {
                if (!f.isCompletedExceptionally() && !f.isCancelled()) {
                    f.thenAccept(url -> {
                        if (url != null) deleteFile(url);
                    });
                }
            });

            throw new CustomException(ErrorCode.S3_UPLOAD_FAILED);
        }

    }

    public String localFileUpload(File file, String folder) {
        String fileName = String.format("%s/%s", folder, file.getName());

        String contentType;

        try {
            contentType = Files.probeContentType(file.toPath());
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_PROCESSING_ERROR);
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromFile(file));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.S3_UPLOAD_FAILED);
        }


        return cloudFrontUrl + "/" + fileName;

    }

    public void deleteFile(String s3Url) {
        String splitStr = cloudFrontUrl + "/";

        if (!s3Url.startsWith(splitStr)) {
            throw new CustomException(ErrorCode.INVALID_FILE_URL);
        }

        String fileName = s3Url.replace(splitStr, "");

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucket).key(fileName).build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.S3_DELETE_FAILED);
        }

    }
}
