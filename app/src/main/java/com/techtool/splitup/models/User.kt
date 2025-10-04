package com.techtool.splitup.models

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val groupIds: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "groupIds" to groupIds
        )
    }
}
