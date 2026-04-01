ALTER TABLE script_project ADD COLUMN art_direction_json LONGTEXT;
ALTER TABLE extracted_asset ADD COLUMN visual_prompt TEXT;
ALTER TABLE extracted_asset ADD COLUMN turnaround_plan_json LONGTEXT;
ALTER TABLE extracted_asset ADD COLUMN turnaround_image_file_id VARCHAR(255);
ALTER TABLE storyboard_shot ADD COLUMN visual_prompt TEXT;
