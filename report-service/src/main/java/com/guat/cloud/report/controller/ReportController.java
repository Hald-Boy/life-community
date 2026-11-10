package com.guat.cloud.report.controller;

import com.guat.cloud.report.dto.Result;
import com.guat.cloud.report.entity.Report;
import com.guat.cloud.report.service.ReportService;
import com.guat.cloud.report.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 举报接口（帖子 + 评论）
 */
@Tag(name = "举报接口", description = "举报帖子或评论")
@RestController
//@RequestMapping("/api/web/report")
@SecurityRequirement(name = "BearerAuth")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /** 提交举报（需登录），reportType 1帖子 2评论 */
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": null
    //}
    @Operation(summary = "提交举报", description = "reportType：1帖子 2评论；targetId 被举报对象ID、reasonType 举报分类、remark 补充描述")
    @PostMapping("/api/web/report/submit")
    public Result<String> submit(HttpServletRequest request,
                               @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                       description = "举报信息（JSON）：reportType、targetId、reasonType、remark", required = true)
                               @RequestBody Report report) {
        reportService.submitReport(UserContext.requireUserId(request), report);
        return Result.success("举报成功");
    }
}
