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
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.WaiterProcess
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.entities.pagination.buildPaginator
import io.ilakeful.lakebot.utils.AudioUtils

import khttp.get
import khttp.post

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
    companion object {
        @JvmField
        val REGEX_TO_REMOVE = "([\\[({](?:\\s+)?)((?:off(?:icial)?|sd|[48]k|(?:f(?:ull\\s+)?)?hd|hq)?((?:\\s+)?(vid(?:eo)?))|live|(?:w(ith|/)(?:\\s+)?)?lyrics|guitar|((?:(instr(?:ument(?:al)?)?|original|official))?(?:\\s+)?(?:(version|(?:re)?mix))?)|acoustics?|(?:f(?:ull\\s+)?)?hd|sd|[48]k|(?:official\\s+)?hq)((?:\\s+)?[]})])"
                .toRegex(RegexOption.IGNORE_CASE)
    }
    override val name = "lyrics"
    override val aliases = listOf("genius")
    override val description = "The command sending the lyrics of the specified or currently playing song"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <song name (optional if the song is playing)>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val noArgumentsMessage = "You haven't specified any arguments!"
        try {
            val query = event.argsRaw
                    ?: AudioUtils[event.guild].audioPlayer.playingTrack?.info?.title?.removeContent(REGEX_TO_REMOVE)
                    ?: throw IllegalArgumentException(noArgumentsMessage)
            songInfo(searchSongs(query), event)
        } catch (e: Exception) {
            if (e is IllegalArgumentException) {
                event.channel.sendFailure(e.message ?: noArgumentsMessage).queue()
            } else {
                event.channel.sendFailure(
                        "Something went wrong! ${e::class.simpleName ?: "Unknown exception"}: ${e.message ?: "absent message"}"
                ).queue()
            }
        }
    }
    private val Song.lyrics: String
        get() {
            val doc = Jsoup.connect(link).userAgent(Immutable.USER_AGENT).get()
            return doc.selectFirst(".lyrics").wholeText().replace(Regex("<(br)(?:(?:\\s+)?/)?>"), "\n")
        }
    private suspend fun songInfo(songs: List<Song>, event: MessageReceivedEvent) {
        if (songs.isNotEmpty()) {
            event.channel.sendEmbed {
                color { Immutable.SUCCESS }
                author("Select Song:") { event.selfUser.effectiveAvatarUrl }
                footer { "Type in \"exit\" to kill the process" }
                for ((index, song) in songs.withIndex()) {
                    appendln { "${index + 1}. ${song.author} - ${song.title}" }
                }
            }.await {
                selectEntity(
                        event = event,
                        message = it,
                        entities = songs,
                        addProcess = true,
                        process = WaiterProcess(
                                users = mutableListOf(event.author),
                                channel = event.textChannel,
                                command = this
                        )
                ) { song ->
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
                                footer(event.author.effectiveAvatarUrl) {
                                    "Page $num/${pages.size} | Requested by ${event.author.asTag}"
                                }
                            }
                        }
                        paginator()
                    } else {
                        event.channel.sendEmbed {
                            author("${song.author} - ${song.title}", song.link) {
                                event.selfUser.effectiveAvatarUrl
                            }
                            color { Immutable.SUCCESS }
                            description { song.lyrics }
                            footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.asTag}" }
                        }.queue()
                    }
                }
            }
        } else {
            event.channel.sendFailure("No results found by the query!").queue()
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