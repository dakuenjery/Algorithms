package algorithms.utils

import java.text.DecimalFormat
import java.text.ParseException

private val bytesUnits = arrayOf("b", "kb", "mb", "gb", "tb")
private val thousandUnits = arrayOf("", "k", "m", "mm", "mmm")

private val regex = """(\d+([.]\d+)?)(\w+)?""".toRegex()
private val numberFormat = DecimalFormat("#.###")


fun Long.getOrder(order: Int = 1024) =
        (Math.log10(this.toDouble()) / Math.log10(order.toDouble())).toInt()


fun Long.getOrderingString(units: Array<String>, order: Int = 1024): String {
    val group = Math.min(this.getOrder(order), units.size-1)
    val value = this.toDouble()/Math.pow(order.toDouble(), group.toDouble())

    return "${numberFormat.format(value)}${units[group]}"
}

fun Long.toBytesString() = getOrderingString(bytesUnits, 1024)
fun Long.toThousandString() = getOrderingString(thousandUnits, 1000)


fun String.getOrderingValue(units: Array<String>, order: Int = 1024): Long {
    val values = regex.find(this)?.groupValues

    val v = when (values?.size) {
        2 -> values[1].toDouble()
        4 -> {
            val unitIndex = units.indexOf(values[3])

            if (unitIndex < 0)
                throw ParseException(this, 0)

            values[1].toDouble() * Math.pow(order.toDouble(), unitIndex.toDouble())
        }
        else -> throw ParseException(this, 0)
    }

    return v.toLong()
}

fun String.toBytes() = getOrderingValue(bytesUnits, 1024)
fun String.toThousand() = getOrderingValue(thousandUnits, 1000)
