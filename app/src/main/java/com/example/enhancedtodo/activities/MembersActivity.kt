package com.example.enhancedtodo.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.enhancedtodo.utils.Constants
import com.example.enhancedtodo.R
import com.example.enhancedtodo.adapters.MemberListItemsAdapter
import com.example.enhancedtodo.databinding.ActivityMembersBinding
import com.example.enhancedtodo.firebase.FirestoreClass
import com.example.enhancedtodo.models.Board
import com.example.enhancedtodo.models.User
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class MembersActivity : BaseActivity() {

    private lateinit var binding: ActivityMembersBinding

    private lateinit var mBoardDetails: Board
    private lateinit var mAssignedMembersList: ArrayList<User>
    private var anyChangesMade: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_members)

        binding = ActivityMembersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpActionBar()

        if (intent.hasExtra(Constants.BOARD_DETAIL)) {
            mBoardDetails = intent.getParcelableExtra(Constants.BOARD_DETAIL)!!
        }

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAssignedMembersListDetails(this, mBoardDetails.assignedTo)

    }

    fun setUpMembersList(list: ArrayList<User>) {
        mAssignedMembersList = list
        hideProgressDialog()

        binding.rvMembersList.layoutManager = LinearLayoutManager(this)
        binding.rvMembersList.setHasFixedSize(true)

        val adapter = MemberListItemsAdapter(this, list)
        binding.rvMembersList.adapter = adapter

    }

    fun memberDetails(user: User) {
        mBoardDetails.assignedTo.add(user.id)
        FirestoreClass().assignedMemberToBoard(this, mBoardDetails, user)
    }

    private fun setUpActionBar() {
        val toolbarMembers =
            findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_members_activity)
        setSupportActionBar(toolbarMembers)

        val actionbar = supportActionBar
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true)
            actionbar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
            actionbar.title = resources.getString(R.string.members)

        }
        toolbarMembers.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_member, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_member -> {
                dialogSearchMember()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun dialogSearchMember() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_search_member)
        dialog.findViewById<TextView>(R.id.tv_add).setOnClickListener {
            val email = dialog.findViewById<EditText>(R.id.et_email_search_member).text.toString()

            if (email.isNotEmpty()) {
                dialog.dismiss()
                showProgressDialog(resources.getString(R.string.please_wait))
                FirestoreClass().getMemberDetails(this, email)
            } else {
                Toast.makeText(
                    this@MembersActivity,
                    "Please enter member email address.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dialog.findViewById<TextView>(R.id.tv_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        if (anyChangesMade) {
            setResult(Activity.RESULT_OK)
        }

        super.onBackPressed()
    }

    fun memberAssignSuccess(user: User) {
        hideProgressDialog()
        mAssignedMembersList.add(user)

        anyChangesMade = true

        setUpMembersList(mAssignedMembersList)

        SendNotificationToUserAsyncTask(mBoardDetails.name, user.fcmToken).execute()
    }

    private inner class SendNotificationToUserAsyncTask(
        val boardName: String,
        val token: String,
    ) : AsyncTask<Any, Void, String>() {
        override fun doInBackground(vararg params: Any?): String {
            var result: String
            var connection: HttpURLConnection? = null
            try {
                val url = URL(Constants.FCM_BASE_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.doOutput = true
                connection.doInput = true
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"

                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("charset", "utf-8")
                connection.setRequestProperty("Accept", "application/json")

                connection.setRequestProperty(
                    Constants.FCM_AUTHORIZATION,
                    "${Constants.FCM_KEY}=${Constants.FCM_SERVER_KEY}"

                )

                connection.useCaches = false

                val wr = DataOutputStream(connection.outputStream)
                val jsonRequest = JSONObject()
                val dataObject = JSONObject()
                dataObject.put(Constants.FCM_KEY_TITLE, "Assigned to the board $boardName")
                dataObject.put(
                    Constants.FCM_KEY_MESSAGE,
                    "You have been assigned to the Board by ${mAssignedMembersList[0].name}"
                )
                jsonRequest.put(Constants.FCM_KEY_DATA, dataObject)
                jsonRequest.put(Constants.FCM_KEY_TO, token)

                wr.writeBytes(jsonRequest.toString())
                wr.flush()
                wr.close()

                val httpResultCode: Int = connection.responseCode
                if (httpResultCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream

                    val reader = BufferedReader(InputStreamReader(inputStream))

                    val sb = StringBuilder()
                    var line: String?
                    try{
                        while(reader.readLine().also { line = it } != null){
                            sb.append(line + "\n")
                        }
                    }catch (e: IOException){
                        e.printStackTrace()
                    }finally {
                        try {
                            inputStream.close()
                        }catch (e: IOException){
                            e.printStackTrace()
                        }
                    }

                    result = sb.toString()
                }else{
                    result = connection.responseMessage
                }

            } catch (e: SocketTimeoutException){
                result = "Connection Timeout"
            }catch (e: Exception){
                result = "Error: " + e.message
            }finally {
                connection?.disconnect()
            }

            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            hideProgressDialog()
            if (result != null) {
                Log.e("JSON Response wait ", result)
            }
        }

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog(resources.getString(R.string.please_wait))
        }

    }

}