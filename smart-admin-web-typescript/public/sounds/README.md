# 通知音效文件说明

## 所需文件

请将通知音效文件命名为 `notification.mp3` 并放置在此目录下。

## 音效要求

- **格式**: MP3
- **文件名**: `notification.mp3`
- **时长**: 建议 1-2 秒
- **音量**: 适中,不要太大声
- **类型**: 轻快、不刺耳的提示音

## 推荐音效来源

### 1. 免费音效网站
- [Freesound](https://freesound.org/) - 搜索 "notification" 或 "message"
- [Zapsplat](https://www.zapsplat.com/) - 免费音效库
- [Mixkit](https://mixkit.co/free-sound-effects/) - 高质量免费音效

### 2. 系统默认音效
可以使用操作系统的默认通知音:
- **Windows**: `C:\Windows\Media\Windows Notify.wav` (需转换为MP3)
- **macOS**: `/System/Library/Sounds/` 目录下的音效文件
- **Linux**: `/usr/share/sounds/` 目录下的音效文件

### 3. 在线工具转换
如果下载的是 WAV 格式,可使用以下工具转换为 MP3:
- [Online Audio Converter](https://online-audio-converter.com/)
- [CloudConvert](https://cloudconvert.com/wav-to-mp3)

## 临时解决方案

如果暂时没有音效文件,通知管理器会自动跳过音效播放,只显示桌面通知。

## 示例代码生成音效

如果您会编程,也可以使用 Web Audio API 生成简单的提示音:

```javascript
// 生成简单的"叮"声
const audioContext = new AudioContext();
const oscillator = audioContext.createOscillator();
const gainNode = audioContext.createGain();

oscillator.connect(gainNode);
gainNode.connect(audioContext.destination);

oscillator.frequency.value = 800; // 频率 800Hz
oscillator.type = 'sine';

gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5);

oscillator.start(audioContext.currentTime);
oscillator.stop(audioContext.currentTime + 0.5);
```

不过建议使用现成的音效文件以获得更好的用户体验。
