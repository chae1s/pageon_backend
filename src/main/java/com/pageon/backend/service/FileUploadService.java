package com.pageon.backend.service;

import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.cloudfront-url}")
    private String cloudFrontUrl;

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

    public List<String> uploadMultiple(MultipartFile[] files, String folder) {
        List<String> uploaded = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                uploaded.add(upload(file, folder));
            }
            return uploaded;
        } catch (Exception e) {
            // 이미 올라간 파일 삭제
            uploaded.forEach(this::deleteFile);
            throw e;
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
