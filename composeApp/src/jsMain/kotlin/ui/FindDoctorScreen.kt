package ui

import io.kvision.core.Container
import io.kvision.core.onEvent
import io.kvision.form.check.checkBox
import io.kvision.form.select.select
import io.kvision.form.text.Text
import io.kvision.form.text.text
import io.kvision.html.InputType
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h2
import io.kvision.html.h3
import io.kvision.html.p
import io.kvision.panel.SimplePanel
import io.kvision.panel.hPanel
import io.kvision.panel.simplePanel
import io.kvision.panel.vPanel
import state.PatientState
import ui.components.bookingModal
import ui.components.doctorProfileModal
import ui.components.updateAvatar

import api.DoctorApiClient
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.interns.project.dto.DoctorSearchFilterDto
import org.interns.project.dto.DoctorSearchResultDto
import org.interns.project.dto.UserResponseDto
import org.w3c.dom.url.URLSearchParams

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
private val doctorProfilesCache: MutableMap<Long, UserResponseDto> = mutableMapOf()

private fun specToId(name: String): Int? = when (name.lowercase()) {
    "терапевт" -> 1
    "педиатр" -> 2
    "стоматолог" -> 3
    "гинеколог" -> 4
    "уролог" -> 5
    "кардиолог" -> 6
    "невролог" -> 7
    "эндокринолог" -> 8
    "дерматолог" -> 9
    "офтальмолог" -> 10
    "отоларинголог" -> 11
    "психолог" -> 12
    "психотерапевт" -> 13
    "психиатр" -> 14
    "ортопед" -> 15  // "Травматолог-ортопед" имеет id 15
    "гастроэнтеролог" -> 16
    "аллерголог" -> 17  // "Аллерголог-иммунолог"
    "иммунолог" -> 17   // "Аллерголог-иммунолог"
    "диетолог" -> 18
    "онколог" -> 19
    "хирург" -> 20
    "ревматолог" -> 21
    else -> null
}

private fun isProfileComplete(): Boolean {
    if (!Session.isLoggedIn) return false
    return !(Session.fullName().isNullOrBlank() || Session.dateOfBirth == null ||
            Session.gender.isNullOrBlank() || Session.phoneNumber.isNullOrBlank())
}

private suspend fun loadDoctors(
    query: String,
    specialties: Set<String>,
    location: String?,
    date: String?
): Result<List<DoctorSearchResultDto>> {

    val filter = DoctorSearchFilterDto(
        city = location,
        specializationIds = specialties.mapNotNull { specToId(it) },
        minRating = null,
        date = date,
        limit = 50
    )

    return DoctorApiClient().searchDoctors(filter)
}

private fun DoctorSearchResultDto.matchesQuery(query: String, profile: UserResponseDto?): Boolean {
    if (query.isBlank()) return true
    val normalized = query.lowercase()
    val combined = listOfNotNull(
        profile?.surname,
        profile?.name,
        profile?.patronymic,
        profession,
        info,
        specializationNames.joinToString(" "),
        city,
        id.toString(),
        userId.toString()
    ).joinToString(" ")
    return combined.lowercase().contains(normalized)
}

private fun DoctorSearchResultDto.toUiProfile(profile: UserResponseDto?): DoctorProfile {
    val fullName = profile?.let {
        listOfNotNull(it.surname, it.name, it.patronymic).joinToString(" ").trim()
    }
    val safeName = if (fullName.isNullOrBlank()) "Доктор №${id}" else fullName

    return DoctorProfile(
        doctorId = id,
        name = safeName,
        specialty = specializationNames.joinToString().ifBlank { profession },
        rating = rating ?: 0.0,
        experienceYears = experience ?: 0,
        price = (price ?: 0.0).toInt(),
        location = city ?: "Не указан",
        bio = info ?: "Информация отсутствует",
        gender = profile?.gender,
        avatarUrl = profile?.avatar
    )
}

