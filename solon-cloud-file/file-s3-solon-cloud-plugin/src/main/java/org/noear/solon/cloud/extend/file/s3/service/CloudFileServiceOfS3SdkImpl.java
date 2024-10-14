package org.noear.solon.cloud.extend.file.s3.service;

import org.noear.solon.Utils;
import org.noear.solon.cloud.exception.CloudFileException;
import org.noear.solon.cloud.extend.file.s3.utils.BucketUtils;
import org.noear.solon.cloud.model.Media;
import org.noear.solon.cloud.service.CloudFileService;
import org.noear.solon.core.Props;
import org.noear.solon.core.handle.Result;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Date;

public class CloudFileServiceOfS3SdkImpl implements CloudFileService {
    private final String bucketDef;
    private final S3Client client;
    private final S3Presigner s3Presigner;

    public S3Client getClient() {
        return client;
    }

    public CloudFileServiceOfS3SdkImpl(String bucketDef, Props props) {
        this.bucketDef = bucketDef;
        this.client = BucketUtils.createClient(props);
        this.s3Presigner = BucketUtils.createClientPresigner(props);
    }

    @Override
    public boolean exists(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            throw new CloudFileException(e);
        }
    }

    @Override
    public String getTempUrl(String bucket, String key, Date expiration) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getObjectRequest)
                    .signatureDuration(Duration.ofMillis(30))
                    .build();

            URL url = s3Presigner.presignGetObject(presignRequest).url();
            return url != null ? url.toString() : null;
        } catch (Exception e) {
            throw new CloudFileException(e);
        }
    }

    @Override
    public Media get(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            // 使用 GetObjectRequest 而不是 GetUrlRequest
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            // 获取对象流
            ResponseInputStream<GetObjectResponse> responseInputStream = client.getObject(getObjectRequest);
            GetObjectResponse response = responseInputStream.response();

            // 获取响应的内容类型和内容大小
            String contentType = response.contentType();
            long contentSize = response.contentLength();

            // 返回新的 Media 对象，包含对象的输入流、内容类型和大小
            return new Media(responseInputStream, contentType, contentSize);
        } catch (Exception e) {
            throw new CloudFileException(e);
        }
    }

    @Override
    public Result put(String bucket, String key, Media media) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        String streamMime = media.contentType();
        if (Utils.isEmpty(streamMime)) {
            streamMime = "text/plain; charset=utf-8";
        }

        try {
            // 构建 PutObjectRequest
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(streamMime)
                    .build();
            // 将 InputStream 转换为 RequestBody
            RequestBody requestBody = RequestBody.fromInputStream(media.body(), media.contentSize());

            // 上传对象
            client.putObject(putObjectRequest, requestBody);

            return Result.succeed();
        } catch (Exception ex) {
            throw new CloudFileException(ex);
        }
    }


    @Override
    public Result delete(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            client.deleteObject(deleteObjectRequest);
            return Result.succeed();
        } catch (Exception e) {
            throw new CloudFileException(e);
        }
    }
}
