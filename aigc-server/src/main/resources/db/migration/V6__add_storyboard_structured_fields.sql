ALTER TABLE storyboard_shot
    ADD COLUMN shot_type VARCHAR(64);

ALTER TABLE storyboard_shot
    ADD COLUMN camera_move VARCHAR(255);

ALTER TABLE storyboard_shot
    ADD COLUMN emotion VARCHAR(255);
