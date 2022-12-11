package com.beautybarber.app.ui.activities.streaming

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.beautybarber.app.databinding.ActivityLiveSessionBinding
import com.beautybarber.app.models.Message
import com.beautybarber.app.utils.Constants.APP_ID
import com.beautybarber.app.utils.Constants.CHANNEL_NAME
import com.beautybarber.app.utils.Constants.TOKEN_FOR_LIVE
import com.beautybarber.app.utils.SharedPreferences
import com.beautybarber.app.utils.Utilities
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtm.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class LiveSessionActivity : AppCompatActivity() {
    private lateinit var channelName: String
    private lateinit var binding:ActivityLiveSessionBinding
    private var userRole:Int=0
    private var rtcEngine: RtcEngine?=null
    private var rtmClient: RtmClient?=null
    private var rtmChannel: RtmChannel?=null
    private lateinit var msgAdapter:LiveMsgAdapter
    private val msgList=ArrayList<Message>()

    companion object{
        const val USER_ROlE="userRole"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityLiveSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val token=intent.getStringExtra(TOKEN_FOR_LIVE)
        channelName=intent.getStringExtra(CHANNEL_NAME)!!
        val userId=SharedPreferences.getUserID(this)?.toInt()
        token?.let { initAgoraEngineAndJoinChannel(it,userId) }

//        Toast.makeText(this,token, Toast.LENGTH_LONG).show()

        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
        binding.messageText.setOnClickListener {
            binding.sendMsgButton.visibility= View.VISIBLE
        }

        binding.sendMsgButton.setOnClickListener {
            if(binding.messageText.text.isNotEmpty()){
//                Toast.makeText(this,"sending",Toast.LENGTH_SHORT).show()
                sendChannelMessage()
            }
        }

//        binding.msgScrollView.fullScroll(ScrollView.FOCUS_UP)

        msgAdapter= LiveMsgAdapter()
        binding.rvMsg.layoutManager=LinearLayoutManager(this)
        binding.rvMsg.adapter=msgAdapter

        getTokenForLiveMessaging()
    }

    private fun initAgoraEngineAndJoinChannel(token: String, userId: Int?) {
        initAgoraEngine()

        rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        rtcEngine?.setClientRole(userRole!!)
        rtcEngine?.enableVideo()

//        if(userRole==1)
//            setUpLocalVideo()
//        else
//            binding.localVideoViewContainer.visibility=View.GONE

        joinChannel(token,userId)
    }

    private val mRtcEventHandler=object : IRtcEngineEventHandler(){
        var count:Int=0
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.i("VideoStreamingActivity","Join user success : $uid")
            count=1
            runOnUiThread {
                setUpRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
            onRemoteUserLeft()
            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.i("VideoStreamingActivity","Join channel success : $channel")
            lifecycleScope.launch {
                delay(6000)
                if(count==0){
                    runOnUiThread { Toast.makeText(this@LiveSessionActivity,"Host has not joined yet, ",Toast.LENGTH_SHORT).show() }
                }
            }
        }

    }
    private fun initAgoraEngine() {
        try{
            rtcEngine= RtcEngine.create(this, APP_ID,mRtcEventHandler)
        }catch(e:Exception){
            Log.e("VideoStreamingActivity","rtc: "+e.message.toString())
        }
    }

    private fun joinChannel(token: String, userId: Int?) {
        rtcEngine?.setDefaultAudioRoutetoSpeakerphone(true)
        rtcEngine?.joinChannel(token,channelName,null,userId!!)
    }
    private fun setUpRemoteVideo(uid:Int){
        if(binding.remoteVideoViewContainer.childCount>=1) return
        val surfaceView= RtcEngine.CreateRendererView(this)
        //  surfaceView.setZOrderMediaOverlay(true)
        binding.remoteVideoViewContainer.addView(surfaceView)
        rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT,uid))
        surfaceView.tag=uid

        binding.sendMessageContainer.visibility=View.VISIBLE
