package redstone.artregister

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


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
//        val isOnSaleText = root.findViewById<TextView>(R.id.textIsOnSale)
        val viewTransButton = root.findViewById<Button>(R.id.buttonTransactions)
        val transferButton = root.findViewById<Button>(R.id.buttonTransfer)

        mainActivity.clearCardID()
        Thread {
            while (true) {
                mainActivity.tryToGetCardID()?.let { cardId ->
                    mainActivity.runOnUiThread {
                        mainActivity.showLoadingDialog()
                    }
                    // Wait for network operation to complete
                    val pieceInfo = client.getPieceInfo(cardId)

                    mainActivity.runOnUiThread {
                        mainActivity.dismissLoadingDialog()
                        if (pieceInfo == null) { // Network error
                            Toast.makeText(
                                context, R.string.network_error, Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            if (pieceInfo.pieceName == null) {// Empty info
                                Toast.makeText(
                                    context, R.string.piece_not_found, Toast.LENGTH_SHORT
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
                                            requireContext(), R.color.my_dark_text_color
                                        )
                                    )
                                }
                                // Set the text to "You" if the username matches
                                if (pieceInfo.ownerName == username) {
                                    transferButton.visibility = View.VISIBLE
                                    transferButton.setOnClickListener {
                                        onTransferButtonClicked(
                                            cardId
                                        )
                                    }
                                    ownerNameText.text = getString(R.string.you)
                                    ownerNameText.setTextColor(Color.GREEN)
                                } else {
                                    transferButton.visibility = View.INVISIBLE

                                    ownerNameText.text = pieceInfo.ownerName
                                    ownerNameText.setTextColor(
                                        getColor(
                                            requireContext(), R.color.my_dark_text_color
                                        )
                                    )
                                }
//                                isOnSaleText.text = if (pieceInfo.isOnSale) "Yes" else "No"
                                viewTransButton.setOnClickListener {
                                    onViewTransactionsButtonClicked(
                                        cardId
                                    )
                                }
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

    fun onViewTransactionsButtonClicked(cardId: String) {
        mainActivity.showLoadingDialog()
        Thread {
            val transactions = client.getTransactions(cardId)
            mainActivity.runOnUiThread {
                mainActivity.dismissLoadingDialog()

                if (transactions == null) {
                    Toast.makeText(
                        context, R.string.network_error, Toast.LENGTH_SHORT
                    ).show()
                    return@runOnUiThread
                }

                if (transactions.isEmpty()) {
                    Toast.makeText(
                        context, R.string.no_transaction, Toast.LENGTH_SHORT
                    ).show()
                    return@runOnUiThread
                }

                val transListView = RecyclerView(requireContext())
                transListView.layoutManager = LinearLayoutManager(context)
                val adbTransList = AlertDialog.Builder(context).apply {
                    setTitle(R.string.my_creations)
                    setView(transListView)
                    setNegativeButton("Close", null)
                }
                transListView.adapter = TransListAdapter(transactions, username!!, requireContext())
                adbTransList.show()
            }
        }.start()
    }

    fun onTransferButtonClicked(cardId: String) {
        val newOwnerInput = EditText(context)
        newOwnerInput.setHint(R.string.new_owner)
        AlertDialog.Builder(context).apply {
            setTitle("Transfer Piece Ownership")
            setView(newOwnerInput)
            setPositiveButton("🆗") { _, _ ->
                mainActivity.showLoadingDialog()
                Thread {
                    val newOwnerId = client.getUserId(newOwnerInput.text.toString())

                    if (newOwnerId == null) {
                        mainActivity.dismissLoadingDialog()
                        mainActivity.runOnUiThread {
                            Toast.makeText(
                                context, R.string.network_error, Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@Thread
                    }

                    if (newOwnerId == -1) {
                        mainActivity.dismissLoadingDialog()
                        mainActivity.runOnUiThread {
                            Toast.makeText(
                                context, R.string.user_not_found, Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@Thread
                    }

                    if (newOwnerId == userId) {
                        mainActivity.dismissLoadingDialog()
                        mainActivity.runOnUiThread {
                            Toast.makeText(
                                context, R.string.cannot_transfer_self, Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@Thread
                    }

                    val result = client.submitTransfer(userId!!, cardId, newOwnerId)
                    mainActivity.runOnUiThread {
                        mainActivity.dismissLoadingDialog()
                        if (result == null) {
                            Toast.makeText(
                                context, R.string.network_error, Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }

                        if (!result) {
                            Toast.makeText(
                                context, R.string.fail, Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }

                        Toast.makeText(
                            context, R.string.success, Toast.LENGTH_SHORT
                        ).show()
                    }
                }.start()
            }
            setNegativeButton("🆖", null)
            show()
        }
    }

    private class TransListAdapter(
        private val dataSet: List<Transaction>,
        private val username: String,
        private val context: Context
    ) : RecyclerView.Adapter<TransListAdapter.TransViewHolder>() {
        class TransViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val oldOwnerText: TextView = view.findViewById(R.id.textOldOwner)
            val newOwnerText: TextView = view.findViewById(R.id.textNewOwner)
            val datetimeText: TextView = view.findViewById(R.id.textDatetime)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup, viewType: Int
        ): TransViewHolder {
            // Inflate your item layout and return a ViewHolder
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.transaction_list_item, parent, false)

            return TransViewHolder(view)
        }

        override fun onBindViewHolder(holder: TransViewHolder, position: Int) {
            if (position == 0) // The first item is a header
                return

            val oldOwner = dataSet[position - 1].oldOwner
            val newOwner = dataSet[position - 1].newOwner

            if (oldOwner == username) {
                holder.oldOwnerText.text = context.getString(R.string.you)
                holder.oldOwnerText.setTextColor(Color.GREEN)
            } else {
                holder.oldOwnerText.text = oldOwner
                holder.oldOwnerText.setTextColor(context.getColor(R.color.my_dark_text_color))
            }

            if (newOwner == username) {
                holder.newOwnerText.text = context.getString(R.string.you)
                holder.newOwnerText.setTextColor(Color.GREEN)
            } else {
                holder.newOwnerText.text = newOwner
                holder.newOwnerText.setTextColor(context.getColor(R.color.my_dark_text_color))
            }

            holder.datetimeText.text = dataSet[position - 1].dt
        }

        override fun getItemCount() = dataSet.size + 1
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