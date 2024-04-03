package com.shimh.controller;

import javax.servlet.http.HttpServletRequest;

import com.shimh.common.annotation.LogAnnotation;
import com.shimh.utils.RSAUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.shimh.common.constant.Base;
import com.shimh.common.constant.ResultCode;
import com.shimh.common.result.Result;
import com.shimh.entity.User;
import com.shimh.oauth.OAuthSessionManager;
import com.shimh.service.UserService;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 登录
 *
 * @author shimh
 * <p>
 * 2018年1月23日
 */
@RestController
public class LoginController {
    private static final String PRIVATE_SECRET_KEY = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBALjQeWQUagvWibLj30HSPvkshLTN+R23T9oThc6EzgXfXq+TTPEVlMuCF8FgVpT9Y+E9s01GaowR4zVloI24fTkSS982uDbKNjK6FkZZ0bgNWfHvL1il5aMmxeaUOK8zC37JSXGjEIoCtzDzjoKoSo2v6tUPdpi+FP5Wdy4LJwEJAgMBAAECgYEAgsnuDjgwL/58Zh/DAEa0kAvEQlu9Xx06Il9MgzESx68ix+fbPIWETlzbSNtPipKjm2PZvucFQejqvQAVlhWob4TaQqMpwcH62GAf6Z7jiJ8FNtOyzfGJ/UykGkBuhk3jFdR+wsKoFJUUyAx/RiyqRUJYj98ki6ZaQcAYosSc0PECQQDvBjhdh/Ohgpj9Op4p8l0RX/mdPty7ew7XCjbRYi11n6nRAYg5VXoaHpddRvUPukJnXpm8Q4Q4iETI5s8NBh4VAkEAxfClIosvFcx0NvbQxOvQ8CSdySmLJ18Fmyy7PqWb36WgCbFClm4xY/MAZAEB01A0baLKKpxPQ2g9IARegAQIJQJACxGzrIkuPC2LHjcHuhOSQcq7CZAusrP5NPYxIbM1Pbw+JgK3J0iRFgKSqewuTyMmDhlwbyqFRgTxgohF0GmXHQJBAMULx8h4mDnyG6rvz2qJeqjlOrIcCiv6eyE7yXcW8/IS9htP/AK21bIzIStsmT2cdWTDDtCWZI2tAlSSJOT5noUCQCDJ1LWOw25MIvURFZFPooszOsljhd8oo+8BShWWkkAxtBSxm/+BdLssZKQgrPnGD9X2a4IeQBDYM42aQ70zdVk=";
    private static final String PUBLIC_SECRET_KEY  = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC40HlkFGoL1omy499B0j75LIS0zfkdt0/aE4XOhM4F316vk0zxFZTLghfBYFaU/WPhPbNNRmqMEeM1ZaCNuH05EkvfNrg2yjYyuhZGWdG4DVnx7y9YpeWjJsXmlDivMwt+yUlxoxCKArcw846CqEqNr+rVD3aYvhT+VncuCycBCQIDAQAB";
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    @LogAnnotation(module = "登录", operation = "登录")
    public Result login(@RequestBody User user) {
        Result r = new Result();
        executeLogin(user.getAccount(), user.getPassword(),null, r);
        return r;
    }

    @PostMapping("/register")
    //@RequiresRoles(Base.ROLE_ADMIN)
    @LogAnnotation(module = "注册", operation = "注册")
    public Result register(@RequestBody User user) {

        Result r = new Result();

        User temp = userService.getUserByAccount(user.getAccount());
        if (null != temp) {
            r.setResultCode(ResultCode.USER_HAS_EXISTED);
            return r;
        }

        String account = user.getAccount();
        String password = user.getPassword();

        Long userId = userService.saveUser(user);

        if (userId > 0) {
            executeLogin(account, null, password,r);
        } else {
            r.setResultCode(ResultCode.USER_Register_ERROR);
        }
        return r;
    }


    private void executeLogin(String account, String enpassword,String password, Result r) {
        //密码加密

        try {
            Subject subject = SecurityUtils.getSubject();
            String originalPassword="";
            if(enpassword!=null){
                // 解密
                originalPassword= RSAUtils.decode(password,this.PRIVATE_SECRET_KEY);
            }else{
                originalPassword = password;
            }


            UsernamePasswordToken token = new UsernamePasswordToken(account, originalPassword);
            subject.login(token);

            User currentUser = userService.getUserByAccount(account);
            subject.getSession().setAttribute(Base.CURRENT_USER, currentUser);

            r.setResultCode(ResultCode.SUCCESS);
            r.simple().put(OAuthSessionManager.OAUTH_TOKEN, subject.getSession().getId());
        } catch (UnknownAccountException e) {
            r.setResultCode(ResultCode.USER_NOT_EXIST);
        } catch (LockedAccountException e) {
            r.setResultCode(ResultCode.USER_ACCOUNT_FORBIDDEN);
        } catch (AuthenticationException e) {
            r.setResultCode(ResultCode.USER_LOGIN_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            r.setResultCode(ResultCode.ERROR);
        }

    }

    @RequestMapping(value = "/handleLogin")
    public Result handleLogin(HttpServletRequest request) {
        String id = request.getHeader(OAuthSessionManager.OAUTH_TOKEN);
        System.out.println("超时登录。。。:" + id);
        return Result.error(ResultCode.SESSION_TIME_OUT);
    }


    @GetMapping("/logout")
    @LogAnnotation(module = "退出", operation = "退出")
    public Result logout() {

        Result r = new Result();
        Subject subject = SecurityUtils.getSubject();
        subject.logout();

        r.setResultCode(ResultCode.SUCCESS);
        return r;
    }
}