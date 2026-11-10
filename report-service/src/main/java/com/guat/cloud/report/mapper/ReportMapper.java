package com.guat.cloud.report.mapper;

import com.guat.cloud.report.dto.ReportVO;
import com.guat.cloud.report.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ReportMapper {
    //查询举报记录
    Report findByUserAndTarget(@Param("userId") Long userId,
                               @Param("reportType") Integer reportType,
                               @Param("targetId") Long targetId);
    // 插入举报记录
    int insert(Report report);

    Report findById(@Param("id") Long id);

    /** 更新审核状态：1 审核通过(下架) 2 驳回 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 举报总数（管理端），status 可空=全部 */
    long countReports(@Param("status") Integer status);

    /** 举报分页列表（管理端），带目标内容/作者信息 */
    List<ReportVO> listReports(@Param("status") Integer status,
                               @Param("offset") int offset,
                               @Param("limit") int limit);
}
