package io

import java.io.*

class BinaryReader(private val file: File) : IReader {
    private val stream = DataInputStream(BufferedInputStream(FileInputStream(file)))

    private var cached = false
    private var cachedValue = 0

    override fun readAll(buffer: MutableList<Int>) {
        while (stream.available() > 0)
            buffer.add(stream.readInt())
    }

    override fun readBlock(buffer: IntArray): Int {
        var i = 0

        while (i < buffer.size && stream.available() > 0)
            buffer[i++] = stream.readInt()

        return i
    }

    override fun readValue(): Int {
        return stream.readInt()
    }

    override val isEof: Boolean
        get() = stream.available() == 0

    override fun getCachedValue(): Int {
        if (!cached) {
            if (!cacheNextValue())
                throw EOFException(file.absolutePath)
        }

        return cachedValue
    }

    override fun cacheNextValue(): Boolean {
        return if (!isEof) {
            cached = true
            cachedValue = readValue()
            true
        } else
            false
    }

    override fun close() {
        stream.close()
    }
}