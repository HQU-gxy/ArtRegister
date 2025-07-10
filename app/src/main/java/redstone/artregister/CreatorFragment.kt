package redstone.artregister

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
            Log.i(TAG, "New piece!")
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