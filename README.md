# WhatsAlarm-clean
Reads whatsapp notification to ring alarm.
gradle clean assembleDebug

adb logcat -c
adb logcat *:E OR adb logcat *:E | findstr com.example.whatsalarm
