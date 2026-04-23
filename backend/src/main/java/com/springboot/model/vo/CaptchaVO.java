package com.springboot.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class CaptchaVO implements Serializable {

    private String captchaId;

    private String captchaImageBase64;

    private long expireAt;
}
