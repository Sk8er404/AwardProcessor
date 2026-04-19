package org.com.code.certificateProcessor.security;

import org.com.code.certificateProcessor.mapper.AdminMapper;
import org.com.code.certificateProcessor.mapper.StudentMapper;
import org.com.code.certificateProcessor.pojo.enums.Auth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception{
        AuthenticationManager authenticationManager = authenticationConfiguration.getAuthenticationManager();
        return authenticationManager;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .formLogin(form -> form.disable())//禁用默认登录页面
                .logout(config->config.disable())//禁用默认登出页面
                /**
                 * 禁用 HTTP Basic 认证。
                 *
                 * 背景解释：
                 * HTTP Basic 是一种非常简单的认证方式,客户端每次请求时，都在 Header 里带上
                 *
                 * Authorization: Basic base64(username:password)
                 *
                 * Spring Security 默认是支持的。但这种方式有几个问题：
                 *
                 * 明文用户名和密码（即使 base64 也不是加密，只是编码）；
                 *
                 * 不适合前后端分离；
                 * 不支持 token；
                 * 每次请求都传用户名密码，不安全。
                 *
                 * 所以：
                 * 禁用它后，Spring Security 就不会再尝试使用这种方式认证请求。
                 * 这通常用于要改用 JWT 或 自定义 Token 认证的项目。
                 */
                .httpBasic(httpBasic -> httpBasic.disable())
                /**
                 * 设置 Session 策略为 无状态（STATELESS）。
                 *
                 * 背景解释：
                 * Spring Security 默认是基于 Session 的,登录成功后，Spring 会创建一个 Session,之后的请求只要带上 Session ID（Cookie），就不用再登录。
                 *
                 * 但如果你做的是：
                 * 前后端分离；
                 * RESTful API；
                 * JWT 或 token 认证；
                 *
                 * 那你通常希望：
                 * 每次请求都独立验证（即每个请求都带 token）；
                 * 不依赖服务器保存 Session；
                 * 更容易做负载均衡。
                 *
                 * 所以：
                 *
                 * SessionCreationPolicy.STATELESS 告诉 Spring Security,不要创建、使用或存储任何 HTTP Session。
                 * 也就是说：
                 * 不会自动登录；
                 * 不会有 Session 缓存；
                 * 每个请求都要重新验证（比如通过 JWT）。
                 */
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable()) // 禁用 CSRF 保护（仅在开发环境中使用，生产环境应启用）
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/student/signUp").permitAll()
                        .requestMatchers("/api/student/signIn").permitAll() // 确保登录页面可访问
                        .requestMatchers("/api/admin/signIn").permitAll()
                        .requestMatchers("/api/standardAward/cursorGetBatchByAdmin",
                                "/api/standardAward/createBatch",
                                "/api/standardAward/updateBatch",
                                "/api/standardAward/deleteBatch",
                                "/api/admin/me",
                                "/api/admin/getSubmissionProgress",
                                "/api/admin/getStudentSubmissionByStudentId",
                                "/api/admin/reviewSubmission",
                                "/api/admin/getStudentInfo",
                                "/api/admin/updateInfo").hasAnyAuthority(Auth.ADMIN.getName(),Auth.SUPER_ADMIN.getName())
                        .requestMatchers(
                                "/api/admin/signUp",// 只有超级管理员才能注册新的管理员账号
                                "/api/admin/updateAuth",
                                "/api/admin/cursorQueryAdmin").hasAuthority(Auth.SUPER_ADMIN.getName())
                        .requestMatchers("/api/standardAward/cursorGetBatchByStudent").hasAnyAuthority(Auth.STUDENT.getName())
                        .anyRequest().authenticated() // 其他所有请求都需要认证
                )
                //添加JWT认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
