package redstone.artregister

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View.inflate
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import redstone.artregister.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var username: String
    private var usertype = 0  // 0 for collector, 1 for creator

    private var nfcSupported = false
    private var cardIDGotten: String? = null

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

    companion object {
        const val TAG = "Main"
        val USERNAME_KEY = stringPreferencesKey("username")
        val USERTYPE_KEY = intPreferencesKey("usertype")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @OptIn(DelicateCoroutinesApi::class) GlobalScope.launch {
            username = dataStore.data.map {
                it[USERNAME_KEY] ?: "Shit"
            }.first()

            usertype = dataStore.data.map {
                it[USERTYPE_KEY] ?: 0
            }.first()

            if (usertype >= resources.getStringArray(R.array.user_types).size) usertype = 0

            switchFragment(usertype)
        }


        val testNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (testNfcAdapter != null) {
            nfcSupported = true
            nfcAdapter = testNfcAdapter
        }

        binding.topBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_username -> {
                    editUsernameDialog()
                    true
                }

                R.id.menu_foo -> {
                    true
                }

                else -> false
            }
        }
    }

    private fun editUsernameDialog() {
        val adbModifyUser = AlertDialog.Builder(this)
        val theView = inflate(this, R.layout.user_settings, null)
        val usernameInput = theView.findViewById<EditText>(R.id.usernameText)
        val userTypeSpinner = theView.findViewById<Spinner>(R.id.userTypeSpinner)

        userTypeSpinner.adapter = ArrayAdapter.createFromResource(
            this, R.array.user_types, android.R.layout.simple_spinner_item
        )
        userTypeSpinner.setSelection(usertype)
//        userTypeSpinner.onItemSelectedListener = object : OnItemSelectedListener {
//            override fun onItemSelected(
//                parent: AdapterView<*>?,
//                view: View?,
//                position: Int,
//                id: Long
//            ) {
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>?) {
//                Log.i(TAG, "Nothing selected")
//            }
//
//        }

        usernameInput.setText(username)
        adbModifyUser.setTitle("Modify User")
        adbModifyUser.setView(theView)
        adbModifyUser.setPositiveButton(
            "🆗"
        ) { _, _ ->
            @OptIn(DelicateCoroutinesApi::class) GlobalScope.launch {
                username = usernameInput.text.toString()
                usertype = userTypeSpinner.selectedItemId.toInt()
                dataStore.edit {
                    it[USERNAME_KEY] = username
                    it[USERTYPE_KEY] = usertype
                }
                switchFragment(usertype)
            }
            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
        }
        adbModifyUser.setNegativeButton("🆖", null)
        adbModifyUser.show()
    }

    private fun switchFragment(id: Int) {
        supportFragmentManager.beginTransaction().replace(
            R.id.fragmentContainerView, when (id) {
                0 -> CollectorFragment.newInstance(username)
                1 -> CreatorFragment.newInstance(username)
                else -> BlankFragment()
            }
        ).commit()
    }

    override fun onResume() {
        super.onResume()
        if (!nfcSupported) {
            return
        }
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED))

        val techListsArray =
            arrayOf(arrayOf<String>(NfcA::class.java.name, MifareClassic::class.java.name))

        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_MUTABLE
        )

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, techListsArray)
    }

    override fun onPause() {
        super.onPause()
        if (nfcSupported) nfcAdapter.disableForegroundDispatch(this)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val collectedTag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)

        Log.i(TAG, "Collected $collectedTag")
        if (collectedTag != null) {
            cardIDGotten = byteArrayToHexString(collectedTag.id)
        }
    }

    private fun byteArrayToHexString(ba: ByteArray): String {
        val sb = StringBuilder(ba.size * 2)
        for (b in ba) sb.append(String.format("%02X", b))
        return sb.toString()
    }

}
