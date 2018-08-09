import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.defaultLazy
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import io.BinaryIntFactory
import io.TextIntFactory
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.math.min
import kotlin.system.measureTimeMillis

class App : CliktCommand() {
    override fun run() = Unit
}

class SortCmd : CliktCommand(name = "sort") {

    private val source by argument()
            .file(exists = true, fileOkay = true, folderOkay = false, readable = true)

    private val dest by argument().file().defaultLazy {
        source.let {
            return@defaultLazy File("${it.absolutePath}.sorted")
        }
    }

    private val factory by option()
            .switch("--text-int" to TextIntFactory(), "--binary-int" to BinaryIntFactory())
            .default(TextIntFactory())

    override fun run() {
        println("Source file: ${source.canonicalPath}")
        println("Dest file: ${dest.canonicalPath}")

        val t = StopwatchConsole()

        val ms = measureTimeMillis {
            externalSort(source, dest, factory)
        }

        t.cancel()

        println("\rSorted in ${ms.toMillisTime()} minutes.")
    }
}

class GenerateCmd : CliktCommand(name = "generate") {

    private val dest by argument().file().defaultLazy {
        val sizes = arrayOf("", "k", "m", "mm")
        val (v, i) = size.humanityRound()
        val name = "genfile_${Date().time}_$v${sizes[min(i, sizes.size-1)]}_${factory.javaClass.simpleName}"
        File(name)
    }

    private val size by option("-s", "--size").convert {
        val sizes = mapOf(
                "k" to 1_000f,
                "m" to 1_000_000f,
                "mm" to 1_000_000_000f
        )

        val values = """(\d+)(\w)?""".toRegex().find(it)?.groupValues

        val v = when (values?.size) {
            2 -> values[1].toFloat()
            3 -> values[1].toFloat() * sizes[values[2]]!!
            else -> throw Exception()
        }

        v.toLong()
    }.required()

    private val factory by option()
            .switch("--text-int" to TextIntFactory(), "--binary-int" to BinaryIntFactory())
            .default(TextIntFactory())

    override fun run() {
        val data = Random().ints(size).toArray()

        val t = StopwatchConsole()

        val ms = measureTimeMillis {
            factory.createWriter(dest).use {
                it.writeBlock(data, data.size)
            }
        }

        t.cancel()

        println("\rFile has been created in ${ms.toMillisTime()} minutes!")
        println("Path: ${dest.canonicalPath}")
        println("Size: ${dest.length().humanitySize()}")
    }
}

class StopwatchConsole : Timer() {
    private val startTime: Long = System.currentTimeMillis()
    private val iconArr = arrayOf("   ", ".  ", ".. ", "...")

    private val task = object : TimerTask() {
        override fun run() {
            val elapsed = System.currentTimeMillis() - startTime
            print("\rWorking${iconArr[(elapsed % iconArr.size).toInt()]} ${elapsed.toMillisTime()}")        }
    }

    init {
        this.schedule(task, 0, 100)
    }
}

fun main(args: Array<String>) = App()
        .subcommands(GenerateCmd(), SortCmd()).main(args)

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

fun Long.toMillisTime(): String {
    val min = (this / 1000 / 60).toString().padStart(2, '0')
    val sec = (this / 1000 % 60).toString().padStart(2, '0')
    val ms = (this % 1000).toString().padStart(3, '0')

    return "$min:$sec:$ms"
}