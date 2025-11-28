package utils

fun normalizeGender(value: String?): String? = when (value?.uppercase()) {
    "M", "MALE", "М", "МУЖ", "МУЖСКОЙ" -> "M"
    "F", "FEMALE", "Ж", "ЖЕН", "ЖЕНСКИЙ" -> "F"
    else -> null
}