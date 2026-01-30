# 升级到多语言 Whisper 模型

## 快速升级指南

如果您之前安装的是英文专用版本（`tiny.en`），请按照以下步骤升级到多语言版本以支持中英文混合识别。

## 步骤 1: 删除旧模型文件

删除 `app/src/main/assets/models/whisper-tiny/` 目录下的所有文件：
```bash
# 旧文件（英文专用）
tiny.en-encoder.int8.onnx  ❌
tiny.en-decoder.int8.onnx  ❌
tiny.en-tokens.txt         ❌
```

## 步骤 2: 下载多语言模型

下载地址：[sherpa-onnx-whisper-tiny.tar.bz2](https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-tiny.tar.bz2)

**重要**：确保下载的是 `tiny.tar.bz2`，**不是** `tiny.en.tar.bz2`

## 步骤 3: 解压并复制新文件

将以下文件复制到 `app/src/main/assets/models/whisper-tiny/`：
```bash
# 新文件（多语言版本）
tiny-encoder.int8.onnx  ✅
tiny-decoder.int8.onnx  ✅
tiny-tokens.txt         ✅
```

## 步骤 4: 验证文件结构

确认目录结构如下：
```
app/src/main/assets/models/whisper-tiny/
├── tiny-encoder.int8.onnx
├── tiny-decoder.int8.onnx
└── tiny-tokens.txt
```

## 步骤 5: 重新构建并测试

1. 在 Android Studio 中点击 **Build → Clean Project**
2. 然后点击 **Build → Rebuild Project**
3. 运行应用并测试中英文混合识别

## 测试用例

录制以下音频测试多语言识别：
- **纯中文**："今天天气真不错"
- **纯英文**："Hello world, how are you"
- **中英混合**："我想喝一杯 coffee"
- **中英混合**："这个 project 进展得很顺利"

## 常见问题

### Q: 升级后应用崩溃或无法识别

**解决方案**：
1. 确认文件名正确（不带 `.en`）
2. 检查文件大小是否正常（编码器约 40MB，解码器约 40MB）
3. 尝试 Clean Project 并重新构建

### Q: 识别准确率变化

多语言模型在中英文混合场景下表现更好，但在纯英文场景下可能略低于专用英文模型。这是正常的权衡。

### Q: 可以同时保留两个版本吗？

不建议。两个模型会占用双倍空间（~150MB），且代码只能使用一个配置。

## 技术说明

### 模型对比

| 特性 | 英文专用 (tiny.en) | 多语言 (tiny) |
|------|-------------------|---------------|
| 文件大小 | ~75 MB | ~75 MB |
| 支持语言 | 仅英文 | 99种语言（含中英文） |
| 中文识别 | ❌ 不支持 | ✅ 支持 |
| 英文识别 | ✅ 极佳 | ✅ 优秀 |
| 中英混合 | ❌ 不支持 | ✅ 支持 |

### 配置变更

代码中的关键变更：
```kotlin
// 旧配置（英文专用）
language = "en"
encoder = "tiny.en-encoder.int8.onnx"

// 新配置（多语言）
language = "zh"  // 优先中文，同时支持英文
encoder = "tiny-encoder.int8.onnx"
```

## 需要帮助？

如果遇到问题，请查看：
1. `README_WHISPER.md` - 完整的安装和配置指南
2. `MEMORY.md` - 项目开发记录
3. Logcat 日志 - 搜索 "WhisperHelper" 查看错误信息

---

升级完成后，您的应用将支持：
- ✅ 中文语音识别
- ✅ 英文语音识别
- ✅ 中英文混合识别
- ✅ 完全离线运行
