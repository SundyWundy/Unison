package com.example.unison2

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Sign_Up : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private fun saveUserToDatabase(email: String){

        val re = Regex("[^A-Za-z0-9 ]")
        val uid = re.replace(email, "")
        val uid2 = FirebaseAuth.getInstance().uid ?: ""

        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val user = User (uid2, email, "https://firebasestorage.googleapis.com/v0/b/unison-cd1b4.appspot.com/o/images%2F8cbbb1c3-175d-4717-8758-6b1cc120ee6a?alt=media&token=a72e261a-20c6-43d7-bc9b-ddbec6b7b333",
            "", "", "", "", "", "","")
        ref.setValue(user)
                .addOnSuccessListener {
                   Log.d("SignUp", "saved user to firebase database")
                }
                .addOnFailureListener{
                    Log.d("SignUp", "failed to save into database, ${it.message}")
                }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign__up)

        val re = Regex("[^A-Za-z0-9 ]")
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        val e_mail : EditText = findViewById(R.id.email)
        val pass_word : EditText = findViewById(R.id.password)
        val pass_word_again : EditText = findViewById(R.id.password_again)
        val errormsg = findViewById<TextView>(R.id.errmsg)

        val signUpBttn = findViewById<Button>(R.id.sign_up)

        fun hideKeyboard(view: View) {
            view?.apply {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }

        e_mail.setOnFocusChangeListener(View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                hideKeyboard(v)
            }
        })
        pass_word.setOnFocusChangeListener(View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                hideKeyboard(v)
            }
        })
        pass_word_again.setOnFocusChangeListener(View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                hideKeyboard(v)
            }
        })

        //These prevent the program from crashing from passing null values to firebase
        signUpBttn.setOnClickListener {

            if(e_mail.text.isEmpty()){
                e_mail.setError("Must Enter an Email")
                return@setOnClickListener
            }
            if(pass_word.text.isEmpty()){
                pass_word.setError("Must Enter Password")
                return@setOnClickListener
            }
            if(pass_word_again.text.isEmpty()){
                pass_word_again.setError("Must Confirm Password")
                return@setOnClickListener
            }
            //check if both passwords match
            if(pass_word.text.toString() != pass_word_again.text.toString()){
                pass_word_again.setError("Passwords Don't Match")
                Toast.makeText(this, "passwords don't match", Toast.LENGTH_SHORT).show()

            }else { //if they match add, create a new user with the email and password
                auth.createUserWithEmailAndPassword(e_mail.text.toString(), pass_word.text.toString())
                        .addOnCompleteListener(this){task->

                            if (task.isSuccessful) {
                                errormsg.setText("Account Successfully Added")
                                val user = auth.currentUser

                                saveUserToDatabase(e_mail.text.toString())

                                val intent = Intent(this, MainActivity :: class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                                intent.putExtra("passemail",re.replace(e_mail.text.toString(),""))

                                startActivity(intent)

                            } else {
                                // If sign in fails, display a message to the user.
                                hideKeyboard(it)
                                errormsg.setText("Authentication failed, ${task.exception?.message}")
                            }
                        }
                Toast.makeText(this, "username = ${e_mail.text.toString()} password = ${pass_word.text.toString()}", Toast.LENGTH_SHORT).show()

            }
        }

    }
}
