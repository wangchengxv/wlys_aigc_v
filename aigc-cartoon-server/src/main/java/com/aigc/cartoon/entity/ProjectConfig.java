package com.aigc.cartoon.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_project_config")
public class ProjectConfig {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long projectId;
    
    private String dialogModel;
    
    private String imageModel;
    
    private String videoModel;
    
    private String audioModel;
    
    private String script;
    
    private Integer episodeCount;
    
    private String batchModel;
    
    private String batchRatio;
    
    private String batchQuality;
    
    private String batchMethod;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}