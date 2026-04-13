package org.com.code.certificateProcessor.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import org.com.code.certificateProcessor.pojo.dto.response.adminResponse.AdminInfoResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.com.code.certificateProcessor.mapper.StudentMapper;
import org.com.code.certificateProcessor.pojo.dto.request.adminRequest.AdminRequest;
import org.com.code.certificateProcessor.pojo.dto.request.adminRequest.UpdateAdminAuthRequest;
import org.com.code.certificateProcessor.pojo.dto.response.adminResponse.AdminInfoResponse;
import org.com.code.certificateProcessor.pojo.dto.response.adminResponse.AdminSignInResponse;
import org.com.code.certificateProcessor.pojo.dto.response.adminResponse.CreateAdminResponse;
import org.com.code.certificateProcessor.pojo.dto.response.awardSubmissionResponse.AdminAwardSubmissionResponse;
import org.com.code.certificateProcessor.pojo.entity.Admin;
import org.com.code.certificateProcessor.pojo.entity.AwardSubmission;
import org.com.code.certificateProcessor.pojo.structMap.AdminStructMap;
import org.com.code.certificateProcessor.pojo.validation.group.CreateGroup;
import org.com.code.certificateProcessor.pojo.validation.group.SignInGroup;
import org.com.code.certificateProcessor.pojo.validation.group.UpdateGroup;
import org.com.code.certificateProcessor.pojo.dto.request.*;
import org.com.code.certificateProcessor.pojo.dto.response.CursorPageResponse;
import org.com.code.certificateProcessor.pojo.dto.response.awardSubmissionResponse.BaseAwardSubmissionResponse;
import org.com.code.certificateProcessor.pojo.dto.response.studentResponse.StudentInfoResponse;
import org.com.code.certificateProcessor.pojo.enums.AwardSubmissionStatus;
import org.com.code.certificateProcessor.service.admin.AdminService;
import org.com.code.certificateProcessor.service.awardSubmission.AwardSubmissionService;
import org.com.code.certificateProcessor.service.student.StudentService;
import org.com.code.certificateProcessor.pojo.validation.ValidEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminController {
    @Autowired
    private AdminService adminService;
    @Autowired
    private AwardSubmissionService awardSubmissionService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    AdminStructMap adminStructMap;


    @PostMapping("/signUp")
    public ResponseEntity<Object> signUp(
            @RequestBody
            @JsonView(CreateGroup.class)
            @Validated(CreateGroup.class)
            @NotNull
            AdminRequest adminRequest) {
        Admin admin = adminService.addAdmin(adminStructMap.toAdmin(adminRequest));
        return ResponseEntity.created(null).body(adminStructMap.toCreateAdminResponse(admin));
    }
    @PostMapping("/signIn")
    public ResponseEntity<Object> signIn(
            @RequestBody
            @JsonView(SignInGroup.class)
            @Validated(SignInGroup.class)
            @NotNull
            AdminRequest adminRequest) {
        AdminSignInResponse adminSignInResponse = adminService.adminSignIn(adminRequest.getUsername(),
                adminRequest.getPassword());
        return ResponseEntity.ok(adminSignInResponse);
    }
    @GetMapping("/me")
    public ResponseEntity<Object> me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AdminInfoResponse adminInfoResponse = adminStructMap.toAdminInfoResponse(adminService.getAdminByUserName(username));
        return ResponseEntity.ok(adminInfoResponse);
    }

    /**
     * 获取提交进度
     * @param cursorPageRequest
     * @param status
     * @return
     */
    @PostMapping("/getSubmissionProgress")
    public ResponseEntity<Object> getSubmissionProgress(
            @RequestBody
            @Valid
            CursorPageRequest cursorPageRequest,
            @RequestParam
            @NotEmpty(message = "status 列表不能为空")
            List<@ValidEnum(enumClass = AwardSubmissionStatus.class) String> status) {

        CursorPageResponse<? extends BaseAwardSubmissionResponse> submissionProgress =
                awardSubmissionService.cursorQuerySubmissionByStatus(cursorPageRequest,status, true,null);

        return ResponseEntity.ok(submissionProgress);
    }

    /**
     * 获取某个学生的游标查询的提交记录
     * @param cursorPageRequest
     * @param status
     * @param studentId
     * @return
     */
    @PostMapping("/getStudentSubmissionByStudentId")
    public ResponseEntity<Object> getStudentManualApprovedSubmissionsByStudentId(
            @RequestBody
            @Valid
            @NotNull(message = "cursorPageRequest 不能为空")
            CursorPageRequest cursorPageRequest,
            @RequestParam
            @NotEmpty(message = "status 列表不能为空")
            List<@NotNull @ValidEnum(enumClass = AwardSubmissionStatus.class) String> status,
            @NotNull
            String studentId) {
        Boolean isExist = studentMapper.ifStudentExist(studentId);
        if(!isExist){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("学生不存在");
        }

        CursorPageResponse<? extends BaseAwardSubmissionResponse> submissionProgress =
                awardSubmissionService.cursorQuerySubmissionByStatus(cursorPageRequest,status, true,studentId);

        return ResponseEntity.status(HttpStatus.OK).body(submissionProgress);
    }

    @PutMapping("/reviewSubmission")
    public ResponseEntity<Object> reviewSubmission(
            @RequestBody
            @Valid
            @NotNull
            ReviewSubmissionRequest reviewSubmissionRequest) {
        awardSubmissionService.reviewSubmissionRequest(reviewSubmissionRequest);

        return ResponseEntity.ok("审核成功");
    }

    @PostMapping("/getStudentInfo")
    public ResponseEntity<Object> getStudentInfo(
            @RequestBody
            @Valid
            @NotNull
            CursorPageRequest cursorPageRequest) {

        CursorPageResponse<StudentInfoResponse> studentInfoResponseCursorPageResponse
                = studentService.cursorQueryStudent(cursorPageRequest);

        return ResponseEntity.ok(studentInfoResponseCursorPageResponse);
    }

    @PutMapping("/updateInfo")
    public ResponseEntity<Object> updateInfo(
            @RequestBody
            @Validated(UpdateGroup.class)
            @JsonView(UpdateGroup.class)
            @NotNull
            AdminRequest adminRequest) {
        Admin admin = adminStructMap.toAdmin(adminRequest);
        adminService.updateAdminInfo(admin);
        return ResponseEntity.ok("更新成功");
    }

    @PostMapping("/cursorQueryAdmin")
    public ResponseEntity<Object> getAllAdmins(
            @RequestBody
            @Valid
            @NotNull
            CursorPageRequest cursorPageRequest
    ) {
        CursorPageResponse<AdminInfoResponse> adminInfoResponseCursorPageResponse
                = adminService.cursorQueryAdmin(cursorPageRequest);
        return ResponseEntity.ok(adminInfoResponseCursorPageResponse);
    }

    @PutMapping("/updateAuth")
    public ResponseEntity<Object> updateAuth(
            @RequestBody
            @NotNull
            UpdateAdminAuthRequest updateAdminAuthRequest) {
        String username = updateAdminAuthRequest.getUsername();
        String auth = updateAdminAuthRequest.getAuth();
        adminService.updateAdminAuth(username,auth);
        return ResponseEntity.ok("更新管理员权限成功");
    }
}
