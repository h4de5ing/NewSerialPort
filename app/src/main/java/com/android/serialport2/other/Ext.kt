package com.android.serialport2.other

/**
 * 将源数组追加到目标数组
 *
 * @param byte1 Sou1原数组1
 * @param byte2 Sou2原数组2
 * @param size   长度
 * @return  返回一个新的数组，包括了原数组1和原数组2
 */
private fun arrayAppend(byte1: ByteArray?, byte2: ByteArray?, size: Int): ByteArray? {
    return if (byte1 == null && byte2 == null) {
        null
    } else if (byte1 == null) {
        val byte3 = ByteArray(size)
        System.arraycopy(byte2, 0, byte3, 0, size)
        byte3
    } else if (byte2 == null) {
        val byte3 = ByteArray(byte1.size)
        System.arraycopy(byte1, 0, byte3, 0, byte1.size)
        byte3
    } else {
        val byte3 = ByteArray(byte1.size + size)
        System.arraycopy(byte1, 0, byte3, 0, byte1.size)
        System.arraycopy(byte2, 0, byte3, byte1.size, size)
        byte3
    }
}

/**
 * 数组拼接
 * @return {0x01}+{0x02,0x03}={0x01,0x02,0x03}
 */
fun ByteArray.add(data: ByteArray): ByteArray {
    val resultData = ByteArray(this.size + data.size)
    System.arraycopy(this, 0, resultData, 0, this.size)
    resultData[this.size] = data.size.toByte()
    System.arraycopy(data, 0, resultData, this.size, data.size)
    return resultData
}

/**
 * 字符串转数组
 * 输入: 000102030405060708
 * @return 返回 {0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08}
 */
fun String.toHexByteArray(): ByteArray {
    var newStr = this.replace(" ", "")
    val data = ByteArray(newStr.length / 2)
    try {
        if (newStr.length % 2 != 0)
            newStr = newStr.substring(0, newStr.length - 1) + "0" +
                    newStr.substring(newStr.length - 1, newStr.length)
        for (j in data.indices) {
            data[j] = (Integer.valueOf(newStr.substring(j * 2, j * 2 + 2), 16) and 0xff).toByte()
        }
    } catch (_: Exception) {
    }
    return data
}

/**
 * 数组打印
 * @param {0x01,0x02,0x03}
 * @return 01 02 03
 */
fun ByteArray.toHexString(): String = this.toHexString(this.size)
fun ByteArray.toHexString(length: Int): String {
    val sb = StringBuilder()
    val hex =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    for (i in 0 until length) {
        val value: Int = this[i].toInt() and 0xff
        sb.append(hex[value / 16]).append(hex[value % 16]).append(" ")
    }
    return sb.toString()
}