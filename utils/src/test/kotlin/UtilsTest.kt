import algorithms.utils.toBytes
import algorithms.utils.toBytesString
import algorithms.utils.toThousand
import algorithms.utils.toThousandString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class UtilsTest {

    @ParameterizedTest
    @CsvSource("100000, 100k", "100, 100", "1024, 1.024k")
    fun thousandToString(input: Long, output: String) {
        val str = input.toThousandString()
        assertEquals(output, str)
    }

    @ParameterizedTest
    @CsvSource("100k, 100000", "11.04k, 11040")
    fun stringToThousand(input: String, output: Long) {
        val bytes = input.toThousand()
        assertEquals(output, bytes)
    }

    @ParameterizedTest
    @CsvSource("102401, 100.001kb", "100, 100b", "1024, 1kb",
            "1000000000000000000, 909494.702tb")
    fun bytesToString(input: Long, output: String) {
        val str = input.toBytesString()
        assertEquals(output, str)
    }

    @ParameterizedTest
    @CsvSource("100kb, 102400", "12.04kb, 12328")
    fun stringToBytes(input: String, output: Long) {
        val bytes = input.toBytes()
        assertEquals(output, bytes)
    }
}