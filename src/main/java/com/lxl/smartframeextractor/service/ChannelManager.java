package com.lxl.smartframeextractor.service;

import com.lxl.smartframeextractor.config.ExtractorProperties;
import com.lxl.smartframeextractor.core.FramePusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.lxl.smartframeextractor.core.RtspWorker;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 通道管理器，负责管理RTSP通道的生命周期
 */
@Service
public class ChannelManager {
  private static final Logger log = LoggerFactory.getLogger(ChannelManager.class);
  
  private final ExtractorProperties cfg;                // 配置信息
  private final FramePusher pusher;                     // 帧推送器

  private final ExecutorService pool;                   // 线程池，用于运行RtspWorker
  private final ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>(); // 通道配置映射

  /**
   * 构造方法
   * @param cfg 配置信息
   * @param pusher 帧推送器
   */
  public ChannelManager(ExtractorProperties cfg, FramePusher pusher) {
    this.cfg = cfg;
    this.pusher = pusher;
    
    // 初始化线程池
    this.pool = Executors.newFixedThreadPool(cfg.getWorkerThreads());
  }

  /**
   * 通道配置记录
   */
  public record Channel(String cameraId, String rtspUrl, boolean running) {}

  /**
   * 运行时通道记录
   */
  private record RuntimeChannel(RtspWorker worker, Future<?> future, String rtspUrl) {}

  private final ConcurrentHashMap<String, RuntimeChannel> runtime = new ConcurrentHashMap<>(); // 运行时通道映射

  /**
   * 新增或更新通道配置
   * @param cameraId 摄像头ID
   * @param rtspUrl RTSP流地址
   */
  public void upsert(String cameraId, String rtspUrl) {
    boolean isRunning = runtime.containsKey(cameraId);
    Channel newChannel = new Channel(cameraId, rtspUrl, isRunning);
    Channel oldChannel = channels.put(cameraId, newChannel);
    
    if (oldChannel == null) {
      log.info("camera={} 新增通道配置，运行状态: {}", cameraId, isRunning);
    } else {
      log.info("camera={} 更新通道配置，旧地址: {}, 新地址: {}, 运行状态: {}", 
              cameraId, oldChannel.rtspUrl(), rtspUrl, isRunning);
    }
  }

  /**
   * 启动通道
   * @param cameraId 摄像头ID
   * @throws IllegalArgumentException 通道不存在时抛出
   */
  public void start(String cameraId) {
    Channel ch = channels.get(cameraId);
    if (ch == null) {
      log.error("camera={} 启动失败: 通道不存在", cameraId);
      throw new IllegalArgumentException("cameraId not found");
    }
    
    if (runtime.containsKey(cameraId)) {
      log.warn("camera={} 已经在运行中", cameraId);
      return;
    }

    // 创建并启动RtspWorker
    RtspWorker w = new RtspWorker(cameraId, ch.rtspUrl(), cfg, pusher);
    Future<?> f = pool.submit(w);
    
    RuntimeChannel rc = new RuntimeChannel(w, f, ch.rtspUrl());
    runtime.put(cameraId, rc);
    
    // 更新通道状态为运行中
    Channel updatedChannel = new Channel(cameraId, ch.rtspUrl(), true);
    channels.put(cameraId, updatedChannel);
    log.info("camera={} 通道启动成功", cameraId);
  }

  /**
   * 停止通道
   * @param cameraId 摄像头ID
   */
  public void stop(String cameraId) {
    RuntimeChannel rc = runtime.remove(cameraId);
    if (rc == null) {
      log.warn("camera={} 停止失败: 通道未运行", cameraId);
      return;
    }
    
    // 停止RtspWorker
    rc.worker.shutdown();
    
    // 取消Future
    boolean canceled = rc.future.cancel(true);
    
    // 更新通道状态为已停止
    Channel ch = channels.get(cameraId);
    if (ch != null) {
      Channel updatedChannel = new Channel(cameraId, ch.rtspUrl(), false);
      channels.put(cameraId, updatedChannel);
      log.info("camera={} 通道停止成功", cameraId);
    }
  }

  /**
   * 获取所有通道配置
   * @return 通道配置映射
   */
  public Map<String, Channel> list() {
    return Map.copyOf(channels);
  }

  /**
   * 获取指定通道配置
   * @param cameraId 摄像头ID
   * @return 通道配置，不存在时返回null
   */
  public Channel get(String cameraId) {
    return channels.get(cameraId);
  }
}