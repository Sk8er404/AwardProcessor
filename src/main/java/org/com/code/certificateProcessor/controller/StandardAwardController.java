package org.com.code.certificateProcessor.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.com.code.certificateProcessor.pojo.dto.request.standardAwardRequest.CreateAndUpdateStandardAwardListRequest;
import org.com.code.certificateProcessor.pojo.dto.request.standardAwardRequest.CursorFilteredQueryStandardAwardRequest;
import org.com.code.certificateProcessor.pojo.dto.request.standardAwardRequest.DeleteStandardAwardListRequest;
import org.com.code.certificateProcessor.pojo.dto.request.standardAwardRequest.StandardAwardRequest;
import org.com.code.certificateProcessor.pojo.dto.response.standardAwardResponse.AdminStandardAwardInfoResponse;
import org.com.code.certificateProcessor.pojo.dto.response.standardAwardResponse.BaseStandardAwardInfoResponse;
import org.com.code.certificateProcessor.pojo.entity.StandardAward;
import org.com.code.certificateProcessor.pojo.structMap.StandardAwardStructMap;
import org.com.code.certificateProcessor.pojo.validation.group.CreateGroup;
import org.com.code.certificateProcessor.pojo.validation.group.DeleteGroup;
import org.com.code.certificateProcessor.pojo.validation.group.GetGroup;
import org.com.code.certificateProcessor.pojo.validation.group.UpdateGroup;
import org.com.code.certificateProcessor.pojo.dto.request.*;
import org.com.code.certificateProcessor.pojo.dto.response.CursorPageResponse;
import org.com.code.certificateProcessor.service.standardAward.StandardAwardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/standardAward")
@Validated
public class StandardAwardController {
    @Autowired
    private StandardAwardService standardAwardService;
    @Autowired
    StandardAwardStructMap standardAwardStructMap;


    @GetMapping("/getById")
    public ResponseEntity<Object> getById(
            @RequestParam
            @NotNull
            @NotEmpty
            String standardAwardId) {


        StandardAward standardAward = standardAwardService.getStandardAwardById(standardAwardId);
        AdminStandardAwardInfoResponse standardAwardInfoResponse = standardAwardStructMap.toAdminStandardAwardInfoResponse(standardAward);
        return ResponseEntity.ok(standardAwardInfoResponse);
    }


    @PostMapping("/cursorFilteredGetBatchByAdmin")
    public ResponseEntity<Object> cursorFilteredGetBatchByAdmin(
            @RequestBody
            @Validated({GetGroup.class})
            @NotNull
            CursorFilteredQueryStandardAwardRequest queryStandardAwardRequest) {

        CursorPageResponse<? extends BaseStandardAwardInfoResponse> cursorPageResponse
                = standardAwardService.cursorQueryStandardAward(
                        queryStandardAwardRequest.getCursorPageRequest(),
                        queryStandardAwardRequest.getStandardAwardRequest(),
                        null);
        return ResponseEntity.ok(cursorPageResponse);
    }

    @PostMapping("/cursorFilteredGetBatchByStudent")
    public ResponseEntity<Object> cursorFilteredGetBatchByStudent(
            @RequestBody
            @Validated(GetGroup.class)
            @NotNull
            CursorFilteredQueryStandardAwardRequest queryStandardAwardRequest) {

        String studentId = SecurityContextHolder.getContext().getAuthentication().getName();
        CursorPageResponse<? extends BaseStandardAwardInfoResponse> cursorPageResponse
                = standardAwardService.cursorQueryStandardAward(
                        queryStandardAwardRequest.getCursorPageRequest(),
                        queryStandardAwardRequest.getStandardAwardRequest(),
                        studentId);
        return ResponseEntity.ok(cursorPageResponse);
    }

    /**
     * 创建多个奖状
     * @param createStandardAwardListRequest
     * @return
     */
    @PostMapping("/createBatch")
    public ResponseEntity<Object> createBatch(
            @RequestBody
            @JsonView(CreateGroup.class)
            @Validated(CreateGroup.class)
            @NotNull
            CreateAndUpdateStandardAwardListRequest createStandardAwardListRequest) {

        List<StandardAward> standardAwardList = standardAwardStructMap.toStandardAwardList(createStandardAwardListRequest.getRequestList());
        standardAwardService.addBatchStandardAward(standardAwardList);

        return ResponseEntity.ok("创建成功");
    }

    @PutMapping("/updateBatch")
    public ResponseEntity<Object> updateBatch(
            @RequestBody
            @JsonView(UpdateGroup.class)
            @Validated(UpdateGroup.class)
            @NotNull
            CreateAndUpdateStandardAwardListRequest updateStandardAwardListRequest) {
        List<StandardAward> standardAwardList = standardAwardStructMap.toStandardAwardList(updateStandardAwardListRequest.getRequestList());
        standardAwardService.updateBatchStandardAward(standardAwardList);
        return ResponseEntity.ok("更新成功");
    }

    @DeleteMapping("/deleteBatch")
    public ResponseEntity<Object> delete(
            @RequestBody
            @JsonView(DeleteGroup.class)
            @Validated(DeleteGroup.class)
            @NotNull
            DeleteStandardAwardListRequest deleteStandardAwardListRequest) {

        standardAwardService.deleteStandardAward(deleteStandardAwardListRequest.getStandardAwardIdList());
        return ResponseEntity.ok("删除成功");
    }
}
