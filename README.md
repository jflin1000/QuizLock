# QuizLock（学习锁屏）

Android TV / 投影仪用的「答题才能继续看」小工具。默认每 5 分钟弹出一道全屏选择题，答对才能继续。

- 包名：`com.lin.quizlock`
- 当前版本：`1.3.0`（versionCode 4）
- 科目：数学口算（小学**六年级**：多位数乘除、带括号四则、小数、分数通分/约分/带分数、百分数、单位换算、估算）、英语单词、语文古诗词、科学常识；数学抽题约占 45%
- 家长设置密码默认：`0000`

## 极米 H3S（GMUI 6.11）注意

该机型设置里通常**没有开发者选项**（连点版本号无效）。需要先用第三方 [code-ant/z7x-adb](https://github.com/code-ant/z7x-adb) 打开网络 ADB（端口 `5555`），再安装本 App。

答题界面使用**全屏 Activity**，不依赖悬浮窗权限（极米上 `SYSTEM_ALERT_WINDOW` 常被系统忽略）。

更完整的步骤见 [安装说明.md](./安装说明.md)。

## 题库

数学口算由 `MathGenerator` 在本机随机生成，不放在 JSON 里。英语、古诗、科学放在仓库根目录 [`banks/`](./banks)：

| 文件 | 内容 |
|------|------|
| `english.json` | 中英互译（人教版 PEP 六年级词汇，并保留一部分基础词） |
| `poems.json` | 古诗上下句、作者 |
| `science.json` | 科学常识选择题 |

开机、前台服务启动、打开主界面时，App 会从下面目录拉取这三个文件（家长设置里可改，留空保存即恢复默认）：

`https://raw.githubusercontent.com/jflin1000/QuizLock/main/banks`

拉取成功会写入应用私有缓存。没网、地址不通或 JSON 不合法时，用缓存；缓存也没有，或安装包里的 `version` 更高，则用打包进去的同一份 JSON。仍读不出文件时，退回代码里的基础词表，避免出不了题。

英语题大约 **70%** 来自 `level >= 6` 的词，**30%** 复习基础词。干扰项和正确答案同一档，避免靠生词长相猜。

改题不必重装 APK：编辑 `banks/*.json`，把 `version` 加 1，提交到 `main`。投影仪联网后下次开机或打开 App 会拉到新题。只改安装包、不加大 `version` 时，已有同版本或更高版本的缓存仍优先，直到联网拉到仓库里的文件。

### english.json

```json
{
  "version": 2,
  "subject": "英语单词",
  "words": [
    {"en": "post office", "zh": "邮局", "level": 6},
    {"en": "apple", "zh": "苹果", "level": 4}
  ]
}
```

- `version`：整数。安装包里的版本**大于**缓存版本时，离线也用安装包。
- `level`：可选，缺省当作 6。
- 同一个文件里 `en`、`zh` 各自不要重复，否则一道题会有两个正确答案（重复项会被跳过）。
- 至少 4 个有效词。

### poems.json

```json
{
  "version": 1,
  "subject": "语文古诗词",
  "couplets": [
    {"prev": "床前明月光", "next": "疑是地上霜", "title": "静夜思", "author": "李白"}
  ],
  "titles": [
    {"title": "静夜思", "author": "李白", "first": "床前明月光"}
  ]
}
```

`titles` 可以省略。省略或不足 4 条时，用 `couplets` 里的篇名和作者出「作者是谁」。

### science.json

```json
{
  "version": 1,
  "subject": "科学常识",
  "items": [
    {
      "prompt": "太阳系中离太阳最近的行星是？",
      "answer": "水星",
      "distractors": ["金星", "地球", "火星"]
    }
  ]
}
```

`distractors` 至少 3 个，且不要和 `answer` 相同。

## 构建

```bash
# 需要本机 Android SDK；写入 local.properties 的 sdk.dir
./gradlew :app:assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`（无私有 keystore 时为 unsigned，视 Gradle 配置而定）。

## License

个人家庭使用；按需自行修改。
