package com.android.serialport2.other

import android.content.Context
import android.os.Build
import android.os.Looper
import android.os.Process
import android.widget.Toast
import com.android.serialport2.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CrashHandler : Thread.UncaughtExceptionHandler {
    companion object {
        val instance: CrashHandler = CrashHandler()
    }

    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    private var mContext: Context? = null
    private val infos: MutableMap<String, String> = mutableMapOf()
    private var logDir: File? = null

    private val lock = Any()

    @Volatile
    private var installed: Boolean = false

    fun init(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            if (installed) return
            installed = true
            mContext = appContext
            logDir = resolveLogDir(appContext)
            mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(this)
        }
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        try {
            if (!handleException(ex) && mDefaultHandler != null) {
                mDefaultHandler?.uncaughtException(thread, ex)
                return
            } else {
                try {
                    Thread.sleep(3000)
                } catch (_: InterruptedException) {
                }
                Process.killProcess(Process.myPid())
                exitProcess(1)
            }
        } catch (_: Throwable) {
        }
    }

    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) return false
        val ctx = mContext ?: return false
        try {
            Thread {
                try {
                    Looper.prepare()
                    Toast.makeText(
                        ctx,
                        "串口工具异常退出,正在收集日志",
                        Toast.LENGTH_LONG
                    )
                        .show()
                    Looper.loop()
                } catch (_: InterruptedException) {
                }
            }.start()
        } catch (_: Throwable) {
        }
        collectDeviceInfo()
        saveCrashInfo2File(ex)
        return true
    }

    fun collectDeviceInfo() {
        synchronized(lock) {
            infos.clear()
            infos["versionName"] = BuildConfig.VERSION_NAME
            infos["versionCode"] = "${BuildConfig.VERSION_CODE}"
            // 设备信息：使用白名单，避免全反射导致日志过大/含敏感字段
            infos["sdkInt"] = Build.VERSION.SDK_INT.toString()
            infos["release"] = Build.VERSION.RELEASE ?: "unknown"
            infos["incremental"] = Build.VERSION.INCREMENTAL ?: "unknown"
            infos["brand"] = Build.BRAND ?: "unknown"
            infos["manufacturer"] = Build.MANUFACTURER ?: "unknown"
            infos["model"] = Build.MODEL ?: "unknown"
            infos["device"] = Build.DEVICE ?: "unknown"
            infos["product"] = Build.PRODUCT ?: "unknown"
            infos["hardware"] = Build.HARDWARE ?: "unknown"
            infos["fingerprint"] = Build.FINGERPRINT ?: "unknown"
        }
    }

    private fun saveCrashInfo2File(ex: Throwable) {
        val sb = StringBuilder()
        sb.append("time=").append(nowTimestamp()).append('\n')
        sb.append("thread=").append(Thread.currentThread().name).append('\n')
        val infoSnapshot: Map<String, String> = synchronized(lock) { HashMap(infos) }
        for ((key, value) in infoSnapshot.entries) {
            sb.append(key).append('=').append(value).append('\n')
        }
        val writer: Writer = StringWriter()
        val printWriter = PrintWriter(writer)
        ex.printStackTrace(printWriter)
        var cause = ex.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()
        val result = writer.toString()
        sb.append(result)
        val time = nowTimestamp()
        val fileName = "$time.log"
        val primaryFile = File(logDir, fileName)
        tryWriteText(primaryFile, sb.toString())
    }

    private fun resolveLogDir(ctx: Context): File {
        val ext = ctx.getExternalFilesDir("LOG")
        val dir = ext ?: File(ctx.filesDir, "LOG")
        if (!dir.exists()) {
            try {
                dir.mkdirs()
            } catch (_: Exception) {
            }
        }
        return dir
    }

    private fun nowTimestamp(): String {
        return try {
            SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        } catch (_: Throwable) {
            Date().time.toString()
        }
    }

    private fun tryWriteText(file: File, content: String) {
        return try {
            file.parentFile?.let {
                if (!it.exists()) {
                    try {
                        it.mkdirs()
                    } catch (_: Exception) {
                    }
                }
            }
            file.outputStream().use { it.write(content.toByteArray()) }
        } catch (_: Throwable) {
        }
    }
}