package com.lxl.smartframeextractor.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

/**
 * 帧接收器控制器，用于接收并保存推送的图像帧
 */
@RestController
@RequestMapping("/api/frames")
public class FrameReceiverController {
    private static final Logger log = LoggerFactory.getLogger(FrameReceiverController.class);
    
    private static final String DEFAULT_SAVE_PATH = System.getProperty("user.dir") + File.separator + "received_frames";
    
    /**
     * 接收图像帧并保存到本地
     * @param cameraId 摄像头ID
     * @param timestamp 时间戳
     * @param image 图像文件
     * @return 响应结果
     */
    @PostMapping
    public ResponseEntity<String> receiveFrame(
            @RequestParam("cameraId") String cameraId,
            @RequestParam("timestamp") Long timestamp,
            @RequestParam("image") MultipartFile image) {
        
        try {
            // 创建保存目录（如果不存在）
            File saveDir = new File(DEFAULT_SAVE_PATH);
            
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }
            
            // 生成文件名：时间戳.jpg
            String fileName = Instant.now().toEpochMilli() + ".jpg";
            File saveFile = new File(saveDir, fileName);
            
            // 保存文件
            image.transferTo(saveFile);
            
            log.info("接收来自摄像头 {} 的帧，时间戳: {}，保存路径: {}", 
                    cameraId, timestamp, saveFile.getAbsolutePath());
            
            return ResponseEntity.ok("Frame received and saved successfully");
        } catch (IOException e) {
            log.error("保存摄像头 {} 的帧失败: {}", cameraId, e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to save frame");
        }
    }
}