//        rtcEngine?.setEnableSpeakerphone(true)
    }

    private fun onRemoteUserLeft(){
        binding.remoteVideoViewContainer.removeAllViews()
//        Handler().postDelayed(Runnable {
//            if(binding.remoteVideoViewContainer.childCount==0) {
//                finish()
//                Toast.makeText(this,"Stream has ended!",Toast.LENGTH_SHORT).show()
//            }
//        },6000)
    }

    private fun getTokenForLiveMessaging(){
        Utilities.showProgressBarDialog(this)
        val userId=SharedPreferences.getName(this)

        val url = "http://beautybarber2.jumppace.com:8080/rtm/$userId"
        val newsJsonObjectRequest = object : JsonObjectRequest(
            Request.Method.GET, url, null,

            { response ->
                if (Utilities.isShowing()) {
                    Utilities.dismissProgressDialog()
                }
                try{
                    val rtmToken=response.getString("rtmToken")
//                    Toast.makeText(this,rtmToken,Toast.LENGTH_SHORT).show()
                    initializeRtmClient()
                    logInToRtmSystem(rtmToken,userId)
                }catch(e:Exception){
                    Toast.makeText(this,"Error: ${e.message.toString()}",Toast.LENGTH_SHORT).show()
                }
            },
            {
                if (Utilities.isShowing()) {
                    Utilities.dismissProgressDialog()
                }
                Toast.makeText(this,it.message, Toast.LENGTH_LONG).show()
            })
        {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> {
                val headers=HashMap<String,String>()
                headers["User-Agent"]="Mozilla/5.0"
                return headers
            }
        }

        // Access the RequestQueue through your singleton class.
        MySingleton.getInstance(this).addToRequestQueue(newsJsonObjectRequest)
    }

    private fun initializeRtmClient() {
        try{
            rtmClient=RtmClient.createInstance(this, APP_ID,rtmClientListener())
        }catch (e:Exception){
            Log.e("VideoStreamingActivity","rtm: "+e.message.toString())
        }
    }

    private fun rtmClientListener(): RtmClientListener? = object:RtmClientListener{
        override fun onConnectionStateChanged(state: Int, reason: Int) {
            val text = "Connection state changed to ${state.toString()}Reason: ${reason.toString()}"
            Log.d("rtmConnection",text)
        }

        override fun onMessageReceived(p0: RtmMessage?, p1: String?) {

        }

        override fun onImageMessageReceivedFromPeer(p0: RtmImageMessage?, p1: String?) {

        }

        override fun onFileMessageReceivedFromPeer(p0: RtmFileMessage?, p1: String?) {

        }

        override fun onMediaUploadingProgress(p0: RtmMediaOperationProgress?, p1: Long) {

        }

        override fun onMediaDownloadingProgress(p0: RtmMediaOperationProgress?, p1: Long) {

        }

        override fun onTokenExpired() {
        }

        override fun onPeersOnlineStatusChanged(p0: MutableMap<String, Int>?) {
        }
    }

    private fun logInToRtmSystem(rtmToken: String, uid: String?) {
        rtmClient?.login(rtmToken,uid.toString(),object: ResultCallback<Void> {
            override fun onSuccess(responseInfo: Void?) {
                Log.i("VideoStreamingActivity","Login to RtmSystem successfully")
                joinRtmChannel(uid)
            }
            override fun onFailure(errorInfo: ErrorInfo) {
                val text: CharSequence =
                    "User: $uid failed to log in to the RTM system!$errorInfo"
                Log.e("VideoStreamingActivity","rtm: "+errorInfo.toString())
                val duration = Toast.LENGTH_SHORT
                runOnUiThread {
                    val toast = Toast.makeText(applicationContext, text, duration)
                    toast.show()
                }
            }
        })
    }

    private fun joinRtmChannel(uid: String?) {
        val rtmChannelListener=object: RtmChannelListener{
            override fun onMemberCountUpdated(p0: Int) {
            }

            override fun onAttributesUpdated(p0: MutableList<RtmChannelAttribute>?) {
            }

            override fun onMessageReceived(p0: RtmMessage?, p1: RtmChannelMember?) {
                val text = p0?.text.toString()
                runOnUiThread {
                    if (p1 != null) {
                        writeToMessageHistory(p1.userId,text)
                    }
                }
            }

            override fun onImageMessageReceived(p0: RtmImageMessage?, p1: RtmChannelMember?) {
            }

            override fun onFileMessageReceived(p0: RtmFileMessage?, p1: RtmChannelMember?) {
            }

            override fun onMemberJoined(p0: RtmChannelMember?) {
                Log.i("VideoStreamingActivity","${p0?.userId}: Member has joined")
            }

            override fun onMemberLeft(p0: RtmChannelMember?) {
            }
        }

        try{
            rtmChannel=rtmClient?.createChannel(channelName,rtmChannelListener)
        }catch(e: Exception){
            Log.e("VideoStreamingActivity","rtmChannel: "+e.message.toString())
        }

        rtmChannel?.join(object: ResultCallback<Void> {
            override fun onSuccess(responseInfo: Void?) {
                Log.i("VideoStreamingActivity","Member has joined Successfully")
            }
            override fun onFailure(errorInfo: ErrorInfo) {
                val text: CharSequence =
                    "User: $uid failed to join the RTM Channel!$errorInfo"
                val duration = Toast.LENGTH_SHORT
                Log.e("VideoStreamingActivity","rtm: "+errorInfo.toString())

                runOnUiThread {
                    val toast = Toast.makeText(applicationContext, text, duration)
                    toast.show()
                }
            }
        })
    }

    private fun sendChannelMessage(){
        val rtmMessage=rtmClient?.createMessage()
        rtmMessage?.text=binding.messageText.text.toString()

        val username=SharedPreferences.getName(this)
        rtmChannel?.sendMessage(rtmMessage,object: ResultCallback<Void> {
            override fun onSuccess(responseInfo: Void?) {
                val text = "$username : ${rtmMessage?.text.toString()}"
                runOnUiThread { username?.let {
                    binding.messageText.text.clear()
                    writeToMessageHistory(it,rtmMessage?.text.toString()) }
                }
            }
            override fun onFailure(errorInfo: ErrorInfo) {
                runOnUiThread {
                    val toast = Toast.makeText(applicationContext, "Failed to send Message: ${errorInfo.toString()}", Toast.LENGTH_SHORT)
                    toast.show()
                }
            }
        })
    }

    private fun writeToMessageHistory(username:String,text: String) {
//        binding.tvMessage.append(text+"\n")
        msgList.add(Message(username,text,System.currentTimeMillis()))
        msgAdapter.messages=msgList.toList()
        binding.rvMsg.smoothScrollToPosition(msgAdapter.messages.size)

//        Toast.makeText(this,text,Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtmChannel?.leave(null)
        rtcEngine=null
    }
}