# WhatsAlarm-clean
Reads whatsapp notification to ring alarm.
gradle clean assembleDebug or gradle assembleDebug --stacktrace

adb logcat -c
adb logcat *:E OR adb logcat *:E | findstr com.example.whatsalarm
