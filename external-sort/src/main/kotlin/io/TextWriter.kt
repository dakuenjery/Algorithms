package io

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class TextWriter(file: File) : IWriter {
    private val stream = BufferedWriter(FileWriter(file))
    private var dirty = false

    override fun writeBlock(buffer: IntArray, n: Int) {
        if (dirty)
            stream.write(", ")

        for (i in 0..n-2)
            stream.write("${buffer[i]}, ")

        stream.write("${buffer[n-1]}")

        dirty = true
    }

    override fun writeValue(value: Int) {
        if (dirty)
            stream.write(", ")

        stream.write("$value")

        dirty = true
    }

    override fun close() {
        stream.flush()
        stream.close()
    }
}