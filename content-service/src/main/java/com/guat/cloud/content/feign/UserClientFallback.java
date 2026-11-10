package com.guat.cloud.content.feign;

import com.guat.cloud.content.dto.Result;
import com.guat.cloud.content.dto.UserInfo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserClientFallback implements UserClient {

    @Override
    public Result<UserInfo> getUserInfo(Long id) {
        return null;
    }

    @Override
    public Result<List<UserInfo>> batchGetUserInfo(List<Long> ids) {
        return null;
    }

    @Override
    public String testSlow() {
        return "【降级兜底】user-service 暂时不可用，这是兜底数据";
    }

    // ⚠️ UserClient 接口里其他方法也要全部实现（ implements 强制要求）
    // 其他方法暂时返回 null 或空对象就行，例如：
    // @Override
    // public UserInfo getUserInfo(Integer id) { return null; }
}
