package com.aigc.cartoon.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_storyboard")
public class Storyboard {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long scriptId;
    
    private Integer sceneNumber;
    
    private String sceneDescription;
    
    private Integer duration;
    
    private String bgPrompt;
    
    private String characterPrompt;
    
    private String audioUrl;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}