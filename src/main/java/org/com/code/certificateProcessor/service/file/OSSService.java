package org.com.code.certificateProcessor.service.file;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.*;
import org.com.code.certificateProcessor.exception.OSSException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class OSSService {

    private final String BUCKET_NAME;
    private final String ENDPOINT;

    OSSClient ossClient;

    public OSSService(@Value("${aliyun.oss.endpoint}")String ENDPOINT,
                      @Value("${aliyun.oss.accessKeyId}")String ACCESS_KEY_ID,
                      @Value("${aliyun.oss.accessKeySecret}")String ACCESS_KEY_SECRET,
                      @Value("${aliyun.oss.bucketName}")String BUCKET_NAME) {
       try {
           this.BUCKET_NAME = BUCKET_NAME;
           this.ENDPOINT = ENDPOINT;


           OSSClient ossClient = new OSSClient(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);

           if(!ossClient.doesBucketExist(BUCKET_NAME)){
               throw new OSSException("存储桶不存在");
           }
           this.ossClient=ossClient;
       }catch (OSSException e){
           throw e;
       }catch (Exception e){
           throw new OSSException("存储桶不存在",e);
       }
    }

    // 获取OSS文件路径
    public String getObjectKey(String fileName, String mimeType){
        StringBuilder objectKey = new StringBuilder();

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String formattedDate = now.format(formatter);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return  objectKey.append(mimeType).append("/").append(formattedDate).append("/")
                .append(SecurityContextHolder.getContext().getAuthentication().getName()).append("/")
                .append(uuid).append("-").append(fileName).toString();
    }

    public String getUploadId(String detectedMimeType, String imageObjectKey) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(
                BUCKET_NAME, imageObjectKey);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(detectedMimeType);
        request.setObjectMetadata(objectMetadata);

        InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
        String uploadId = result.getUploadId();
        return uploadId;
    }

    public UploadPartResult getUploadPartResult(String uploadId, int chunkSerialNumber, InputStream inputStream, long fileChunkSize, String imageObjectKey) {
        UploadPartRequest uploadPartRequest = new UploadPartRequest();
        uploadPartRequest.setBucketName(BUCKET_NAME);
        uploadPartRequest.setKey(imageObjectKey);
        uploadPartRequest.setUploadId(uploadId);
        uploadPartRequest.setPartNumber(chunkSerialNumber);
        uploadPartRequest.setInputStream(inputStream);
        uploadPartRequest.setPartSize(fileChunkSize);

        // 执行上传操作
        UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
        return uploadPartResult;
    }

    public void completeMultipartUploadRequest(String uploadId, String imageObjectKey, List<PartETag> partETags) {
        CompleteMultipartUploadRequest completeMultipartUploadRequest
                = new CompleteMultipartUploadRequest(BUCKET_NAME, imageObjectKey, uploadId, partETags);
        ossClient.completeMultipartUpload(completeMultipartUploadRequest);
    }

    public void deleteFile(String objectKey){
        try {
            /**
             *  objectKey = "video/20250525/19233392121872384/94c0d50c88524b0494f76ae8112b1bf1-肯德基疯狂星期四.mp4"
             */
            ossClient.deleteObject(BUCKET_NAME, objectKey);
        } catch (Exception e) {
            throw new OSSException("删除文件失败",e);
        }
    }

    /**
     * ACL 设为 private + 使用临时签名 URL（Presigned URL）
     * 防止
     * 1.URL 被猜到 ,ACL=private，没签名打不开
     * 2.URL 被泄露（比如复制给别人）,签名 URL 有短时效（如5分钟），过期失效
     * 3.盗链（其他网站 <img src="你的图">） 签名 URL 无法被第三方长期使用；还可配合 Referer 防盗链
     * 4.未授权用户访问 后端可以在生成 URL 前做权限校验
     * @param objectKey
     * @param expireSeconds
     * @return
     */
    public String generateTemporaryCompressedImageUrl(String objectKey, int expireSeconds) {
        Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(BUCKET_NAME, objectKey, HttpMethod.GET);
        request.setExpiration(expiration);

        // 1. 定义一个固定的、适用于AI模型的压缩样式。
        //    这避免了在签名URL之前需要知道图片原始大小的"鸡生蛋"问题。
        String fixedCompressionStyle = "image/resize,w_1024/quality,q_85";

        // 2. 在生成签名 *之前* 将处理参数添加为查询参数。
        //    这是为预签名URL添加OSS处理的正确方法。
        request.addQueryParameter("x-oss-process", fixedCompressionStyle);

        // 3. 生成预签名URL。
        //    SDK会自动将 "x-oss-process" 参数及其值包含在签名计算中。
        String temporaryImageURL =  ossClient.generatePresignedUrl(request).toString();

        // 4. 直接返回这个已包含压缩参数的、签名有效的URL。
        return temporaryImageURL;
    }
    /**
     * 为什么不能简单地动态压缩？
     * 因为“动态压缩”这个动作本身，就需要知道“图片信息”（比如宽度）。
     *
     * 为了实现“动态压缩”，正确但极其缓慢的流程是这样的：
     *
     * 生成第1个URL（用于获取信息）：
     *
     * 在 OSSService 中，为 objectKey 生成一个带 x-oss-process=image/info 签名的URL，设置一个很短的过期时间（比如10秒）。
     *
     * 发起网络请求（服务器自己请求OSS）：
     *
     * 服务器（OSSService）立刻使用 RestTemplate（就像 OssImageUtil.getImageInfo 里那样）去请求这个第1个URL。
     *
     * OSS返回一个包含图片宽高的JSON。
     *
     * 决定压缩样式：
     *
     * 代码解析这个JSON，发现宽度是4000px，于是决定使用 image/resize,w_1024... 作为压缩样式。
     *
     * 生成第2个URL（最终给AI的URL）：
     *
     * 再次为同一个 objectKey，生成一个带 x-oss-process=image/resize,w_1024... 签名的URL，设置过期时间为180秒。
     *
     * 返回第2个URL：
     *
     * 这个URL最终被返回给AI。
     *
     * 这个流程的致命问题：
     *
     * 为了生成一个URL，额外增加了一次往返OSS的网络请求（在步骤2）。如果在一个API中要返回10张图片的URL（比如 cursorQuerySubmissionByStatus），这个API就会多发起10次HTTP请求，接口会变得非常慢。
     */
}
