import io.IOFactory
import io.IReader
import java.io.EOFException
import java.io.File
import java.io.IOException

fun externalSort(infile: File, outfile: File, factory: IOFactory, maxMemory: Int = 1000) {
    val alg = Algorithms()

    val reader = factory.createReader(infile)

    val filesize = infile.length()

    val memCount = maxMemory

    val blockCount = (filesize / memCount).toInt()
    val blockSize = (filesize/blockCount).toInt()

    val tempFiles = mutableListOf<File>()

    val buffer = IntArray(memCount)

    // step 1
    try {
        while (!reader.isEof) {
            tempFiles.add(File.createTempFile("oomsort", "temp.bin").apply {
                deleteOnExit()
            })

            val n = alg.createBlock(reader, buffer)
            factory.createWriter(tempFiles.last()).use {
                it.writeBlock(buffer, n)
            }
        }
    } catch (ex: EOFException) {
        reader.close()
    } catch (ex: IOException) {
        throw ex
    }

    // step 2
    val writer = factory.createWriter(outfile)

    alg.setupMerge(tempFiles.map { factory.createReader(it) })

    while (true) {
        val n = alg.getSortedBlock(buffer)

        if (n <= 0)
            break

        writer.writeBlock(buffer, n)
    }

    writer.close()
}

private class Algorithms {
    private lateinit var blockReaders: MutableList<IReader>

    fun createBlock(reader: IReader, buffer: IntArray): Int {
        val n = reader.readBlock(buffer)
        buffer.sort(toIndex = n)
        return n
    }

    fun setupMerge(readers: List<IReader>) {
        blockReaders = readers.toMutableList()
    }

    fun getSortedBlock(writeBuffer: IntArray): Int {
        var n = 0

        while (n < writeBuffer.size && blockReaders.isNotEmpty()) {
            var i = 0
            var value = Int.MAX_VALUE
            var index = -1

            while (i < blockReaders.size) {
                val v = blockReaders[i].getCachedValue()

                if (v <= value) {
                    value = v
                    index = i
                }

                i += 1
            }

            if (index > -1) {
                writeBuffer[n++] = value

                if (blockReaders[index].isEof)
                    blockReaders.removeAt(index)
                else
                    blockReaders[index].cacheNextValue()
            }
        }

        return n
    }
}