package org.com.code.certificateProcessor.service.standardAward;

import org.com.code.certificateProcessor.pojo.dto.request.CursorPageRequest;
import org.com.code.certificateProcessor.pojo.dto.response.standardAwardResponse.AdminStandardAwardInfoResponse;
import org.com.code.certificateProcessor.pojo.dto.response.standardAwardResponse.BaseStandardAwardInfoResponse;
import org.com.code.certificateProcessor.pojo.dto.request.standardAwardRequest.StandardAwardRequest;
import org.com.code.certificateProcessor.pojo.dto.response.CursorPageResponse;
import org.com.code.certificateProcessor.pojo.entity.StandardAward;

import java.util.List;

public interface StandardAwardService {
    StandardAward getStandardAwardById(String standardAwardId);
    CursorPageResponse<? extends BaseStandardAwardInfoResponse> cursorQueryStandardAward(CursorPageRequest cursorPageRequest, StandardAwardRequest standardAwardRequest, String studentId);
    void addBatchStandardAward(List<StandardAward> standardAwardList);
    void updateBatchStandardAward(List<StandardAward> standardAwardList);
    void deleteStandardAward(List<String> standardAwardIdList);
}
