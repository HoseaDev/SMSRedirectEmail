# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**MessageRelayer** — an Android app (Java) that intercepts incoming SMS and forwards them via email (SMTP) or SMS to another phone number. Based on [MessageRelayer](https://github.com/HaoFeiWang/MessageRelayer) with additional features: internal SMS forwarding, dual-SIM support with SIM tail number in email titles, verification code extraction in subjects, and AndroidX migration.

Package: `com.hosea.messagerelayer`
Min SDK: 22 / Target SDK: 34 / Java 17 / Gradle 8.3.2 + AGP 8.5.0

## Build Commands

```bash
./gradlew assembleDebug       # Debug APK
./gradlew assembleRelease     # Release APK (no ProGuard minification)
./gradlew test                # Unit tests
./gradlew connectedAndroidTest # Instrumentation tests
./gradlew clean               # Clean build outputs
```

## Architecture

### Message Flow

1. **MessageReceiver** (BroadcastReceiver) receives `SMS_RECEIVED` intent
2. Checks blacklist via `DataBaseManager.getSmsIntercept()`
3. Starts **SmsService** (IntentService) with SMS content, sender mobile, and subscription ID
4. SmsService applies forwarding rules (keyword filter + contact filter from DB)
5. Forwards via **EmailRelayerManager** (SMTP) and/or **SmsRelayerManager** (send SMS to target number)

### Key Components

| Layer | Classes |
|-------|---------|
| **Entry** | `StartActivity` → `MainActivity` (3 config sections: SMS, Email, Rules) |
| **Receivers** | `MessageReceiver` (SMS), `BatterReceiver` (battery events) |
| **Services** | `SmsService` (IntentService, handles forwarding logic), `ForegroundService` (keep-alive), `AccessibilitySampleService` (WeChat automation, mostly disabled) |
| **Relay Managers** | `EmailRelayerManager` (JavaMail SMTP), `SmsRelayerManager` (dual-SIM aware SMS sending) |
| **Config/Storage** | `NativeDataManager` (SharedPreferences wrapper), `DataBaseManager`/`DataBaseHelper` (SQLite for contacts & blacklist) |
| **Constants** | `Constant` (SP keys, DB schema, email provider names), `SMSConfig` (DB table/column names for SMS intercept) |

### Data Storage

- **SharedPreferences** (`settingConf`): All relay toggles, email SMTP config, target mobile, keyword sets, prefix/suffix — accessed via `NativeDataManager`
- **SQLite**: Two tables — `contact` (forwarding whitelist) and `sms_intercept` (blacklist) — managed by `DataBaseManager`

### Email Configuration

Supports QQ, 163, 126, Gmail, Outlook, and custom SMTP servers. SSL enabled by default. Email subject auto-detects verification codes (4-6 digit patterns) and includes SIM card tail number.

### Inner SMS Forwarding

When enabled, SMS from a configured number is parsed with a delimiter rule (e.g., `#`). Content like `123#456` sends "456" to phone number "123" using the same SIM that received it.

## Key Libraries

- `com.sun.mail:android-mail:1.6.7` — JavaMail for SMTP
- `com.yanzhenjie:permission:2.0.0-rc4` — Runtime permissions
- `com.blankj:utilcodex:1.31.1` — Android utility library (logging via `LogUtils`)
- `com.google.android.flexbox:flexbox:3.0.0` — FlexboxLayout for keyword chips

## Language

The codebase, UI strings, comments, and commit messages are in **Chinese**. Follow this convention.
