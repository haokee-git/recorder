# Whisper 语音识别集成指南

本应用使用 **OpenAI Whisper tiny 模型** 实现完全离线的语音转文本功能。

## 所需文件

### 1. Sherpa-ONNX AAR 库

**文件名**: `sherpa-onnx-static-link-onnxruntime-1.12.23.aar`
**大小**: ~27.4 MB
**下载地址**: [GitHub Releases](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.12.23)

**安装步骤**:
1. 从上述链接下载 `sherpa-onnx-static-link-onnxruntime-1.12.23.aar`
2. 将 AAR 文件放入 `app/libs/` 目录
3. 如果下载的是不同版本，请同时修改 `app/build.gradle.kts` 中的文件名

### 2. Whisper Tiny 模型文件（多语言版本）

**模型包**: `sherpa-onnx-whisper-tiny.tar.bz2`
**解压后大小**: ~75 MB
**下载地址**: [直接下载链接](https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-tiny.tar.bz2)

**重要提示**：使用**多语言版本**（不带 `.en` 后缀），支持中英文混合识别

**所需文件**:
- `tiny-encoder.int8.onnx` - 编码器模型
- `tiny-decoder.int8.onnx` - 解码器模型
- `tiny-tokens.txt` - Token 映射表

**安装步骤**:
1. 下载并解压 `sherpa-onnx-whisper-tiny.tar.bz2`（注意：**不是** `.en` 版本）
2. 在项目中创建目录：`app/src/main/assets/models/whisper-tiny/`
3. 将以下文件复制到该目录：
   ```
   app/src/main/assets/models/whisper-tiny/
   ├── tiny-encoder.int8.onnx
   ├── tiny-decoder.int8.onnx
   └── tiny-tokens.txt
   ```

## 文件结构

完成上述步骤后，项目结构应如下：

```
AndroidProject/
├── app/
│   ├── libs/
│   │   └── sherpa-onnx-static-link-onnxruntime-1.12.23.aar
│   └── src/
│       └── main/
│           └── assets/
│               └── models/
│                   └── whisper-tiny/
│                       ├── tiny-encoder.int8.onnx
│                       ├── tiny-decoder.int8.onnx
│                       └── tiny-tokens.txt
└── ...
```

## 验证

构建并运行应用后：

1. **查看日志**：在 Logcat 中搜索 "WhisperHelper" 或 "SpeechToTextHelper"
2. **成功初始化**：应看到 "Whisper initialized successfully" 日志
3. **失败提示**：如果模型文件缺失，会看到 "Model files not found" 错误

## 使用方法

1. **录制音频**：点击右下角麦克风按钮录制感言
   - 支持纯中文、纯英文、中英文混合语音
2. **转换为文本**：
   - 选中一条或多条原始感言
   - 点击工具栏"批量转换"按钮
   - 等待转换完成（可能需要几秒钟）
3. **查看结果**：转换后的文本会显示在感言卡片中

## 常见问题

### Q: 模型文件太大，能不能用更小的模型？

A: Whisper tiny 是最小的模型（~75MB）。如果希望进一步减小体积，可以考虑：
- 使用量化版本（已经是 int8 量化）
- 首次启动时从网络下载模型（需修改代码）

### Q: 支持中文和英文混合识别吗？

A: ✅ **支持！** 本项目使用的是 Whisper **多语言版本**（`tiny.tar.bz2`），可以：
- 识别纯中文语音
- 识别纯英文语音
- 识别中英文混合语音

配置已设置为 `language = "zh"`，优先识别中文，同时能自动处理英文。

**注意**：如果您之前下载的是 `tiny.en` 英文专用版本，请按照上面的安装步骤重新下载多语言版本。

### Q: 识别准确率不高怎么办？

A: 可以尝试：
1. 使用更大的模型（base, small, medium）
2. 录音时保持环境安静，靠近麦克风
3. 清晰缓慢地说话

### Q: 转换速度太慢？

A: 优化建议：
1. 确保使用的是 int8 量化模型
2. 在 `WhisperHelper.kt` 中增加 `numThreads` 参数
3. 考虑使用 GPU 加速（需要 NNAPI 或 GPU 后端支持）

## 技术细节

- **模型格式**: ONNX (Open Neural Network Exchange)
- **推理引擎**: ONNX Runtime
- **音频要求**: 16kHz 单声道 PCM
- **支持格式**: M4A, MP3, WAV (自动解码和重采样)
- **线程数**: 2 (可在 WhisperHelper.kt 中修改)

## 参考资源

- **Sherpa-ONNX**: https://github.com/k2-fsa/sherpa-onnx
- **Whisper 官方文档**: https://k2-fsa.github.io/sherpa/onnx/pretrained_models/whisper/
- **模型下载页面**: https://github.com/k2-fsa/sherpa-onnx/releases

## 许可证

- **Whisper 模型**: MIT License (OpenAI)
- **Sherpa-ONNX**: Apache License 2.0
- **ONNX Runtime**: MIT License

---

如有问题，请参考项目的 CLAUDE.md 和 MEMORY.md 文档。
