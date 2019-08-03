/*
 * Copyright 2017-2019 (c) Alexander "ILakeful" Shevchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ilakeful.lakebot.utils

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchResult
import com.google.api.services.youtube.model.Video

import io.ilakeful.lakebot.Immutable

import java.io.IOException
import java.time.Duration

object YouTubeUtils {
    private val KEY = Immutable.YOUTUBE_API_KEY
    @JvmField
    val YOUTUBE = YouTube.Builder(NetHttpTransport(), JacksonFactory()) { _ -> }.setApplicationName("lakebot").build()
    @JvmField
    val VIDEO_REGEX = "(?:https?://)?(?:youtu\\.be/|(?:(www\\.)|(m\\.)|(music\\.))?youtube\\.com/watch(?:\\.php)?\\?.*v=)([\\w\\d-_]){11}(?:(?<list>(PL|LL|FL|UU)[\\w\\d-_]+))?".toRegex()
    @Throws(IOException::class)
    fun getResults(query: String, limit: Long = 5): List<SearchResult> {
        val list = YOUTUBE.search().list("id,snippet")
        list.key = KEY
        list.q = query
        list.fields = "items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)"
        list.maxResults = limit
        list.type = "video"
        return list.execute().items
    }
    @Throws(IOException::class)
    fun getVideos(query: String, limit: Long = 5): List<Video> {
        val results: List<SearchResult> = getResults(query, limit)
        val idsToQuery = mutableListOf<String>()
        for (result in results) {
            idsToQuery += result.id.videoId
        }
        val ids: String = idsToQuery.joinToString(",")
        val searcher: YouTube.Videos.List = YOUTUBE.videos().list("id, snippet, contentDetails")
        searcher.key = KEY
        searcher.id = ids
        return searcher.execute().items
    }
    @Throws(IOException::class)
    fun getLink(query: String): String? {
        val yt: String? = if (getVideos(query).isNotEmpty()) "https://www.youtube.com/watch?v=${getVideos(query)[0].id}" else null
        return if (query matches VIDEO_REGEX) query else yt
    }
    @Throws(IOException::class)
    fun getDuration(video: Video): Long = Duration.parse(video.contentDetails.duration).toMillis()
}