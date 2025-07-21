package redstone.artregister

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment


/**
 * A simple [Fragment] subclass.
 * Use the [CollectorFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CollectorFragment : Fragment() {
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
        client = mainActivity.getHttpClient()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_collector, container, false)
        val scanCardHint = root.findViewById<TextView>(R.id.textWaitForCard)
        val pieceInfoLayout = root.findViewById<View>(R.id.layoutPieceInfo)
        val pieceNameText = root.findViewById<TextView>(R.id.textPieceName)
        val creatorNameText = root.findViewById<TextView>(R.id.textCreatorName)
        val ownerNameText = root.findViewById<TextView>(R.id.textOwnerName)
        val isOnSaleText = root.findViewById<TextView>(R.id.textIsOnSale)

        val loadingDialog = createLoadingDialog(requireContext())

        mainActivity.clearCardID()
        Thread {
            while (true) {
                mainActivity.tryToGetCardID()?.let {
                    mainActivity.runOnUiThread {
                        loadingDialog.show()
                    }
                    // Wait for network operation to complete
                    val pieceInfo = client.getPieceInfo(it)

                    mainActivity.runOnUiThread {
                        loadingDialog.dismiss()
                        if (pieceInfo == null) { // Network error
                            Toast.makeText(
                                context,
                                R.string.network_error,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            if (pieceInfo.pieceName == null) {// Empty info
                                Toast.makeText(
                                    context,
                                    R.string.piece_not_found,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                scanCardHint.visibility = View.GONE
                                pieceInfoLayout.visibility = View.VISIBLE
                                pieceNameText.text = pieceInfo.pieceName
                                // Set the text to "You" if the username matches
                                if (pieceInfo.creatorName == username) {
                                    creatorNameText.text = getString(R.string.you)
                                    creatorNameText.setTextColor(Color.GREEN)
                                } else {
                                    creatorNameText.text = pieceInfo.creatorName
                                    creatorNameText.setTextColor(
                                        getColor(
                                            requireContext(),
                                            R.color.my_dark_text_color
                                        )
                                    )
                                }
                                // Set the text to "You" if the username matches
                                if (pieceInfo.ownerName == username) {
                                    ownerNameText.text = getString(R.string.you)
                                    ownerNameText.setTextColor(Color.GREEN)
                                } else {
                                    ownerNameText.text = pieceInfo.ownerName
                                    ownerNameText.setTextColor(
                                        getColor(
                                            requireContext(),
                                            R.color.my_dark_text_color
                                        )
                                    )
                                }
                                isOnSaleText.text = if (pieceInfo.isOnSale) "Yes" else "No"
                            }
                        }
                    }
                    mainActivity.clearCardID()
                }
                Thread.sleep(500)
            }
        }.start()


        return root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param username The username of the collector.
         * @param userId The user ID of the collector.
         * @return A new instance of fragment CollectorFragment.
         */
        @JvmStatic
        fun newInstance(username: String, userId: Int) = CollectorFragment().apply {
            arguments = Bundle().apply {
                putString("username", username)
                putInt("user_id", userId)
            }
        }
    }
}