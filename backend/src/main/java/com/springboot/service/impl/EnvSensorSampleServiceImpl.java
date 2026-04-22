package com.springboot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springboot.model.entity.EnvSensorSample;
import com.springboot.service.EnvSensorSampleService;
import com.springboot.mapper.EnvSensorSampleMapper;
import org.springframework.stereotype.Service;

/**
* @description 针对表【env_sensor_sample(环境传感器采样表)】的数据库操作Service实现
*/
@Service
public class EnvSensorSampleServiceImpl extends ServiceImpl<EnvSensorSampleMapper, EnvSensorSample>
    implements EnvSensorSampleService{

}




