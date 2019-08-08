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

package io.ilakeful.lakebot.commands.music

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.WAITER_PROCESSES
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.WaiterProcess
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.entities.pagination.buildPaginator
import io.ilakeful.lakebot.utils.AudioUtils

import khttp.get
import khttp.post

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

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
            return "Bearer ${post(api, headers = emptyMap(), data = data).jsonObject.getString("access_token")}"
        }
    override val name = "lyrics"
    override val aliases = listOf("genius")
    override val description = "The command sending the lyrics of the specified or current playing song"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <song name (optional if song is being played now by bot)>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val process = WaiterProcess(mutableListOf(event.author), event.textChannel)
        val arguments = event.argsRaw
        if (arguments !== null) {
            val songs = searchSongs(arguments)
            if (songs.isNotEmpty()) {
                event.channel.sendMessage(buildEmbed {
                    color { Immutable.SUCCESS }
                    author("Select The Song:") { event.selfUser.effectiveAvatarUrl }
                    footer { "Type in \"exit\" to kill the process" }
                    for ((index, song) in songs.withIndex()) {
                        appendln { "${index + 1}. ${song.author} - ${song.title}" }
                    }
                }).await {
                    WAITER_PROCESSES += process
                    selectSong(event, it, songs, process)
                }
            } else {
                event.sendFailure("Couldn't find that track!").queue()
            }
        } else if (AudioUtils[event.guild].audioPlayer.playingTrack !== null) {
            val track = AudioUtils[event.guild].audioPlayer.playingTrack.info.title
            val regex = "(?:[\\[({])?((official)(?:\\s+(video))?|live|lyrics|guitar|((original|official)\\s+|re)mix|acoustics?|hd|sd|4k|full\\s+hd|hq)(?:[]})])?"
                    .toRegex(RegexOption.IGNORE_CASE)
            val songs = searchSongs(track.removeContent(regex))
            if (songs.isNotEmpty()) {
                event.channel.sendMessage(buildEmbed {
                    color { Immutable.SUCCESS }
                    author("Select The Song:") { event.selfUser.effectiveAvatarUrl }
                    footer { "Type in \"exit\" to kill the process" }
                    for ((index, song) in songs.withIndex()) {
                        appendln { "${index + 1}. ${song.author} - ${song.title}" }
                    }
                }).await {
                    WAITER_PROCESSES += process
                    selectSong(event, it, songs, process)
                }
            } else {
                event.sendFailure("Couldn't find that track!").queue()
            }
        } else {
            event.sendFailure("You specified no content!").queue()
        }
    }
    private val Song.lyrics: String
        get() {
            val doc = Jsoup.connect(link).userAgent(Immutable.USER_AGENT).get()
            return doc.selectFirst(".lyrics").wholeText().replace(Regex("<(br)(?:(?:\\s+)?/)?>"), "\n")
        }
    private suspend fun selectSong(event: MessageReceivedEvent, msg: Message, songs: List<Song>, process: WaiterProcess) {
        val c = event.channel.awaitMessage(event.author)?.contentRaw
        if (c !== null) {
            if (c.isInt) {
                if (c.toInt() in 1..songs.size) {
                    msg.delete().queue()
                    val song = songs[c.toInt() - 1]
                    WAITER_PROCESSES -= process
                    if (song.lyrics.length > 2048) {
                        val paginator = buildPaginator<Char> {
                            size { 2048 }
                            list { song.lyrics.toCharArray().toList() }
                            event { event }
                            embed { num, pages ->
                                for (char in pages[num - 1]) {
                                    append { char.toString() }
                                }
                                author("${song.author} - ${song.title}", song.link) {
                                    event.selfUser.effectiveAvatarUrl
                                }
                                color { Immutable.SUCCESS }
                                footer(event.author.effectiveAvatarUrl) { "Page $num/${pages.size} | Requested by ${event.author.tag}" }
                            }
                        }
                        paginator()
                    } else {
                        event.channel.sendMessage(buildEmbed {
                            author("${song.author} - ${song.title}", song.link) {
                                event.selfUser.effectiveAvatarUrl
                            }
                            color { Immutable.SUCCESS }
                            description { song.lyrics }
                            footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.tag}" }
                        }).queue()
                    }
                } else {
                    event.sendFailure("Try again!").await { selectSong(event, msg, songs, process) }
                }
            } else if (c.toLowerCase() == "exit") {
                msg.delete().queue()
                WAITER_PROCESSES -= process
                event.sendSuccess("Process successfully stopped!").queue()
            } else {
                event.sendFailure("Try again!").await { selectSong(event, msg, songs, process) }
            }
        } else {
            msg.delete().queue()
            WAITER_PROCESSES -= process
            event.sendFailure("Time is up!").queue()
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