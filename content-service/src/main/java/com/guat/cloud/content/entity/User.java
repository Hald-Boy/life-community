package com.guat.cloud.content.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户实体（登录/注册时作为请求体，查询时返回；password 仅请求时使用）")
public class User {
    private Integer id;                     //用户id
    private String username;                //用户名
    private String password;                //BCrypt加密密码
    @Schema(description = "身份：0普通用户 1管理员", example = "0")
    private int role;                       //用户的身份（0代表用户，1代表管理员，默认是0）
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public LocalDateTime createTime;
    private String userAccount;             // 8位唯一账号，不可修改
    private String phone;                   // 手机号，唯一
    private String avatar;                  // 头像URL
    private String cover;                   // 封面图URL
    private String bio;                     // 个人简介
    private LocalDate birthday;             // 生日
    private String location;                // 所在地
    private Integer totalLikeCount;         // 获赞总数
    private Integer followCount;            // 关注数
    private Integer fanCount;               // 粉丝数
    @Schema(description = "账号状态：0正常 1封禁", example = "0")
    private Integer status;                 //账号的状态 0正常 1封禁
    private Integer isDeleted;              // 逻辑删除 0未删除 1已删除
    private LocalDateTime updateTime;

    public static User from(User user) {
        if (user == null) {
            return null;
        }
        User info = new User();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setUserAccount(user.getUserAccount());
        info.setPhone(user.getPhone());
        info.setAvatar(user.getAvatar());
        info.setCover(user.getCover());
        info.setBio(user.getBio());
        info.setLocation(user.getLocation());
        info.setBirthday(user.getBirthday());
        info.setTotalLikeCount(user.getTotalLikeCount());
        info.setFollowCount(user.getFollowCount());
        info.setFanCount(user.getFanCount());
        info.setRole(user.getRole());
        return info;
    }
}
