import algorithms.utils.toBytesString
import algorithms.utils.toThousand
import algorithms.utils.toThousandString
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.defaultLazy
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import io.BinaryIntFactory
import io.IOFactory
import io.TextIntFactory
import java.io.Closeable
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

class App : CliktCommand() {
    override fun run() = Unit
}

abstract class FactoryCmd(help: String = "", epilog: String = "", name: String? = null, invokeWithoutSubcommand: Boolean = false) :
        CliktCommand(help, epilog, name, invokeWithoutSubcommand)
{
    val factory by option()
            .switch(
                    "--text-int" to TextIntFactory() as IOFactory,
                    "--binary-int" to BinaryIntFactory() as IOFactory
            )
            .default(TextIntFactory())
}

class SortCmd : FactoryCmd(name = "sort") {

    private val inmemory by option("--inmemory").flag()

    private val memory by option("-m", "--memory").convert {
        val sizes = mapOf(
                "kb" to 1_000f,
                "mb" to 1_000_000f,
                "gb" to 1_000_000_000f
        )

        val values = """(\d+)(\w\w)?""".toRegex().find(it)?.groupValues

        val v = when (values?.size) {
            2 -> values[1].toFloat()
            3 -> values[1].toFloat() * sizes[values[2]]!!
            else -> throw Exception()
        }

        v.toInt()
    }.required()

    private val source by argument()
            .file(exists = true, fileOkay = true, folderOkay = false, readable = true)

    private val dest by argument().file().defaultLazy {
        source.let {
            return@defaultLazy File("${it.absolutePath}.sorted")
        }
    }

    override fun run() {
        println("Source file: ${source.canonicalPath}")
        println("Dest file: ${dest.canonicalPath}")

        if (inmemory) {
            val t = StopwatchConsole()
            val ms = measureTimeMillis {
                val buffer = mutableListOf<Int>()
                factory.createReader(source).readAll(buffer)
                buffer.sort()
                factory.createWriter(dest).writeAll(buffer)
            }
            t.cancel()

            println("\rSorted in ${ms.toMillisTime()} minutes.")
        } else {
            val blockingQueue: BlockingQueue<Message> = ArrayBlockingQueue<Message>(5)

            thread {
                while (true) {
                    val m = blockingQueue.poll(100, TimeUnit.SECONDS)?.message() ?: break
                    print(m)
                }
            }

            val ms = measureTimeMillis {
                externalSort(source, dest, factory, memory, blockingQueue = blockingQueue)
            }

            println("\nSorted in ${ms.toMillisTime()} minutes.")
        }
    }
}

class GenerateCmd : FactoryCmd(name = "generate") {

    private val dest by argument().file().defaultLazy {
        val name = "genfile_${Date().time}_${size.toThousandString()}_${factory.javaClass.simpleName}"
        File(name)
    }

    private val size by option("-s", "--size")
            .convert { it.toThousand() }
            .required()

    override fun run() {
        lateinit var data: IntArray

        StopwatchConsole("Generate data").use {
            measureTimeMillis {
                data = Random().ints(size).toArray()
            }
        }.let {
            println("\rData has been generated in ${it.toMillisTime()} minutes")
        }

        StopwatchConsole("Writing").use {
            measureTimeMillis {
                factory.createWriter(dest).use {
                    it.writeBlock(data, data.size)
                }
            }
        }.let {
            println("\rFile has been created in ${it.toMillisTime()} minutes!")
        }

        println("Path: ${dest.canonicalPath}")
        println("Size: ${dest.length().toBytesString()}")
    }
}

class BenchmarkCmd : FactoryCmd(name = "benchmark") {

    override fun run() {
        println("Running benchmark: ${factory.javaClass.simpleName}")

        val sizes = arrayOf(10_000, 100_000, 1_000_000)
        val files = sizes.map { File.createTempFile("benckmark", "").apply { deleteOnExit() } }

        val data = Random().ints(1_000_000).toArray()

        arrayOf("10k", "100k", "1m").forEachIndexed { i, msg ->

            val w = factory.createWriter(files[i])

            StopwatchConsole("\t$msg").use {
                measureTimeMillis { w.writeBlock(data, sizes[i]) }
            }.let {
                println("\r\t$msg: ${it.toMillisTime()}")
            }
        }
    }
}

class StopwatchConsole(val text: String = "Working") : Timer(), Closeable {
    private val startTime: Long = System.currentTimeMillis()
    private val iconArr = arrayOf("   ", ".  ", ".. ", "...")

    private val task = object : TimerTask() {
        override fun run() {
            val elapsed = System.currentTimeMillis() - startTime
            print("\r$text${iconArr[(elapsed % iconArr.size).toInt()]} ${elapsed.toMillisTime()} ")        }
    }

    override fun close() {
        this.cancel()
    }

    init {
        this.schedule(task, 0, 100)
    }
}

fun main(args: Array<String>) = App()
        .subcommands(GenerateCmd(), SortCmd(), BenchmarkCmd()).main(args)

fun Long.toMillisTime(): String {
    val min = (this / 1000 / 60).toString().padStart(2, '0')
    val sec = (this / 1000 % 60).toString().padStart(2, '0')
    val ms = (this % 1000).toString().padStart(3, '0')

    return "$min:$sec:$ms"
}