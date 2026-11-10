package com.guat.cloud.content.dto;

import com.guat.cloud.content.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 对外返回的用户信息（不含密码）
 */
@Schema(description = "用户对外信息（不含密码），编辑个人资料时作为请求体")
@Data
@NoArgsConstructor
public class UserInfo {

    private Integer id;// id
    private String username;// 用户名
    private String userAccount;// 账号
    private String phone;// 手机号
    private String avatar;//头像url
    private String cover;// 主页封面图url
    private String bio;// 个人简介
    private String location;// 所在地
    @Schema(description = "生日（格式 yyyy-MM-dd）", example = "2000-01-01")
    private LocalDate birthday;// 生日
    private Integer totalLikeCount;// 获赞数
    private Integer followCount;// 关注数
    private Integer fanCount;// 粉丝数
    private Integer role; // 身份

    public static UserInfo from(User user) {
        if (user == null) {
            return null;
        }
        UserInfo info = new UserInfo();
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