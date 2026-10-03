package com.example.service

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import com.example.model.ToolExecution
import com.example.model.ToolStatus

class AndroidActionManager(private val context: Context) {

    fun executeTool(toolName: String, params: Map<String, String>): ToolExecution {
        return try {
            when (toolName.lowercase()) {
                "open_app" -> handleOpenApp(params["app_name"] ?: "")
                "open_settings" -> handleOpenSettings(params["setting_type"] ?: "")
                "search_web" -> handleSearchWeb(params["query"] ?: "")
                "make_call" -> handleMakeCall(params["contact_or_number"] ?: "")
                "set_timer" -> handleSetTimer(params["seconds"]?.toIntOrNull() ?: 60, params["label"] ?: "Zornia Timer")
                "set_alarm" -> handleSetAlarm(
                    params["hour"]?.toIntOrNull() ?: 8,
                    params["minutes"]?.toIntOrNull() ?: 0,
                    params["label"] ?: "Zornia Alarm"
                )
                "control_media" -> handleMediaControl(params["action"] ?: "toggle")
                "get_battery_status" -> handleBatteryStatus()
                else -> ToolExecution(
                    toolName = toolName,
                    arguments = params,
                    status = ToolStatus.FAILED,
                    errorMessage = "Action '$toolName' is not supported yet."
                )
            }
        } catch (e: Exception) {
            ToolExecution(
                toolName = toolName,
                arguments = params,
                status = ToolStatus.FAILED,
                errorMessage = "Could not complete action: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun handleOpenApp(appName: String): ToolExecution {
        val cleanName = appName.trim().lowercase()
        val pm: PackageManager = context.packageManager

        // Known direct package mappings
        val knownPackages = mapOf(
            "whatsapp" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "spotify" to "com.spotify.music",
            "chrome" to "com.android.chrome",
            "google chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "calendar" to "com.google.android.calendar",
            "clock" to "com.google.android.deskclock",
            "calculator" to "com.google.android.calculator"
        )

        // Check explicit intents first for system utilities
        if (cleanName.contains("camera")) {
            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (cameraIntent.resolveActivity(pm) != null) {
                context.startActivity(cameraIntent)
                return ToolExecution(
                    toolName = "open_app",
                    arguments = mapOf("app_name" to appName),
                    status = ToolStatus.SUCCESS,
                    userFriendlyMessage = "Camera is open."
                )
            }
        }

        if (cleanName.contains("dialer") || cleanName == "phone") {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            return ToolExecution(
                toolName = "open_app",
                arguments = mapOf("app_name" to appName),
                status = ToolStatus.SUCCESS,
                userFriendlyMessage = "Phone dialer is open."
            )
        }

        if (cleanName.contains("setting")) {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            return ToolExecution(
                toolName = "open_app",
                arguments = mapOf("app_name" to appName),
                status = ToolStatus.SUCCESS,
                userFriendlyMessage = "Settings are open."
            )
        }

        val targetPackage = knownPackages[cleanName]
        if (targetPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ToolExecution(
                    toolName = "open_app",
                    arguments = mapOf("app_name" to appName),
                    status = ToolStatus.SUCCESS,
                    userFriendlyMessage = "$appName is open."
                )
            }
        }

        // Generic search by installed app label
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label.contains(cleanName) || cleanName.contains(label)) {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ToolExecution(
                        toolName = "open_app",
                        arguments = mapOf("app_name" to appName),
                        status = ToolStatus.SUCCESS,
                        userFriendlyMessage = "$appName is open."
                    )
                }
            }
        }

        // If app isn't installed on device, gracefully open browser / market
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$cleanName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (marketIntent.resolveActivity(pm) != null) {
            context.startActivity(marketIntent)
            ToolExecution(
                toolName = "open_app",
                arguments = mapOf("app_name" to appName),
                status = ToolStatus.SUCCESS,
                userFriendlyMessage = "Looking for $appName on Google Play."
            )
        } else {
            handleSearchWeb("Download $appName Android")
        }
    }

    private fun handleOpenSettings(settingType: String): ToolExecution {
        val action = when (settingType.lowercase().trim()) {
            "wifi", "wi-fi", "internet" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "screen", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume", "audio" -> Settings.ACTION_SOUND_SETTINGS
            "battery", "power" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "apps", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
            "date", "time" -> Settings.ACTION_DATE_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        return ToolExecution(
            toolName = "open_settings",
            arguments = mapOf("setting_type" to settingType),
            status = ToolStatus.SUCCESS,
            userFriendlyMessage = "${if (settingType.isBlank()) "System" else settingType.replaceFirstChar { it.uppercase() }} settings opened."
        )
    }

    private fun handleSearchWeb(query: String): ToolExecution {
        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val executed = if (searchIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(searchIntent)
            true
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            true
        }

        return ToolExecution(
            toolName = "search_web",
            arguments = mapOf("query" to query),
            status = if (executed) ToolStatus.SUCCESS else ToolStatus.FAILED,
            userFriendlyMessage = "Searching the web for '$query'."
        )
    }

    private fun handleMakeCall(contactOrNumber: String): ToolExecution {
        val cleanNumber = contactOrNumber.filter { it.isDigit() || it == '+' }
        val intent = if (cleanNumber.isNotEmpty()) {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
        } else {
            Intent(Intent.ACTION_DIAL)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return ToolExecution(
            toolName = "make_call",
            arguments = mapOf("contact_or_number" to contactOrNumber),
            status = ToolStatus.SUCCESS,
            userFriendlyMessage = if (cleanNumber.isNotEmpty()) "Dialer ready for $contactOrNumber." else "Opening phone dialer."
        )
    }

    private fun handleSetTimer(seconds: Int, label: String): ToolExecution {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ToolExecution(
                toolName = "set_timer",
                arguments = mapOf("seconds" to seconds.toString(), "label" to label),
                status = ToolStatus.SUCCESS,
                userFriendlyMessage = "Timer set for ${seconds} seconds ($label)."
            )
        } else {
            ToolExecution(
                toolName = "set_timer",
                arguments = mapOf("seconds" to seconds.toString()),
                status = ToolStatus.FAILED,
                errorMessage = "Clock timer app is not available on this device."
            )
        }
    }

    private fun handleSetAlarm(hour: Int, minutes: Int, label: String): ToolExecution {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            val formattedTime = String.format("%02d:%02d", hour, minutes)
            ToolExecution(
                toolName = "set_alarm",
                arguments = mapOf("hour" to hour.toString(), "minutes" to minutes.toString(), "label" to label),
                status = ToolStatus.SUCCESS,
                userFriendlyMessage = "Alarm set for $formattedTime ($label)."
            )
        } else {
            ToolExecution(
                toolName = "set_alarm",
                arguments = mapOf("hour" to hour.toString()),
                status = ToolStatus.FAILED,
                errorMessage = "Alarm clock app is not available on this device."
            )
        }
    }

    private fun handleMediaControl(action: String): ToolExecution {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val keyCode = when (action.lowercase()) {
            "play", "pause", "toggle" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }

        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))

        return ToolExecution(
            toolName = "control_media",
            arguments = mapOf("action" to action),
            status = ToolStatus.SUCCESS,
            userFriendlyMessage = "Media $action triggered."
        )
    }

    private fun handleBatteryStatus(): ToolExecution {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = batteryManager?.isCharging == true

        val message = if (level >= 0) {
            "Battery is at $level%${if (isCharging) " (Charging)" else ""}."
        } else {
            "Battery status unavailable."
        }

        return ToolExecution(
            toolName = "get_battery_status",
            arguments = mapOf("level" to level.toString(), "charging" to isCharging.toString()),
            status = ToolStatus.SUCCESS,
            userFriendlyMessage = message
        )
    }
}
