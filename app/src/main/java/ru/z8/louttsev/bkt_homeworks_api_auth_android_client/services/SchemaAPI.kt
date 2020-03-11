package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services

import java.util.UUID

private const val API_URL = "https://api-auth-server-luttcev.herokuapp.com/api/v1/"

enum class SchemaAPI(val route: String) {
    POSTS("${API_URL}posts"),
    ADS("${API_URL}ads"),
    MEDIA("${API_URL}media"),
    REGISTRATION("${API_URL}registration"),
    AUTHENTICATION("${API_URL}authentication"),
    ME("${API_URL}me");

    fun routeWith(count: Int) = this.route + "/${count}"

    fun routeWith(postID: UUID, action: SocialAction) = this.route + "/${postID}" + "/${action}"

    enum class Mode {
        POST, DELETE
    }

    enum class SocialAction(private val action: String) {
        LIKE("like"),
        COMMENT("comment"),
        SHARE("share"),
        HIDE("hide");

        override fun toString(): String {
            return action
        }
    }
}
