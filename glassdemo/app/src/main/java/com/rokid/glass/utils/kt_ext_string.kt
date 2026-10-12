package com.rokid.glass.utils



fun String?.value(default: String = "", func: ((String) -> Unit)? = null): String {
    val v = this?.trim()
    return if (!v.isNullOrEmpty()) {
        func?.invoke(v)
        v
    } else {
        val def = default.trim()
        if (def.isNotEmpty()) {
            func?.invoke(default)
        }
        def
    }
}

fun String.eq(str: String): Boolean = trim() == str.trim()

fun String?.isSameNotEmpty(str: String?): Boolean {
    if (this == null || str == null) {
        return false
    }
    val s1 = this.trim()
    val s2 = str.trim()
    if (s1.isEmpty() && s2.isEmpty()) {
        return false
    }
    return s1 == s2
}

fun String?.getFloat(): String? {
    // 定义正则表达式，匹配整数或浮点数
    // Define a regular expression that matches integers or floating-point numbers.
    val regex = Regex("[+-]?\\d+(\\.\\d+)?")

    // 使用正则表达式查找第一个匹配的数字
    // Find the first matching number using the regular expression.
    val matchResult = regex.find(this ?: return null)

    // 如果找到匹配的数字，则返回该数字的字符串表示，否则返回 null
    // Return the matching number as a string, or null if no number is found.
    return matchResult?.value
}