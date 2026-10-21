# TickBeat

Android 节奏训练（Kotlin + Jetpack Compose）。

## 功能

### 节拍器
- BPM 30–300（滑条 / ±1）
- 拍号：常用预设一键选择，或滚动上/下数字（1–12 与 1/2/4/8/16）
- 强拍 / 弱拍 click，拍点指示

### 侧边栏功能（左上角菜单）
- **节拍器**：BPM / 拍号 / click
- **跟拍**：听 click 敲击，实时 Perfect/Good/Miss 与成绩报告
- **节奏模仿**：听示范 → 复打 → 与 beatmap 对比打分
- **视奏节奏**：时值块谱面 + 播放头，按谱跟打
- **听辨纠错**：听错误节奏并选择问题类型
- **音程练习**：一八度键盘，听根音后弹出目标音程（大小/纯 + 增减 + 倍增/倍减）并即时判定

## 打开工程

用 Android Studio 打开本目录，同步 Gradle 后运行到真机或模拟器。

```bash
./gradlew :app:assembleDebug
```

## 技术说明

- 节拍 / 示范由 `AudioTrack` 独立线程按 PCM 调度
- 内容格式为 JSON beatmap（`assets/beatmaps/` + 内置曲库）
- 判定：Perfect ≤40ms，Good ≤80ms（`domain/Judge.kt`）
