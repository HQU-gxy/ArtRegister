package redstone.artregister

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.inflate
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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
    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            username = it.getString("username")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_creator, container, false)

        val newPieceButton = root.findViewById<Button>(R.id.button_new_piece)
        newPieceButton.setOnClickListener {
            Log.d(TAG, "New piece!")
            val mainActivity = activity as MainActivity
            // Don't proceed if NFC is not supported
            if (!mainActivity.isNFCSupported()) {
                Toast.makeText(
                    context,
                    requireContext().resources.getString(R.string.nfc_unsupported),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val theView = inflate(context, R.layout.new_piece, null)
            val adbNewPiece = AlertDialog.Builder(context)
            adbNewPiece.apply {
                setTitle("Register New Piece")
                setView(theView)
                setNegativeButton("🆖", null)
            }
            val dialog = adbNewPiece.show()

            var cardID: String? = null
            val pieceNameInput = theView.findViewById<EditText>(R.id.pieceNameText)
            val scanCardText = theView.findViewById<TextView>(R.id.scanCardHintText)
            val commitButton = theView.findViewById<Button>(R.id.commitPieceButton)
            commitButton.setOnClickListener {
                Log.i(TAG, "Piece: ${pieceNameInput.text}, ID: $cardID")
                dialog.dismiss()
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

        // Inflate the layout for this fragment
        return root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param username Parameter 1.
         * @return A new instance of fragment CreatorFragment.
         */
        @JvmStatic
        fun newInstance(username: String) = CreatorFragment().apply {
            arguments = Bundle().apply {
                putString("username", username)
            }
        }
    }
}