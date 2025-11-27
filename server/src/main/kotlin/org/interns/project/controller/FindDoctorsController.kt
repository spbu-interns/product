package org.interns.project.controller

import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.interns.project.dto.ApiResponse
import org.interns.project.users.model.DoctorSearchFilter
import org.interns.project.users.repo.ApiUserRepo

class FindDoctorsController(private val apiUserRepo: ApiUserRepo) {

    fun registerRoutes(routing: Routing) {
        routing.route("/doctors") {
            get("/search") {
                try {
                    val filter = DoctorSearchFilter(
                        specializationIds = call.request.queryParameters.getAll("specialization_ids")
                            ?.map { it.toInt() },
                        city = call.request.queryParameters["city"],
                        region = call.request.queryParameters["region"],
                        metro = call.request.queryParameters["metro"],
                        onlineOnly = call.request.queryParameters["online_only"]?.toBoolean() ?: false,
                        minPrice = call.request.queryParameters["min_price"]?.toDouble(),
                        maxPrice = call.request.queryParameters["max_price"]?.toDouble(),
                        minRating = call.request.queryParameters["min_rating"]?.toDouble(),
                        gender = call.request.queryParameters["gender"],
                        minAge = call.request.queryParameters["min_age"]?.toInt(),
                        maxAge = call.request.queryParameters["max_age"]?.toInt(),
                        minExperience = call.request.queryParameters["min_experience"]?.toInt(),
                        maxExperience = call.request.queryParameters["max_experience"]?.toInt(),
                        date = call.request.queryParameters["date"],
                        limit = call.request.queryParameters["limit"]?.toInt() ?: 20,
                        offset = call.request.queryParameters["offset"]?.toInt() ?: 0
                    )

                    val results = apiUserRepo.searchDoctors(filter)
                    println(results)
                    call.respond(
                        ApiResponse(
                            success = true,
                            data = results,
                            error = null
                        )
                    )
                } catch (e: Exception) {
                    call.application.log.error("Search doctors error", e)
                    call.respond(
                        ApiResponse(
                            success = false,
                            data = null,
                            error = "Search failed: ${e.message}"
                        )
                    )
                }
            }
        }
    }
}