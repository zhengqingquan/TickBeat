# TickBeat

Android 节拍器（Kotlin + Jetpack Compose）。

## 功能

- BPM 30–300（滑条 / ±1）
- 拍号：常用预设一键选择；点「自定义」滚动选择上/下数字（1–12 与 1/2/4/8/16）
- 强拍更高音 click，剩余拍普通 click
- 当前拍可视化指示
- 播放时保持屏幕常亮

## 打开工程

用 Android Studio 打开本目录，同步 Gradle 后运行到真机或模拟器。

命令行（需已配置 `ANDROID_HOME`）：

```bash
./gradlew :app:assembleDebug
```

## 技术说明

节拍由 `AudioTrack` 在独立音频线程按 PCM 流调度，避免 UI 定时器抖动。
