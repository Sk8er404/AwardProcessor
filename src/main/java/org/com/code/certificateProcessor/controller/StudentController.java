package org.com.code.certificateProcessor.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.apache.tika.Tika;
import org.com.code.certificateProcessor.pojo.dto.response.awardSubmissionResponse.BaseAwardSubmissionResponse;
import org.com.code.certificateProcessor.pojo.dto.response.studentResponse.CreateStudentResponse;
import org.com.code.certificateProcessor.pojo.dto.response.studentResponse.StudentInfoResponse;
import org.com.code.certificateProcessor.pojo.dto.response.studentResponse.StudentSignInResponse;
import org.com.code.certificateProcessor.pojo.entity.Student;
import org.com.code.certificateProcessor.pojo.structMap.StudentStructMap;
import org.com.code.certificateProcessor.pojo.validation.group.CreateGroup;
import org.com.code.certificateProcessor.pojo.validation.group.SignInGroup;
import org.com.code.certificateProcessor.pojo.validation.group.UpdateGroup;
import org.com.code.certificateProcessor.pojo.dto.request.CursorPageRequest;
import org.com.code.certificateProcessor.pojo.dto.request.StudentRequest;
import org.com.code.certificateProcessor.pojo.dto.response.CursorPageResponse;
import org.com.code.certificateProcessor.pojo.enums.AwardSubmissionStatus;
import org.com.code.certificateProcessor.rocketMQ.producer.SubmissionProducer;
import org.com.code.certificateProcessor.service.awardSubmission.AwardSubmissionService;
import org.com.code.certificateProcessor.service.standardAward.StandardAwardService;
import org.com.code.certificateProcessor.service.student.StudentService;
import org.com.code.certificateProcessor.service.file.FileManageService;
import org.com.code.certificateProcessor.pojo.validation.ValidEnum;
import org.com.code.certificateProcessor.util.mapKey.FileUploadMapKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.com.code.certificateProcessor.rocketMQ.MQConstants;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@Validated
public class StudentController {
    @Autowired
    private StudentService studentService;
    @Autowired
    private FileManageService fileManageService;
    @Autowired
    private AwardSubmissionService awardSubmissionService;
    @Autowired
    private RedisTemplate<String, Object> objectRedisTemplate;
    @Autowired
    private SubmissionProducer submissionProducer;
    @Autowired
    private StudentStructMap studentStructMap;

