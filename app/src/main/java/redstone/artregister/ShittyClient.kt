package redstone.artregister

import android.util.Log
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import redstone.artregister.MainActivity.Companion.TAG

/**
 * A simple HTTP client for interacting with the Art Register server.
 *
 * All the network operations are blocking, so they should be called from a background thread.
 */
class ShittyClient(private val serverAddress: String) {
    private enum class Status {
        OK,
        NOT_FOUND,
        ALREADY_EXISTS,
        VALUE_ERROR,
    }

    private val httpClient = OkHttpClient.Builder().connectTimeout(
        5, java.util.concurrent.TimeUnit.SECONDS
    ).build()

    /**
     * Parse the response from the server.
     *
     * This function takes a JSON response string and extracts the status and value.
     *
     * @param responseText The JSON response string from the server.
     * @return A Pair containing the status and the value (or error message if an error occurred).
     */
    private fun parseResponse(responseText: String): Pair<Status, Any> {
        val responseJSON = JSONObject(responseText)
        val status = Status.entries[responseJSON.getInt("status")]
        val value = responseJSON.get("value")

        return Pair(status, value)
    }

    /**
     * Get the user ID for a given username.
     *
     * This function sends a GET request to the server to retrieve the user ID
     * associated with the provided username.
     *
     * @param username The username for which to retrieve the user ID.
     * @return The user ID as an integer, or -1 if an error occurs.
     */
    fun getUserId(username: String): Int {
        val request =
            Request.Builder().url("http://$serverAddress/get_user_id?username=$username").get()
                .build()
        try {
            val responseText = httpClient.newCall(request).execute().body.string()

            Log.d(TAG, "Login response: $responseText")
            val (status, value) = parseResponse(responseText)

            if (status != Status.OK) {
                Log.e(TAG, "Error getting user ID: $value")
                return -1
            }

            Log.d(TAG, "User ID: $value")
            return value as Int
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user ID", e)
            return -1
        }
    }

    /**
     * Submit a new piece of art to the server.
     *
     * This function sends a POST request to the server with the user ID,
     * piece name, and card ID of the new piece.
     *
     * @param userId The ID of the user submitting the piece.
     * @param pieceName The name of the piece being submitted.
     * @param cardID The UID of the tag associated with the piece.
     */
    fun submitNewPiece(userId: Int, pieceName: String, cardID: String): Boolean {
        val formBodyBuilder = FormBody.Builder()
        formBodyBuilder.add("user_id", userId.toString())
        formBodyBuilder.add("piece_name", pieceName)
        formBodyBuilder.add("piece_uid", cardID)
        val formBody = formBodyBuilder.build()

        val request = Request.Builder()
            .url("http://$serverAddress/new_piece").post(formBody).build()

        try {


            val responseText = httpClient.newCall(request).execute().body.string()
            Log.d(TAG, "Submit new piece response: $responseText")
            val (status, value) = parseResponse(responseText)
            if (status != Status.OK) {
                Log.e(TAG, "Error submitting new piece: $value")
                return false
            }
            Log.d(TAG, "New piece submitted successfully.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting new piece", e)
            return false
        }
    }

    fun getCreations(userId: Int): List<PieceInfo>? {
        val request = Request.Builder()
            .url("http://$serverAddress/creator_get_pieces?user_id=$userId").get().build()

        try {
            val responseText = httpClient.newCall(request).execute().body.string()

            Log.d(TAG, "Login response: $responseText")
            val (status, value) = parseResponse(responseText)
            when (status) {
                Status.OK -> {
                    Log.d(TAG, "Creations received successfully.")
                    val pieces = value as JSONArray
                    val pieceList = mutableListOf<PieceInfo>()
                    for (i in 0 until pieces.length()) {
                        val pieceJSON = pieces.getJSONObject(i)
                        val pieceInfo = PieceInfo(
                            pieceJSON.getString("name"),
                            pieceJSON.getString("creator"),
                            pieceJSON.getString("owner")
                        )
                        pieceList.add(pieceInfo)
                    }
                    return pieceList
                }

                Status.NOT_FOUND -> {
                    Log.i(TAG, "No creations found for user ID $userId")
                    return emptyList()
                }

                else -> {
                    Log.e(TAG, "Error getting creations: $value")
                    return null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting creations", e)
            return null
        }
    }
}