/*
 * Copyright (c) 2020 Eric Wei. All rights reserved.
 */

package com.ericwei.podster.service

import okhttp3.*
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory

class RssFeedService : FeedService {
    override fun getFeed(xmlFileURL: String, callBack: (RssFeedResponse?) -> Unit) {
        val request = Request.Builder()
            .url(xmlFileURL)
            .build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callBack(null)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val dbFactory = DocumentBuilderFactory.newInstance()
                        val dbBuilder = dbFactory.newDocumentBuilder()
                        //read RSS file into Document object
                        val doc = dbBuilder.parse(responseBody.byteStream())
                        return
                    }
                }
                callBack(null)
            }
        })
    }
}

interface FeedService {
    fun getFeed(xmlFileURL: String, callBack: (RssFeedResponse?) -> Unit)

    companion object {
        val instance: FeedService by lazy {
            RssFeedService()
        }
    }
}