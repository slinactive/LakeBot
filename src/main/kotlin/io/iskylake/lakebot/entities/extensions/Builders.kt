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

package io.iskylake.lakebot.entities.extensions

import net.dv8tion.jda.core.*
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.hooks.EventListener

import java.awt.Color
import java.time.OffsetDateTime
import java.time.temporal.TemporalAccessor

// Builders
inline fun buildJDA(accountType: AccountType = AccountType.BOT, lazyBuilder: JDABuilder.() -> Unit): JDA = JDABuilder(accountType).apply(lazyBuilder)()
inline fun buildEmbed(lazyBuilder: EmbedBuilder.() -> Unit): MessageEmbed = EmbedBuilder().apply(lazyBuilder).build()
inline fun buildMessage(lazyBuilder: MessageBuilder.() -> Unit): Message = MessageBuilder().apply(lazyBuilder).build()
// JDABuilder Extensions
operator fun JDABuilder.invoke() = this.buildAsync()
inline infix fun <T: EventListener> JDABuilder.eventListener(lazy: () -> T) = this.addEventListener(lazy())
inline infix fun JDABuilder.token(lazy: () -> String) = this.setToken(lazy())
inline infix fun JDABuilder.onlineStatus(lazy: () -> OnlineStatus) = this.setStatus(lazy())
inline infix fun JDABuilder.game(lazy: () -> Game) = this.setGame(lazy())
// MessageBuilder Extensions
inline infix fun MessageBuilder.embed(crossinline lazy: EmbedBuilder.() -> Unit) = this.setEmbed(EmbedBuilder().apply(lazy).build())
inline infix fun MessageBuilder.content(crossinline lazy: () -> String) = this.setContent(lazy())
inline infix fun MessageBuilder.append(crossinline lazy: () -> CharSequence) = this.append(lazy())
inline infix fun MessageBuilder.appendln(crossinline lazy: () -> CharSequence) = this.append("${lazy()}\n")
inline infix fun MessageBuilder.mention(crossinline lazy: () -> IMentionable) = this.append(lazy())
inline infix fun MessageBuilder.tts(crossinline lazy: () -> Boolean) = this.setTTS(lazy())
operator fun MessageBuilder.invoke() = this.build()
// EmbedBuilder Extensions
fun EmbedBuilder.setTimestamp() = this.setTimestamp(OffsetDateTime.now())
fun EmbedBuilder.timestamp(lazy: () -> TemporalAccessor = { OffsetDateTime.now() }) = this.setTimestamp(lazy())
inline infix fun EmbedBuilder.description(crossinline lazy: () -> String) = this.setDescription(lazy())
inline infix fun EmbedBuilder.append(crossinline lazy: () -> CharSequence) = this.appendDescription(lazy())
inline infix fun EmbedBuilder.appendln(crossinline lazy: () -> CharSequence) = this.appendDescription("${lazy()}\n")
inline infix fun EmbedBuilder.field(crossinline lazy: () -> MessageEmbed.Field) = this.addField(lazy())
inline infix fun EmbedBuilder.color(crossinline lazy: () -> Color) = this.setColor(lazy())
inline infix fun EmbedBuilder.thumbnail(crossinline lazy: () -> String) = this.setThumbnail(lazy())
inline infix fun EmbedBuilder.image(crossinline lazy: () -> String) = this.setImage(lazy())
inline infix fun EmbedBuilder.author(crossinline name: () -> String) = this.setAuthor(name())
inline fun EmbedBuilder.author(name: String, link: String? = null, crossinline picture: () -> String): EmbedBuilder {
    return this.setAuthor(name, link, picture())
}
inline fun EmbedBuilder.title(url: String? = null, crossinline lazy: () -> String) = this.setTitle(lazy(), url)
inline fun EmbedBuilder.footer(url: String? = null, crossinline lazy: () -> String) = this.setFooter(lazy(), url)
inline fun EmbedBuilder.field(inline: Boolean = false, title: String, crossinline desc: () -> String) = this.addField(title, desc(), inline)
operator fun EmbedBuilder.invoke() = build()
