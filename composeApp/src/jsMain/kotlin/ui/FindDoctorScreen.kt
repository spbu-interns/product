package ui

import io.kvision.core.Container
import io.kvision.core.onEvent
import io.kvision.form.check.checkBox
import io.kvision.form.select.select
import io.kvision.form.text.text
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h2
import io.kvision.html.h3
import io.kvision.html.p
import io.kvision.panel.SimplePanel
import io.kvision.panel.hPanel
import io.kvision.panel.simplePanel
import io.kvision.panel.vPanel
import ui.components.bookingModal

import api.DoctorApiClient
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.interns.project.dto.DoctorSearchFilterDto
import org.interns.project.dto.DoctorSearchResultDto

private data class DoctorProfile(
    val name: String,
    val specialty: String,
    val rating: Double,
    val experienceYears: Int,
    val price: Int,
    val location: String,
    val bio: String
)

private enum class SortOption(val label: String, val comparator: Comparator<DoctorProfile>) {
    RATING_DESC("Рейтинг (по убыванию)", compareByDescending<DoctorProfile> { it.rating }.thenBy { it.name }),
    RATING_ASC("Рейтинг (по возрастанию)", compareBy<DoctorProfile> { it.rating }.thenBy { it.name }),
    EXPERIENCE_DESC("Стаж (по убыванию)", compareByDescending<DoctorProfile> { it.experienceYears }.thenBy { it.name }),
    EXPERIENCE_ASC("Стаж (по возрастанию)", compareBy<DoctorProfile> { it.experienceYears }.thenBy { it.name }),
    PRICE_DESC("Цена (по убыванию)", compareByDescending<DoctorProfile> { it.price }.thenBy { it.name }),
    PRICE_ASC("Цена (по возрастанию)", compareBy<DoctorProfile> { it.price }.thenBy { it.name });

    companion object {
        fun from(value: String?): SortOption = entries.firstOrNull { it.name == value } ?: RATING_DESC
    }
}

private val cities = listOf(
    "Москва",
    "Санкт-Петербург",
    "Новосибирск",
    "Екатеринбург",
    "Казань",
    "Нижний Новгород",
    "Челябинск",
    "Самара",
    "Ростов-на-Дону",
    "Уфа"
)

var loadedDoctors: List<DoctorSearchResultDto> = emptyList()

private fun specToId(name: String): Int? = when (name.lowercase()) {
    "кардиолог" -> 1
    "педиатр" -> 2
    "невролог" -> 3
    "ортопед" -> 4
    "офтальмолог" -> 5
    "терапевт" -> 6
    else -> null
}

private suspend fun loadDoctors(
    query: String,
    specialties: Set<String>,
    location: String?
): Result<List<DoctorSearchResultDto>> {

    val filter = DoctorSearchFilterDto(
        city = location,
        specializationIds = specialties.mapNotNull { specToId(it) },
        minRating = null,
        limit = 50
    )

    return DoctorApiClient().searchDoctors(filter)
}

private fun DoctorSearchResultDto.toUiProfile() = DoctorProfile(
    name = "Доктор №${id}",
    specialty = specializationNames.joinToString().ifBlank { profession },
    rating = rating ?: 0.0,
    experienceYears = experience ?: 0,
    price = (price ?: 0.0).toInt(),
    location = city ?: "Не указан",
    bio = info ?: "Информация отсутствует"
)

