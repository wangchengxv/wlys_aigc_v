package com.aigc.cartoon.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_project")
public class Project {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String description;
    
    private String coverImage;
    
    private Long userId;
    
    private String status;
    
    private String style;
    
    private String styleTemplateId;
    
    private String visualStylePrompt;
    
    private String visualStyleMode;
    
    private Boolean visualStyleLongTextMode;
    
    private String customStyleText;
    
    private String aspectRatio;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
