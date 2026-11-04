# QuizLock（学习锁屏）

Android TV / 投影仪用的「答题才能继续看」小工具。默认每 5 分钟弹出一道全屏选择题，答对才能继续。

- 包名：`com.lin.quizlock`
- 当前版本：`1.1.0`
- 科目：数学口算、英语单词、语文古诗词、科学常识（小学高年级）
- 家长设置密码默认：`0000`

## 极米 H3S（GMUI 6.11）注意

该机型设置里通常**没有开发者选项**（连点版本号无效）。需要先用第三方 [code-ant/z7x-adb](https://github.com/code-ant/z7x-adb) 打开网络 ADB（端口 `5555`），再安装本 App。

答题界面使用**全屏 Activity**，不依赖悬浮窗权限（极米上 `SYSTEM_ALERT_WINDOW` 常被系统忽略）。

更完整的步骤见 [安装说明.md](./安装说明.md)。

## 构建

```bash
# 需要本机 Android SDK；写入 local.properties 的 sdk.dir
./gradlew :app:assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`（无私有 keystore 时为 unsigned/debug-signed，视 Gradle 配置而定）。

## License

个人家庭使用；按需自行修改。
