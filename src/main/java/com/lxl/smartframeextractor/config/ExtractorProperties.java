package com.lxl.smartframeextractor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "extractor")
public class ExtractorProperties {
  private String algoUrl;

  private int maxCheckFps = 5;
  private long minPushIntervalMs = 300;

  private int detectWidth = 320;
  private int detectHeight = 180;

  private double madThreshold = 2.5;
  private double motionAreaRatioThreshold = 0.003;

  private long keepAlivePushIntervalSec = 20;
  private int jpegQuality = 80;

  private int rtspOpenTimeoutMs = 5000;
  private int rtspReadTimeoutMs = 5000;
  private long reconnectBackoffMs = 2000;

  private int workerThreads = 64;

  public String getAlgoUrl() {
    return algoUrl;
  }

  public void setAlgoUrl(String algoUrl) {
    this.algoUrl = algoUrl;
  }

  public int getMaxCheckFps() {
    return maxCheckFps;
  }

  public void setMaxCheckFps(int maxCheckFps) {
    this.maxCheckFps = maxCheckFps;
  }

  public long getMinPushIntervalMs() {
    return minPushIntervalMs;
  }

  public void setMinPushIntervalMs(long minPushIntervalMs) {
    this.minPushIntervalMs = minPushIntervalMs;
  }

  public int getDetectWidth() {
    return detectWidth;
  }

  public void setDetectWidth(int detectWidth) {
    this.detectWidth = detectWidth;
  }

  public int getDetectHeight() {
    return detectHeight;
  }

  public void setDetectHeight(int detectHeight) {
    this.detectHeight = detectHeight;
  }

  public double getMadThreshold() {
    return madThreshold;
  }

  public void setMadThreshold(double madThreshold) {
    this.madThreshold = madThreshold;
  }

  public double getMotionAreaRatioThreshold() {
    return motionAreaRatioThreshold;
  }

  public void setMotionAreaRatioThreshold(double motionAreaRatioThreshold) {
    this.motionAreaRatioThreshold = motionAreaRatioThreshold;
  }

  public long getKeepAlivePushIntervalSec() {
    return keepAlivePushIntervalSec;
  }

  public void setKeepAlivePushIntervalSec(long keepAlivePushIntervalSec) {
    this.keepAlivePushIntervalSec = keepAlivePushIntervalSec;
  }

  public int getJpegQuality() {
    return jpegQuality;
  }

  public void setJpegQuality(int jpegQuality) {
    this.jpegQuality = jpegQuality;
  }

  public int getRtspOpenTimeoutMs() {
    return rtspOpenTimeoutMs;
  }

  public void setRtspOpenTimeoutMs(int rtspOpenTimeoutMs) {
    this.rtspOpenTimeoutMs = rtspOpenTimeoutMs;
  }

  public int getRtspReadTimeoutMs() {
    return rtspReadTimeoutMs;
  }

  public void setRtspReadTimeoutMs(int rtspReadTimeoutMs) {
    this.rtspReadTimeoutMs = rtspReadTimeoutMs;
  }

  public long getReconnectBackoffMs() {
    return reconnectBackoffMs;
  }

  public void setReconnectBackoffMs(long reconnectBackoffMs) {
    this.reconnectBackoffMs = reconnectBackoffMs;
  }

  public int getWorkerThreads() {
    return workerThreads;
  }

  public void setWorkerThreads(int workerThreads) {
    this.workerThreads = workerThreads;
  }
}