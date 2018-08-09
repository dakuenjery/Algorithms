import io.IOFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.*
import java.util.*
import java.util.stream.Stream
import kotlin.collections.ArrayList
import kotlin.streams.toList

fun genArray(size: Int, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): IntArray {
    val time = Date().time
    val rand = Random(time)
    val arr = rand.ints(size.toLong(), min, max).toList().toIntArray()
    return arr
}

class ExternalSortTest {

    class FactoriesProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(Arguments.of(TextIntFactory()), Arguments.of(BinaryIntFactory()))
        }
    }

    class ArgsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                    Arguments.of(intArrayOf(3, 89, 72, 92, 47, 45, 51, 37, 37, 62, 22, 97), 4, false, TextIntFactory()),
                    Arguments.of(intArrayOf(3, 89, 72, 92, 47, 45, 51, 37, 37, 62, 22, 97), 4, false, BinaryIntFactory()),

                    Arguments.of(genArray(10_000), 200, false, TextIntFactory()),
                    Arguments.of(genArray(10_000), 200, false, BinaryIntFactory()),

                    Arguments.of(genArray(50_000), 500, false, TextIntFactory()),
                    Arguments.of(genArray(50_000), 500, false, BinaryIntFactory())

            )
        }
    }

    @ParameterizedTest
    @ArgumentsSource(FactoriesProvider::class)
    fun ioTest(factory: IOFactory) {
        val file = File.createTempFile("tmp", "file")

        val arr = intArrayOf(1, 2, 3, 4, 5)

        factory.createWriter(file).use {
            it.writeBlock(arr, arr.size)
        }

        val buf = IntArray(5)
        val reader = factory.createReader(file)
        val n = reader.readBlock(buf)

        assertArrayEquals(arr, buf)

        file.delete()
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProvider::class)
    fun externalSortTest(arr: IntArray, maxMem: Int, saveFiles: Boolean, factory: IOFactory) {
        val infile = File.createTempFile("oomsort_", "_unsorted.bin")
        val outfile = File.createTempFile("oomsort_", "_sorted.bin")

        if (!saveFiles) {
            infile.deleteOnExit()
            outfile.deleteOnExit()
        } else {
            println("unsorted: ${infile.absolutePath}")
            println("sorted: ${outfile.absolutePath}")
        }

        factory.createWriter(infile).use {
            it.writeBlock(arr, arr.size)
        }

        val sortedArr = ArrayList(arr.sorted())

        externalSort(infile, outfile, factory, maxMem)

        var i = 0

        try {
            factory.createReader(outfile).use {
                for (value in sortedArr) {
                    assertEquals(value, it.readValue(), "ind: $i")
                    i += 1
                }
            }

        } catch (ex: EOFException) { }

        assertEquals(arr.size, i)
    }

}