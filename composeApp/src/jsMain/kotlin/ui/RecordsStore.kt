package ui

import kotlin.js.Date
import kotlin.random.Random

data class MyRecord(
    val id: String,
    var topic: String,
    val createdAt: String,
    var specialty: String,
    var content: String = ""
)

object RecordsStore {
    private val items = mutableListOf<MyRecord>()

    fun all(): List<MyRecord> = items

    fun add(topic: String, specialty: String): MyRecord {
        val rec = MyRecord(
            id = Random.nextInt(1_000_000).toString(),
            topic = topic,
            createdAt = Date().toLocaleString(),
            specialty = specialty,
            content = ""
        )
        items.add(rec)
        return rec
    }

    fun byId(id: String): MyRecord? = items.firstOrNull { it.id == id }

    fun update(id: String, topic: String? = null, specialty: String? = null, content: String? = null) {
        byId(id)?.let {
            if (topic != null) it.topic = topic
            if (specialty != null) it.specialty = specialty
            if (content != null) it.content = content
        }
    }

    fun delete(id: String) {
        items.removeAll { it.id == id }
    }
}