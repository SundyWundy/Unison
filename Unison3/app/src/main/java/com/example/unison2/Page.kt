package com.example.unison2

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.util.Log
import android.view.MotionEvent
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*


class Page : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    fun hideKeyboard(view: View) {
        view?.apply {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        val re = Regex("[^A-Za-z0-9 ]")


        val user_name : EditText = findViewById(R.id.username)
        val pass_word : EditText = findViewById(R.id.password)
        val buttonSignIn : Button = findViewById(R.id.sign_in)
        val errormsg : TextView = findViewById(R.id.errmsg)

        val persist_password : CheckBox = findViewById(R.id.persist_password)
        val auto_sign_in : CheckBox = findViewById(R.id.auto_sign_in)

        val prefs = getSharedPreferences("config", MODE_PRIVATE)
        val editor = prefs.edit()

        val username = prefs.getString("Username", "")
        val password = prefs.getString("Password", "")


        buttonSignIn.setOnClickListener{

            if(user_name.text.isEmpty()){
                persist_password.isChecked = true
                user_name.setError("Must Enter Username")
                return@setOnClickListener
            }
            if(pass_word.text.isEmpty()){
                pass_word.setError("Must Enter Password")
                return@setOnClickListener
            }
            auth.signInWithEmailAndPassword(user_name.text.toString(), pass_word.text.toString())
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {

                            if (persist_password.isChecked)
                            {
                                editor.putString("Username", user_name.text.toString())
                                editor.putString("Password", pass_word.text.toString())
                            }
                            else
                            {
                                editor.putString("Username", "")
                                editor.putString("Password", "")
                            }

                            if (auto_sign_in.isChecked)
                            {
                                editor.putBoolean("Auto_Sign_In", true)
                            }
                            else
                            {
                                editor.putBoolean("Auto_Sign_In", false)
                            }

                            editor.commit()

                            // Sign in success, update UI with the signed-in user's information
                            val intent = Intent(this, MainActivity :: class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.putExtra("passemail",re.replace(user_name.text.toString(),""))
                            startActivity(intent)
                        } else {
                            // If sign in fails, display a message to the user.

                            //TO DO: hide keyboard here so they can see the error msg
                            //TO DO: remove spaces from username, seems to be a common error
                            hideKeyboard(it)
                            errormsg.setText("Sign in failed, ${task.exception?.message}")
                        }
                    }
        }


        user_name.setOnFocusChangeListener(View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                hideKeyboard(v)
            }
        })
        pass_word.setOnFocusChangeListener(View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                hideKeyboard(v)
            }
        })

        // if user presses enter key, it will click on the sign-in button
        pass_word.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                //Perform Code
                val inputManager:InputMethodManager =getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.hideSoftInputFromWindow(currentFocus?.windowToken, InputMethodManager.SHOW_FORCED)
                return@OnKeyListener true
            }
            false
        })
        //////////////////

        val buttonSignUp : Button = findViewById(R.id.sign_up)
        buttonSignUp.setOnClickListener{
            val intent = Intent(this, Sign_Up :: class.java)
            startActivity(intent)
        }

        val forgotPassword: Button = findViewById(R.id.forgot)
        forgotPassword.setOnClickListener{
            val intent = Intent(this, Forgot_Password :: class.java)
            startActivity(intent)
        }
        val autosignin = prefs.getBoolean("Auto_Sign_In", false)

        if (autosignin)
        {
            auto_sign_in.isChecked = true
            buttonSignIn.performClick()
        }
    }
}
