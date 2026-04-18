SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'script_project' AND COLUMN_NAME = 'owner_id'
        ),
        'SELECT 1',
        'ALTER TABLE script_project ADD COLUMN owner_id VARCHAR(64) NULL AFTER project_id'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'script_project' AND COLUMN_NAME = 'owner_name'
        ),
        'SELECT 1',
        'ALTER TABLE script_project ADD COLUMN owner_name VARCHAR(128) NULL AFTER owner_id'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'script_project' AND COLUMN_NAME = 'org_unit_id'
        ),
        'SELECT 1',
        'ALTER TABLE script_project ADD COLUMN org_unit_id VARCHAR(64) NULL AFTER owner_name'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'script_project' AND COLUMN_NAME = 'course_id'
        ),
        'SELECT 1',
        'ALTER TABLE script_project ADD COLUMN course_id VARCHAR(64) NULL AFTER org_unit_id'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'script_project' AND COLUMN_NAME = 'style_template_id'
        ),
        'SELECT 1',
        'ALTER TABLE script_project ADD COLUMN style_template_id VARCHAR(64) NULL AFTER visual_style'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'script_project' AND INDEX_NAME = 'idx_script_project_owner_id'
        ),
        'SELECT 1',
        'CREATE INDEX idx_script_project_owner_id ON script_project (owner_id)'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'script_project' AND INDEX_NAME = 'idx_script_project_course_id'
        ),
        'SELECT 1',
        'CREATE INDEX idx_script_project_course_id ON script_project (course_id)'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'script_project' AND INDEX_NAME = 'idx_script_project_style_template_id'
        ),
        'SELECT 1',
        'CREATE INDEX idx_script_project_style_template_id ON script_project (style_template_id)'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS style_template (
    template_id VARCHAR(64) PRIMARY KEY,
    scope VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(128) NULL,
    traits TEXT NULL,
    full_prompt LONGTEXT NULL,
    style_key VARCHAR(128) NULL,
    owner_id VARCHAR(64) NULL,
    owner_name VARCHAR(128) NULL,
    org_unit_id VARCHAR(64) NULL,
    course_id VARCHAR(64) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_style_template_scope (scope),
    INDEX idx_style_template_owner (owner_id),
    INDEX idx_style_template_course (course_id),
    INDEX idx_style_template_updated (updated_at)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    actor_user_id VARCHAR(64) NULL,
    actor_user_name VARCHAR(128) NULL,
    org_unit_id VARCHAR(64) NULL,
    course_id VARCHAR(64) NULL,
    details_json LONGTEXT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_log_entity (entity_type, entity_id),
    INDEX idx_audit_log_actor (actor_user_id),
    INDEX idx_audit_log_created (created_at)
);

