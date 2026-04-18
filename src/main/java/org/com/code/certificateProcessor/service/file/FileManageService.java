package org.com.code.certificateProcessor.service.file;

import com.aliyun.oss.model.*;
import de.huxhorn.sulky.ulid.ULID;
import org.com.code.certificateProcessor.exception.OSSException;
import org.com.code.certificateProcessor.mapper.AwardSubmissionMapper;
import org.com.code.certificateProcessor.pojo.dto.oss.PartETagDTO;
import org.com.code.certificateProcessor.pojo.entity.AwardSubmission;
import org.com.code.certificateProcessor.pojo.enums.AwardSubmissionStatus;
import org.com.code.certificateProcessor.util.mapKey.FileUploadMapKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class FileManageService {
    @Autowired
    private OSSService ossService;
    @Autowired
    private RedisTemplate<String, Object> objectRedisTemplate;
    @Autowired
    AwardSubmissionMapper awardSubmissionMapper;



    /**
     * 每段的大小5MB
     * 最大分片数2
     * 多次上传片段的总和最大 5 * 2 = 10MB
     */
    public static final long PART_SIZE = 5 * 1024 * 1024;
    public static final long MAX_PART_COUNT = 2;
    public static final long MAX_TOTAL_SIZE = PART_SIZE * MAX_PART_COUNT;

    public static final String UPLOAD_ID_KEY_PREFIX = "OSSUploadId_";
    /**
     * 记录文件是否临时被撤销的 key
     * 当 IfSubmissionGotRevoked uploadId 0   代表文件正常
     * 当 IfSubmissionGotRevoked uploadId 1   代表文件被撤销
     */
    public static final String IfSubmissionGotRevoked = "IfSubmissionGotRevoked";

    public static final Set<String> VALID_FILE_MIME_TYPES = new HashSet(Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"));
    public boolean isValidFile(String detectedMimeType) {
        if (VALID_FILE_MIME_TYPES.contains(detectedMimeType)) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param fileName
     * @param fileSize
     * @return
     */
    public Map<String, Object> initMultipartUpload(String fileName,String detectedMimeType,long fileSize){
        try {
            //生成文件在OSS存储的路径
            String imageObjectKey = ossService.getObjectKey(fileName,detectedMimeType);
            //给文件分成许多小段
            int totalPartCount = (int) Math.ceil((double) fileSize / PART_SIZE);

            String uploadId = ossService.getUploadId(detectedMimeType, imageObjectKey);

            Map<String, Object> uploadInfo = new HashMap<>();
            uploadInfo.put(FileUploadMapKey.studentId, SecurityContextHolder.getContext().getAuthentication().getName());
            uploadInfo.put(FileUploadMapKey.fileName, fileName);
            uploadInfo.put(FileUploadMapKey.imageObjectKey, imageObjectKey);
            uploadInfo.put(FileUploadMapKey.fileSize, fileSize);
            uploadInfo.put(FileUploadMapKey.totalPartCount, totalPartCount);
            uploadInfo.put(FileUploadMapKey.completedPartNumber, 0);
            uploadInfo.put(FileUploadMapKey.currentUploadedFileSize,0L);
            uploadInfo.put(FileUploadMapKey.completedPartETags, new ArrayList<PartETagDTO>());

            objectRedisTemplate.opsForValue().set(UPLOAD_ID_KEY_PREFIX+uploadId, uploadInfo, Duration.ofHours(1));

            Map<String, Object> map = new HashMap<>();
            map.put(FileUploadMapKey.uploadId, uploadId);
            map.put(FileUploadMapKey.totalPartCount, totalPartCount);
            map.put(FileUploadMapKey.completedPartNumber, 0);
            return map;
        }catch (Exception e) {
            throw new OSSException("初始化上传失败，请重新上传",e);
        }
    }



    public void uploadPart(String uploadId, Map<String, Object> uploadInfo, int chunkSerialNumber, InputStream inputStream,long fileChunkSize) throws Exception {
        try {
            String imageObjectKey = (String) uploadInfo.get(FileUploadMapKey.imageObjectKey);

            UploadPartResult uploadPartResult = ossService.getUploadPartResult(uploadId, chunkSerialNumber, inputStream, fileChunkSize, imageObjectKey);

            int completedPartNumber = (int)uploadInfo.get(FileUploadMapKey.completedPartNumber);
            long currentUploadedFileSize = (long) uploadInfo.get(FileUploadMapKey.currentUploadedFileSize);
            uploadInfo.put(FileUploadMapKey.completedPartNumber, completedPartNumber + 1);
            uploadInfo.put(FileUploadMapKey.currentUploadedFileSize, currentUploadedFileSize + fileChunkSize);



            List<PartETagDTO> completedPartETags = (List<PartETagDTO>) uploadInfo.get(FileUploadMapKey.completedPartETags);
            completedPartETags.add(new PartETagDTO(chunkSerialNumber, uploadPartResult.getPartETag().getETag()));
            uploadInfo.put(FileUploadMapKey.completedPartETags, completedPartETags);

            objectRedisTemplate.opsForValue().set(UPLOAD_ID_KEY_PREFIX + uploadId, uploadInfo);
        } catch (Exception e) {
            throw new OSSException("上传失败，请重新上传",e);
        }
    }


    /**
     * @param uploadId
     * @param uploadInfo
     * @return
     */

    public Map<String, Object> completeMultipartUpload(String uploadId,Map<String, Object> uploadInfo) {
        try {
            List<PartETagDTO> completedPartETags = (List<PartETagDTO>) uploadInfo.get(FileUploadMapKey.completedPartETags);
            if (completedPartETags == null || completedPartETags.isEmpty()) {
                throw new OSSException("结束分段失败");
            }
            completedPartETags.sort(Comparator.comparingInt(PartETagDTO::getPartNumber));
            List<PartETag> partETags = completedPartETags.stream().map(PartETagDTO::toPartETag).collect(Collectors.toList());

            String imageObjectKey = (String) uploadInfo.get(FileUploadMapKey.imageObjectKey);
            ossService.completeMultipartUploadRequest(uploadId, imageObjectKey, partETags);

            String imageTemporaryUrl = ossService.generateTemporaryCompressedImageUrl(imageObjectKey,180);

            ULID ulid = new ULID();
            String submissionId = ulid.nextULID();
            uploadInfo.put(FileUploadMapKey.submissionId, submissionId);
            uploadInfo.put(FileUploadMapKey.imageTemporaryUrl, imageTemporaryUrl);

            AwardSubmission awardSubmission = AwardSubmission.builder()
                            .submissionId(submissionId)
                            .studentId((String) uploadInfo.get(FileUploadMapKey.studentId))
                            .imageObjectKey(imageObjectKey)
                            .status(AwardSubmissionStatus.AI_PROCESSING)
                            .build();

            /**
             * uploadInfo 包含:
             *
             * {
             *   "studentId": "123456789",
             *   "currentUploadedFileSize": [
             *     "java.lang.Long",
             *     517578
             *   ],
             *   "fileName": "奖状.png",
             *   "imageObjectKey": "image/png/20251103/123456789/7c77437cae7046ad82d5546fe608f196-奖状.png",
             *   "fileSize": [
             *     "java.lang.Long",
             *     1789952
             *   ],
             *   "completedPartETags": [
             *     "java.util.ArrayList",
             *     [
             *       {
             *         "@class": "org.com.code.certificateProcessor.pojo.dto.oss.PartETagDTO",
             *         "partNumber": 1,
             *         "etag": "5B2632B458E88B3CDCED13B84608B2C8"
             *       }
             *     ]
             *   ],
             *   "completedPartNumber": 1,
             *   "totalPartCount": 1
             * }
             */

            awardSubmissionMapper.addAwardSubmission(awardSubmission);
            objectRedisTemplate.execute(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection){
                    objectRedisTemplate.delete(UPLOAD_ID_KEY_PREFIX + uploadId);
                    objectRedisTemplate.opsForHash().put(IfSubmissionGotRevoked,submissionId, imageObjectKey);
                    return null;
                }
            });
            return uploadInfo;
        } catch (OSSException e){
            throw e;
        }
        catch (Exception e) {
            throw new OSSException("结束分段失败",e);
        }
    }
}
