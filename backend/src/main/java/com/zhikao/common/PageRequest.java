package com.zhikao.common;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * 分页请求参数封装
 */
@Data
public class PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前页（从1开始） */
    private Long current = 1L;

    /** 每页数量 */
    private Long size = 10L;

    public long getOffset() {
        return (current - 1) * size;
    }
}
