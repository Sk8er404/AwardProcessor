package org.com.code.certificateProcessor.service.admin.impl;

import org.com.code.certificateProcessor.exception.AdminTableException;
import org.com.code.certificateProcessor.exception.ResourceNotFoundException;
import org.com.code.certificateProcessor.mapper.AdminMapper;
import org.com.code.certificateProcessor.pojo.dto.request.CursorPageRequest;
import org.com.code.certificateProcessor.pojo.dto.response.CursorPageResponse;
import org.com.code.certificateProcessor.pojo.dto.response.adminResponse.AdminInfoResponse;
import org.com.code.certificateProcessor.pojo.dto.response.adminResponse.AdminSignInResponse;
import org.com.code.certificateProcessor.pojo.entity.Admin;
import org.com.code.certificateProcessor.pojo.enums.Auth;
import org.com.code.certificateProcessor.pojo.structMap.AdminStructMap;
import org.com.code.certificateProcessor.security.CustomAuthenticationToken;
import org.com.code.certificateProcessor.service.BaseCursorPageService;
import org.com.code.certificateProcessor.service.JWTService;
import org.com.code.certificateProcessor.service.admin.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminImpl extends BaseCursorPageService<Admin> implements AdminService {
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JWTService jwtService;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    AdminStructMap adminStructMap;

    @Override
    @Transactional
    public Admin addAdmin(Admin admin) {
        try {

            admin.setPassword(bCryptPasswordEncoder.encode(admin.getPassword()));
            admin.setAuth(Auth.ADMIN.getName());
            int rowAffected = adminMapper.addAdmin(admin);
            if (rowAffected != 1) {
                throw new AdminTableException("添加管理员失败");
            }
            return admin;

        }catch (AdminTableException e){
            throw e;
        }catch (Exception e){
            throw new AdminTableException("添加管理员失败",e);
        }
    }

    @Override
    public AdminSignInResponse adminSignIn(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new CustomAuthenticationToken(username, password, Auth.ADMIN.getName())
        );
        String token = jwtService.getJwtToken(username, authentication.getAuthorities().toArray()[0].toString());

        return new AdminSignInResponse(username, authentication.getAuthorities().toArray()[0].toString(), token);
    }

    @Override
    public Admin getAdminByUserName(String username) {
        try {
            return adminMapper.getAdminByUserName(username);
        } catch (Exception e) {
            throw new ResourceNotFoundException("管理员信息不存在");
        }
    }

    @Override
    @Transactional
    public void updateAdminInfo(Admin admin) {
        try {
            admin.setUsername(SecurityContextHolder.getContext().getAuthentication().getName());
            if (admin.getPassword() != null && !admin.getPassword().isEmpty())
                admin.setPassword(bCryptPasswordEncoder.encode(admin.getPassword()));
            adminMapper.updateAdminInfo(admin);
        } catch (Exception e) {
            throw new AdminTableException("数据库异常，更新管理员信息失败", e);
        }
    }

    @Override
    public CursorPageResponse<AdminInfoResponse> cursorQueryAdmin(CursorPageRequest cursorPageRequest) {
        String lastStrId = cursorPageRequest.getLastId();
        int pageSize = cursorPageRequest.getPageSize();
        try {
            CursorPageResponse<Admin> adminList;
            if(pageSize < 0){
                adminList =  fetchPage(lastStrId, - pageSize, adminMapper::getPreviousAdmin, Admin::getUsername);
            }else{
                adminList =  fetchPage(lastStrId, pageSize, adminMapper::getLatterAdmin, Admin::getUsername);
            }

            List<AdminInfoResponse> adminInfoResponses = adminStructMap.toAdminInfoResponseList(adminList.getList());
            return new CursorPageResponse<>(
                    adminInfoResponses,
                    adminList.getMinId(),
                    adminList.getMaxId(),
                    adminList.getHasNext()
            );

        } catch (Exception e) {
            throw new AdminTableException("数据库异常，获取管理员列表信息失败", e);
        }
    }

     @Override
    @Transactional
    public void updateAdminAuth(String username,String auth) {
        try {
            int rowAffected = adminMapper.updateAdminAuth(username, auth);
            if (rowAffected != 1) {
                throw new AdminTableException("数据库异常，更新管理员权限失败");
            }
        }catch (AdminTableException e){
            throw e;
        }catch (Exception e){
            throw new AdminTableException("数据库异常，更新管理员权限失败",e);
        }
    }
}
