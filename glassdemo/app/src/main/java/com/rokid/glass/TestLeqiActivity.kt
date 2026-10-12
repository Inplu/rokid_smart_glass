package com.rokid.glass

import com.rokid.glass.utils.demoString
import com.rokid.glesse.R

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.rokid.glass.base.BaseGlassActivity
import com.rokid.glesse.databinding.ActivityTestLeqiBinding
import com.rokid.security.glass3.open.sdk.GlassSdk
import com.rokid.security.glass3.open.sdk.callback.LeqiInterceptor
import com.rokid.security.glass3.open.sdk.uitls.log.L
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TestLeqiActivity : BaseGlassActivity() {

    private val TAG = "TestLeqiActivity::"
    private lateinit var binding: ActivityTestLeqiBinding
    private var logCount = 0
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private var logJob: Job? = null

    private val leqiInterceptor = object : LeqiInterceptor {
        override fun onLeqiInstruction(extra: String?): Boolean {
            val time = dateFormat.format(Date())
            appendLog(demoString(R.string.demo_leqi_command_received_extra, time, extra))
            L.d(TAG, "Leqi 指令拦截: extra=$extra")
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTestLeqiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        GlassSdk.registerLeqiInterceptor(this, leqiInterceptor)
        binding.tvInterceptorStatus.text = demoString(R.string.demo_interceptor_registered)
        appendLog(demoString(R.string.demo_leqiinterceptor_registered))
        updateStatus()

        logJob = lifecycleScope.launch {
            while (isActive) {
                delay(2000)
                updateStatus()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        GlassSdk.unregisterLeqiInterceptor()
        binding.tvInterceptorStatus.text = demoString(R.string.demo_interceptor_not_registered)
        appendLog(demoString(R.string.demo_leqiinterceptor_unregistered))
        logJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        logJob?.cancel()
    }

    private fun updateStatus() {
        binding.tvForegroundStatus.text = demoString(R.string.demo_foreground_yes)
    }

    private fun appendLog(msg: String) {
        logCount++
        val text = binding.tvLog.text.toString()
        val newText = if (text == demoString(R.string.demo_none)) "$logCount. $msg" else "$text\n$logCount. $msg"
        val lines = newText.split("\n")
        val trimmed = if (lines.size > 50) lines.takeLast(50).joinToString("\n") else newText
        binding.tvLog.text = trimmed
    }
}
