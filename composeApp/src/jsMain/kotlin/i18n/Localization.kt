package i18n

import api.ApiConfig
import io.kvision.i18n.I18n

object Localization {
    private const val DEFAULT_LANGUAGE = "ru"
    private val supportedLanguages = setOf("en", "ru")

    private val translations: Map<String, Map<String, String>> = mapOf(
        "en" to loadTranslations("./i18n/en.json"),
        "ru" to loadTranslations("./i18n/ru.json"),
    )

    private var listeners = mutableSetOf<() -> Unit>()

    private var _currentLanguage: String = ApiConfig.getLanguagePreference()?.takeIf { it in supportedLanguages }
        ?: DEFAULT_LANGUAGE
        set(value) {
            field = if (value in supportedLanguages) value else DEFAULT_LANGUAGE
            ApiConfig.setLanguagePreference(field)
            I18n.language = field
        }

    val currentLanguage: String
        get() = _currentLanguage

    init {
        I18n.language = _currentLanguage
    }

    fun translate(key: String): String {
        return translations[_currentLanguage]?.get(key)
            ?: translations[DEFAULT_LANGUAGE]?.get(key)
            ?: key
    }

    fun setLanguage(language: String) {
        if (language == _currentLanguage) return
        _currentLanguage = language
        listeners.forEach { it.invoke() }
    }

    fun toggleLanguage() {
        val next = if (_currentLanguage == "en") "ru" else "en"
        setLanguage(next)
    }

    fun addLanguageChangeListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeLanguageChangeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun loadTranslations(resourcePath: String): Map<String, String> {
        val require: dynamic = js("require")
        val data = require(resourcePath)
        val keys = js("Object.keys")(data).unsafeCast<Array<String>>()
        val result = mutableMapOf<String, String>()
        keys.forEach { key ->
            result[key] = data[key] as String
        }
        return result
    }
}

fun t(key: String): String = Localization.translate(key)