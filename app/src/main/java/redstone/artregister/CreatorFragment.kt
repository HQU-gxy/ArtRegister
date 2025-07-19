package redstone.artregister

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.inflate
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import redstone.artregister.MainActivity.Companion.TAG

/**
 * A simple [Fragment] subclass.
 * Use the [CreatorFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreatorFragment : Fragment() {
    private var userId: Int? = null
    private var username: String? = null

    private lateinit var client: ShittyClient
    private lateinit var mainActivity: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            username = it.getString("username")
            userId = it.getInt("user_id")
        }
        mainActivity = activity as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        client = mainActivity.getHttpClient()
        val root = inflater.inflate(R.layout.fragment_creator, container, false)

        val newPieceButton = root.findViewById<Button>(R.id.buttonNewPiece)
        newPieceButton.setOnClickListener { onNewPieceButtonClicked() }

        // Inflate the layout for this fragment
        return root
    }

    private fun onNewPieceButtonClicked() {
        Log.d(TAG, "New piece!")

        // Don't proceed if NFC is not supported
        if (!mainActivity.isNFCSupported()) {
            Toast.makeText(
                context,
                requireContext().resources.getString(R.string.nfc_unsupported),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val theView = inflate(context, R.layout.new_piece, null)
        val adbNewPiece = AlertDialog.Builder(context)
        adbNewPiece.apply {
            setTitle("Register New Piece")
            setView(theView)
            setNegativeButton("🆖", null)
        }
        val newPieceDialog = adbNewPiece.show()

        var cardID: String? = null

        val pieceNameInput = theView.findViewById<EditText>(R.id.pieceNameText)
        val scanCardText = theView.findViewById<TextView>(R.id.scanCardHintText)
        val commitButton = theView.findViewById<Button>(R.id.commitPieceButton)
        commitButton.setOnClickListener {
            // Don't proceed if the name is not filled
            pieceNameInput.text.ifEmpty {
                Toast.makeText(context, R.string.name_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pieceName = pieceNameInput.text.toString()
            Log.i(TAG, "Piece: $pieceName, ID: $cardID")
            // Create a loading dialog
            val loadingDialog = AlertDialog.Builder(context).apply {
                setCancelable(false)
                setView(ProgressBar(context))
            }.show()

            lifecycleScope.launch {
                // Wait for the result from the server
                Thread {
                    client.submitNewPiece(userId!!, pieceName, cardID!!)
                }.start()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()
                    loadingDialog.dismiss()
                    newPieceDialog.dismiss()
                }
            }
        }

        // Wait for a card scanned
        mainActivity.clearCardID()
        lifecycleScope.launch {
            while (cardID == null) {
                cardID = mainActivity.tryToGetCardID()
                delay(500)
            }

            withContext(Dispatchers.Main) {
                scanCardText.setText(R.string.card_scanned)
                commitButton.isEnabled = true
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param username The username of the creator.
         * @param userId The user ID of the creator.
         * @return A new instance of fragment CreatorFragment.
         */
        @JvmStatic
        fun newInstance(username: String, userId: Int) =
            CreatorFragment().apply {
                arguments = Bundle().apply {
                    putString("username", username)
                    putInt("user_id", userId)
                }
            }
    }
}