    @PostMapping("/signUp")
    public ResponseEntity<Object> signUp(
            @RequestBody
            @JsonView(CreateGroup.class)
            @Validated(CreateGroup.class)
            @NotNull
            StudentRequest studentRequest) {
        Student student = studentStructMap.toStudent(studentRequest);
        CreateStudentResponse createStudentResponse = studentStructMap.toCreateStudentResponse(studentService.addStudent(student));;
        return ResponseEntity.created(null).body(createStudentResponse);
    }
    @PostMapping("/signIn")
    public ResponseEntity<Object> signIn(
            @RequestBody
            @JsonView(SignInGroup.class)
            @Validated(SignInGroup.class)
            @NotNull
            StudentRequest studentRequest) {
        StudentSignInResponse studentSignInResponse =
                studentService.studentSignIn(studentRequest.getStudentId(), studentRequest.getPassword());

        return ResponseEntity.ok(studentSignInResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<Object> me() {
        String studentId = SecurityContextHolder.getContext().getAuthentication().getName();
        StudentInfoResponse studentInfoResponse = studentStructMap.toStudentInfoResponse( studentService.getStudentById(studentId));
        Double sumOfScore = awardSubmissionService.sumApprovedScoreByStudentId(studentId);
        studentInfoResponse.setSumOfScore(sumOfScore);

        return ResponseEntity.ok(studentInfoResponse);
    }

    /**
     *
     * @param fileName 比如 doc.txt
     * @param fileSize 单位 字节
     * @return
     */
    @PostMapping("/initSubmission")
    public ResponseEntity<Object> initSubmission(
            @RequestParam("fileName")
            @NotBlank
            String fileName,
            @RequestParam("fileSize")
            @NotNull(message = "文件大小不能为空")
            Long fileSize) {
        String detectedMimeType = new Tika().detect(fileName);

        if (!fileManageService.isValidFile(detectedMimeType))
            return ResponseEntity.badRequest().body("文件类型错误");

        Map<String, Object> uploadInfo = fileManageService.initMultipartUpload(fileName,detectedMimeType, fileSize);
        return ResponseEntity.ok(uploadInfo);
    }

    /**
     *
     * @param fileChunk
     * @param chunkSerialNumber 从1开始
     * @param uploadId
     * @return
     * @throws Exception
     */
    @PostMapping("/uploadPart")
    public ResponseEntity<Object> uploadPart(
            @RequestParam("fileChunk")
            @NotNull(message = "文件分片不能为空")
            MultipartFile fileChunk,
            @RequestParam("chunkSerialNumber")
            @NotNull(message = "分片序号不能为空")
            Integer chunkSerialNumber,
            @RequestParam("uploadId")
            @NotBlank(message = "上传ID不能为空")
            String uploadId) throws Exception {
        Map<String, Object> uploadInfo = (Map<String, Object>) objectRedisTemplate.opsForValue().get(fileManageService.UPLOAD_ID_KEY_PREFIX + uploadId);
        if (uploadInfo == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("上传信息不存在");

        int totalPartCount = (int) uploadInfo.get(FileUploadMapKey.totalPartCount);
        if (chunkSerialNumber > totalPartCount|| chunkSerialNumber < 1)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("分片序号超出范围");

        long fileChunkSize = fileChunk.getSize();

        // 只在处理第一个分片时检查文件类型
        if (chunkSerialNumber == 1) { // 假设分片从1开始
            Tika tika = new Tika();
            try (InputStream inputStream = fileChunk.getInputStream()) {
                if (fileChunkSize > fileManageService.PART_SIZE)
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("分片大小超过限制");

                /**
                 * 先检查 fileChunkSize 的大小再检查文件类型，
                 * 因为检查 文件类型 要读取整个流，耗时耗性能，
                 * 如果是检查文件类型成功结果发现文件太大了报错，那就白白花那么多时间和性能检查类型了
                 * 还不如再一开始的时候先检查文件大小再检测文件类型更合理
                 */
                String detectedMimeType = tika.detect(inputStream); // Tika会读取流来检测
                if (!fileManageService.isValidFile(detectedMimeType))
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("文件类型错误");
            }
        }


        try (InputStream inputStream = fileChunk.getInputStream()){
            if (fileChunkSize > fileManageService.PART_SIZE)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("分片大小超过限制");

            if(fileChunkSize+ (long) uploadInfo.get(FileUploadMapKey.currentUploadedFileSize)>fileManageService.MAX_TOTAL_SIZE){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("总的文件大小超出限制");
            }
            fileManageService.uploadPart(uploadId,uploadInfo, chunkSerialNumber, inputStream,fileChunkSize);
        }

        uploadInfo.remove(FileUploadMapKey.imageObjectKey);
        return ResponseEntity.status(HttpStatus.OK).body(uploadInfo);
    }

    @PostMapping("/completeSubmission")
    public ResponseEntity<Object> completeSubmission(
            @RequestParam("uploadId")
            @NotBlank(message = "上传 ID 不能为空")
            String uploadId) {
        Map<String, Object> uploadInfo = (Map<String, Object>) objectRedisTemplate.opsForValue().get(fileManageService.UPLOAD_ID_KEY_PREFIX + uploadId);
        if (uploadInfo == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("上传信息不存在");


        fileManageService.completeMultipartUpload(uploadId, uploadInfo);

        Map<String,Object> completeUploadInfo = new HashMap<>();
        completeUploadInfo.put(FileUploadMapKey.imageObjectKey,uploadInfo.get(FileUploadMapKey.imageObjectKey));
        completeUploadInfo.put(FileUploadMapKey.submissionId,uploadInfo.get(FileUploadMapKey.submissionId));
        completeUploadInfo.put(FileUploadMapKey.studentId,uploadInfo.get(FileUploadMapKey.studentId));
        completeUploadInfo.put(FileUploadMapKey.status,uploadInfo.get(FileUploadMapKey.status));

        submissionProducer.asyncSendMessage(completeUploadInfo, MQConstants.Topic.SUBMISSION, MQConstants.Tag.STUDENT_AWARD_SUBMISSION);

        uploadInfo.remove(FileUploadMapKey.imageObjectKey);
        return ResponseEntity.status(HttpStatus.OK).body(uploadInfo);
    }

    @DeleteMapping("/revokeSubmission")
    public ResponseEntity<Object> revokeSubmission(
            @RequestParam("submissionId")
            @NotBlank(message = "submissionId 不能为空")
            String submissionId) {
        String studentId = SecurityContextHolder.getContext().getAuthentication().getName();
        awardSubmissionService.revokeSubmission(submissionId,studentId);
        return ResponseEntity.status(HttpStatus.OK).body("撤销提交成功");
    }

    @PostMapping("/getSubmissionProgress")
    public ResponseEntity<Object> getSubmissionProgress(
            @RequestBody
            @Valid
            @NotNull(message = "cursorPageRequest 不能为空")
            CursorPageRequest cursorPageRequest,
            @RequestParam
            @NotEmpty(message = "status 列表不能为空")
            List<@NotNull @ValidEnum(enumClass = AwardSubmissionStatus.class) String> status) {
        String studentId = SecurityContextHolder.getContext().getAuthentication().getName();

        CursorPageResponse<? extends BaseAwardSubmissionResponse> submissionProgress =
                awardSubmissionService.cursorQuerySubmissionByStatus(cursorPageRequest,status, false,studentId);

        return ResponseEntity.status(HttpStatus.OK).body(submissionProgress);
    }

    @PutMapping("/updateInfo")
    public ResponseEntity<Object> updateInfo(
            @RequestBody
            @JsonView(UpdateGroup.class)
            @Validated(UpdateGroup.class)
            @NotNull(message = "studentRequest 不能为空")
            StudentRequest studentRequest) {
        Student student = studentStructMap.toStudent(studentRequest);
        studentService.updateStudentInfo(student);
        return ResponseEntity.status(HttpStatus.OK).body("更新学生信息成功");
    }
}
