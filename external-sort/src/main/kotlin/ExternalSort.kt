import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import java.io.*
import java.lang.Exception
import java.util.*
import kotlin.math.min
import kotlin.system.measureTimeMillis

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


interface IOFactory {
    fun createReader(file: File): IReader
    fun createWriter(file: File): IWriter
}

class BinaryIntFactory : IOFactory {
    override fun createReader(file: File) = BinaryReader(file)
    override fun createWriter(file: File) = BinaryWriter(file)
}

class TextIntFactory : IOFactory {
    override fun createReader(file: File) = TextReader(file)
    override fun createWriter(file: File) = TextWriter(file)
}


interface IReader : Closeable {
    fun readBlock(buffer: IntArray): Int
    fun readValue(): Int
    fun getCachedValue(): Int
    fun cacheNextValue(): Boolean
    val isEof: Boolean
}

interface IWriter : Closeable {
    fun writeBlock(buffer: IntArray, n: Int)
    fun writeValue(value: Int)
}

class BinaryReader(private val file: File) : IReader {
    private val stream = DataInputStream(FileInputStream(file))

    private var cached = false
    private var cachedValue = 0

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

class TextReader(private val file: File) : IReader {
    private val scanner = Scanner(BufferedReader(FileReader(file)))
    private var cached = false
    private var cachedValue: Int = 0

    init {
        scanner.useDelimiter(", ")
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

class Args(parser: ArgParser) {
    enum class Work {
        Gen, Sort
    }

    val genmode by parser.flagging("--gen", help = "Generate new file")

    val factory by parser.mapping(
            "--text-int" to TextIntFactory(),
            "--binary-int" to BinaryIntFactory(),
            help = "Data type which will be used in working"
    )

    val size by parser.storing("-s", "--size", help = "items in file") {
        val sizes = mapOf(
                "k" to 1_000f,
                "m" to 1_000_000f,
                "mm" to 1_000_000_000f
        )

        val values = """(\d+)(\w)?""".toRegex().find(this)?.groupValues

        val v = when (values?.size) {
            2 -> values[1].toFloat()
            3 -> values[1].toFloat() * sizes[values[2]]!!
            else -> throw Exception()
        }

        return@storing v.toLong()
    }.default<Long?>(null).addValidator {
        if (genmode && this.value == null)
            throw InvalidArgumentException("You must use --size with --gen flag")
    }

    val source by parser.positional("SOURCE", "source file").default<String?>(null)

    val dest by parser.positional("DEST", "destination file").default {
        val sizes = arrayOf("", "k", "m", "mm")

        when (genmode) {
            true -> {
                val (v, i) = size!!.humanityRound()
                "genfile_${Date().time}_$v${sizes[min(i, sizes.size-1)]}_${factory.javaClass.simpleName}"
            }
            false -> {
                this.source?.let {
                    val dotInd = it.lastIndexOf('_')
                    val f = it.substring(0, dotInd)
                    return@default "${f}_${Date().time}_${factory.javaClass.simpleName}"
                }
                ""
            }
        }
    }
}

fun main(appArgs: Array<String>) = mainBody {
    val args = ArgParser(appArgs).parseInto(::Args)

    if (args.genmode && args.size != null) {
        val file = File(args.dest)

        val data = Random().ints(args.size!!).toArray()

        val ms = measureTimeMillis {
            args.factory.createWriter(file).use {
                it.writeBlock(data, data.size)
            }
        }

        val mins = ms / 1000 / 60
        val secs = ms / 1000 % 60

        println("File has been created in $mins:$secs minutes!")
        println("Path: ${file.canonicalPath}")
        println("Size: ${file.length().humanitySize()}")
    } else {
        val source = File(args.source)
        val dest = File(args.dest)

        println("Starting...")
        println("Source file: ${source.canonicalPath}")
        println("Dest file: ${dest.canonicalPath}")

        val ms = measureTimeMillis {
            externalSort(source, dest, args.factory)
        }

        val mins = ms / 1000 / 60
        val secs = ms / 1000 % 60

        println("Sorted in $mins:$secs minutes!")
    }
}

fun Long.humanityRound(): Pair<Float, Int> {
    var v = this.toFloat()
    var i = 0

    while (v > 1000) {
        v /= 1000
        i += 1
    }

    return Pair(v, i)
}

fun Long.humanitySize(): String {
    val sizes = arrayOf("byte", "kb", "mb", "gb")
    val (v, i) = this.humanityRound()
    return "%.2f%s".format(v, sizes[min(i, sizes.size-1)])
}