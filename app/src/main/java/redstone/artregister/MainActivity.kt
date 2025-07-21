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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import redstone.artregister.databinding.ActivityMainBinding

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")
private lateinit var client: ShittyClient

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var nfcAdapter: NfcAdapter

    private lateinit var serverAddress: String
    private var username: String? = null
    private var usertype: Int? = null  // 0 for collector, 1 for creator
    private var userId = -1  // From the server, -1 means not logged in

    private var nfcSupported = false
    private var cardIDGotten: String? = null


    companion object {
        const val TAG = "Main"
        val SERVER_ADDRESS_KEY = stringPreferencesKey("server_address")
        val USERNAME_KEY = stringPreferencesKey("username")
        val USERTYPE_KEY = intPreferencesKey("usertype")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            serverAddress = dataStore.data.map {
                it[SERVER_ADDRESS_KEY] ?: "localhost:2333"
            }.first()

            username = dataStore.data.map {
                it[USERNAME_KEY]
            }.first()

            usertype = dataStore.data.map {
                it[USERTYPE_KEY]
            }.first()

            if (username == null || usertype == null) {
                withContext(Dispatchers.Main) {
                    editUsernameDialog(false)
                }
                return@launch
            }
            if (usertype!! >= resources.getStringArray(R.array.user_types).size) {
                Log.w(TAG, "Invalid usertype, setting it to 0")
                usertype = 0
            }

            client = ShittyClient(serverAddress)
            val loadingDialog = createLoadingDialog(this@MainActivity)
            loadingDialog.show()
            Thread {
                userId = client.getUserId(username!!)
                runOnUiThread {
                    loadingDialog.dismiss()
                    if (userId == -1) {
                        Toast.makeText(
                            this@MainActivity, R.string.network_error, Toast.LENGTH_SHORT
                        ).show()
                        return@runOnUiThread
                    }
                    supportFragmentManager.beginTransaction().replace(
                        R.id.fragmentContainerView, when (usertype) {
                            0 -> CollectorFragment.newInstance(username!!, userId)
                            1 -> CreatorFragment.newInstance(username!!, userId)

                            else -> BlankFragment()
                        }
                    ).commit()

                }
            }.start()

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

                R.id.menu_server_address -> {
                    editServerAddressDialog()
                    true
                }

                else -> false
            }
        }
    }

    private fun editUsernameDialog(isCancellable: Boolean = true) {
        val theView = inflate(this, R.layout.user_settings, null)
        val usernameInput = theView.findViewById<EditText>(R.id.usernameText)
        val userTypeSpinner = theView.findViewById<Spinner>(R.id.userTypeSpinner)

        userTypeSpinner.adapter = ArrayAdapter.createFromResource(
            this, R.array.user_types, android.R.layout.simple_spinner_dropdown_item
        )
        usertype?.let { userTypeSpinner.setSelection(it) }
        username?.let { usernameInput.setText(it) }

        val adbModifyUser = AlertDialog.Builder(this)
        adbModifyUser.apply {
            setTitle("Modify User")
            setView(theView)
            setCancelable(isCancellable)
            setPositiveButton(
                "🆗"
            ) { _, _ ->
                username = usernameInput.text.toString()
                usertype = userTypeSpinner.selectedItemId.toInt()

                lifecycleScope.launch {
                    dataStore.edit {
                        it[USERNAME_KEY] = username!!
                        it[USERTYPE_KEY] = usertype!!
                    }
                    withContext(Dispatchers.Main) {
                        recreate()
                    }
                }
                Toast.makeText(this@MainActivity, R.string.success, Toast.LENGTH_SHORT).show()
            }
            if (isCancellable) setNegativeButton("🆖", null)
            show()
        }
    }

    private fun editServerAddressDialog(isCancellable: Boolean = true) {
        val addressInput = EditText(this)
        addressInput.setText(serverAddress)

        val adbEditAddress = AlertDialog.Builder(this)
        adbEditAddress.apply {
            setTitle("Edit Server Address")
            setView(addressInput)
            setCancelable(isCancellable)
            setPositiveButton(
                "🆗"
            ) { _, _ ->
                // Handle server address change
                Toast.makeText(this@MainActivity, R.string.success, Toast.LENGTH_SHORT).show()
                serverAddress = addressInput.text.toString()
                lifecycleScope.launch {
                    dataStore.edit {
                        it[SERVER_ADDRESS_KEY] = serverAddress
                    }
                }
            }
            if (isCancellable) setNegativeButton("🆖", null)
            show()
        }
    }

    fun isNFCSupported(): Boolean = nfcSupported
    fun tryToGetCardID(): String? = cardIDGotten
    fun clearCardID() {
        cardIDGotten = null
    }

    fun getHttpClient(): ShittyClient = client


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
