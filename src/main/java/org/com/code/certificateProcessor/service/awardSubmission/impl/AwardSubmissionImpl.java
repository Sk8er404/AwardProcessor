package org.com.code.certificateProcessor.service.awardSubmission.impl;

import org.com.code.certificateProcessor.exception.AwardSubmissionException;
import org.com.code.certificateProcessor.mapper.AwardSubmissionMapper;
import org.com.code.certificateProcessor.pojo.dto.StudentScoreDto;
import org.com.code.certificateProcessor.pojo.dto.request.CursorPageRequest;
import org.com.code.certificateProcessor.pojo.dto.response.awardSubmissionResponse.BaseAwardSubmissionResponse;
import org.com.code.certificateProcessor.pojo.entity.AwardSubmission;
import org.com.code.certificateProcessor.pojo.dto.request.ReviewSubmissionRequest;
import org.com.code.certificateProcessor.pojo.dto.response.CursorPageResponse;
import org.com.code.certificateProcessor.pojo.enums.AwardSubmissionStatus;
import org.com.code.certificateProcessor.pojo.structMap.AwardSubmissionStructMap;
import org.com.code.certificateProcessor.service.BaseCursorPageService;
import org.com.code.certificateProcessor.service.awardSubmission.AwardSubmissionService;
import org.com.code.certificateProcessor.service.file.FileManageService;
import org.com.code.certificateProcessor.service.file.OSSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AwardSubmissionImpl extends BaseCursorPageService<AwardSubmission> implements AwardSubmissionService {
    @Autowired
    private AwardSubmissionMapper awardSubmissionMapper;
    @Autowired
    private OSSService ossService;
    @Autowired
    private RedisTemplate<String, Object> objectRedisTemplate;
    @Autowired
    AwardSubmissionStructMap awardSubmissionStructMap;


    @Override
    public void revokeSubmission(String submissionId, String studentId) {
        try {
            /**
             *  <delete id="deleteAwardSubmission">
             *         delete from award_submission
             *         where submissionId = #{submissionId}
             *         and studentId = #{studentId} and status = 'AI_PROCESSING'
             *  </delete>
             */
            String imageObjectKey = awardSubmissionMapper.getSubmissionImageObjectKey(submissionId, studentId);
            ossService.deleteFile(imageObjectKey);
            awardSubmissionMapper.deleteAwardSubmission(submissionId, studentId);

            objectRedisTemplate.opsForHash().delete(FileManageService.IfSubmissionGotRevoked, submissionId);
        } catch (Exception e) {
            throw new AwardSubmissionException("数据库异常，撤销提交失败", e);
        }
    }

    @Override
    public CursorPageResponse<? extends BaseAwardSubmissionResponse> cursorQuerySubmissionByStatus(
            CursorPageRequest cursorPageRequest,
            List<String> status,
            Boolean isAdmin,
            String studentId) {

        int pageSize = cursorPageRequest.getPageSize();
        String lastId = cursorPageRequest.getLastId();

        try {
            CursorPageResponse<AwardSubmission> cursorPage;
            if (pageSize < 0)
                cursorPage = fetchAwardSubmissionPage(lastId, -pageSize, awardSubmissionMapper::getPreviousSubmission, AwardSubmission::getSubmissionId, status, isAdmin, studentId);
            else
                cursorPage = fetchAwardSubmissionPage(lastId, pageSize, awardSubmissionMapper::getLatterSubmission, AwardSubmission::getSubmissionId, status, isAdmin, studentId);

            List<? extends BaseAwardSubmissionResponse> awardSubmissionResponses;
            if (isAdmin)
                awardSubmissionResponses = awardSubmissionStructMap.toAdminAwardSubmissionResponseList(cursorPage.getList());
            else
                awardSubmissionResponses = awardSubmissionStructMap.toBaseAwardSubmissionResponseList(cursorPage.getList());

            awardSubmissionResponses.forEach(
                    baseAwardSubmissionResponse ->
                            baseAwardSubmissionResponse.setTemporaryImageURL(
                                    ossService.generateTemporaryCompressedImageUrl(
                                            baseAwardSubmissionResponse.getTemporaryImageURL(), 180
                                    )
                            )
            );
            return new CursorPageResponse<>(awardSubmissionResponses, cursorPage.getMinId(), cursorPage.getMaxId(), cursorPage.getHasNext());
        } catch (Exception e) {
            throw new AwardSubmissionException("数据库异常，游标查询获取提交进度失败", e);
        }
    }

    @Override
    public void reviewSubmissionRequest(ReviewSubmissionRequest request) {
        try {
            AwardSubmissionStatus submissionStatus = awardSubmissionMapper.getSubmissionStatus(request.getSubmissionId());
            if (submissionStatus == AwardSubmissionStatus.AI_PROCESSING) {
                throw new AwardSubmissionException("AI 处理中，无法人工审核");
            }

            AwardSubmission awardSubmission = AwardSubmission.builder()
                    .submissionId(request.getSubmissionId())
                    .reviewedBy(SecurityContextHolder.getContext().getAuthentication().getName())
                    .build();
            if (request.getFinalScore() != null) {
                awardSubmission.setFinalScore(request.getFinalScore());
            }
            if (request.getUpdateReason() != null) {
                awardSubmission.setReason(request.getUpdateReason());
            }


            if (request.getUpdateStatus().equals(ReviewSubmissionRequest.UpdateStatus.MANUAL_APPROVED.name())) {
                awardSubmission.setStatus(AwardSubmissionStatus.MANUAL_APPROVED);
            } else if (request.getUpdateStatus().equals(ReviewSubmissionRequest.UpdateStatus.MANUAL_REJECTED.name())) {
                awardSubmission.setStatus(AwardSubmissionStatus.MANUAL_REJECTED);
            }
            awardSubmissionMapper.updateAwardSubmission(awardSubmission);
        }catch (AwardSubmissionException e){
            throw e;
        } catch (Exception e) {
            throw new AwardSubmissionException("数据库异常，审核提交失败", e);
        }
    }

    @Override
    public Double sumApprovedScoreByStudentId(String studentId) {
        try {
            List<StudentScoreDto> studentScoreDtoList = awardSubmissionMapper.sumApprovedScoreByStudentIdList(List.of(studentId));
            if (studentScoreDtoList.isEmpty())
                return 0.0;
            return studentScoreDtoList.get(0).getSumOfScore();
        } catch (Exception e) {
            throw new AwardSubmissionException("数据库异常，获取提交进度失败", e);
        }
    }
}