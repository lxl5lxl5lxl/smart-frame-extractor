package com.lxl.smartframeextractor.core;

import com.lxl.smartframeextractor.config.ExtractorProperties;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_core.*;

/**
 * 运动检测器，用于检测视频帧中的运动
 */
public class MotionFilter {
  private static final Logger log = LoggerFactory.getLogger(MotionFilter.class);
  
  private final ExtractorProperties cfg;      // 配置信息
  private Mat prevGraySmall;                  // 上一帧的灰度缩小图

  private long lastPushMs = 0;                // 上次推送帧的时间
  private long lastAnyPushMs = 0;             // 上次任何推送的时间（包括心跳）

  /**
   * 构造方法
   * @param cfg 配置信息
   */
  public MotionFilter(ExtractorProperties cfg) {
    this.cfg = cfg;
  }

  /**
   * 判断是否需要推送当前帧
   * @param bgrFrame BGR格式的图像帧
   * @param nowMs 当前时间戳（毫秒）
   * @return 是否需要推送
   */
  public boolean shouldPush(Mat bgrFrame, long nowMs) {
    // 1. 心跳推送机制：如果长时间没有推送，强制推送一帧
    if (lastAnyPushMs > 0 && (nowMs - lastAnyPushMs) >= cfg.getKeepAlivePushIntervalSec() * 1000L) {
      lastPushMs = nowMs;
      lastAnyPushMs = nowMs;
      return true;
    }

    // 2. 最小推送间隔：避免短时间内推送过多帧
    if (lastPushMs > 0 && (nowMs - lastPushMs) < cfg.getMinPushIntervalMs()) {
      return false;
    }

    // 3. 帧预处理：转换为灰度图并缩小尺寸，减少计算量
    Mat gray = new Mat();
    cvtColor(bgrFrame, gray, COLOR_BGR2GRAY); // 转换为灰度图

    Mat small = new Mat();
    resize(gray, small, new Size(cfg.getDetectWidth(), cfg.getDetectHeight()), 0, 0, INTER_LINEAR); // 缩小尺寸
    GaussianBlur(small, small, new Size(5, 5), 0); // 高斯模糊，减少噪声

    // 4. 初始化背景：第一帧直接保存为背景，不进行检测
    if (prevGraySmall == null) {
      prevGraySmall = small.clone();
      return false;
    }

    // 5. 运动检测算法
    Mat diff = new Mat();
    absdiff(small, prevGraySmall, diff); // 计算当前帧与背景帧的差异

    // 计算平均绝对差（MAD）
    double mad = mean(diff).get(0);

    // 二值化处理，突出运动区域
    Mat bin = new Mat();
    threshold(diff, bin, 15, 255, THRESH_BINARY);

    // 计算运动区域比例
    double motionArea = countNonZero(bin);
    double totalArea = bin.rows() * bin.cols();
    double ratio = motionArea / totalArea;

    // 更新背景
    prevGraySmall = small.clone();

    // 6. 判断是否检测到运动：MAD或运动区域比例超过阈值
    boolean motion = (mad >= cfg.getMadThreshold()) || (ratio >= cfg.getMotionAreaRatioThreshold());
    
    if (motion) {
      log.info("检测到运动! 更新推送时间: {} -> {}", lastPushMs, nowMs);
      lastPushMs = nowMs;
      lastAnyPushMs = nowMs;
      return true;
    }
    return false;
  }
}