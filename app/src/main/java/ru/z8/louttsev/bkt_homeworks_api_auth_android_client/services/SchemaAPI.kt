package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services

import java.util.UUID

private const val API_URL = "https://api-auth-server-luttcev.herokuapp.com/api/v1/"

enum class SchemaAPI(val route: String) {
    POSTS("${API_URL}posts"),
    ADS("${API_URL}ads"),
    MEDIA("${API_URL}media");

    fun routeWith(count: Int) = this.route + "/${count}"

    fun routeWith(postID: UUID, action: SocialAction) = this.route + "/${postID}" + "/${action}"

    enum class Mode {
        POST, DELETE
    }

    enum class SocialAction(private val action: String) {
        LIKE("like"),
        COMMENT("comment"),
        SHARE("share");

        override fun toString(): String {
            return action
        }
    }
}
