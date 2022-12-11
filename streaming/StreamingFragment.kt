package com.beautybarber.app.ui.activities.streaming

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.beautybarber.app.databinding.FragmentStreamingBinding
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas

/**
 * This Fragment is not in use. LiveSessionActivity replace it
 * */
class StreamingFragment : Fragment() {
    private var _binding:FragmentStreamingBinding?=null
    private val binding get() = _binding!!

    private var channelName:String="1"
    private var userRole:Int=0
    private var rtcEngine: RtcEngine?=null
//    private var token=""

    companion object{
        const val USER_ROlE="userRole"
        const val CHANNEL_NAME="channelName"
        const val APP_ID="06e0574051d14714b7ce69eac307e6d6"
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding=FragmentStreamingBinding.inflate(layoutInflater)


        getToken()

        return binding.root
    }
    private fun getToken(){
        val url = "http://beautybarber2.jumppace.com:8080/rtc/1/audience/uid/453456"
        val newsJsonObjectRequest = object : JsonObjectRequest(
            Request.Method.GET, url, null,

            { response ->
//                val newsJsonArray = response.getJSONArray("articles")
                val token=response.getString("rtcToken")
                Toast.makeText(requireContext(),token,Toast.LENGTH_LONG).show()
                initAgoraEngineAndJoinChannel(token)

            },
            {
                Toast.makeText(requireContext(),it.message,Toast.LENGTH_LONG).show()
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
        MySingleton.getInstance(requireContext()).addToRequestQueue(newsJsonObjectRequest)
    }
    private fun initAgoraEngineAndJoinChannel(token: String) {
        initAgoraEngine()

        rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        rtcEngine?.setClientRole(userRole!!)
        rtcEngine?.enableVideo()

//        if(userRole==1)
//            setUpLocalVideo()
//        else
//            binding.localVideoViewContainer.visibility=View.GONE

        joinChannel(token)
    }

    private val mRtcEventHandler=object : IRtcEngineEventHandler(){
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.i("VideoStreamingActivity","Join user success : $uid")
//            runOnUiThread {
                setUpRemoteVideo(uid)
//            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
//            runOnUiThread {
                onRemoteUserLeft()
//            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.i("VideoStreamingActivity","Join channel success : $uid")
        }
    }
    private fun initAgoraEngine() {
        try{
            rtcEngine=RtcEngine.create(requireContext(), APP_ID,mRtcEventHandler)
        }catch(e:Exception){
            Log.e("VideoStreamingActivity",e.message.toString())
        }
    }

    private fun joinChannel(token: String) {
        rtcEngine?.joinChannel(token,channelName,null,453456)
    }
    private fun setUpRemoteVideo(uid:Int){
        if(binding.remoteVideoViewContainer.childCount>=1) return
        val surfaceView=RtcEngine.CreateRendererView(requireContext())
        //  surfaceView.setZOrderMediaOverlay(true)
        binding.remoteVideoViewContainer.addView(surfaceView)
        rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView,VideoCanvas.RENDER_MODE_FIT,uid))
        surfaceView.tag=uid
    }

    private fun onRemoteUserLeft(){
        binding.remoteVideoViewContainer.removeAllViews()

    }

    override fun onDestroy() {
        super.onDestroy()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine=null
    }
}