fun Container.findDoctorScreen(onLogout: () -> Unit) {
    val params = URLSearchParams(window.location.search)
    var searchQuery = Session.pendingDoctorSearchQuery ?: params.get("query") ?: ""
    Session.pendingDoctorSearchQuery = null
    val pendingSpecialty = Session.pendingDoctorSpecialty
    Session.pendingDoctorSpecialty = null
    val doctorApi = DoctorApiClient()

    headerBar(
        mode = when {
            Session.accountType.equals("DOCTOR", ignoreCase = true) -> HeaderMode.DOCTOR
            Session.isLoggedIn -> HeaderMode.PATIENT
            else -> HeaderMode.PUBLIC
        },
        active = NavTab.FIND,
        onLogout = onLogout
    )

    var selectedLocation: String? = null
    var selectedDate: String? = null
    var sortOption = SortOption.RATING_DESC
    val selectedSpecialties = mutableSetOf<String>().apply {
        pendingSpecialty?.let { add(it) }
    }

    lateinit var resultsPanel: SimplePanel
    lateinit var searchField: Text

    val bookingModalController = bookingModal(onAppointmentsUpdated = {
        Session.userId?.let { PatientState.loadPatientDashboard(it) }
    })
    val profileModalController = doctorProfileModal(onBook = {
        bookingModalController.open(it)
    })
    val onBookDoctor: (DoctorProfile) -> Unit = { bookingModalController.open(it) }

    suspend fun enrichProfiles(doctors: List<DoctorSearchResultDto>) {
        doctors.forEach { doc ->
            if (!doctorProfilesCache.containsKey(doc.userId)) {
                doctorApi.getUserProfile(doc.userId).onSuccess { doctorProfilesCache[doc.userId] = it }
            }
        }
    }

    div(className = "find-page") {
        div(className = "container") {

            h2("Найти врача", className = "find-title")

            hPanel(className = "find-search-bar") {
                searchField = text {
                    placeholder = "Введите имя, специализацию или симптом"
                    addCssClass("find-search-input")
                    value = searchQuery
                }

                button("Поиск", className = "btn btn-primary find-search-button").onClick {
                    searchQuery = searchField.value?.trim().orEmpty()

                    MainScope().launch {
                        val result = loadDoctors(searchQuery, selectedSpecialties, selectedLocation, selectedDate)
                        result.onSuccess {
                            loadedDoctors = it
                            enrichProfiles(loadedDoctors)
                        }
                            .onFailure { println("Ошибка загрузки врачей: ${it.message}") }

                        renderResultsSort(
                            resultsPanel,
                            loadedDoctors,
                            doctorProfilesCache,
                            sortOption,
                            onBookDoctor,
                            profileModalController::open,
                            searchQuery
                        )
                    }
                }
            }

            div(className = "find-layout") {
                vPanel(className = "find-sidebar") {
                    h3("Фильтры", className = "find-sidebar-title")

                    div(className = "find-filter-card") {
                        div(className = "find-filter-header") {
                            h3("Сортировка", className = "find-filter-title")
                            val sortSelect = select(options = SortOption.entries.map { it.name to it.label }) {
                                value = sortOption.name
                                addCssClass("find-select")
                            }
                            sortSelect.onEvent {
                                change = {
                                    sortOption = SortOption.from(sortSelect.value)
                                    renderResultsSort(resultsPanel, loadedDoctors, doctorProfilesCache, sortOption, onBookDoctor, profileModalController::open, searchQuery)
                                }
                            }
                        }
                    }

                    // ---------- Specialties ----------
                    div(className = "find-filter-card") {
                        h3("Специальность", className = "find-filter-title")
                        val specialties = listOf(
                            "Терапевт",
                            "Педиатр",
                            "Стоматолог",
                            "Гинеколог",
                            "Уролог",
                            "Кардиолог",
                            "Невролог",
                            "Эндокринолог",
                            "Дерматолог",
                            "Офтальмолог",
                            "Отоларинголог",
                            "Ортопед",
                            "Гастроэнтеролог",
                            "Аллерголог",
                            "Хирург"
                        )
                        specialties.forEach { specialty ->
                            checkBox(selectedSpecialties.contains(specialty), label = specialty) {
                                addCssClass("find-checkbox")
                                onEvent {
                                    change = {
                                        if (value == true) selectedSpecialties.add(specialty)
                                        else selectedSpecialties.remove(specialty)

                                        MainScope().launch {
                                            val result = loadDoctors(searchQuery, selectedSpecialties, selectedLocation, selectedDate)
                                            println(result)
                                            result.onSuccess {
                                                loadedDoctors = it
                                                enrichProfiles(loadedDoctors)
                                            }
                                                .onFailure { println("Ошибка: ${it.message}") }

                                            renderResultsSort(resultsPanel, loadedDoctors, doctorProfilesCache, sortOption, onBookDoctor, profileModalController::open, searchQuery)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---------- City ----------
                    div(className = "find-filter-card") {
                        div(className = "find-filter-header") {
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
                                        val result = loadDoctors(searchQuery, selectedSpecialties, selectedLocation, selectedDate)
                                        println(result)
                                        result.onSuccess {
                                            loadedDoctors = it
                                            enrichProfiles(loadedDoctors)
                                        }
                                            .onFailure { println("Ошибка: ${it.message}") }

                                        renderResultsSort(resultsPanel, loadedDoctors, doctorProfilesCache, sortOption, onBookDoctor, profileModalController::open, searchQuery)
                                    }
                                }
                            }
                        }
                    }

                    // ---------- Date ----------
                    div(className = "find-filter-card") {
                        h3("Дата приёма", className = "find-filter-title")

                        val dateInput = text(
                            value = selectedDate ?: "",
                            type = InputType.DATE,
                        ) {
                            addCssClass("find-date-input")
                        }

                        dateInput.onEvent {
                            change = {
                                selectedDate = dateInput.value?.takeIf { it.isNotBlank() }

                                MainScope().launch {
                                    val result = loadDoctors(
                                        searchQuery,
                                        selectedSpecialties,
                                        selectedLocation,
                                        selectedDate,
                                    )

                                    result
                                        .onSuccess {
                                            loadedDoctors = it
                                            enrichProfiles(loadedDoctors)
                                        }
                                        .onFailure { println("Ошибка: ${it.message}") }

                                    renderResultsSort(
                                        resultsPanel,
                                        loadedDoctors,
                                        doctorProfilesCache,
                                        sortOption,
                                        onBookDoctor,
                                        profileModalController::open,
                                        searchQuery,
                                    )
                                }
                            }
                        }
                    }
                }

                resultsPanel = simplePanel(className = "find-results")
            }
        }
    }

    MainScope().launch {
        val result = loadDoctors(searchQuery, selectedSpecialties, selectedLocation, selectedDate)
        result.onSuccess {
            loadedDoctors = it
            enrichProfiles(loadedDoctors)
        }
            .onFailure { println("Ошибка загрузки врачей: ${it.message}") }

        renderResultsSort(resultsPanel, loadedDoctors, doctorProfilesCache, sortOption, onBookDoctor, profileModalController::open, searchQuery)
    }
}

private fun renderResultsSort(
    container: SimplePanel,
    doctors: List<DoctorSearchResultDto>,
    profiles: Map<Long, UserResponseDto>,
    sortOption: SortOption,
    onBook: (DoctorProfile) -> Unit,
    onViewProfile: (DoctorProfile) -> Unit,
    query: String
) {
    container.removeAll()

    val filteredDoctors = doctors.map { it to profiles[it.userId] }.filter { (doc, profile) ->
        doc.matchesQuery(query, profile)
    }

    if (filteredDoctors.isEmpty()) {
        container.div(className = "find-empty") {
            h3("Ничего не найдено")
            p("Попробуйте изменить параметры поиска или выбрать другие фильтры.")
        }
        return
    }

    filteredDoctors
        .map { (doc, profile) -> doc.toUiProfile(profile) }
        .sortedWith(sortOption.comparator)
        .forEach { ui ->
            container.doctorCard(ui, onBook, onViewProfile)
        }
}

private fun Container.doctorCard(
    profile: DoctorProfile,
    onBook: (DoctorProfile) -> Unit,
    onViewProfile: (DoctorProfile) -> Unit
) {
    div(className = "doctor-card") {
        val initials = profile.name
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .take(2)

        div(className = "doctor-card-avatar")
            .apply { updateAvatar(profile.avatarUrl, initials.ifBlank { "Фото" }) }
        div(className = "doctor-card-content") {
            div(className = "doctor-card-header") {
                h3(profile.name, className = "doctor-card-name")
                div(className = "doctor-card-rating") {
                    +"★ ${profile.rating}"
                }
            }
            val genderLabel = when (profile.gender?.uppercase()) {
                "M" -> "Мужчина"
                "F" -> "Женщина"
                else -> null
            }
            p(profile.specialty, className = "doctor-card-specialty")
            p(
                listOfNotNull("Стаж ${profile.experienceYears} лет", genderLabel, profile.location).joinToString(" • "),
                className = "doctor-card-meta"
            )
            p(profile.bio, className = "doctor-card-bio")
            div(className = "doctor-card-footer") {
                div(className = "doctor-card-price-block") {
                    p("${profile.price} ₽ / приём", className = "doctor-card-price")
                    p("Может вырасти при добавлении услуг", className = "doctor-card-price-note")
                }
                div(className = "doctor-card-actions") {
                    button("Посмотреть профиль", className = "btn btn-secondary btn-sm").onClick {
                        onViewProfile(profile)
                    }
                    button("Записаться", className = "btn btn-primary btn-sm") {
//                        disabled = !Session.isLoggedIn

                    }.onClick {
                        if (Session.isLoggedIn) {
                            if (isProfileComplete()){
                                onBook(profile)}
                            else {
                                window.alert("Для записи на прием необходимо заполнить ФИО, дату рождения и номер телефона")
                                if (Session.accountType.equals("DOCTOR", ignoreCase = true)) {
                                    Navigator.showDoctorProfileEdit()
                                } else {
                                    Navigator.showPatientProfileEdit()
                                }
                            }
                        } else {
                            window.alert("Для записи на прием необходимо войти в систему")
                            Navigator.showLogin()
                        }
                    }
                }
            }
        }
    }
}
