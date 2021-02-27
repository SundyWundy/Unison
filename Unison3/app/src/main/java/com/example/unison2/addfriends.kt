package com.example.unison2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.renderscript.Sampler
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_addfriends.*
import kotlinx.android.synthetic.main.user_row_friends.view.*

class addfriends : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addfriends)

//        val adapter = GroupAdapter<ViewHolder>()

//        adapter.add(UserItem())
//        adapter.add(UserItem())
//        adapter.add(UserItem())
 //       friendrecyclerview.adapter = adapter

        fetchUsers()
    }
    private fun fetchUsers(){
        val emailer=intent.getStringExtra("passemail")

        val ref = FirebaseDatabase.getInstance().getReference("userfriend")
        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                val adapter = GroupAdapter<ViewHolder>()
    p0.children.forEach{
        Log.d("Friend", it.toString())
        val user = it.getValue(User::class.java)
        if (user != null) {
            adapter.add(UserItem(user))
        }
    }
                friendrecyclerview.adapter = adapter
            }

            override fun onCancelled(p0: DatabaseError) {
            }
        })
    }
}

class UserItem(val user: User): Item<ViewHolder>(){
    override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.Friendname.text = user.name

    Picasso.get().load(user.profile_pic).into(viewHolder.itemView.imageViewfriend)
    }
    override fun getLayout():Int{
    return R.layout.user_row_friends
    }
}