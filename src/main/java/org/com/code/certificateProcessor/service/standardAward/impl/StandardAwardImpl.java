package org.com.code.certificateProcessor.service.standardAward.impl;

import de.huxhorn.sulky.ulid.ULID;
import org.apache.ibatis.session.SqlSessionFactory;
import org.com.code.certificateProcessor.ElasticSearch.Service.ESStandardAwardService;
import org.com.code.certificateProcessor.exeption.ResourceNotFoundException;
import org.com.code.certificateProcessor.exeption.StandardAwardException;
import org.com.code.certificateProcessor.mapper.StandardAwardMapper;
import org.com.code.certificateProcessor.pojo.dto.document.StandardAwardDocument;
import org.com.code.certificateProcessor.pojo.dto.request.CursorPageRequest;
import org.com.code.certificateProcessor.pojo.dto.response.standardAwardResponse.AdminStandardAwardInfoResponse;
import org.com.code.certificateProcessor.pojo.dto.response.standardAwardResponse.BaseStandardAwardInfoResponse;
import org.com.code.certificateProcessor.pojo.entity.StandardAward;
import org.com.code.certificateProcessor.pojo.dto.request.standardAwardRequest.StandardAwardRequest;
import org.com.code.certificateProcessor.pojo.dto.response.CursorPageResponse;
import org.com.code.certificateProcessor.pojo.structMap.StandardAwardStructMap;
import org.com.code.certificateProcessor.service.BaseCursorPageService;
import org.com.code.certificateProcessor.service.BatchService.BatchExecutorService;
import org.com.code.certificateProcessor.service.standardAward.StandardAwardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StandardAwardImpl extends BaseCursorPageService<StandardAward> implements StandardAwardService {
    @Autowired
    StandardAwardMapper standardAwardMapper;
    @Autowired
    ESStandardAwardService esStandardAwardService;
    @Autowired
    StandardAwardStructMap standardAwardStructMap;
    @Autowired
    private SqlSessionFactory sqlSessionFactory;
    @Autowired
    BatchExecutorService batchExecutorService;


    @Override
    public StandardAward getStandardAwardById(String standardAwardId) {
        try {
            StandardAward standardAward = standardAwardMapper.getStandardAwardById(standardAwardId);
            if (standardAward == null)
                throw new ResourceNotFoundException("标准奖状不存在");
            return standardAward;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceNotFoundException("查询标准奖状详情失败", e);
        }
    }

    @Override
    public CursorPageResponse<? extends BaseStandardAwardInfoResponse> cursorQueryStandardAward(
            CursorPageRequest cursorPageRequest,
            StandardAwardRequest standardAwardRequest,
            String studentId) {

        try {
            StandardAward standardAward = standardAwardStructMap.toStandardAward(standardAwardRequest);
            CursorPageResponse<StandardAward> cursorPage;
            if (cursorPageRequest.getPageSize() < 0)
                cursorPage = fetchStandardAwardPage(cursorPageRequest,standardAward, standardAwardMapper::getPreviousFilteredStandardAward, StandardAward::getStandardAwardId);
            else
                cursorPage = fetchStandardAwardPage(cursorPageRequest,standardAward, standardAwardMapper::getLatterFilteredStandardAward, StandardAward::getStandardAwardId);

            List<? extends BaseStandardAwardInfoResponse> standardAwardInfoResponses;
            if (studentId == null)
                standardAwardInfoResponses = standardAwardStructMap.toAdminStandardAwardInfoResponseList(cursorPage.getList());
            else
                standardAwardInfoResponses = standardAwardStructMap.toBaseStandardAwardInfoResponseList(cursorPage.getList());

            return new CursorPageResponse<>(standardAwardInfoResponses, cursorPage.getMinId(), cursorPage.getMaxId(), cursorPage.getHasNext());
        } catch (Exception e) {
            throw new StandardAwardException("查询标准奖状列表失败", e);
        }
    }

    @Override
    @Transactional
    public void addBatchStandardAward(List<StandardAward> standardAwardList) {
        try {

            String createdBy = SecurityContextHolder.getContext().getAuthentication().getName();
            ULID ulid = new ULID();
            for (StandardAward standardAward : standardAwardList) {
                standardAward.setStandardAwardId(ulid.nextULID());
                standardAward.setCreatedBy(createdBy);
                standardAward.setUpdatedBy(createdBy);
            }

            final int batchSize = 1000;

            batchExecutorService.executeBatch(
                    sqlSessionFactory,
                    StandardAwardMapper.class, standardAwardList,
                    StandardAwardMapper::addStandardAward,
                    batchSize
            );

            List<StandardAwardDocument> standardAwardDocumentList = standardAwardStructMap.toStandardAwardDocumentList(standardAwardList);
            esStandardAwardService.bulkCreateStandardAwardIndex(standardAwardDocumentList);
        } catch (Exception e) {
            throw new StandardAwardException("批量创建标准奖状列表失败", e);
        }
    }

    @Override
    @Transactional
    public void updateBatchStandardAward(List<StandardAward> standardAwardList) {
        try {
            String updatedBy = SecurityContextHolder.getContext().getAuthentication().getName();
            for (StandardAward standardAward : standardAwardList) {
                standardAward.setUpdatedBy(updatedBy);
            }

            final int batchSize = 1000;

            batchExecutorService.executeBatch(
                    sqlSessionFactory,
                    StandardAwardMapper.class, standardAwardList,
                    StandardAwardMapper::updateStandardAward,
                    batchSize
            );

            List<StandardAwardDocument> standardAwardDocumentList = standardAwardStructMap.toStandardAwardDocumentList(standardAwardList);
            esStandardAwardService.updateStandardAwardIndex(standardAwardDocumentList);
        } catch (Exception e) {
            throw new StandardAwardException("批量更新标准奖状列表失败", e);
        }
    }

    @Override
    @Transactional
    public void deleteStandardAward(List<String> standardAwardIdList) {
        try {
            final int batchSize = 1000;

            batchExecutorService.executeBatch(
                    sqlSessionFactory,
                    StandardAwardMapper.class, standardAwardIdList,
                    StandardAwardMapper::deleteStandardAward,
                    batchSize
            );

            esStandardAwardService.deleteStandardAwardIndex(standardAwardIdList);
        } catch (Exception e) {
            throw new StandardAwardException("删除标准奖状列表失败", e);
        }
    }
}