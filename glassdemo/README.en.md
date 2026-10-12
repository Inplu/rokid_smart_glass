# Glass Demo · glassdemo

[中文](README.zh-CN.md) | [English](README.en.md)

[Back to project overview](../README.md)

Focused on individual SDK capabilities, this demo helps developers learn the glasses interfaces, verify hardware capabilities, and build custom glasses applications.

- **Image capture**: Examples of SDK photo capture, video recording, camera sharing, and NV21 frame export.
- **Two-device communication**: Works with the phone demo to demonstrate sending and receiving messages.
- **Voice interaction**: Examples of offline voice commands, online ASR speech recognition, and TTS speech synthesis.
- **Device interaction**: Examples of touchpad and button input, head pose, and device status queries.

## SDK Methods and Source Entry Points

This demo uses the glasses SDK `com.rokid.security:glass3.open.sdk`; the online ASR/TTS examples additionally use `com.rokid.security.sdk:online-speech`. The sections below list the methods actually called in this repository by feature, with source links under each example.

### Bind the Service and Register the Client

- `GlassSdk.isReady()`
- `bindSecurityService(...)`
- `registerClient(...)`
- `release()`

Source: [GlassSdkUtils.kt](app/src/main/java/com/rokid/glass/utils/GlassSdkUtils.kt)

### Take Photos, Record Video, and Record Audio

- `GlassSdk.getGlassMediaService()` →<br>  `addPhotoCallback(...)`
- `takePhoto(...)`
- `startRecord(...)`
- `stopRecord()`
- `startAudioRecord(...)`
- `stopAudioRecord(...)`

Source: [SdkMediaActivity.kt](app/src/main/java/com/rokid/glass/SdkMediaActivity.kt)

### Share Camera NV21 Frames

- `CameraShareHelper.getSupportedPreviewSizes()`
- `initNv21ExportWithConfig(...)`
- `Nv21Callback.onNv21Frame(...)`
- `releaseNv21Export()`

Source: [Nv21ExportFragment.kt](app/src/main/java/com/rokid/glass/camera/Nv21ExportFragment.kt)

### Share the Camera Surface

- `CameraShareHelper.initSurfaceWithConfig(...)`
- `releaseSurface()`

Source: [SurfaceShareFragment.kt](app/src/main/java/com/rokid/glass/camera/SurfaceShareFragment.kt)

### Bluetooth/P2P Messages and Files

- `GlassSdk.getGlassMessageService()` →<br>  `sendTextMessageByClassicBT(...)`
- `sendTextMessageByP2P(...)`
- `setMessageListener(...)`
- The file operator's `sendFile(...)` method.

Source: [Sending example](app/src/main/java/com/rokid/glass/SendMessageActivity.kt), [receiving example](app/src/main/java/com/rokid/glass/MessageReceiveActivity.kt)

### Offline Voice Commands and Device Status

- `GlassSdk.getGlassOfflineCmdService()` →<br>  `add(VoiceAction(...))`
- `getGlassDeviceService()`: Reads `serialNumber` and `deviceStatusInfo`.

Source: [HomeActivity.kt](app/src/main/java/com/rokid/glass/HomeActivity.kt)

### Online ASR/TTS

- `OnlineSpeechSdk(OnlineSpeechSdkConfig(...))` →<br>  `createAsrClient()` →<br>  `attachAudioSource(...)`
- `createTtsClient()` →<br>  `attachStreamPlayer(...)`
- After connecting, use `startAsrWithMic()`.
- `stopAsrWithMic()`
- Call `speak(...)`; call `stop()` / `close()` when finished.

Source: [PrivateSpeechActivity.kt](app/src/main/java/com/rokid/glass/speech/private/PrivateSpeechActivity.kt)

The basic sequence on the glasses side is:

```text
bindSecurityService
  → onServiceConnected
  → registerClient
  → IClientCallback.onReady
  → Use SDK features
```

A successful bind call does not mean that the client is ready; see the source code above for listener registration and release procedures.

The AK/SK values for online speech are speech-service credentials; do not confuse them with the client name.

## Example Usage Notice

The code, configuration, and usage in this demo are provided for reference only; developers do not need to use the same implementation. Adjust or replace them as needed, and complete the required adaptation and verification.

## SDK Documentation

[Rokid Developer Documentation](https://x-docs.rokid.com/docs/)
