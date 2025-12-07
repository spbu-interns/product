package ui

data class DoctorProfile(
    val doctorId: Long,
    val name: String,
    val specialty: String,
    val rating: Double,
    val experienceYears: Int,
    val price: Int,
    val location: String,
    val bio: String,
    val gender: String?,
    val avatarUrl: String?
)