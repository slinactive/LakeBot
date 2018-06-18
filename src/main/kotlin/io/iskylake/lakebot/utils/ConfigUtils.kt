/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

@file:Suppress("UNUSED")
package io.iskylake.lakebot.utils

import com.mongodb.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.entities.extensions.tag

import net.dv8tion.jda.core.entities.User

import org.bson.Document

object ConfigUtils {
    val CLIENT = MongoClient()
    val DATABASE: MongoDatabase = CLIENT.getDatabase("lake-bot")
    // LakeBan
    fun getLakeBans(): List<Document> = DATABASE.getCollection("lakebans").find().toList()
    fun getLakeBan(user: User): Document? = getLakeBans().firstOrNull {
        (it["user"] as List<*>).map { it.toString() }[0] == user.id
    }
    fun putLakeBan(user: User, reason: String) {
        if (user.idLong !in Immutable.DEVELOPERS && !user.isBot) {
            val collection = DATABASE.getCollection("lakebans")
            val ban = getLakeBan(user)
            if (ban === null) {
                val newBan = Document().apply {
                    append("user", listOf(user.id, user.tag))
                    append("reason", reason)
                }
                collection.insertOne(newBan)
            }
        }
    }
    fun clearLakeBan(user: User) {
        if (getLakeBan(user) !== null) {
            DATABASE.getCollection("lakebans").deleteOne(Filters.eq("user", listOf(user.id, user.tag)))
        }
    }
}