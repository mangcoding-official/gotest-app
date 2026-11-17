package com.gotest.app.core.model

/** Indonesian WhatsApp numbers the way the web reads and shows them (web: app/Support/PhoneNumber.php). */
object PhoneNumber {
    private val stored = Regex("^628\\d{7,11}$")

    /** `628…` from what a person types (08…, 8…, +62…, 62…, spaces and dashes allowed); null when not a number. */
    fun normalize(input: String): String? {
        val digits = input.filter(Char::isDigit)
        val withCountry =
            when {
                digits.startsWith("62") -> digits
                digits.startsWith("0") -> "62" + digits.drop(1)
                digits.startsWith("8") -> "62$digits"
                else -> digits
            }
        return withCountry.takeIf(stored::matches)
    }

    /** `0812-3456-7890` for a stored `628…` number. */
    fun display(stored: String): String {
        val local = "0" + stored.removePrefix("62")
        return listOf(
            local.take(DISPLAY_GROUP),
            local.drop(DISPLAY_GROUP).take(DISPLAY_GROUP),
            local.drop(DISPLAY_GROUP * 2),
        ).filter { it.isNotEmpty() }
            .joinToString("-")
    }

    private const val DISPLAY_GROUP = 4
}

/** "Jenjang pendidikan" in the web's order (web: app/Support/EducationLevel.php). */
enum class EducationLevel { Sd, Smp, SmaSmk, Kuliah, Graduated }
