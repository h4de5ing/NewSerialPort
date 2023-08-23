package com.android.serialport2.other

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.os.Process
import android.widget.Toast
import com.android.serialport2.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.system.exitProcess

class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {
    companion object {
        @SuppressLint("StaticFieldLeak")
        val instance = CrashHandler()
    }

    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    private var mContext: Context? = null
    private val appInformation: MutableMap<String, String> = HashMap()

    @SuppressLint("SimpleDateFormat")
    private val formatter: DateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
    private var logDir: File? = null
    fun init(context: Context) {
        mContext = context
        logDir = context.getExternalFilesDir("log")
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler!!.uncaughtException(thread, ex)
        } else {
            try {
                Thread.sleep(3000)
            } catch (ignored: InterruptedException) {
            }
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }

    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) return false
        Thread {
            Looper.prepare()
            Toast.makeText(mContext, mContext?.packageName + " crashed", Toast.LENGTH_SHORT).show()
            Looper.loop()
        }.start()
        collectDeviceInfo()
        saveCrashInfo2File(ex)
        return true
    }

    private fun collectDeviceInfo() {
        try {
            appInformation["versionName"] = BuildConfig.VERSION_NAME
            appInformation["versionCode"] = "${BuildConfig.VERSION_CODE}"
        } catch (ignored: Exception) {
        }
        Build::class.java.declaredFields.apply {
            for (field in this) {
                try {
                    field.isAccessible = true
                    appInformation[field.name] = "${field[null]}"
                } catch (ignored: Exception) {
                }
            }
        }
    }

    private fun saveCrashInfo2File(ex: Throwable) {
        val sb = StringBuilder()
        for ((key, value) in appInformation) {
            sb.append(key).append("=").append(value).append("\n")
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
        try {
            val timestamp = System.currentTimeMillis()
            val time = formatter.format(Date())
            val fileName = time + "_" + timestamp + ".txt"
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                val file = File(logDir, fileName)
                val fos = FileOutputStream(file)
                fos.write(sb.toString().toByteArray())
                fos.close()
            }
        } catch (ignored: Exception) {
        }
    }
}