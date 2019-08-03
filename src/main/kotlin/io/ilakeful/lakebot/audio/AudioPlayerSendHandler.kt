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

package io.ilakeful.lakebot.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame

import net.dv8tion.jda.api.audio.AudioSendHandler

import java.nio.ByteBuffer

class AudioPlayerSendHandler(private val audioPlayer: AudioPlayer) : AudioSendHandler {
    private var lastFrame: AudioFrame? = null
    override fun canProvide(): Boolean {
        lastFrame = audioPlayer.provide()
        return lastFrame != null
    }
    override fun provide20MsAudio(): ByteBuffer {
        return ByteBuffer.wrap(lastFrame!!.data)
    }
    override fun isOpus() = true
}