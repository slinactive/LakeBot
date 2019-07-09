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

package io.iskylake.lakebot.entities.extensions

import net.dv8tion.jda.api.*
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.hooks.EventListener

import java.awt.Color
import java.time.OffsetDateTime
import java.time.temporal.TemporalAccessor

// Builders
inline fun buildJDA(accountType: AccountType = AccountType.BOT, lazyBuilder: JDABuilder.() -> Unit): JDA = JDABuilder(accountType).apply(lazyBuilder)()
inline fun buildEmbed(lazyBuilder: EmbedBuilder.() -> Unit): MessageEmbed = EmbedBuilder().apply(lazyBuilder).build()
inline fun buildMessage(lazyBuilder: MessageBuilder.() -> Unit): Message = MessageBuilder().apply(lazyBuilder).build()
// JDABuilder Extensions
operator fun JDABuilder.invoke() = build()
inline infix fun <T: EventListener> JDABuilder.eventListener(lazy: () -> T) = addEventListeners(lazy())
inline infix fun JDABuilder.token(lazy: () -> String) = setToken(lazy())
inline infix fun JDABuilder.onlineStatus(lazy: () -> OnlineStatus) = setStatus(lazy())
inline infix fun JDABuilder.game(lazy: () -> Activity) = setActivity(lazy())
// MessageBuilder Extensions
inline infix fun MessageBuilder.embed(crossinline lazy: EmbedBuilder.() -> Unit) = setEmbed(EmbedBuilder().apply(lazy).build())
inline infix fun MessageBuilder.content(crossinline lazy: () -> String) = setContent(lazy())
inline infix fun MessageBuilder.append(crossinline lazy: () -> CharSequence) = append(lazy())
inline infix fun MessageBuilder.appendln(crossinline lazy: () -> CharSequence) = append("${lazy()}\n")
inline infix fun MessageBuilder.mention(crossinline lazy: () -> IMentionable) = append(lazy())
inline infix fun MessageBuilder.tts(crossinline lazy: () -> Boolean) = setTTS(lazy())
operator fun MessageBuilder.invoke() = build()
// EmbedBuilder Extensions
fun EmbedBuilder.setTimestamp() = setTimestamp(OffsetDateTime.now())
fun EmbedBuilder.timestamp(lazy: () -> TemporalAccessor = { OffsetDateTime.now() }) = setTimestamp(lazy())
inline infix fun EmbedBuilder.description(crossinline lazy: () -> String) = setDescription(lazy())
inline infix fun EmbedBuilder.append(crossinline lazy: () -> CharSequence) = appendDescription(lazy())
inline infix fun EmbedBuilder.appendln(crossinline lazy: () -> CharSequence) = appendDescription("${lazy()}\n")
inline infix fun EmbedBuilder.field(crossinline lazy: () -> MessageEmbed.Field) = addField(lazy())
inline infix fun EmbedBuilder.color(crossinline lazy: () -> Color?) = setColor(lazy())
inline infix fun EmbedBuilder.thumbnail(crossinline lazy: () -> String?) = setThumbnail(lazy())
inline infix fun EmbedBuilder.image(crossinline lazy: () -> String?) = setImage(lazy())
inline infix fun EmbedBuilder.author(crossinline name: () -> String?) = setAuthor(name())
inline fun EmbedBuilder.author(name: String, link: String? = null, crossinline picture: () -> String?): EmbedBuilder {
    return setAuthor(name, link, picture())
}
inline fun EmbedBuilder.title(url: String? = null, crossinline lazy: () -> String) = setTitle(lazy(), url)
inline fun EmbedBuilder.footer(url: String? = null, crossinline lazy: () -> String) = setFooter(lazy(), url)
inline fun EmbedBuilder.field(inline: Boolean = false, title: String, crossinline desc: () -> String) = addField(title, desc(), inline)
inline fun MessageEmbed.edit(crossinline lazy: EmbedBuilder.() -> Unit) = EmbedBuilder(this).apply(lazy)()
operator fun EmbedBuilder.invoke() = build()