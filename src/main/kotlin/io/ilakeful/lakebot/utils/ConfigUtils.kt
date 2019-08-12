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

@file:Suppress("UNUSED")
package io.ilakeful.lakebot.utils

import com.mongodb.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.entities.extensions.isLBDeveloper

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User

import org.bson.Document

object ConfigUtils {
    val CLIENT = MongoClient()
    val DATABASE: MongoDatabase = CLIENT.getDatabase("lake-bot")
    // Prefix
    fun getPrefix(guild: Guild): String = if (isPrefixEnabled(guild)) {
        DATABASE.getCollection("prefixes")
                .find(Filters.eq("id", guild.id))
                .first()
                ?.getString("prefix") ?: Immutable.DEFAULT_PREFIX
    } else {
        Immutable.DEFAULT_PREFIX
    }
    fun setPrefix(prefix: String, guild: Guild) {
        if (!isPrefixEnabled(guild)) {
            if (prefix != Immutable.DEFAULT_PREFIX) {
                DATABASE.getCollection("prefixes")
                        .insertOne(Document().append("id", guild.id).append("prefix", prefix))
            }
        } else {
            if (prefix == Immutable.DEFAULT_PREFIX) {
                clearPrefix(guild)
            } else {
                DATABASE.getCollection("prefixes")
                        .replaceOne(
                                Filters.eq("id", guild.id),
                                Document().append("id", guild.id).append("prefix", prefix)
                        )
            }
        }
    }
    fun clearPrefix(guild: Guild) {
        if (isPrefixEnabled(guild)) {
            DATABASE.getCollection("prefixes").deleteOne(Filters.eq("id", guild.id))
        }
    }
    fun isPrefixEnabled(guild: Guild): Boolean = DATABASE.getCollection("prefixes")
            .find(Filters.eq("id", guild.id)).first() !== null
    // LakeBan
    fun getLakeBans(): List<Document> = DATABASE.getCollection("lakebans").find().toList()
    fun getLakeBan(user: User): Document? = getLakeBans().firstOrNull {
        it["user"].toString() == user.id
    }
    fun putLakeBan(user: User, reason: String) {
        if (!user.isLBDeveloper && !user.isBot) {
            val collection = DATABASE.getCollection("lakebans")
            if (getLakeBan(user) !== null) {
                collection.replaceOne(
                        Filters.eq("user", user.id),
                        Document().append("user", user.id).append("reason", reason)
                )
            } else {
                val ban = Document().apply {
                    append("user", user.id)
                    append("reason", reason)
                }
                collection.insertOne(ban)
            }
        }
    }
    fun clearLakeBan(user: User) {
        if (getLakeBan(user) !== null) {
            DATABASE.getCollection("lakebans")
                    .deleteOne(Filters.eq("user", user.id))
        }
    }
    // Mute Roles
    fun getMuteRole(guild: Guild): String? {
        val collection = DATABASE.getCollection("muteroles")
        return if (isMuteRoleEnabled(guild)) {
            val bson = collection.find(Filters.eq("id", guild.id)).first()
            bson?.getString("role")
        } else {
            null
        }
    }
    fun setMuteRole(role: String, guild: Guild) {
        if (!isMuteRoleEnabled(guild)) {
            DATABASE.getCollection("muteroles")
                    .insertOne(Document().append("id", guild.id).append("role", role))
        } else {
            DATABASE.getCollection("muteroles")
                    .replaceOne(
                            Filters.eq("id", guild.id),
                            Document().append("id", guild.id).append("role", role)
                    )
        }
    }
    fun clearMuteRole(guild: Guild) {
        if (isMuteRoleEnabled(guild)) {
            DATABASE.getCollection("muteroles").deleteOne(Filters.eq("id", guild.id))
        }
    }
    fun isMuteRoleEnabled(guild: Guild): Boolean = DATABASE.getCollection("muteroles").find(Filters.eq("id", guild.id)).first() !== null
    // Mutes
    fun getMutes(guild: Guild): List<Document> {
        val c = DATABASE.getCollection("mutes")
        return c.find(Filters.eq("guild", guild.id)).toList()
    }
    fun getMute(guild: Guild, user: User) = getMutes(guild).firstOrNull {
        it["user"].toString() == user.id
    }
    fun putMute(
            guild: Guild,
            user: User,
            mod: User,
            reason: String,
            term: Long,
            time: Long = System.currentTimeMillis()
    ) {
        val c = DATABASE.getCollection("mutes")
        val mutes = getMute(guild, user)
        if (mutes === null) {
            val mute = Document().apply {
                val m = Document()
                        .append("reason", reason)
                        .append("time", time)
                        .append("term", term)
                        .append("mod", mod.id)
                append("guild", guild.id)
                append("user", user.id)
                append("mute", m)
            }
            c.insertOne(mute)
        }
    }
    fun clearMutes(guild: Guild) {
        if (getMutes(guild).isNotEmpty()) {
            DATABASE.getCollection("mutes").deleteMany(Filters.eq("guild", guild.id))
        }
    }
    fun clearMute(guild: Guild, user: User) {
        if (getMute(guild, user) !== null) {
            DATABASE.getCollection("mutes")
                    .deleteOne(
                            Filters.and(
                                    Filters.eq("guild", guild.id),
                                    Filters.eq("user", user.id)
                            )
                    )
        }
    }
}