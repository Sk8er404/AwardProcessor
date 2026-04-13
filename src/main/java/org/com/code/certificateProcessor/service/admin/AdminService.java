package org.com.code.certificateProcessor.service.admin;

import org.com.code.certificateProcessor.pojo.dto.request.adminRequest.UpdateAdminAuthRequest;
import org.com.code.certificateProcessor.pojo.dto.request.CursorPageRequest;
import org.com.code.certificateProcessor.pojo.dto.response.CursorPageResponse;
import org.com.code.certificateProcessor.pojo.dto.response.adminResponse.AdminInfoResponse;
import org.com.code.certificateProcessor.pojo.dto.request.adminRequest.AdminRequest;
import org.com.code.certificateProcessor.pojo.dto.response.adminResponse.AdminSignInResponse;
import org.com.code.certificateProcessor.pojo.dto.response.adminResponse.CreateAdminResponse;
import org.com.code.certificateProcessor.pojo.entity.Admin;

public interface AdminService {
    Admin addAdmin(Admin admin);
    Admin getAdminByUserName(String username);
    void updateAdminInfo(Admin admin);

    CursorPageResponse<AdminInfoResponse> cursorQueryAdmin(CursorPageRequest cursorPageRequest);

    void updateAdminAuth(String username,String auth);

    AdminSignInResponse adminSignIn(String username, String password);
}
