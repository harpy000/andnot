# WhatsApp Vault 📱

An Android app that silently captures all incoming WhatsApp messages and images
**before** the sender can delete them. No root required.

---

## How It Works

```
WhatsApp receives message
        ↓
Android fires a notification
        ↓
WhatsApp Vault captures it instantly → saves to local database
        ↓
Sender deletes the message
        ↓
You open WhatsApp Vault → the message is still there ✅
```

### Two capture methods:

| Method | What it captures | How |
|---|---|---|
| **Notification Listener** | Text messages + image previews from notifications | `NotificationListenerService` |
| **Image Observer** | Full-resolution images received in WhatsApp | `FileObserver` on WhatsApp media folder |

---

## Features

- ✅ Captures text messages from any WhatsApp chat
- ✅ Captures group messages (with group name)
- ✅ Saves image previews from notifications  
- ✅ Copies full images from WhatsApp media folder
- ✅ Filter: All messages / Deleted only
- ✅ Search messages by text or sender
- ✅ Runs in background, survives phone restarts
- ✅ 100% offline — no internet required
- ✅ No root needed

---

## Setup Instructions

### Requirements
- Android Studio (Hedgehog or newer): https://developer.android.com/studio
- Android phone running Android 5.0+
- USB cable OR wireless debugging

### Step 1: Open in Android Studio
1. Download/clone this folder
2. Open Android Studio → **Open** → select the `WhatsAppVault` folder
3. Wait for Gradle sync to finish (first time downloads dependencies ~2 min)

### Step 2: Build & Install
**Option A — Run directly on phone:**
1. Enable Developer Options on your phone:
   - Settings → About Phone → tap "Build Number" 7 times
2. Enable USB Debugging in Developer Options
3. Connect phone via USB
4. In Android Studio, select your device and click ▶ Run

**Option B — Build APK to share/install manually:**
1. Build → Generate Signed Bundle/APK → APK
2. Or for debug: Build → Build Bundle(s)/APK(s) → Build APK(s)
3. Find APK at: `app/build/outputs/apk/debug/app-debug.apk`
4. Transfer to phone and install (enable "Install unknown apps" in settings)

### Step 3: Grant Permissions (IMPORTANT)
When you first open the app, it will ask for **Notification Access**:
1. Tap **"Open Settings"** in the dialog
2. Find **"WhatsApp Vault"** in the list
3. Toggle it **ON**
4. Go back to the app — status bar will show **● Active**

For image capturing, also grant **Storage permission** when prompted.

---

## How Images Are Captured

### Method 1: Notification thumbnails
When someone sends you an image, WhatsApp shows a preview in the notification.
The app grabs this thumbnail bitmap and saves it.

**Limitation:** This is a compressed preview, not the full image.

### Method 2: Media folder watcher (Full quality)
The app watches WhatsApp's image download folder:
```
/storage/emulated/0/WhatsApp/Media/WhatsApp Images/
```
The moment a new image appears (downloaded), it's copied to the vault.

**Saved images location:** Private app storage (not visible in Gallery)
```
/data/data/com.whatsappvault/files/WhatsAppVault/Images/
```

---

## Limitations

| Limitation | Reason |
|---|---|
| Only captures from notifications | WhatsApp uses E2E encryption — can't intercept network |
| If you have WhatsApp open, notifications may not fire | Android doesn't fire notifications for active conversations |
| Voice messages / videos not captured | Too large for notification; would need root for full access |
| iOS not supported | iOS doesn't allow notification listener services |

---

## Project Structure

```
WhatsAppVault/
├── app/src/main/
│   ├── java/com/whatsappvault/
│   │   ├── service/
│   │   │   ├── WhatsAppNotificationListener.kt  ← Core listener
│   │   │   ├── ImageObserverService.kt           ← Watches media folder
│   │   │   └── BootReceiver.kt                  ← Auto-restart on boot
│   │   ├── database/
│   │   │   ├── MessageEntity.kt                 ← Data model
│   │   │   ├── MessageDao.kt                    ← Database queries
│   │   │   └── AppDatabase.kt                   ← Room DB setup
│   │   ├── ui/
│   │   │   ├── MainActivity.kt                  ← Message list
│   │   │   ├── MessageDetailActivity.kt         ← View single message
│   │   │   ├── MessageAdapter.kt                ← RecyclerView adapter
│   │   │   ├── MessageViewModel.kt              ← LiveData + filtering
│   │   │   └── SettingsActivity.kt
│   │   └── utils/
│   │       └── ImageSaver.kt                    ← Image copy/save utility
│   └── res/
│       ├── layout/                              ← XML layouts
│       ├── drawable/                            ← Shapes/icons
│       ├── menu/                                ← Menu items
│       └── values/                              ← Colors, strings, themes
└── README.md
```

---

## Privacy & Legal

> ⚠️ **Only use this app on your own device to monitor your own messages.**
> Monitoring someone else's device or messages without their knowledge is
> illegal in most countries. This app is for personal use only.
