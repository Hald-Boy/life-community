package com.guat.cloud.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data                   //生成set/get方法和toString方法的注解
@NoArgsConstructor      //生成无参构造的方法
@AllArgsConstructor     //生成全参构造的方法
public class Result<T> {

    public static final int CODE_SUCCESS = 200;       // 成功
    public static final int CODE_BAD_REQUEST = 400;   // 参数/业务错误
    public static final int CODE_UNAUTHORIZED = 401;  // 未登录
    public static final int CODE_ERROR = 500;         // 服务器异常

    private Integer code;//响应码，1 代表成功; 0 代表失败
    private String msg;  //响应信息 描述字符串
    private T data; //返回的数据

    public static <T> Result<T> success() {return new Result<>(CODE_SUCCESS, "success", null);}
    public static <T> Result<T> success(T data) {return new Result<>(CODE_SUCCESS, "success", data);}


    public static <T> Result<T> error(String msg) {
        return new Result<>(CODE_BAD_REQUEST, msg, null);
    }
    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}