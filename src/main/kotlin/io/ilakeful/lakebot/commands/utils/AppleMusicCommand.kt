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
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.WaiterProcess
import io.ilakeful.lakebot.entities.applemusic.*
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.utils.TimeUtils

import khttp.get

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
    override val description = "The command searching for the specified song on Apple Music and sending the brief information about it"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <song name>"
    private fun searchForSongsAsJSONArray(query: String, limit: Int = 5)
            = get("$API_BASE?term=${URLEncoder.encode(query, "UTF-8")}" +
            "&country=US" +
            "&media=music" +
            "&entity=musicTrack", headers = emptyMap()).jsonObject.getJSONArray("results")
            .filter { (it as JSONObject).optString("kind", null) == "song" }
            .take(limit)
    private fun jsonToSong(json: JSONObject): Song = buildSong {
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
        previewUrl { json.optString("previewUrl", null) }
        id { json.getLong("trackId") }
        artwork { json.optString("artworkUrl100", null) }
        releaseDate { OffsetDateTime.parse(json.getString("releaseDate")) }
        genre { json.getString("primaryGenreName") }
        number { json.getInt("trackNumber") }
        duration { json.getLong("trackTimeMillis") }
        explicit { json.getString("trackExplicitness") == "explicit" }
    }
    private fun searchForSongs(query: String): List<Song> = searchForSongsAsJSONArray(query)
            .map { jsonToSong(it as JSONObject) }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            val results = searchForSongs(arguments)
            if (results.isNotEmpty()) {
                if (results.size > 1) {
                    event.channel.sendEmbed {
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
                    }.await {
                        selectEntity(
                                event = event,
                                message = it,
                                entities = results,
                                addProcess = true,
                                process = WaiterProcess(
                                        users = mutableListOf(event.author),
                                        channel = event.textChannel,
                                        command = this
                                )
                        ) { song ->
                            sendSongInfo(event, song)
                        }
                    }
                } else {
                    val song = results.first()
                    sendSongInfo(event, song)
                }
            } else {
                event.channel.sendFailure("No results found by the query!").queue()
            }
        } else {
            event.channel.sendFailure("You haven't specified any required arguments!").queue()
        }
    }
    private fun sendSongInfo(event: MessageReceivedEvent, song: Song) = event.channel.sendEmbed {
        color { Immutable.SUCCESS }
        author("${song.author.name} - ${song.title}", song.url) { song.artwork ?: event.selfUser.effectiveAvatarUrl }
        thumbnail { song.artwork ?: event.selfUser.effectiveAvatarUrl }
        if (song.previewUrl !== null) {
            description { "Want to play the track's preview? [Here's the link!](${song.previewUrl})"}
        }
        field(true, "Artist:") { "[${song.author.name}](${song.author.url})" }
        field(true, "Song:") { "[${song.title}](${song.url})" }
        field(true, "Album:") { "[${song.album.title}](${song.album.url})" }
        field(true, "ID:") { song.id.toString() }
        field(true, "Release:") { song.releaseDate.format(DateTimeFormatter.RFC_1123_DATE_TIME).removeSuffix("GMT").trim() }
        field(true, "Genre:") { song.genre }
        field(true, "Track Number:") { song.number.toString() }
        field(true, "Duration:") { TimeUtils.asDuration(song.duration) }
        field(true, "Explicit:") { song.isExplicit.asString() }
        footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.asTag}" }
        timestamp()
    }.queue()
}