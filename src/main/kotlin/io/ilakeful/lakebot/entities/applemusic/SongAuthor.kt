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
 *  limitations under the License.
 */

package io.ilakeful.lakebot.entities.applemusic

data class SongAuthor(
        val name: String,
        val id: Long,
        val url: String
)
class SongAuthorBuilder {
    lateinit var nameProperty: String
    lateinit var urlProperty: String
    var idProperty: Long = 0
    inline infix fun name(lazyName: () -> String) {
        nameProperty = lazyName()
    }
    inline infix fun url(lazyUrl: () -> String) {
        urlProperty = lazyUrl()
    }
    inline infix fun id(lazyId: () -> Long) {
        idProperty = lazyId()
    }
    operator fun invoke() = if (::nameProperty.isInitialized && ::urlProperty.isInitialized && idProperty > 0) {
        SongAuthor(nameProperty, idProperty, urlProperty)
    } else throw IllegalStateException("Some important properties were not initialized!")
}
inline fun songAuthor(block: SongAuthorBuilder.() -> Unit) = SongAuthorBuilder().apply(block)()