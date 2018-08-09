package io

import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

class BinaryWriter(file: File) : IWriter {
    private val stream = DataOutputStream(FileOutputStream(file))

    override fun writeBlock(buffer: IntArray, n: Int) {
        for (i in 0 until n)
            stream.writeInt(buffer[i])
    }

    override fun writeValue(value: Int) {
        stream.writeInt(value)
    }

    override fun close() {
        stream.flush()
        stream.close()
    }
}