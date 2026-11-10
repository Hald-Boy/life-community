package com.guat.cloud.user.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

///**
// * 分页查询结果封装
// */
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class PageBean<T> {
//    private Long total; //总记录数
//    private List<T> rows;  //数据列表
//}

/**
 * 通用分页结果
 */
@Data
@NoArgsConstructor
public class PageBean<T> {

    private List<T> list;
    private long total;
    private int pageNum;
    private int pageSize;

    public PageBean(List<T> list, long total, int pageNum, int pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}