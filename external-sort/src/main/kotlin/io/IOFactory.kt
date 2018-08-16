package io

import java.io.Closeable
import java.io.File

interface IOFactory {
    fun createReader(file: File): IReader
    fun createWriter(file: File): IWriter
}

interface IReader : Closeable {
    fun readAll(buffer: MutableList<Int>)
    fun readBlock(buffer: IntArray): Int
    fun readValue(): Int
    fun getCachedValue(): Int
    fun cacheNextValue(): Boolean
    val isEof: Boolean
}

interface IWriter : Closeable {
    fun writeAll(buffer: List<Int>)
    fun writeBlock(buffer: IntArray, n: Int)
    fun writeValue(value: Int)
}

class BinaryIntFactory : IOFactory {
    override fun createReader(file: File) = BinaryReader(file)
    override fun createWriter(file: File) = BinaryWriter(file)
}

class TextIntFactory : IOFactory {
    override fun createReader(file: File) = TextReader(file)
    override fun createWriter(file: File) = TextWriter(file)
}