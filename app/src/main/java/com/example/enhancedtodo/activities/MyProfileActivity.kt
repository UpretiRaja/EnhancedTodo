package com.example.enhancedtodo.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import com.example.enhancedtodo.utils.Constants
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.example.enhancedtodo.R
import com.example.enhancedtodo.databinding.ActivityMyProfileBinding
import com.example.enhancedtodo.firebase.FirestoreClass
import com.example.enhancedtodo.models.User
import java.io.IOException

class MyProfileActivity : BaseActivity() {

    private var mSelectedImageFileUri: Uri? = null
    private lateinit var mUserDetails: User
    private var mProfileImageURL: String = ""

    private lateinit var binding: ActivityMyProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpActionBar()

        FirestoreClass().loadUserData(this@MyProfileActivity)

        binding.ivUserProfileImage.setOnClickListener {

            if (ContextCompat.checkSelfPermission(
                    this@MyProfileActivity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Constants.showImageChooser(this@MyProfileActivity)
            } else {
                ActivityCompat.requestPermissions(
                    this@MyProfileActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    Constants.READ_STORAGE_PERMISSION_CODE
                )
            }

        }

        binding.btnUpdate.setOnClickListener {
            if (mSelectedImageFileUri != null) {
                uploadUserImage()
            } else {
                showProgressDialog(resources.getString(R.string.please_wait))

                updateUserProfileData()
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Constants.showImageChooser(this@MyProfileActivity)
            }
        } else {
            Toast.makeText(
                this@MyProfileActivity,
                "Oops, you just denied the permission for storage. You can enable inside settings",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK
            && requestCode == Constants.PICK_IMAGE_REQUEST_CODE
            && data!!.data != null
        ) {
            mSelectedImageFileUri = data.data

            try {
                Glide
                    .with(this@MyProfileActivity)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(binding.ivUserProfileImage)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun setUpActionBar() {
        val toolbarMyProfile =
            findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_my_profile_activity)
        setSupportActionBar(toolbarMyProfile)

        val actionbar = supportActionBar
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true)
            actionbar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
            actionbar.title = resources.getString(R.string.my_profile_title)
        }

        toolbarMyProfile.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    fun setUserDataInUI(user: User) {

        mUserDetails = user

        Glide
            .with(this@MyProfileActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(binding.ivUserProfileImage)

        binding.etName.setText(user.name)
        binding.etEmail.setText(user.email)
        if (user.mobile != 0L) {
            binding.etMobile.setText(user.mobile.toString())
        }
    }

    private fun updateUserProfileData() {
        val userHashMap = HashMap<String, Any>()

        if (mProfileImageURL.isNotEmpty() && mProfileImageURL != mUserDetails.image) {
            userHashMap[Constants.IMAGE] = mProfileImageURL
        }

        if (binding.etName.text.toString() != mUserDetails.name) {
            userHashMap[Constants.NAME] = binding.etName.text.toString()
        }

        if (binding.etMobile.text.toString() != mUserDetails.mobile.toString()) {
            userHashMap[Constants.MOBILE] = binding.etMobile.text.toString().toLong()
        }

        FirestoreClass().updateUserProfileData(this@MyProfileActivity, userHashMap)

    }

    private fun uploadUserImage() {
        showProgressDialog(resources.getString(R.string.please_wait))

        if (mSelectedImageFileUri != null) {

            val sRef: StorageReference = FirebaseStorage.getInstance().reference.child(
                "USER_IMAGE" + System.currentTimeMillis() + "." + Constants.getFileExtension(
                    this@MyProfileActivity,
                    mSelectedImageFileUri
                )
            )

            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener { taskSnapshot ->
                Log.e(
                    "Firebase Image URL",
                    taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                )

                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                    Log.i(
                        "Downloadable Image URI", uri.toString()
                    )

                    mProfileImageURL = uri.toString()

                    updateUserProfileData()

                }.addOnFailureListener { exception ->
                    Toast.makeText(
                        this@MyProfileActivity,
                        exception.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    hideProgressDialog()
                }
            }

        }
    }

    fun profileUpdateSuccess() {
        hideProgressDialog()

        setResult(Activity.RESULT_OK)
        finish()
    }
}