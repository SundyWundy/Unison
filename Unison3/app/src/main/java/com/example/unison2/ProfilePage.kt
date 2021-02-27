package com.example.unison2

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.*
import com.google.android.gms.common.util.IOUtils.toByteArray
import android.graphics.Bitmap
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.widget.Button
import kotlinx.android.synthetic.main.profile_page.*
import java.io.ByteArrayOutputStream


class ProfilePage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var profilepic: de.hdodenhof.circleimageview.CircleImageView
    private lateinit var profileURi: Uri
    private lateinit var emailz: String




    private fun saveUserToDatabase(profilepicURL: String, emailer:String){

        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$emailer/profile_pic")
        ref.setValue(profilepicURL)
                .addOnSuccessListener {
                    Log.d("msg0", "saved pic to firebase database, $ref")

                }
                .addOnFailureListener{
                    Log.d("msg0", "failed to save pic into database, ${it.message}")
                }
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        startActivityForResult(intent, 10)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val emailer=intent.getStringExtra("passemail")

        if (requestCode == 10 && resultCode == Activity.RESULT_OK && data != null) {
            profileURi = data.data!!
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, profileURi)


            Log.d("msg0 profileuri", profileURi.toString());

            profilepic.setImageBitmap(bitmap)

            uploadImagetoFirebase(emailer);


        }
    }

    private fun uploadImagetoFirebase(emailer:String){

        if(profileURi == null) return
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")
        ref.putFile(profileURi!!)
                .addOnSuccessListener {
                    Toast.makeText(this, "Successfully uploaded image ${it.metadata?.path}", Toast.LENGTH_SHORT).show()
                    Log.d("msg0 upload", "image url: " + it.metadata?.path);


                    ref.downloadUrl.addOnSuccessListener {
                        Log.d("msg1 upload", "image url: " + it);

                        Toast.makeText(this, "File Location $it", Toast.LENGTH_SHORT).show()
                        saveUserToDatabase(it.toString(),emailer)
                    }
                }
    }


    override fun onBackPressed() {

        val intent = Intent(this, MainActivity :: class.java)
        Log.d("checker2", emailz)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("passemail",emailz)


        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_page)

        val emailer=intent.getStringExtra("passemail")
       // val greeting = findViewById<Button>(R.id.greetings)
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        emailz = emailer

//        profile_editprofile.setOnClickListener{
//            val intent = Intent(this, EditProfile::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
//            intent.putExtra("passemail",emailer)
//            startActivity(intent)
//        }


        profile_signout.setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, Page::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        profile_changepassword.setOnClickListener{
            val intent = Intent(this, Changepassword::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("passemail",emailer)
            startActivity(intent)
        }

        //Create Image View and set it to call upload when clicked
        profilepic = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.img_profile)

        val ref = FirebaseDatabase.getInstance().getReference("/users/$emailer")

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                Log.d("msg0 profile", "image url: " + user!!.profile_pic);

                user?.let{
                    viewname.text = it.name
                    viewemail.text = it.email
                }

                if(user!!.profile_pic.length > 2){
                    Picasso.get().load(user!!.profile_pic).into(profilepic)
                }
              //  greeting.setText("Hello ${user!!.email}")
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("msg0", databaseError.message) //Don't ignore errors!
            }
        }
        ref.addListenerForSingleValueEvent(valueEventListener)

        profilepic.setOnClickListener(View.OnClickListener { chooseImage() })



    }

}
