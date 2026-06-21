package com.ecommerceserver.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetFileVO {
    private Long id;
    private String name;
    private String fileType;
    private Long fileSize;
    private Long datasetId;
    private Integer disabled;
    private Date createdAt;
    private Integer hitCount;
}
