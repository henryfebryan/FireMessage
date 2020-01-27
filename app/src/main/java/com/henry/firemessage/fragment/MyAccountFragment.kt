package com.henry.firemessage.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.firebase.ui.auth.AuthUI
import com.henry.firemessage.R
import com.henry.firemessage.SignInActivity
import com.henry.firemessage.util.FirestoreUtil
import com.henry.firemessage.util.StorageUtil
import kotlinx.android.synthetic.main.fragment_my_account.*
import kotlinx.android.synthetic.main.fragment_my_account.view.*
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask
import org.jetbrains.anko.toast
import java.io.ByteArrayOutputStream
import java.util.*

class MyAccountFragment : Fragment() {

    private val RC_SELECT_IMAGE = 2
    private lateinit var selectedImageBytes: ByteArray
    private var pictureJustChanged = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_account, container, false)

        view.apply {
            imageView_profile_picture.setOnClickListener {
                val intent = Intent().apply {
                    type = "image/*"
                    action = Intent.ACTION_GET_CONTENT
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
                }

                startActivityForResult(
                    Intent.createChooser(intent, "Select Image"),
                    RC_SELECT_IMAGE
                )
            }

            btn_save.setOnClickListener {
                if (::selectedImageBytes.isInitialized) {
                    StorageUtil.uploadProfilePhoto(selectedImageBytes) { imagePath ->
                        FirestoreUtil.updateCurrentUser(
                            editText_name.text.toString(),
                            editText_bio.text.toString(),
                            imagePath
                        )
                    }
                } else {
                    FirestoreUtil.updateCurrentUser(
                        editText_name.text.toString(),
                        editText_bio.text.toString(),
                        null
                    )
                }
                activity?.toast("Saving")
            }

            btn_sign_out.setOnClickListener {
                AuthUI.getInstance()
                    .signOut(this@MyAccountFragment.context!!)
                    .addOnCompleteListener {
                        startActivity(activity?.intentFor<SignInActivity>()?.newTask()?.clearTask())
                    }
            }
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SELECT_IMAGE && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val selectedImagePath = data.data

            val selectedImageBmp =
                MediaStore.Images.Media.getBitmap(activity?.contentResolver, selectedImagePath)

            val outputStream = ByteArrayOutputStream()
            selectedImageBmp.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)

            selectedImageBytes = outputStream.toByteArray()

            Glide.with(this)
                .load(selectedImageBytes)
                .into(imageView_profile_picture)

            pictureJustChanged = true
        }
    }

    override fun onStart() {
        super.onStart()
        FirestoreUtil.getCurrentUser { user ->
            if (this@MyAccountFragment.isVisible) {
                editText_name.setText(user.name)
                editText_bio.setText(user.bio)
                if (!pictureJustChanged && user.profilePicturePath != null) {
                    Glide.with(this)
                        .load(StorageUtil.pathToReference(user.profilePicturePath))
                        .placeholder(R.drawable.ic_account_circle_black_24dp)
                        .into(imageView_profile_picture)
                }
            }
        }
    }
}
