package com.lxl.smartframeextractor.core;

import com.lxl.smartframeextractor.config.ExtractorProperties;
import okhttp3.*;
import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

/**
 * 帧推送器，负责将检测到运动的帧编码为JPEG并推送到指定的算法服务器
 */
@Component
public class FramePusher {
  private static final Logger log = LoggerFactory.getLogger(FramePusher.class);
  
  private final OkHttpClient client;    // HTTP客户端
  private final ExtractorProperties cfg; // 配置信息

  /**
   * 构造方法
   * @param cfg 配置信息
   */
  public FramePusher(ExtractorProperties cfg) {
    this.cfg = cfg;
    
    // 初始化OkHttpClient
    this.client = new OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build();
  }

  /**
   * 推送帧到算法服务器
   * @param bgrFrame BGR格式的图像帧
   * @param cameraId 摄像头ID
   * @param tsMs 时间戳（毫秒）
   * @throws IOException 推送过程中发生的异常
   */
  public void push(Mat bgrFrame, String cameraId, long tsMs) throws IOException {
    // 1. 将OpenCV的Mat帧编码为JPEG格式
    var buf = new org.bytedeco.javacpp.BytePointer();
    var params = new org.bytedeco.javacpp.IntPointer(2);
    params.put(0, IMWRITE_JPEG_QUALITY); // 设置JPEG质量参数
    params.put(1, cfg.getJpegQuality()); // 设置JPEG质量值
    
    boolean ok = imencode(".jpg", bgrFrame, buf, params);
    if (!ok) {
      log.error("camera={} 帧编码失败", cameraId);
      throw new IOException("imencode failed");
    }
    
    // 获取编码后的字节数据
    int encodedSize = (int) buf.limit();
    byte[] bytes = new byte[encodedSize];
    buf.get(bytes);

    // 2. 构建HTTP请求
    RequestBody fileBody = RequestBody.create(bytes, MediaType.parse("image/jpeg"));

    // 构建multipart/form-data请求体
    MultipartBody body = new MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("cameraId", cameraId)         // 摄像头ID
        .addFormDataPart("timestamp", String.valueOf(tsMs)) // 时间戳
        .addFormDataPart("image", cameraId + "_" + tsMs + ".jpg", fileBody) // 图像文件
        .build();

    Request req = new Request.Builder().url(cfg.getAlgoUrl()).post(body).build();
    
    // 3. 发送HTTP请求并处理响应
    try (Response resp = client.newCall(req).execute()) {
      if (!resp.isSuccessful()) {
        String errorMsg = "algo resp=" + resp.code() + " " + resp.message();
        log.error("camera={} 帧推送失败: {}", cameraId, errorMsg);
        throw new IOException(errorMsg);
      }
      
      log.info("camera={} 帧推送成功，响应码: {}", cameraId, resp.code());
    } catch (IOException e) {
      log.error("camera={} 帧推送发生IO异常: {}", cameraId, e.getMessage());
      throw e;
    }
  }
}