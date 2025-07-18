package redstone.artregister

import android.util.Log
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import redstone.artregister.MainActivity.Companion.TAG

class ShittyClient(private val serverAddress: String) {
    private val httpClient = OkHttpClient.Builder().connectTimeout(
        5, java.util.concurrent.TimeUnit.SECONDS
    ).build()

    fun getUserId(username: String): Int {
        val request =
            Request.Builder().url("http://$serverAddress/get_user_id?username=$username").get()
                .build()
        try {
            val response = httpClient.newCall(request).execute()
            val responseText = response.body.string()
            Log.d(TAG, "Login response: $responseText")

            val responseJSON = JSONObject(responseText)
            val userId = responseJSON.getInt("value")
            Log.d(TAG, "User ID: $userId")
            return userId

        } catch (e: Exception) {
            Log.e(TAG, "Error getting user ID", e)
            return -1

        }
    }

    fun submitNewPiece(userId: Int, pieceName: String, cardID: String) {
        val formBodyBuilder = FormBody.Builder()
        formBodyBuilder.add("user_id", userId.toString())
        formBodyBuilder.add("piece_name", pieceName)
        formBodyBuilder.add("piece_uid", cardID)
        val formBody = formBodyBuilder.build()

        val request = Request.Builder()
            .url("http://$serverAddress/new_piece").post(formBody).build()

        val response = httpClient.newCall(request).execute()
    }
}