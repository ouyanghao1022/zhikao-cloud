package com.zhikao.common;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结果封装（兼容MyBatis-Plus和前端组件）
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 记录列表 */
    private List<T> records;

    /** 总记录数 */
    private Long total;

    /** 每页数量 */
    private Long size;

    /** 当前页 */
    private Long current;

    /** 总页数 */
    private Long pages;

    public PageResult() {}

    public PageResult(List<T> records, Long total, Long size, Long current, Long pages) {
        this.records = records;
        this.total = total;
        this.size = size;
        this.current = current;
        this.pages = pages;
    }

    public static <T> PageResult<T> of(List<T> records, Long total, Long size, Long current) {
        long pages = total == 0 ? 0 : (total + size - 1) / size;
        return new PageResult<>(records, total, size, current, pages);
    }
}
