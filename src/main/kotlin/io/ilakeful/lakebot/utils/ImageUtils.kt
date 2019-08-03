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

package io.ilakeful.lakebot.utils

import io.ilakeful.lakebot.Immutable

import net.glxn.qrgen.core.image.ImageType
import net.glxn.qrgen.javase.QRCode

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

import javax.imageio.ImageIO

object ImageUtils {
    fun getInvertedImage(url: String) = getInvertedImage(URL(url))
    fun getInvertedImage(url: URL) = getInvertedImage(urlToBytes(url))
    fun getInvertedImage(bytes: ByteArray) = getInvertedImage(bytesToImage(bytes))
    fun getInvertedImage(image: BufferedImage): BufferedImage {
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val rgba = image.getRGB(x, y)
                val a = rgba shr 24 and 0xff shl 24
                var r = rgba shr 16 and 0xff
                var g = rgba shr 8 and 0xff
                var b = rgba and 0xff
                r = 255 - r shl 16
                g = 255 - g shl 8
                b = 255 - b
                image.setRGB(x, y, a or r or g or b)
            }
        }
        return image
    }
    fun imageToBytes(image: BufferedImage): ByteArray {
        val stream = ByteArrayOutputStream()
        ImageIO.write(image, "png", stream)
        return stream.toByteArray()
    }
    fun bytesToImage(bytes: ByteArray) = ImageIO.read(ByteArrayInputStream(bytes))
    fun urlToBytes(url: URL): ByteArray {
        val connection = url.openConnection() as HttpURLConnection
        connection.addRequestProperty("User-Agent", Immutable.USER_AGENT)
        return connection.inputStream.readBytes()
    }
    fun getColorImage(c: Color, width: Int = 150, height: Int = 150): ByteArray {
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = bufferedImage.createGraphics()
        graphics.color = c
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()
        val byteout = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "png", byteout)
        return byteout.toByteArray()
    }
    fun getQRCode(from: String): ByteArray {
        val byteout = QRCode.from(from).withSize(768, 768).to(ImageType.PNG).stream()
        return byteout.toByteArray()
    }
    fun getImagedText(content: List<String>): ByteArray {
        var bufferedImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        var graphics = bufferedImage.createGraphics()
        val font = Font("SF Pro Display", Font.BOLD, 15)
        graphics.font = font
        val fm = graphics.fontMetrics
        val width = fm.stringWidth(content.sortedWith(compareBy { it.count() }).last()) + 3
        val height = fm.height * content.size
        graphics.dispose()
        bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        graphics = bufferedImage.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE)
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        //g.font = font
        //fm = g.fontMetrics
        //g.background = Color(255, 255, 255, 0)
        //g.clearRect(0, 0, width, height)
        graphics.font = font
        var index = fm.ascent
        for (line in content) {
            //g.drawString(line, 0, fm.ascent * (index + 1))
            graphics.drawString(line, 0, index)
            index += fm.height
        }
        graphics.dispose()
        val byteout = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "png", byteout)
        return byteout.toByteArray()
    }
}