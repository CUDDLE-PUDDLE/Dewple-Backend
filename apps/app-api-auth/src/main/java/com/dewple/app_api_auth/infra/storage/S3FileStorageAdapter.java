package com.dewple.app_api_auth.infra.storage;

import com.dewple.user.port.FileStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

@Slf4j
@Component
public class S3FileStorageAdapter implements FileStoragePort {

    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(10);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3FileStorageAdapter(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${app.storage.bucket:}") String bucketName
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    @Override
    public String upload(String key, InputStream inputStream, long contentLength, String contentType) {
        if (bucketName == null || bucketName.isBlank()) {
            log.warn("S3 버킷이 설정되지 않아 파일 업로드를 건너뜁니다. key={}", key);
            return key;
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        log.info("S3 파일 업로드 완료: key={}", key);
        return key;
    }

    @Override
    public String generateDownloadUrl(String key) {
        if (bucketName == null || bucketName.isBlank()) {
            log.warn("S3 버킷이 설정되지 않아 다운로드 URL을 생성할 수 없습니다. key={}", key);
            return "";
        }

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGN_DURATION)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build())
                .build();

        String url = s3Presigner.presignGetObject(presignRequest).url().toString();
        log.info("S3 Presigned URL 생성: key={}", key);
        return url;
    }

    @Override
    public void delete(String key) {
        if (bucketName == null || bucketName.isBlank()) {
            log.warn("S3 버킷이 설정되지 않아 파일 삭제를 건너뜁니다. key={}", key);
            return;
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(request);
        log.info("S3 파일 삭제 완료: key={}", key);
    }
}
