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
 *  limitations under the License.
 */

package io.ilakeful.lakebot.commands.utils

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.WAITER_PROCESSES
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.commands.CommandCategory
import io.ilakeful.lakebot.entities.WaiterProcess
import io.ilakeful.lakebot.entities.applemusic.*
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.utils.TimeUtils
import khttp.get
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.json.JSONObject
import java.net.URLEncoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class AppleMusicCommand : Command {
    companion object {
        internal const val API_BASE = "https://itunes.apple.com/search"
    }
    override val name = "applemusic"
    override val aliases = listOf("apple-music", "am", "itunes")
    override val description = "null"
    override val category = CommandCategory.BETA
    private fun searchForSongsAsJSONArray(query: String, limit: Int = 5)
            = get("$API_BASE?term=${URLEncoder.encode(query, "UTF-8")}" +
            "&country=US" +
            "&media=music" +
            "&entity=musicTrack", headers = emptyMap()).jsonObject.getJSONArray("results").take(limit)
    private fun jsonToSong(json: JSONObject): Song {
        return buildSong {
            title { json.getString("trackName") }
            author {
                name { json.getString("artistName") }
                id { json.getLong("artistId") }
                url { json.getString("artistViewUrl") }
            }
            album {
                title { json.getString("collectionName") }
                titleCensored { json.getString("collectionCensoredName") }
                url { json.getString("collectionViewUrl") }
                id { json.getLong("collectionId") }
                count { json.getInt("trackCount") }
                explicit { json.getString("collectionExplicitness") == "explicit" }
            }
            url { json.getString("trackViewUrl") }
            id { json.getLong("trackId") }
            artwork { json.optString("artworkUrl100", null) }
            releaseDate { OffsetDateTime.parse(json.getString("releaseDate")) }
            genre { json.getString("primaryGenreName") }
            number { json.getInt("trackNumber") }
            duration { json.getLong("trackTimeMillis") }
            explicit { json.getString("trackExplicitness") == "explicit" }
        }
    }
    private fun searchForSongs(query: String): List<Song> = searchForSongsAsJSONArray(query)
            .map { jsonToSong(it as JSONObject) }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            val results = searchForSongs(arguments)
            if (results.isNotEmpty()) {
                if (results.size > 1) {
                    val process = WaiterProcess(mutableListOf(event.author), event.textChannel, this)
                    event.channel.sendMessage(buildEmbed {
                        color { Immutable.SUCCESS }
                        author("Select the Song:") { event.selfUser.effectiveAvatarUrl }
                        footer { "Type in \"exit\" to kill the process | Format: author - track - album" }
                        for ((index, song) in results.withIndex()) {
                            appendln {
                                val isSingle = song.album.title == "${song.title} - Single"
                                "${index + 1}. [${song.author.name}](${song.author.url}) - [${song.title}](${song.url})" +
                                    if (isSingle) "" else " - [${song.album.title}](${song.album.url})"
                            }
                        }
                    }).await {
                        WAITER_PROCESSES += process
                        selectSong(event, it, results, process)
                    }
                } else {
                    val song = results.first()
                    sendSongInfo(event, song)
                }
            }
        }
    }
    private fun sendSongInfo(event: MessageReceivedEvent, song: Song) = event.channel.sendEmbed {
        color { Immutable.SUCCESS }
        author("${song.author.name} - ${song.title}", song.url) { song.artwork ?: event.selfUser.effectiveAvatarUrl }
        thumbnail { song.artwork ?: event.selfUser.effectiveAvatarUrl }
        field(true, "Artist:") { "[${song.author.name}](${song.author.url})" }
        field(true, "Song:") { "[${song.title}](${song.url})" }
        field(true, "Album:") { "[${song.album.title}](${song.album.url})" }
        field(true, "ID:") { song.id.toString() }
        field(true, "Release:") { song.releaseDate.format(DateTimeFormatter.RFC_1123_DATE_TIME).replace(" GMT", "") }
        field(true, "Genre:") { song.genre }
        field(true, "Track Number:") { song.number.toString() }
        field(true, "Duration:") { TimeUtils.asDuration(song.duration) }
        field(true, "Explicit:") { song.isExplicit.asString() }
        footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.asTag}" }
        timestamp()
    }.queue()
    private suspend fun selectSong(
            event: MessageReceivedEvent,
            message: Message,
            songs: List<Song>,
            process: WaiterProcess
    ) {
        val content = event.channel.awaitMessage(event.author)?.contentRaw
        if (content !== null) {
            when {
                content.isInt -> {
                    val index = content.toInt()
                    if (index in 1..songs.size) {
                        message.delete().queue()
                        val song = songs[index - 1]
                        WAITER_PROCESSES -= process
                        sendSongInfo(event, song)
                    } else {
                        event.channel.sendFailure("Try again!").await { selectSong(event, message, songs, process) }
                    }
                }
                content.toLowerCase() == "exit" -> {
                    message.delete().queue()
                    WAITER_PROCESSES -= process
                    event.channel.sendSuccess("Successfully stopped!").queue()
                }
                else -> event.channel.sendFailure("Try again!").await { selectSong(event, message, songs, process) }
            }
        } else {
            message.delete().queue()
            WAITER_PROCESSES -= process
            event.channel.sendFailure("Time is up!").queue()
        }
    }
}