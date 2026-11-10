package com.guat.cloud.report.controller;

import com.guat.cloud.report.dto.PageBean;
import com.guat.cloud.report.dto.ReportHandleResult;
import com.guat.cloud.report.dto.ReportVO;
import com.guat.cloud.report.dto.Result;
import com.guat.cloud.report.service.AdminReportService;
import com.guat.cloud.report.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端-举报审核接口（仅 role=1 管理员可用）
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "管理员处理举报接口", description = "包含管理员获取举报列表接口、举报审核接口")
@SecurityRequirement(name = "BearerAuth")
public class AdminReportController {
    @Autowired
    private AdminReportService adminReportService;

    // 举报列表（需管理员）：status 可空=全部 0待审核 1已下架 2已驳回
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"list": [
    //			{
    //				"reportId": 1,
    //				"reportType": 1,
    //				"targetId": 11,
    //				"reasonType": "色情低俗",
    //				"remark": "该帖子涉及色情低俗内容，请审核员重新审核",
    //				"status": 2,
    //				"createTime": "2026-09-14T11:31:46",
    //				"targetTitle": "今天遇到一件开心的事情",
    //				"targetContent": "这是内容：Content-Type未手动设置，浏览器自动生成multipart/form-data和后端@RequestPart匹配",
    //				"reporterName": "庞媛媛",
    //				"targetUserName": "张三xxx"
    //			}
    //		],
    //		"total": 1,
    //		"pageNum": 1,
    //		"pageSize": 10
    //	}
    //}
    @Operation(summary = "举报列表", description = "需要管理员身份，传入 status")
    @Parameters({
            @Parameter(name = "status", description = "举报的状态：空=全部 0待审核 1已下架 2已驳回", required = false, example = "0"),
            @Parameter(name = "pageNum", description = "页码（第几页）", example = "1"),
            @Parameter(name = "pageSize", description = "一页的记录数", example = "10")
    })
    @GetMapping("/reports")
    public Result<PageBean<ReportVO>> reports(@RequestParam(required = false) Integer status,
                                              @RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              HttpServletRequest request) {
        UserContext.requireAdmin(request);
        return Result.success(adminReportService.listReports(status, pageNum, pageSize));
    }


    //审核举报（需管理员）：action 1通过(下架目标) 2驳回
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"reportId": 1,
    //		"reportType": 1,
    //		"targetId": 11,
    //		"action": 2,
    //		"newStatus": 2,
    //		"targetStatus": null,
    //		"message": "举报已驳回，内容未处理"
    //	}
    //}
    @Operation(summary = "举报审核", description = "需要管理员身份，传入 reportId、action、remark")
    @Parameters({
            @Parameter(name = "reportId", description = "举报的id", example = "1"),
            @Parameter(name = "action", description = "管理员处理行为：1通过，2驳回", example = "1"),
            @Parameter(name = "remark", description = "备注", required = false, example = "我也不知道，或许我应该问AI")
    })
    @PostMapping("/report/handle")
    public Result<ReportHandleResult> handle(@RequestParam Long reportId,
                                             @RequestParam Integer action,
                                             @RequestParam(required = false) String remark,
                                             HttpServletRequest request) {
        UserContext.requireAdmin(request);
        return Result.success(adminReportService.handleReport(
                UserContext.requireUserId(request), reportId, action, remark));
    }
}
