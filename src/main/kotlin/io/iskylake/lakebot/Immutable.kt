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

package io.iskylake.lakebot

import org.slf4j.LoggerFactory

import java.awt.Color

object Immutable {
    const val DEFAULT_PREFIX = "lb!"
    const val VERSION = "1.0-BETA3"
    const val PERMISSIONS = 2146958591L
    const val GITHUB_REPOSITORY = "https://github.com/ilakeful/LakeBot"
    const val SUPPORT_INVITE = "https://discord.gg/QsTevwF"
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 6.3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3416.0 Safari/537.36"
    val LOGGER = LoggerFactory.getLogger("LakeBot")!!
    val BOT_TOKEN = System.getenv("BOT_TOKEN")!!
    val YOUTUBE_API_KEY = System.getenv("YOUTUBE_API_KEY")!!
    val WEATHER_API_KEY = System.getenv("WEATHER_API_KEY")!!
    val GENIUS_ID = System.getenv("GENIUS_ID")!!
    val GENIUS_SECRET = System.getenv("GENIUS_SECRET")!!
    val GEONAME_API_USER = System.getenv("GEONAME_API_USER")!!
    val GOOGLE_API_KEYS = listOf(
            System.getenv("GOOGLE_API1")!!,
            System.getenv("GOOGLE_API2")!!,
            System.getenv("GOOGLE_API3")!!,
            System.getenv("GOOGLE_API4")!!,
            System.getenv("GOOGLE_API5")!!
    )
    val SUCCESS = Color(232, 66, 102)
    val FAILURE = Color(239, 67, 63)
    val CONFIRMATION = Color(118, 255, 3)
    val DEVELOPERS = longArrayOf(337643430903676928)
}