INSERT INTO style_template (
    template_id,
    scope,
    name,
    category,
    traits,
    full_prompt,
    style_key,
    owner_id,
    owner_name,
    org_unit_id,
    course_id,
    enabled,
    created_at,
    updated_at
)
SELECT *
FROM (
    SELECT 'oriental-ink-wash' AS template_id, 'SYSTEM' AS scope, '水墨风' AS name, '国风 / 东方美学' AS category, '黑墨晕染、流动渐变、极简留白，充满东方写意意境' AS traits, '江南水乡夜景，乌篷船在江面缓缓划过，渔火点点，一轮明月挂在天边，桃花花瓣随流水飘落，黑墨在白底上晕染的水墨风格，流动的渐变，极简东方美学，镜头缓慢推进，柔光，8K高清，画面稳定无抖动，无模糊无畸形' AS full_prompt, 'ink-wash' AS style_key, NULL AS owner_id, NULL AS owner_name, NULL AS org_unit_id, NULL AS course_id, TRUE AS enabled, CURRENT_TIMESTAMP AS created_at, CURRENT_TIMESTAMP AS updated_at
    UNION ALL
    SELECT 'oriental-classic-elegance', 'SYSTEM', '古风雅韵', '国风 / 东方美学', '米黄 + 墨绿 + 赭石的传统色调，中式古典场景，温婉雅致', '25岁国风白衣女子在亭中品茶，桃花花瓣缓缓飘落，古风庭院，水墨意境，柔光慢镜，中式对称构图，无现代元素，米黄+墨绿+赭石的古风色调，中景慢推，1080P高清，画面稳定，无畸形无模糊', 'ancient-chinese', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'oriental-cyber-dunhuang', 'SYSTEM', '赛博敦煌', '国风 / 东方美学', '传统敦煌元素与赛博霓虹的融合，复古与未来的碰撞', '赛博敦煌风格，飞天神女在霓虹光效中飞舞，反弹琵琶，全息敦煌壁画在背景闪烁，neon apsara，紫蓝霓虹灯光，传统敦煌配色与未来科技感结合，镜头环绕人物旋转，4K高清，无穿模', 'cyber-dunhuang', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'scifi-cyberpunk', 'SYSTEM', '赛博朋克', '科幻 / 未来', '紫蓝 + 品红的霓虹色调、高对比、全息元素，都市末世感', '雨夜东京街头，全息广告牌闪烁''NEON DREAM''，穿机甲风夹克的年轻人抬头，瞳孔反射出蓝紫光斑，雨水在金属肩甲上滑落拉出细线，霓虹灯浸染，高对比，全息元素，都市末世感，镜头从远景缓慢推进到人物特写，冷色调，4K高清，无模糊', 'cyberpunk', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'scifi-futurism', 'SYSTEM', '未来主义', '科幻 / 未来', '极简科技感、几何线条、冷调金属质感，未来极简美学', '未来主义风格，纯白的未来太空舱内，宇航员操作着透明全息屏幕，几何线条的舱体设计，冷调金属质感，柔和的环境光，镜头从操作台缓慢拉远到全景，展现整个太空舱的宏大结构，8K高清，电影级质感', 'futurism', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'retro-hk', 'SYSTEM', '复古港风', '复古怀旧', '红橙 + 青蓝的对比色调、80 年代香港街头氛围，胶片颗粒感', '夜晚港式茶餐厅，穿花衬衫的年轻男女坐在窗边对话，窗外霓虹招牌闪烁，雨水打在玻璃上留下水痕，红橙与青蓝对比色调，复古港风，柯达5219胶片颗粒感，柔光，中景镜头，缓慢推进，暖色调，9:16竖屏，画面稳定', 'retro-hk', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'retro-film', 'SYSTEM', '复古胶片', '复古怀旧', '胶片颗粒、略微减饱和、暖色调、柔焦边缘，怀旧生活感', '夏日午后的乡村小路，女孩骑着自行车穿过麦田，风吹起她的裙摆，阳光透过树叶洒下斑驳的光影，复古胶片风格，胶片颗粒感，略微减饱和，暖色调，柔焦边缘，跟拍镜头，慢动作，怀旧氛围，16:9横屏', 'retro-film', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'retro-8mm', 'SYSTEM', '8mm 家庭录像', '复古怀旧', '重颗粒、漏光、不稳定画面、褪色，老家庭录像的复古感', '8mm家庭录像风格，90年代的家庭生日派对，孩子们围着蛋糕吹蜡烛，大人在旁边笑着拍照，重颗粒感，轻微漏光，画面轻微晃动，褪色的暖色调，回忆感，中景固定镜头，无多余修饰', 'retro-8mm', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'anime-cel', 'SYSTEM', '二次元动漫', '动画 / 卡通', '赛璐珞风格、鲜艳色彩、夸张动态线条，日系动漫质感', '二次元动画风格，魔法阵在角色脚下旋转展开，符文如弹幕环绕，蓄力到顶点就射出彩虹光束带星形尾迹，蓝发少女站在城市天台，晚风吹动她的校服裙摆，星星点点的光粒在她身边飞舞，赛璐珞风格，鲜艳色彩，夸张运动，动态线条，镜头环绕人物旋转，1080P高清，无穿模无畸形', 'anime', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'anime-pixel', 'SYSTEM', '像素风格', '动画 / 卡通', '8bit 复古像素块、低分辨率质感，怀旧游戏风', '在充满色彩的宇宙中，像素角色穿梭于形态各异、色调独特的星球之间。每个星球上都有奇异的地形和外星生物。近景特写镜头下，玩家角色站在画面中央，正在与一只友好的外星生物对话。画面上方有像素化的星际风暴和能量漩涡特效，缓缓旋转，带来动态感。整体风格复古而充满未来感，色彩鲜明跳跃，8bit像素风格，画面流畅', 'pixel-art', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'anime-puppet', 'SYSTEM', '木偶动画', '动画 / 卡通', '木偶质感、缓慢刻意的帧动画，复古定格动画感', '在昏暗的维多利亚风格客厅里，花边窗帘轻轻飘动。毛毡和木制木偶围坐在圆桌旁，摇曳的烛光映照出它们的身影。一声低语震颤了瓷茶杯，画上的眼睛不安地转动。每个缓慢而刻意的木偶动画帧都让场面更加紧张。相机缓缓向右平移，展示木偶们的每一个细微动作，增强了诡异的氛围，定格动画风格，4K高清', '2d-animation', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'anime-chibi', 'SYSTEM', 'Q 版卡通', '动画 / 卡通', '可爱的 Q 版人物、马卡龙色调，软萌治愈', 'Q版卡通人物，可爱的小猫拟人角色，在甜品店拿着冰淇淋蹦蹦跳跳，马卡龙色调，场景简洁，流畅动画，无穿模无畸形，暖光，中景镜头，画面稳定，治愈氛围，9:16竖屏', '2d-animation', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'film-cinematic', 'SYSTEM', '电影感', '影视 / 专业影视', '宽银幕、浅景深、专业色彩分级，电影级质感', '雪山日出，雪峰从云海中缓缓露出，第一缕阳光把山顶染成金色，山谷还在蓝影之中。镜头从云层缓慢升起，展现整个山脉的全景，广角镜头，史诗感，电影感色彩分级，2.39:1宽银幕，浅景深，8K高清，无抖动', 'live-action', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'film-documentary', 'SYSTEM', '纪录片', '影视 / 专业影视', '手持镜头感、自然光、观察性构图，真实纪实感', '纪录片风格，藏区的牧民在草原上放牧，牦牛在草地上吃草，阳光透过云层洒下，自然光，手持轻微晃动的镜头，观察性构图，真实的生活细节，中景跟拍，16:9横屏，无修饰', 'film-documentary', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'film-noir', 'SYSTEM', '黑色电影', '影视 / 专业影视', '高对比黑白、百叶窗阴影、低调照明，悬疑复古感', '黑色电影风格，昏暗的侦探办公室，侦探坐在办公桌前，百叶窗的阴影在他脸上切割出条纹，台灯的暖光照亮桌上的文件，高对比黑白，低调照明，悬疑氛围，特写镜头，缓慢推进，无多余色彩', 'film-noir', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'film-commercial', 'SYSTEM', '商业广告', '影视 / 专业影视', '干净明亮、精确布光、丝滑运镜，产品展示质感', '高端手表360°缓慢旋转展示，细节特写，纯白简约背景，柔和顶光，高级质感，4K高清，丝滑运镜，适合电商主图，无模糊无抖动', 'film-commercial', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'art-oil', 'SYSTEM', '油画质感', '艺术手绘', '可见笔触、厚涂纹理，古典艺术质感', '印象派油画风格，黄昏的森林里，狐狸在林间缓缓穿行，身形仿佛正在溶解，与背景的模糊森林色彩无缝融合。狐狸轮廓柔和，毛色为淡赭石与灰棕混合，眼神神秘而警觉。画面中可见斑驳的光影透过树冠洒落，营造出一种梦幻、飘渺的氛围，阳光以晕染方式呈现，可见的油画笔触纹理，丰富的厚涂感，跟随镜头，4K高清', 'oil-painting', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'art-watercolor', 'SYSTEM', '水彩风格', '艺术手绘', '透明晕染、柔和色彩、水痕纹理，清新文艺', '水彩风格，春日的樱花林，花瓣随风飘落，女孩在林间漫步，透明的水彩晕染效果，柔和的马卡龙色彩，水痕纹理，清新文艺，镜头缓慢推进，柔光，治愈氛围，1080P高清', 'watercolor', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'misc-vaporwave', 'SYSTEM', '蒸汽波', '其他特色', '复古未来主义、粉紫蓝霓虹、复古雕塑、网格背景，80 年代复古科技风', '蒸汽波风格，复古希腊雕像站在紫色网格背景前，粉蓝霓虹灯光环绕，老式CRT显示器闪烁着复古图案，棕榈树的剪影，80年代复古科技感，aesthetic statue, purple grid，镜头缓慢旋转，高饱和色彩，画面流畅', 'vaporwave', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'misc-nordic', 'SYSTEM', '北欧清新', '其他特色', '白灰 + 浅蓝绿的柔和色调、极简干净，治愈清新', '春日公园野餐，白色餐布上摆放着马卡龙和柠檬水，樱花花瓣缓缓飘落，阳光透过樱花树洒下柔光，白灰+浅蓝绿的北欧清新色调，干净明亮的画面，柔光，中景固定镜头，治愈氛围，莫兰迪低饱和，9:16竖屏', 'nordic', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'misc-gothic', 'SYSTEM', '暗黑哥特', '其他特色', '高对比暗调、穹顶建筑、蜡烛光源，神秘暗黑氛围', '暗黑哥特风格，古老的哥特式教堂穹顶下，摇曳的蜡烛照亮昏暗的走廊，阴影在石墙上拉长，穿着黑色长裙的女人缓缓走过，vaulted ceiling, candle，高对比暗调，神秘氛围，镜头缓慢推进，冷色调，无多余光线', 'gothic-dark', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'misc-pop-art', 'SYSTEM', '波普艺术', '其他特色', '半色调网点、鲜艳撞色，流行艺术风格', '波普艺术风格，街头的涂鸦墙，鲜艳的撞色图案，半色调网点纹理，矿物颜料质感，卡通化的人物形象，色彩明快，镜头环绕展示，高饱和，4K高清', 'pop-art', NULL, NULL, NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM style_template);
