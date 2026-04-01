ALTER TABLE extracted_asset ADD COLUMN storyboard_plan_json LONGTEXT;
ALTER TABLE extracted_asset ADD COLUMN storyboard_translations_json LONGTEXT;
ALTER TABLE extracted_asset ADD COLUMN storyboard_prompt_text LONGTEXT;
ALTER TABLE extracted_asset ADD COLUMN storyboard_image_file_id VARCHAR(255);

ALTER TABLE storyboard_shot ADD COLUMN storyboard_asset_id VARCHAR(64);
ALTER TABLE storyboard_shot ADD COLUMN storyboard_image_file_id VARCHAR(255);
ALTER TABLE storyboard_shot ADD COLUMN storyboard_crop_file_id VARCHAR(255);
ALTER TABLE storyboard_shot ADD COLUMN storyboard_crop_index INT;
ALTER TABLE storyboard_shot ADD COLUMN first_frame_mode VARCHAR(64);