fun Container.findDoctorScreen(onLogout: () -> Unit) {
    headerBar(
        mode = if (Session.isLoggedIn) HeaderMode.PATIENT else HeaderMode.PUBLIC,
        active = NavTab.FIND,
        onLogout = onLogout
    )

    var searchQuery = ""
    var selectedLocation: String? = null
    var sortOption = SortOption.RATING_DESC
    val selectedSpecialties = mutableSetOf<String>()

    lateinit var resultsPanel: SimplePanel

    val bookingModalController = bookingModal()
    val onBookDoctor: (DoctorProfile) -> Unit = { bookingModalController.open(it.name) }

    div(className = "find-page") {
        div(className = "container") {

            h2("Найти врача", className = "find-title")

            hPanel(className = "find-search-bar") {
                val searchField = text {
                    placeholder = "Введите имя, специализацию или симптом"
                    addCssClass("find-search-input")
                }

                button("Поиск", className = "btn btn-primary find-search-button").onClick {
                    searchQuery = searchField.value?.trim().orEmpty()

                    MainScope().launch {
                        val result = loadDoctors(searchQuery, selectedSpecialties, selectedLocation)
                        println(result)
                        result.onSuccess { loadedDoctors = it }
                            .onFailure { println("Ошибка загрузки врачей: ${it.message}") }

                        renderResultsSort(resultsPanel, loadedDoctors, sortOption, onBookDoctor)
                    }
                }
            }

            div(className = "find-layout") {
                vPanel(className = "find-sidebar") {
                    h3("Фильтры", className = "find-sidebar-title")

                    div(className = "find-filter-card") {
                        h3("Сортировка", className = "find-filter-title")
                        val sortSelect = select(options = SortOption.entries.map { it.name to it.label }) {
                            value = sortOption.name
                            addCssClass("find-select")
                        }
                        sortSelect.onEvent {
                            change = {
                                sortOption = SortOption.from(sortSelect.value)
                                renderResultsSort(resultsPanel, loadedDoctors, sortOption, onBookDoctor)
                            }
                        }
                    }

                    // ---------- Specialties ----------
                    div(className = "find-filter-card") {
                        h3("Специальность", className = "find-filter-title")
                        val specialties = listOf("Кардиолог", "Педиатр", "Невролог", "Ортопед", "Офтальмолог", "Терапевт")

                        specialties.forEach { specialty ->
                            checkBox(false, label = specialty) {
                                addCssClass("find-checkbox")
                                onEvent {
                                    change = {
                                        if (value == true) selectedSpecialties.add(specialty)
                                        else selectedSpecialties.remove(specialty)

                                        MainScope().launch {
                                            val result = loadDoctors(searchQuery, selectedSpecialties, selectedLocation)
                                            println(result)
                                            result.onSuccess { loadedDoctors = it }
                                                .onFailure { println("Ошибка: ${it.message}") }

                                            renderResultsSort(resultsPanel, loadedDoctors, sortOption, onBookDoctor)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---------- City ----------
                    div(className = "find-filter-card") {
                        h3("Локация", className = "find-filter-title")
                        val locationSelect = select(
                            options = listOf("" to "Все города") + cities.map { it to it }
                        ) {
                            addCssClass("find-select")
                            value = ""
                        }

                        locationSelect.onEvent {
                            change = {
                                selectedLocation = locationSelect.value?.takeIf { it.isNotBlank() }

                                MainScope().launch {
                                    val result = loadDoctors(searchQuery, selectedSpecialties, selectedLocation)
                                    println(result)
                                    result.onSuccess { loadedDoctors = it }
                                        .onFailure { println("Ошибка: ${it.message}") }

                                    renderResultsSort(resultsPanel, loadedDoctors, sortOption, onBookDoctor)
                                }
                            }
                        }
                    }
                }

                resultsPanel = simplePanel(className = "find-results")
            }
        }
    }

    renderResultsSort(resultsPanel, loadedDoctors, sortOption, onBookDoctor)
}

private fun renderResultsSort(
    container: SimplePanel,
    doctors: List<DoctorSearchResultDto>,
    sortOption: SortOption,
    onBook: (DoctorProfile) -> Unit
) {
    container.removeAll()

    if (doctors.isEmpty()) {
        container.div(className = "find-empty") {
            h3("Ничего не найдено")
            p("Попробуйте изменить параметры поиска или выбрать другие фильтры.")
        }
        return
    }

    doctors
        .map { it.toUiProfile() }
        .sortedWith(sortOption.comparator)
        .forEach { ui ->
            container.doctorCard(ui, onBook)
        }
}

private fun Container.doctorCard(profile: DoctorProfile, onBook: (DoctorProfile) -> Unit) {
    val initials = profile.name
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .take(2)

    div(className = "doctor-card") {
        div(className = "doctor-card-avatar") {
            +(initials.ifBlank { "Фото" })
        }
        div(className = "doctor-card-content") {
            div(className = "doctor-card-header") {
                h3(profile.name, className = "doctor-card-name")
                div(className = "doctor-card-rating") {
                    +"★ ${profile.rating}"
                }
            }
            p(profile.specialty, className = "doctor-card-specialty")
            p("Стаж ${profile.experienceYears} лет • ${profile.location}", className = "doctor-card-meta")
            p(profile.bio, className = "doctor-card-bio")
            div(className = "doctor-card-footer") {
                p("от ${profile.price} ₽ / приём", className = "doctor-card-price")
                div(className = "doctor-card-actions") {
                    button("Посмотреть профиль", className = "btn btn-secondary btn-sm")
                    button("Записаться", className = "btn btn-primary btn-sm").onClick { onBook(profile) }
                }
            }
        }
    }
}
