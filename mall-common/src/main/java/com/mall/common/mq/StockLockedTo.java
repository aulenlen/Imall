package com.mall.common.mq;

import lombok.Data;


@Data
public class StockLockedTo {
    private Long id;
    private TaskDetailTo detail;
}
