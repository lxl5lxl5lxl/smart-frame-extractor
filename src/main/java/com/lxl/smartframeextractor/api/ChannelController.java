package com.lxl.smartframeextractor.api;

import com.lxl.smartframeextractor.service.ChannelManager;
import javax.validation.constraints.NotBlank;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/channels")
@Validated
public class ChannelController {
  private final ChannelManager mgr;

  public ChannelController(ChannelManager mgr) {
    this.mgr = mgr;
  }

  public record UpsertReq(@NotBlank String cameraId, @NotBlank String rtspUrl) {}

  @PostMapping
  public void upsert(@RequestBody @Valid UpsertReq req) {
    mgr.upsert(req.cameraId(), req.rtspUrl());
  }

  @PostMapping("/{cameraId}/start")
  public void start(@PathVariable String cameraId) {
    mgr.start(cameraId);
  }

  @PostMapping("/{cameraId}/stop")
  public void stop(@PathVariable String cameraId) {
    mgr.stop(cameraId);
  }

  @GetMapping
  public Map<String, ChannelManager.Channel> list() {
    return mgr.list();
  }

  @GetMapping("/{cameraId}")
  public ChannelManager.Channel get(@PathVariable String cameraId) {
    return mgr.get(cameraId);
  }
}
