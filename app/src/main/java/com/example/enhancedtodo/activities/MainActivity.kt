package com.example.enhancedtodo.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.enhancedtodo.R
import com.example.enhancedtodo.adapters.BoardItemsAdapter
import com.example.enhancedtodo.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.example.enhancedtodo.firebase.FirestoreClass
import com.example.enhancedtodo.models.Board
import com.example.enhancedtodo.models.User
import com.example.enhancedtodo.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : BaseActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    companion object {
        const val MY_PROFILE_REQUEST_CODE: Int = 11
        const val CREATE_BOARD_REQUEST_CODE: Int = 12
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var mUserName: String
    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpActionBar()

        binding.navView.setNavigationItemSelectedListener(this)

        mSharedPreferences =
            this.getSharedPreferences(Constants.PROJECT_MANAGEDB_PREFERENCES, Context.MODE_PRIVATE)
        val tokenUpdated = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED, false)
        if(tokenUpdated){
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().loadUserData(this, true)
        }else{
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        updateFCMToken(token)
                        //Log.d(TAG, "FCM token: $token")
                    } else {
                        Log.e("FCM Token", "Failed to get FCM token", task.exception)
                    }
                }

        }

        FirestoreClass().loadUserData(this@MainActivity, true)

    }

    fun populateBoardsListToUI(boardsList: ArrayList<Board>) {
        hideProgressDialog()

        val rvBoardList = findViewById<RecyclerView>(R.id.rv_boards_list)
        val tvNoBoardsAvailable = findViewById<TextView>(R.id.tv_no_boards_available)

        if (boardsList.size > 0) {
            rvBoardList.visibility = View.VISIBLE
            tvNoBoardsAvailable.visibility = View.GONE

            rvBoardList.layoutManager = LinearLayoutManager(this)
            rvBoardList.setHasFixedSize(true)

            val adapter = BoardItemsAdapter(this, boardsList)
            rvBoardList.adapter = adapter

            adapter.setOnClickListener(object : BoardItemsAdapter.OnClickListener {
                override fun onClick(position: Int, model: Board) {
                    val intent = Intent(this@MainActivity, TaskListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID, model.documentId)
                    startActivity(intent)
                }

            })
        } else {
            rvBoardList.visibility = View.GONE
            tvNoBoardsAvailable.visibility = View.VISIBLE
        }

    }

    private fun setUpActionBar() {
        val toolbarMain =
            findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_main_activity)
        setSupportActionBar(toolbarMain)
        toolbarMain.setNavigationIcon(R.drawable.ic_action_navigation_menu)
        toolbarMain.setNavigationOnClickListener {
            toggleDrawer()
        }

        val fab = findViewById<FloatingActionButton>(R.id.fab_create_board)
        fab.setOnClickListener {
            val intent = Intent(this@MainActivity, CreateBoard::class.java)
            intent.putExtra(Constants.NAME, mUserName)
            startActivityForResult(intent, CREATE_BOARD_REQUEST_CODE)
        }
    }

    private fun toggleDrawer() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    @SuppressLint("GestureBackNavigation", "MissingSuperCall")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            doubleBackToExit()
        }
    }

    fun updateNavigationUserDetails(user: User, readBoardsList: Boolean) {
        hideProgressDialog()
        mUserName = user.name

        Glide
            .with(this@MainActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(findViewById(R.id.nav_user_image))

        findViewById<TextView>(R.id.tv_username).text = user.name

        if (readBoardsList) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getBoardsList(this)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK
            && requestCode == MY_PROFILE_REQUEST_CODE
        ) {
            FirestoreClass().loadUserData(this)
        } else if (resultCode == Activity.RESULT_OK
            && requestCode == CREATE_BOARD_REQUEST_CODE
        ) {
            FirestoreClass().getBoardsList(this)
        } else {
            Log.e("Cancelled", "Cancelled")
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.nav_my_profile -> {
                startActivityForResult(
                    Intent(
                        this@MainActivity,
                        MyProfileActivity::class.java
                    ),
                    MY_PROFILE_REQUEST_CODE
                )
            }

            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()

                mSharedPreferences.edit().clear().apply()

                val intent = Intent(this@MainActivity, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    fun tokenUpdateSuccess() {
        hideProgressDialog()

        val editor: SharedPreferences.Editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().loadUserData(this, true)
    }

    private fun updateFCMToken(token: String) {
        val userHashMap = HashMap<String, Any>()
        userHashMap[Constants.FCM_TOKEN] = token

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().updateUserProfileData(this, userHashMap)
    }
}