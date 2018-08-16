import io.IOFactory
import io.IReader
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.util.concurrent.BlockingQueue

class Message(private val str: String?, private vararg val args: Any) {
    fun message(): String? {
        return str?.format(*args)
    }
}

fun externalSort(infile: File, outfile: File, factory: IOFactory, maxMemory: Int = 1000, blockingQueue: BlockingQueue<Message>) {
    val alg = Algorithms()

    val reader = factory.createReader(infile)

    val memCount = maxMemory

    val tempFiles = mutableListOf<File>()

    val buffer = IntArray(memCount)

    blockingQueue.put(Message("\nStep 1\n"))

    var proceed = 0

    // step 1
    try {
        while (!reader.isEof) {
            tempFiles.add(alg.createTempFile())

            val t1 = System.currentTimeMillis()

            val n = alg.createBlock(reader, buffer)
            proceed += n

            val t2 = System.currentTimeMillis()

            factory.createWriter(tempFiles.last()).use {
                it.writeBlock(buffer, n)
            }

            val t3 = System.currentTimeMillis()

            blockingQueue.put(Message("\rTemp files: %d: proceed: %d, sorting: %dms, writing: %dms",
                                            tempFiles.size,      proceed,       t2-t1,      t3-t2))
        }
    } catch (ex: EOFException) {
        reader.close()
    } catch (ex: IOException) {
        throw ex
    }

    // step 2
    val writer = factory.createWriter(outfile)
    alg.setupMerge(tempFiles.map { factory.createReader(it) })

    blockingQueue.put(Message("\nStep 2\n"))

    proceed = 0

    while (true) {
        val t0 = System.currentTimeMillis()
        val n = alg.getSortedBlock(buffer)

        if (n <= 0)
            break

        val t1 = System.currentTimeMillis()

        proceed += n
        writer.writeBlock(buffer, n)

        val t2 = System.currentTimeMillis()

        blockingQueue.put(Message("\rMerge: proceed: %d, read: %dms, write: %dms",
                                                proceed,        t1-t0,      t2-t1))
    }

    blockingQueue.put(Message(null))

    writer.close()
}

private class Algorithms {
    private lateinit var blockReaders: MutableList<IReader>

    fun createTempFile(): File {
        return File.createTempFile("oomsort", "temp.bin").apply {
            deleteOnExit()
        }
    }

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