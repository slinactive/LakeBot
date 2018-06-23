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

package io.iskylake.lakebot

import com.mongodb.MongoClient

import io.iskylake.lakebot.commands.`fun`.*
import io.iskylake.lakebot.commands.animals.*
import io.iskylake.lakebot.commands.audio.*
import io.iskylake.lakebot.commands.developer.*
import io.iskylake.lakebot.commands.general.*
import io.iskylake.lakebot.commands.info.*
import io.iskylake.lakebot.commands.moderation.*
import io.iskylake.lakebot.commands.utils.*
import io.iskylake.lakebot.entities.EventWaiter
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.entities.handlers.CommandHandler
import io.iskylake.lakebot.entities.handlers.EventHandler
import io.iskylake.lakebot.utils.ConfigUtils

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Game.streaming
import net.dv8tion.jda.core.entities.Game.watching
import net.dv8tion.jda.core.entities.User

import kotlin.system.exitProcess

val DEFAULT_COMMANDS = listOf(
        // Animals
        AlpacaCommand(),
        BirdCommand(),
        CatCommand(),
        DogCommand(),
        // Audio
        JoinCommand(),
        JumpCommand(),
        JumpBackCommand(),
        JumpForwardCommand(),
        LeaveCommand(),
        LoopCommand(),
        PauseCommand(),
        PlayCommand(),
        PlayingCommand(),
        QueueCommand(),
        RemoveCommand(),
        RestartCommand(),
        ResumeCommand(),
        SelectCommand(),
        SkipCommand(),
        StopCommand(),
        YouTubePlayCommand(),
        // Developer
        EvalCommand(),
        LakeBanCommand(),
        LakeUnbanCommand(),
        ShutdownCommand(),
        // Fun
        AkinatorCommand(),
        GuessGameCommand(),
        InvertCommand(),
        SayCommand(),
        TextToImageCommand(),
        // General
        AboutCommand(),
        HelpCommand(),
        InviteCommand(),
        PingCommand(),
        SupportCommand(),
        UptimeCommand(),
        // Info
        EmotesCommand(),
        ServerCommand(),
        UserCommand(),
        // Moderation
        PrefixCommand(),
        PruneCommand(),
        // Utils
        ColorCommand(),
        EmojiCommand(),
        GoogleCommand(),
        QRCommand(),
        ShortenerCommand(),
        UrbanCommand(),
        WeatherCommand(),
        YouTubeCommand()
)
val USERS_WITH_PROCESSES = mutableListOf<User>()
lateinit var DISCORD: JDA
private lateinit var client: MongoClient
fun main(args: Array<String>) = try {
    runBlocking {
        client = ConfigUtils.CLIENT
        delay(1000)
        DISCORD = buildJDA {
            token { Immutable.BOT_TOKEN }
            eventListener { EventHandler }
            eventListener { EventWaiter }
            game { watching("loading..") }
            onlineStatus { OnlineStatus.DO_NOT_DISTURB }
        }
        for (command in DEFAULT_COMMANDS) {
            CommandHandler += command
        }
        System.setProperty("lakebot.version", Immutable.VERSION)
        System.setProperty("kotlin.version", KotlinVersion.CURRENT.toString())
        DISCORD.presence.game = streaming("${Immutable.VERSION} | ${Immutable.DEFAULT_PREFIX}help", "https://twitch.tv/raidlier")
        DISCORD.presence.status = OnlineStatus.ONLINE
    }
} catch (e: Exception) {
    e.printStackTrace()
    exitProcess(0)
}