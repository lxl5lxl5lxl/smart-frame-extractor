package com.lxl.smartframeextractor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.lxl.smartframeextractor.config.ExtractorProperties;

@SpringBootApplication
@EnableConfigurationProperties(ExtractorProperties.class)
public class SmartFrameExtractorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartFrameExtractorApplication.class, args);
    }

}