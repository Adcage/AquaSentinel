ALTER TABLE camera_device
    ADD COLUMN rotation INT DEFAULT 0 COMMENT '画面旋转角度(0/90/180/270)' AFTER enabled;
