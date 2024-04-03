package com.shimh.config;

/**
 * @ClassNAME XssHttpServletRequestWrapper
 * @Description TODO
 * @Author jiayongchao
 * @Date 2024/3/31 22:54
 * @Version 1.0
 */

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONUtil;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HttpServletRequest的包装类，用于防止XSS攻击
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }


    /**
     * 重写getParameter方法，对参数值进行HTML标签清理
     */
    @Override
    public String getParameter(String name) {
        String value= super.getParameter(name);
        if(!StrUtil.hasEmpty(value)){
            value=HtmlUtil.cleanHtmlTag(value);
        }
        return value;
    }

    /**
     * 重写getParameterValues方法，对参数值数组进行HTML标签清理
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] values= super.getParameterValues(name);
        if(values!=null){
            for (int i=0;i<values.length;i++){
                String value=values[i];
                if(!StrUtil.hasEmpty(value)){
                    value=HtmlUtil.cleanHtmlTag(value);
                }
                values[i]=value;
            }
        }
        return values;
    }

    /**
     * 重写getParameterMap方法，对参数值映射进行HTML标签清理
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameters = super.getParameterMap();
        LinkedHashMap<String, String[]> map=new LinkedHashMap();
        if(parameters!=null){
            for (String key:parameters.keySet()){
                String[] values=parameters.get(key);
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    if (!StrUtil.hasEmpty(value)) {
                        value = HtmlUtil.cleanHtmlTag(value);
                    }
                    values[i] = value;
                }
                map.put(key,values);
            }
        }
        return map;
    }

    /**
     * 重写getHeader方法，对请求头进行HTML标签清理
     */
    @Override
    public String getHeader(String name) {
        String value= super.getHeader(name);
        if (!StrUtil.hasEmpty(value)) {
            value = HtmlUtil.cleanHtmlTag(value);
        }
        return value;
    }

    /**
     * 重写getInputStream方法，对请求体进行HTML标签清理
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        InputStream in= super.getInputStream();
        InputStreamReader reader=new InputStreamReader(in, Charset.forName("UTF-8"));
        BufferedReader buffer=new BufferedReader(reader);
        StringBuffer body=new StringBuffer();
        String line=buffer.readLine();
        while(line!=null){
            body.append(line);
            line=buffer.readLine();
        }
        buffer.close();
        reader.close();
        in.close();
        Map<String,Object> map=JSONUtil.parseObj(body.toString());
        Map<String,Object> result=new LinkedHashMap<>();
        for(String key:map.keySet()){
            Object val=map.get(key);
            if(val instanceof String){
                if(!StrUtil.hasEmpty(val.toString())){
                    result.put(key, HtmlUtil.cleanHtmlTag(val.toString()));
                }
            }
            else {
                result.put(key,val);
            }
        }
        String json= JSONUtil.toJsonStr(result);
        ByteArrayInputStream bain=new ByteArrayInputStream(json.getBytes());
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return bain.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }
        };
    }
}

