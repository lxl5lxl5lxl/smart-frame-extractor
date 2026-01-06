package com.lxl.smartframeextractor.core;

import com.lxl.smartframeextractor.config.ExtractorProperties;

import java.io.IOException;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTSP流处理线程，负责从RTSP流拉取帧、进行运动检测并推送检测到的帧
 */
public class RtspWorker implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(RtspWorker.class);

  private final String cameraId;      // 摄像头ID
  private final String rtspUrl;       // RTSP流地址
  private final ExtractorProperties cfg; // 配置信息
  private final FramePusher pusher;   // 帧推送器
  private final MotionFilter filter;  // 运动检测器

  private volatile boolean stop = false; // 停止标志

  /**
   * 构造方法
   * @param cameraId 摄像头ID
   * @param rtspUrl RTSP流地址
   * @param cfg 配置信息
   * @param pusher 帧推送器
   */
  public RtspWorker(String cameraId, String rtspUrl, ExtractorProperties cfg, FramePusher pusher) {
    this.cameraId = cameraId;
    this.rtspUrl = rtspUrl;
    this.cfg = cfg;
    this.pusher = pusher;
    this.filter = new MotionFilter(cfg);
  }

  /**
   * 停止工作线程
   */
  public void shutdown() { stop = true; }

  @Override
  public void run() {
    OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
    log.info("camera={} 工作线程启动", cameraId);

    while (!stop) { // 外层循环：持续尝试连接和重连RTSP流
      try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspUrl)) {
        // 配置FFmpeg抓流器参数
        grabber.setOption("rtsp_transport", "tcp"); // 使用TCP传输RTSP流，更可靠
        grabber.setOption("stimeout", String.valueOf(cfg.getRtspOpenTimeoutMs() * 1000L)); // 连接超时时间（微秒）
        grabber.setOption("rw_timeout", String.valueOf(cfg.getRtspReadTimeoutMs() * 1000L)); // 读写超时时间（微秒）
        grabber.setOption("fflags", "nobuffer"); // 不使用缓冲区，降低延迟
        grabber.setOption("flags", "low_delay"); // 低延迟模式
        grabber.setOption("max_delay", "500000"); // 最大延迟500ms

        // 启动抓流器
        grabber.start();
        
        // 计算帧检测间隔，控制检测频率，避免过高CPU占用
        long minCheckIntervalMs = Math.max(1, 1000L / Math.max(1, cfg.getMaxCheckFps()));
        long lastChecked = 0;

        // 内层循环：持续抓取和处理帧
        while (!stop) {
          // 从RTSP流抓取一帧
          var frame = grabber.grabImage();
          if (frame == null) {
            // 抓取到空帧，跳过
            continue;
          }

          long now = System.currentTimeMillis();
          // 控制检测频率，只在间隔时间后处理帧
          if (now - lastChecked < minCheckIntervalMs) {
            // 跳过当前帧，但继续抓取下一帧以防止RTSP流缓冲区溢出
            continue;
          }
          lastChecked = now;

          // 将JavaCV Frame转换为OpenCV Mat格式，用于后续处理
          var mat = converter.convert(frame);
          if (mat == null || mat.empty()) {
            // 转换失败，跳过
            continue;
          }

          // 调用运动过滤器，判断是否需要推送该帧
          if (filter.shouldPush(mat, now)) {
            log.info("camera={} 检测到运动，推送帧到算法服务器", cameraId);
            try {
              // 推送帧到配置的算法服务器
              pusher.push(mat, cameraId, now);
            } catch (IOException e) {
              // 推送失败，记录日志
              log.warn("camera={} 推送帧失败: {}", cameraId, e.getMessage());
            }
          }
        }
      } catch (Exception e) {
        // 抓取过程中发生异常，记录日志
        log.warn("camera={} 抓取异常: {}", cameraId, e.getMessage());
        try {
          // 延迟后重试，避免频繁重连
          Thread.sleep(cfg.getReconnectBackoffMs());
        } catch (InterruptedException ignored) {
          // 中断异常，退出循环
          log.info("camera={} 工作线程被中断，退出重连循环", cameraId);
          break;
        }
      }
    }

    log.info("camera={} 工作线程停止", cameraId);
  }
}