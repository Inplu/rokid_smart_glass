package com.rokid.phone

import com.rokid.phone.utils.demoString
import com.rokid.phone.R

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.rokid.phone.databinding.ActivityMessageBinding
import com.rokid.phone.utils.ExternalAudioPlayer
import com.rokid.security.phone.sdk.api.PSecuritySDK
import com.rokid.security.phone.sdk.api.msg.listener.FileReceiveListener
import com.rokid.security.phone.sdk.api.msg.listener.FileReceiveV2Listener
import com.rokid.security.phone.sdk.api.msg.listener.IMessageListener
import kotlinx.coroutines.launch

class MessageReceiveActivity : ComponentActivity() {

    private val TAG = "MessageReceiveActivity"
    private lateinit var binding: ActivityMessageBinding
    private lateinit var audioPlayer: ExternalAudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "初始化")
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioPlayer = ExternalAudioPlayer()
        audioPlayer.start()

        PSecuritySDK.getMessageService()?.addMessageListener(mMessageListener)
        PSecuritySDK.getMessageService()?.getFileOperater()?.addFileReceiveV2Listener(mFileReceiveListener)
        PSecuritySDK.getMessageService()?.getBtFileOperater()?.addFileReceiveV2Listener(mFileReceiveListener)
    }

    private val logBuilder = StringBuilder(4000)
    private fun log(msg: String) {
        Log.e(TAG, msg)
        lifecycleScope.launch {
            if (logBuilder.length > 4000) {
                logBuilder.clear()
            }
            logBuilder.insert(0, "$msg\n")
            binding.tvLog.text = logBuilder.toString()
        }
    }

    private val mFileReceiveListener = object : FileReceiveV2Listener {
        override fun onStart(filePath: String) {
            super.onStart(filePath)
            log(demoString(R.string.demo_started_receiving_file, filePath))
        }

        override fun onProgressChanged(filePath: String,progress: Float) {
            log(demoString(R.string.demo_file_reception_progress, progress))
        }

        override fun onComplete(filePath: String) {
            log(demoString(R.string.demo_file_received, filePath))
        }

        override fun onFail() {
            log(demoString(R.string.demo_failed_to_receive_file))
        }

        override fun onCancel(filePath: String) {
            log(demoString(R.string.demo_sender_canceled_file_transfer, filePath))
        }
    }

    private val mMessageListener = object : IMessageListener {
        @SuppressLint("SetTextI18n")
        override fun onClassicBTTextMessage(msg: String, clientId: String) {
            super.onClassicBTTextMessage(msg, clientId)
            log(demoString(R.string.demo_onclassicbttextmessage_status_32, msg, clientId))
        }

        @SuppressLint("SetTextI18n")
        override fun onP2PTextMessage(msg: String, clientId: String) {
            super.onP2PTextMessage(msg, clientId)
            log(demoString(R.string.demo_onp2ptextmessage_status_33, msg, clientId))
        }

        override fun onClassicBTAudioStream(buffer: ByteArray) {
            log(demoString(R.string.demo_bluetooth_audio_received_bytes, buffer.size))
            audioPlayer.writeAudioData(buffer)
        }
    }

    override fun onDestroy() {
        DeviceLinkerManager.stopAudioStream()
        super.onDestroy()
        PSecuritySDK.getMessageService()?.removeMessageListener(mMessageListener)
        audioPlayer.release()
    }

}
