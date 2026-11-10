package com.guat.cloud.report.service;

import com.guat.cloud.report.dto.PageBean;
import com.guat.cloud.report.dto.ReportHandleResult;
import com.guat.cloud.report.dto.ReportVO;
import com.guat.cloud.report.dto.Result;
import com.guat.cloud.report.entity.Report;

public interface AdminReportService {

    /** 举报列表（管理端），status 可空=全部：0待审核 1已下架 2已驳回 */
    PageBean<ReportVO> listReports(Integer status, int pageNum, int pageSize);

    /** 审核举报：action 1通过(下架目标) 2驳回 */
    ReportHandleResult handleReport(Integer adminId, Long reportId, Integer action, String remark);
}
