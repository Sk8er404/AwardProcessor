package org.com.code.certificateProcessor.security.authenticationProvider;

import org.com.code.certificateProcessor.exeption.CredentialsException;
import org.com.code.certificateProcessor.exeption.UnauthorizedException;
import org.com.code.certificateProcessor.pojo.enums.Auth;
import org.com.code.certificateProcessor.security.CustomAuthenticationToken;
import org.com.code.certificateProcessor.security.userDetailService.AdminUserDetailsService;
import org.com.code.certificateProcessor.security.userDetailService.StudentUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
    @Autowired
    AdminUserDetailsService adminUserDetailsService;
    @Autowired
    StudentUserDetailsService studentUserDetailsService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        try {
            // 1. 将 authentication 转为我们的自定义 Token
            CustomAuthenticationToken token = (CustomAuthenticationToken) authentication;

            /**
             * 背景:
             * 在 Spring Security 的认证流程中，Authentication 对象保存了用户的登录信息，其中最关键的属性是：
             * principal（认证主体）
             * credentials（凭证）
             *
             * 在自定义的 CustomAuthenticationProvider 中，需要从 CustomAuthenticationToken 里取出用户名进行验证，因此就出现了：
             * token.getName()  vs  token.getPrincipal().toString()
             *
             * 因为认证前后，principal 的类型会变化：
             * 认证前：是用户名（String）
             * 认证后：是完整的用户对象（UserDetails）
             *
             * 如果直接用 getPrincipal().toString()，在认证后会变成打印整个对象信息而不是用户名。
             * 而 getName() 会自动根据情况取出用户名（认证前是字符串，认证后会调用 UserDetails.getUsername()）。
             */
            String username = token.getName();
            String password = token.getCredentials().toString();
            String signInType = token.getSignInType();


            // 2. 根据 loginType 选择 UserDetailsService
            UserDetailsService userDetailsService;
            if (Auth.ADMIN.getName().equals(signInType)) {
                userDetailsService = adminUserDetailsService;
            } else if (Auth.STUDENT.getName().equals(signInType)) {
                userDetailsService = studentUserDetailsService;
            } else {
                throw new CredentialsException("不合理的登录类型");
            }

            // 3. 加载用户信息
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 4. 检查密码
            if (!passwordEncoder.matches(password, userDetails.getPassword())) {
                throw new UnauthorizedException("认证失败");
            }

            // 5. 认证成功，返回一个 *已认证* 的新 Token
            return new CustomAuthenticationToken(
                    userDetails, // principal
                    null,        // credentials (清除密码)
                    userDetails.getAuthorities(), // 权限
                    signInType
            );
        }catch (CredentialsException | UnauthorizedException e){
            throw e;
        }catch (Exception e){
            throw new UnauthorizedException("认证失败",e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication){
        return CustomAuthenticationToken.class.isAssignableFrom(authentication);
    }
    /**
     * supports() 方法告诉 Spring Security:
     * 如果传入的 authentication 类型是 CustomAuthenticationToken（或它的子类），返回 true
     * 其他类型则返回 false
     * “只要是 CustomAuthenticationToken（或其子类），我 CustomAuthenticationProvider 能处理。”
     *
     * Spring Security 里有很多种 Authentication：
     *
     * UsernamePasswordAuthenticationToken（默认用户名密码登录）
     * JwtAuthenticationToken（JWT 登录）
     * 自定义的 CustomAuthenticationToken
     *
     * 假设系统中有多个 Provider：
     *
     * DaoAuthenticationProvider
     * JwtAuthenticationProvider
     * CustomAuthenticationProvider
     *
     *
     * Spring Security 会在认证时依次调用它们：
     *
     * for (AuthenticationProvider provider : providers) {
     *     if (provider.supports(authentication.getClass())) {
     *         provider.authenticate(authentication);
     *         ...
     *     }
     * }
     *
     *
     * 所以，如果不重写 supports()，Spring 就不知道你的 Provider 该不该接管这个 Token。
     *
     * 如果不写（或写错）：
     * Spring Security 可能不会调用你的 CustomAuthenticationProvider
     * 报错：No AuthenticationProvider found for ...
     * 或被错误的 Provider 接管导致认证失败
     *
     */

}
