/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
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

package io.iskylake.lakebot.commands.audio

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.USERS_WITH_PROCESSES
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.utils.AudioUtils

import khttp.get
import khttp.post

import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

import org.json.JSONArray

import org.jsoup.Jsoup

import java.net.URLEncoder

class LyricsCommand : Command {
    data class Song(val author: String, val title: String, val link: String)
    private val token: String
        get() {
            val api = "https://api.genius.com/oauth/token"
            val data = mapOf(
                    "client_id" to Immutable.GENIUS_ID,
                    "client_secret" to Immutable.GENIUS_SECRET,
                    "grant_type" to "client_credentials"
            )
            return "Bearer ${post(api, data = data).jsonObject.getString("access_token")}"
        }
    private val songEmbed = fun(song: Song, event: MessageReceivedEvent) = buildEmbed {
        author("${song.author} - ${song.title}", song.link) {
            event.selfUser.effectiveAvatarUrl
        }
        color { Immutable.SUCCESS }
        description { song.lyrics.safeSubstring(end = 2048) }
        footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.tag}" }
    }
    override val name = "lyrics"
    override val description = "beta command"
    override val category = io.iskylake.lakebot.commands.CommandCategory.BETA
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            val songs = searchSongs(arguments)
            if (songs.isNotEmpty()) {
                event.channel.sendMessage(buildEmbed {
                    for ((index, song) in songs.withIndex()) {
                        appendln { "${index + 1}. ${song.author} - ${song.title}" }
                    }
                }).await {
                    USERS_WITH_PROCESSES += event.author
                    selectSong(event, it, songs)
                }
            } else {
                event.sendError("that song couldn't be found").queue()
            }
        } else if (AudioUtils[event.guild].audioPlayer.playingTrack !== null) {
            val track = AudioUtils[event.guild].audioPlayer.playingTrack.info.title
            val regex = "(?:[\\[({])?((official)\\s+(video)|live|lyrics|guitar|remix|acoustics?)(?:[]})])?".toRegex(RegexOption.IGNORE_CASE)
            val song = searchSongs(track.removeContent(regex)).firstOrNull()
            if (song !== null) {
                event.channel.sendMessage(songEmbed(song, event)).queue()
            } else {
                event.sendError("couldn't find that track").queue()
            }
        } else {
            event.sendError("you specified no content").queue()
        }
    }
    private val Song.lyrics: String
        get() {
            val doc = Jsoup.connect(link).userAgent(Immutable.USER_AGENT).get()
            return doc.selectFirst(".lyrics").wholeText().replace(Regex("<(br)(?:(?:\\s+)?/)?>"), "\n")
        }
    private suspend fun selectSong(event: MessageReceivedEvent, msg: Message, songs: List<Song>) {
        val c = event.channel.awaitMessage(event.author)?.contentRaw
        if (c !== null) {
            if (c.isInt) {
                if (c.toInt() in 1..songs.size) {
                    msg.delete().queue()
                    val song = songs[c.toInt() - 1]
                    event.channel.sendMessage(songEmbed(song, event)).queue()
                    USERS_WITH_PROCESSES -= event.author
                } else {
                    event.sendError("Try again!").await { selectSong(event, msg, songs) }
                }
            } else if (c.toLowerCase() == "exit") {
                msg.delete().queue()
                USERS_WITH_PROCESSES -= event.author
                event.sendSuccess("Process successfully stopped!").queue()
            } else {
                event.sendError("Try again!").await { selectSong(event, msg, songs) }
            }
        } else {
            msg.delete().queue()
            USERS_WITH_PROCESSES -= event.author
            event.sendError("Time is up!").queue()
        }
    }
    private fun searchSongs(title: String): List<Song> {
        val api = "https://api.genius.com"
        val endpoint = "/search?q=${URLEncoder.encode(title, "UTF-8")}"
        val requestUrl = "$api$endpoint"
        val response = get(requestUrl, mapOf("Authorization" to token))
        val hits = JSONArray(response.jsonObject.getJSONObject("response").getJSONArray("hits").take(5))
        return mutableListOf<Song>().apply {
            for (i in 0 until hits.count()) {
                val json = hits.getJSONObject(i).getJSONObject("result")
                this += Song(json.getJSONObject("primary_artist").getString("name"), json.getString("title"), json.getString("url"))
            }
        }
    }
}