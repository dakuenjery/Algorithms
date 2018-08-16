package io

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.lang.Exception
import java.util.*

class TextReader(private val file: File) : IReader {
    private val scanner = Scanner(BufferedReader(FileReader(file)))
    private var cached = false
    private var cachedValue: Int = 0

    init {
        scanner.useDelimiter(", ")
    }

    override fun readAll(buffer: MutableList<Int>) {
        while (scanner.hasNextInt()) {
            buffer.add(scanner.nextInt())
        }
    }

    override fun readBlock(buffer: IntArray): Int {
        var i = 0

        while (i < buffer.size && scanner.hasNextInt()) {
            buffer[i++] = scanner.nextInt()
        }

        return i
    }

    override fun readValue(): Int {
        try {
            return scanner.nextInt()
        } catch (ex: Exception) {
            throw ex
        }
    }

    override fun getCachedValue(): Int {
        if (!cached) {
            cachedValue = readValue()
            cached = true
        }

        return cachedValue
    }

    override fun cacheNextValue() = try {
        cached = true
        cachedValue = readValue()
        true
    } catch (ex: Exception) {
        false
    }

    override val isEof: Boolean
        get() = !scanner.hasNextInt()

    override fun close() {
        scanner.close()
    }
}