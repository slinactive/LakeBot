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

package io.ilakeful.lakebot

import com.mongodb.MongoClient

import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.EventWaiter
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.entities.handlers.CommandHandler
import io.ilakeful.lakebot.entities.handlers.EventHandler
import io.ilakeful.lakebot.utils.ConfigUtils

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity.streaming
import net.dv8tion.jda.api.entities.Activity.watching
import net.dv8tion.jda.api.entities.User

import org.reflections.Reflections

import kotlin.system.exitProcess

val USERS_WITH_PROCESSES = mutableSetOf<User>()
lateinit var DISCORD: JDA
private lateinit var client: MongoClient
fun main() = try {
    runBlocking {
        client = ConfigUtils.CLIENT
        delay(1000)
        Immutable.LOGGER.info("MongoDB was successfully loaded!")
        val commandPackage = Reflections("io.ilakeful.lakebot.commands")
        DISCORD = buildJDA {
            token { Immutable.BOT_TOKEN }
            eventListener { EventHandler }
            eventListener { EventWaiter }
            game { watching("loading..") }
            onlineStatus { OnlineStatus.DO_NOT_DISTURB }
        }
        Immutable.LOGGER.info("JDA was successfully built!")
        for (commandWrapper in commandPackage.getSubTypesOf<Command>()) {
            val command = commandWrapper.getDeclaredConstructor().newInstance()
            if (command.category != io.ilakeful.lakebot.commands.CommandCategory.ANIMALS) {
                CommandHandler += command
            }
        }
        Immutable.LOGGER.info("CommandHandler successfully loaded all ${CommandHandler.registeredCommands.size} commands!")
        System.setProperty("lakebot.version", Immutable.VERSION)
        System.setProperty("kotlin.version", KotlinVersion.CURRENT.toString())
        DISCORD.presence.activity = streaming("${Immutable.VERSION} | ${Immutable.DEFAULT_PREFIX}help", "https://twitch.tv/raidlier")
        DISCORD.presence.setStatus(OnlineStatus.ONLINE)
        Immutable.LOGGER.info("LakeBot is successfully loaded!")
    }
} catch (e: Exception) {
    e.printStackTrace()
    exitProcess(0)
}