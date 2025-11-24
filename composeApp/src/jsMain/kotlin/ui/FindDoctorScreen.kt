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
    EXPERIENCE_DESC(
        "Стаж (по убыванию)",
        compareByDescending<DoctorProfile> { it.experienceYears }.thenBy { it.name }
    ),
    EXPERIENCE_ASC(
        "Стаж (по возрастанию)",
        compareBy<DoctorProfile> { it.experienceYears }.thenBy { it.name }
    ),
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

private val doctorProfiles = listOf(
    DoctorProfile(
        name = "Анна Смирнова",
        specialty = "Кардиолог",
        rating = 4.9,
        experienceYears = 12,
        price = 3400,
        location = "Москва",
        bio = "Специализируется на профилактике сердечно-сосудистых заболеваний и телемедицине."
    ),
    DoctorProfile(
        name = "Игорь Кузнецов",
        specialty = "Педиатр",
        rating = 4.7,
        experienceYears = 9,
        price = 2500,
        location = "Санкт-Петербург",
        bio = "Помогает детям с частыми простудами и проблемами иммунитета, ведёт приём онлайн."
    ),
    DoctorProfile(
        name = "Светлана Полякова",
        specialty = "Невролог",
        rating = 4.8,
        experienceYears = 15,
        price = 3800,
        location = "Казань",
        bio = "Работает со взрослыми пациентами с мигренями и расстройствами сна."
    ),
    DoctorProfile(
        name = "Алексей Громов",
        specialty = "Ортопед",
        rating = 4.6,
        experienceYears = 11,
        price = 3200,
        location = "Екатеринбург",
        bio = "Ведёт пациентов после травм и операций, консультирует спортсменов."
    ),
    DoctorProfile(
        name = "Мария Руднева",
        specialty = "Офтальмолог",
        rating = 4.9,
        experienceYears = 14,
        price = 3600,
        location = "Москва",
        bio = "Эксперт по коррекции зрения и ведению пациентов с глаукомой."
    ),
    DoctorProfile(
        name = "Олег Данилов",
        specialty = "Терапевт",
        rating = 4.5,
        experienceYears = 7,
        price = 2200,
        location = "Новосибирск",
        bio = "Проводит комплексные чек-апы и сопровождает пациентов с хроническими заболеваниями."
    ),
    DoctorProfile(
        name = "Виктория Ермакова",
        specialty = "Кардиолог",
        rating = 4.4,
        experienceYears = 6,
        price = 2600,
        location = "Ростов-на-Дону",
        bio = "Работает с пациентами после Covid-19, помогает восстановить работу сердца."
    ),
    DoctorProfile(
        name = "Сергей Лебедев",
        specialty = "Терапевт",
        rating = 4.8,
        experienceYears = 18,
        price = 3100,
        location = "Самара",
        bio = "Сильная сторона — выстраивание долгосрочных программ наблюдения."
    )
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
                    renderResults(
                        resultsPanel,
                        searchQuery,
                        selectedSpecialties,
                        selectedLocation,
                        sortOption,
                        onBookDoctor
                    )
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
                                renderResults(
                                    resultsPanel,
                                    searchQuery,
                                    selectedSpecialties,
                                    selectedLocation,
                                    sortOption,
                                    onBookDoctor
                                )
                            }
                        }
                    }

                    div(className = "find-filter-card") {
                        h3("Специальность", className = "find-filter-title")
                        val specialties = listOf("Кардиолог", "Педиатр", "Невролог", "Ортопед", "Офтальмолог", "Терапевт")
                        specialties.forEach { specialty ->
                            checkBox(false, label = specialty) {
                                addCssClass("find-checkbox")
                                onEvent {
                                    change = {
                                        if (value == true) selectedSpecialties.add(specialty) else selectedSpecialties.remove(specialty)
                                        renderResults(
                                            resultsPanel,
                                            searchQuery,
                                            selectedSpecialties,
                                            selectedLocation,
                                            sortOption,
                                            onBookDoctor
                                        )
                                    }
                                }
                            }
                        }
                    }

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
                                renderResults(
                                    resultsPanel,
                                    searchQuery,
                                    selectedSpecialties,
                                    selectedLocation,
                                    sortOption,
                                    onBookDoctor
                                )
                            }
                        }
                    }
                }

                resultsPanel = simplePanel(className = "find-results")
            }
        }
    }

    renderResults(resultsPanel, searchQuery, selectedSpecialties, selectedLocation, sortOption, onBookDoctor)
}

private fun renderResults(
    container: SimplePanel,
    query: String,
    specialties: Set<String>,
    location: String?,
    sortOption: SortOption,
    onBook: (DoctorProfile) -> Unit
) {
    container.removeAll()
    val normalizedQuery = query.trim().lowercase()
    val filtered = doctorProfiles
        .asSequence()
        .filter { profile ->
            val matchesQuery = normalizedQuery.isBlank() || listOf(
                profile.name,
                profile.specialty,
                profile.location,
                profile.bio
            ).any { it.lowercase().contains(normalizedQuery) }

            val matchesSpecialty = specialties.isEmpty() || specialties.contains(profile.specialty)
            val matchesLocation = location.isNullOrBlank() || profile.location == location

            matchesQuery && matchesSpecialty && matchesLocation
        }
        .sortedWith(sortOption.comparator)
        .toList()

    if (filtered.isEmpty()) {
        container.div(className = "find-empty") {
            h3("Ничего не найдено")
            p("Попробуйте изменить параметры поиска или выбрать другие фильтры.")
        }
        return
    }

    filtered.forEach { profile ->
        container.doctorCard(profile, onBook)
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