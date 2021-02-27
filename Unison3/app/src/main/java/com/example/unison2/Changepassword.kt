package com.example.unison2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_changepassword.*


class Changepassword : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changepassword)

        val emailer=intent.getStringExtra("passemail")
        auth = FirebaseAuth.getInstance()

        button_back.setOnClickListener{
            val intent = Intent(this, ProfilePage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("passemail",emailer)
            startActivity(intent)
        }

        button_changepass.setOnClickListener{
            changePassword()
        }



    }

    private fun changePassword(){
        if(editText_currentpass.text.isNotEmpty() && editText_newpass.text.isNotEmpty() && editText_confirmpass.text.isNotEmpty()){

            if(editText_newpass.text.toString().equals(editText_confirmpass.text.toString())){
                val user = auth.currentUser
                if(user!=null && user.email != null){
                    // Get auth credentials from the user for re-authentication. The example below shows
                    // email and password credentials but there are multiple possible providers,
                    // such as GoogleAuthProvider or FacebookAuthProvider.
                    val credential = EmailAuthProvider
                            .getCredential(user.email!!, editText_currentpass.text.toString())

                    // Prompt the user to re-provide their sign-in credentials
                    user?.reauthenticate(credential)
                            ?.addOnCompleteListener {
                                if(it.isSuccessful){
                                    //Toast.makeText(this,"Re-Authentication success.",Toast.LENGTH_SHORT).show()
                                    user?.updatePassword(editText_newpass.text.toString())
                                            ?.addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    Toast.makeText(this,"Password changed successfully.",Toast.LENGTH_SHORT).show()
                                                    auth.signOut()
                                                    startActivity(Intent(this, Page::class.java))
                                                    finish()

                                                }
                                            }
                                } else{
                                    Toast.makeText(this,"Password doesn't match.",Toast.LENGTH_SHORT).show()
                                }
                            }
                } else{
                    startActivity(Intent(this, Page::class.java))
                    finish()
                }
            } else{
                Toast.makeText(this,"Password doesn't match",Toast.LENGTH_SHORT).show()
            }
        } else{
            Toast.makeText(this, "Please enter all the fields", Toast.LENGTH_SHORT).show()
        }
    }
}
