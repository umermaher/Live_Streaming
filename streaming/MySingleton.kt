package com.beautybarber.app.ui.activities.streaming

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

class MySingleton constructor(context: Context) {
    companion object {
        @Volatile
        private var INTANCE: MySingleton? = null
        fun getInstance(context: Context) =
            INTANCE ?: synchronized(this) {
                INTANCE ?: MySingleton(context).also {
                    INTANCE = it
                }
            }
    }

    val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }

    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }
}