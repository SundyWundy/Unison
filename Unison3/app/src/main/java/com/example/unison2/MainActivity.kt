package com.example.unison2

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout

import com.alamkanak.weekview.MonthLoader
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewEvent
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_page.*
import kotlinx.android.synthetic.main.single_click_popup.view.*
import java.io.IOException
import java.time.Year
import java.util.*
import java.util.List
import kotlin.random.Random



class MainActivity : AppCompatActivity(), WeekView.EventClickListener, MonthLoader.MonthChangeListener,
    WeekView.EventLongPressListener, NavigationView.OnNavigationItemSelectedListener {

    val PERMISSION_ID = 42
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
    var addressesSTRING = ""
    private var mWeekViewType = TYPE_WEEK_VIEW
    private var mWeekView: WeekView? = null
    internal var todaycheck = 0
    internal var emailer = ""

    internal var events: MutableList<WeekViewEvent> = ArrayList()


    lateinit var toolbar: Toolbar
    lateinit var drawerLayout: DrawerLayout
    lateinit var navView: NavigationView

    var personalevents = mutableListOf<String>()
    var group1events = mutableListOf<String>()
    var group2events = mutableListOf<String>()
    var groupcheck = 0
    val colorarray = arrayOf(R.color.redEvent, R.color.greenEvent, R.color.orangeEvent, R.color.blueEvent)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.navigationView)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, 0, 0
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)

        emailer=intent.getStringExtra("passemail")
        Log.d("checker1", emailer)

        leftSideBar ()

        val profile = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profilebutton)

        val ref = FirebaseDatabase.getInstance().getReference("/users/$emailer")

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                Log.d("msg0 main", "image url: " + user!!.profile_pic);

                if(user!!.profile_pic.length > 2){

                    Picasso.get().load(user!!.profile_pic).into(profile)

                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("msg0", databaseError.message) //Don't ignore errors!
            }
        }
        ref.addListenerForSingleValueEvent(valueEventListener)

        profile.setOnClickListener {
            val intent = Intent(this, ProfilePage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("passemail",emailer)
            startActivity(intent)
        }
        // Get a reference for the week view in the layout.
        mWeekView = findViewById(R.id.weekView) as WeekView

        sevenDayView()

        mWeekView!!.setOnEventClickListener(this)

        // The week view has infinite scrolling horizontally. We have to provide the events of a
        // month every time the month changes on the week view.
        mWeekView!!.setMonthChangeListener(this)

        // Set long press listener for events.
        mWeekView!!.eventLongPressListener = this

        GetandSet()

        //This listens for changes in the data of people within your groups and updates your calendar as a result
        val listenerRef = FirebaseDatabase.getInstance().getReference("users")
            .addValueEventListener(object: ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    val children = p0.children

                    val ref = FirebaseDatabase.getInstance().getReference("users/$emailer")
                    val valueEventListener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val user = dataSnapshot.getValue(User::class.java)

                            children.forEach{
                                if(user!!.group1members.contains(it.key.toString())){
                                    Log.d("msg22", it.child("events").value.toString())
                                    GetandSet()
                                }
                                if(user!!.group2members.contains(it.key.toString())){
                                    Log.d("msg22", it.child("events").value.toString())
                                    GetandSet()
                                }
                            }

                        }
                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.d("msg0", databaseError.message) //Don't ignore errors!
                        }
                    }
                    ref.addListenerForSingleValueEvent(valueEventListener)
                }
            })

        var floatit = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.floater)
        floatit!!.setOnClickListener(View.OnClickListener {

            val emptyevent = WeekViewEvent()

            addEventPopUp(emptyevent,"","",0,"","","")
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_today -> {
                mWeekView!!.goToToday()
                return true
            }
            R.id.chatid -> {
                Toast.makeText(this@MainActivity, "You Clicked Chat", Toast.LENGTH_SHORT).show()
            }
            R.id.leaveid -> {

                val dialogBuilder = AlertDialog.Builder(this)
                dialogBuilder.setView(R.layout.leave_group_popup)
                val alert = dialogBuilder.create()
                alert.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
                alert.show()

                val leaveButton = alert?.findViewById<Button>(R.id.confirmLeave)
                val cancelButton = alert?.findViewById<Button>(R.id.cancelLeave)
                val errormsg = alert?.findViewById<TextView>(R.id.informer3)
                val groupradio = alert?.findViewById<RadioGroup>(R.id.invitewindowradiogroup)
                var groupnum = 0

                groupradio?.setOnCheckedChangeListener { group, checkedId ->
                    if (R.id.group1 == checkedId) {
                        groupnum = 1
                    }
                    else if (R.id.group2 == checkedId){
                        groupnum = 2
                    }
                }
                val ref1 = FirebaseDatabase.getInstance().getReference("/users/$emailer")
                val valueEvent = object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {}
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val user = dataSnapshot.getValue(User::class.java)
                        var groupsList = listOf<String>(user!!.group1members, user!!.group2members)

                        leaveButton?.setOnClickListener(View.OnClickListener setOnClickListener@{

                            if (groupnum == 0) {
                                errormsg?.setText("You must choose a group.")
                                return@setOnClickListener
                            }
                            if(groupsList[groupnum-1].length < 2){
                                errormsg?.setText("This group is already empty.")
                                return@setOnClickListener
                            }
                            leaveGroup(groupnum)
                            Toast.makeText(this@MainActivity, "Groupnum is $groupnum Email is $emailer", Toast.LENGTH_SHORT).show()
                            alert.cancel()
                        })

                    }
                }
                ref1.addListenerForSingleValueEvent(valueEvent)

                cancelButton?.setOnClickListener(View.OnClickListener { alert.cancel() })

            }
            R.id.logoutid -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, Page::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

        }

        return super.onOptionsItemSelected(item)
    }

    override fun onMonthChange(newYear: Int, newMonth: Int): MutableList<WeekViewEvent>? {

    //these first few lines just fix an issue where the calendar didn't automatically open on today's date
        if (todaycheck == 0){
            mWeekView!!.goToToday();
        }
        todaycheck++


        val month = newMonth - 1
        return getEventsForMonth(newYear, month)
    }

    private fun getEventsForMonth(year: Int, month: Int): MutableList<WeekViewEvent>? {


        val tempList = ArrayList<WeekViewEvent>()
        for (weekViewEvent in events) {
            if (weekViewEvent.startTime.get(Calendar.MONTH) == month && weekViewEvent.startTime.get(Calendar.YEAR) == year
            ) {
                tempList.add(weekViewEvent)
            }

        }

        return tempList
    }
    private fun leftSideBar (){
        val radioGroup1 = findViewById<RadioGroup>(R.id.viewselectid)
        radioGroup1?.setOnCheckedChangeListener { group, checkedId ->
            if (R.id.dayviewid == checkedId) {
                mWeekViewType = TYPE_DAY_VIEW
                mWeekView!!.numberOfVisibleDays = 1

                // Lets change some dimensions to best fit the view.
                mWeekView!!.columnGap =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        8f,
                        resources.displayMetrics
                    ).toInt()
                mWeekView!!.textSize =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        12f,
                        resources.displayMetrics
                    ).toInt()
                mWeekView!!.eventTextSize =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        12f,
                        resources.displayMetrics
                    ).toInt()

            }
            else if (R.id.threedayid == checkedId){
                mWeekViewType = TYPE_THREE_DAY_VIEW
                mWeekView!!.numberOfVisibleDays = 3

                // Lets change some dimensions to best fit the view.
                mWeekView!!.columnGap =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        8f,
                        resources.displayMetrics
                    ).toInt()
                mWeekView!!.textSize =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        12f,
                        resources.displayMetrics
                    ).toInt()
                mWeekView!!.eventTextSize =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        12f,
                        resources.displayMetrics
                    ).toInt()
            }
            else if (R.id.sevendayid == checkedId){
                sevenDayView ()
            }
        }

        val radioGroup2 = findViewById<RadioGroup>(R.id.radioCals)
        radioGroup2?.check(R.id.radiopersonalCal )
        radioGroup2?.setOnCheckedChangeListener { group, checkedId ->
            if (R.id.radiopersonalCal == checkedId) {
                groupcheck = 0
                GetandSet()
            }
            else if (R.id.radiogroup1 == checkedId){
                groupcheck = 1
                GetandSet()

            }
            else if (R.id.radiogroup2 == checkedId){
                groupcheck = 2
                GetandSet()
            }
        }
        setRadio()

    }
    private fun getEventTitle(time: Calendar, time2: Calendar): String {
        return String.format(
            "%d:%02d,%d:%02d,%s/%d/%d",
            time.get(Calendar.HOUR_OF_DAY),
            time.get(Calendar.MINUTE),

            time2.get(Calendar.HOUR_OF_DAY),
            time2.get(Calendar.MINUTE),

            time.get(Calendar.MONTH) + 1,
            time.get(Calendar.DAY_OF_MONTH),
            time.get(Calendar.YEAR)
        )
    }

    override fun onEventClick(event: WeekViewEvent, eventRect: RectF) {

        var timestuff = getEventTitle(event.startTime, event.endTime)
        var timeList = timestuff.split(",").toMutableList()

        if(timeList[0].split(":")[0].toInt() > 12 ){

            timeList[0] = (timeList[0].split(":")[0].toInt() - 12).toString()+":"+timeList[0].split(":")[1]+" PM"

        }
        else{
            timeList[0] = (timeList[0].split(":")[0].toInt()).toString()+":"+timeList[0].split(":")[1]+" AM"
        }
        if(timeList[1].split(":")[0].toInt() > 12){

            timeList[1] = (timeList[1].split(":")[0].toInt() - 12).toString()+":"+timeList[1].split(":")[1]+" PM"

        }
        else{
            timeList[1] = (timeList[1].split(":")[0].toInt()).toString()+":"+timeList[1].split(":")[1]+" AM"
        }

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(R.layout.single_click_popup)
        val alert = dialogBuilder.create()
        alert.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        alert.show()

        val Tinfo = alert.findViewById<TextView>(R.id.titleinfo)
        val Linfo = alert.findViewById<TextView>(R.id.locationinfo)
        val timeInfo = alert.findViewById<TextView>(R.id.timeinfo)
        val editButton = alert.findViewById<Button>(R.id.editbttn)
        //added for view members
        val viewmembttn = alert.findViewById<Button>(R.id.viewmembers_bttn)

        viewmembttn?.setOnClickListener{
            val intent = Intent(this, addfriends::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("passemail",emailer)
            startActivity(intent)
        }



        Tinfo?.setText(event.name)


        val ref = FirebaseDatabase.getInstance().getReference("/users/$emailer")

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)

                //var personaleventss = user!!.events.replace("→","→*").split("*") as MutableList<String>

                var personaleventss = user!!.events
                var group1eventss = user!!.group1
                var group2eventss = user!!.group2

                var fireList = listOf<String>("events", "group1", "group2")
                var stringList = listOf<String>(personaleventss, group1eventss, group2eventss)

                var thelist = stringList[groupcheck].replace("→","→*").split("*") as MutableList<String>

                var y = 0
                while (y < thelist.size) {

                    if (event.id.toString() == thelist[y].dropLast(1)) {
                        break;
                    }
                    y += 11

                }
                addressesSTRING = thelist[y+2].dropLast(1)
                Linfo?.setText(Html.fromHtml("<p><u>${addressesSTRING}</u></p>"))
                Linfo?.setOnClickListener(View.OnClickListener {

                    var weGotPermission = checkPermissions()
                    Log.d("checker", "location is ${weGotPermission}")

                    if(weGotPermission == false){
                        requestPermissions()

                    }
                    else{
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_ID)


                    }
                })




            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("msg0", databaseError.message) //Don't ignore errors!
            }
        }
        ref.addListenerForSingleValueEvent(valueEventListener)


        timeInfo?.setText( "${timeList[2]} from ${timeList[0]} to ${timeList[1]}" )

        if (groupcheck != 0){
            editButton?.visibility = View.INVISIBLE
        }
        else {
            editButton?.setOnClickListener(View.OnClickListener {
                alert.cancel()
                addEventPopUp(
                    event,
                    event.name,
                    event.location,
                    event.color,
                    timeList[2],
                    timeList[0],
                    timeList[1]
                )
            })
        }
    }

    override fun onEventLongPress(event: WeekViewEvent, eventRect: RectF) {

        if(groupcheck == 0) {
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setView(R.layout.long_click_popup)
            val alert = dialogBuilder.create()
            alert.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
            alert.show()

            val okButton = alert.findViewById<Button>(R.id.okID)
            val cancelButton = alert.findViewById<Button>(R.id.cancelID)

            okButton?.setOnClickListener(View.OnClickListener {
                events.remove(event)
                mWeekView!!.notifyDatasetChanged()
                alert.cancel()
                editor(event, "", "delete")
                //deleteEvent(event)
            })
            cancelButton?.setOnClickListener(View.OnClickListener { alert.cancel() })
        }

        Toast.makeText(this@MainActivity, "Long pressed event: " + event.name, Toast.LENGTH_SHORT)
            .show()
    }

    override fun onPointerCaptureChanged(hasCapture: Boolean) {

    }
    private fun sevenDayView (){
        mWeekView!!.numberOfVisibleDays = 7

        // Lets change some dimensions to best fit the view.
        mWeekView!!.columnGap =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                2f,
                resources.displayMetrics
            ).toInt()
        mWeekView!!.textSize =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                10f,
                resources.displayMetrics
            ).toInt()
        mWeekView!!.eventTextSize =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                10f,
                resources.displayMetrics
            ).toInt()
    }
    private fun addEventPopUp(passedEvent: WeekViewEvent, etitle: String, elocation: String, ecolor: Int, edate: String, estart: String, eEnd: String){
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(R.layout.add_page)
        val alert = dialogBuilder.create()
        alert.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        alert.show()
        //This is stuff inside the pop up
        val insertEvent = alert.findViewById<EditText>(R.id.editevent)
        val insertLoca = alert.findViewById<EditText>(R.id.locationID)
        val starttime = alert.findViewById<Button>(R.id.startid)
        val endtime = alert.findViewById<Button>(R.id.endid)

        var SamPm = ""
        var EamPm = ""

        val mDisplayData = alert.findViewById<Button>(R.id.dateid)
        var mDataSetListener: DatePickerDialog.OnDateSetListener? = null

        var colorstr = 0

        val radioGroup = alert.findViewById<RadioGroup>(R.id.rg)
        radioGroup?.setOnCheckedChangeListener { group, checkedId ->
            if (R.id.redid == checkedId) colorstr = R.color.redEvent
            else if (R.id.blueid == checkedId) colorstr = R.color.blueEvent
            else if (R.id.greenid == checkedId) colorstr = R.color.greenEvent
            else if (R.id.orangeid == checkedId) colorstr = R.color.orangeEvent
        }
        mDisplayData?.setOnClickListener(View.OnClickListener {
            val cal = Calendar.getInstance()

            var year: Int
            var month:Int
            var day:Int
            var dialog: DatePickerDialog? = null

            year = cal.get(Calendar.YEAR)
            month = cal.get(Calendar.MONTH)
            day = cal.get(Calendar.DAY_OF_MONTH)

            dialog = DatePickerDialog(alert.getContext(),
                mDataSetListener, year, month, day)

            dialog.show()
        })
        mDataSetListener = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->

            val date = (month + 1).toString() + "/" + day + "/" + year

            if (mDisplayData != null) {
                mDisplayData.setText(date)
            }
        }
        starttime?.setOnClickListener(View.OnClickListener {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { timePicker, hourOfDay, pickedminutes ->
                var hourOfDay = hourOfDay
                //var amPm : String

                if (hourOfDay >= 12) {
                    SamPm = "PM"
                    if (hourOfDay > 12) {
                        hourOfDay = hourOfDay - 12
                    } else {
                        hourOfDay = 12
                    }

                } else {
                    if (hourOfDay == 0) {
                        hourOfDay = 12
                    }
                    SamPm = "AM"
                }
                if (starttime != null) {
                    starttime.setText(String.format("%2d:%02d", hourOfDay, pickedminutes) +" "+ SamPm)
                }
            }, currentHour, currentMinute, false)

            timePickerDialog.show()
        })
        endtime?.setOnClickListener(View.OnClickListener {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { timePicker, hourOfDay, minutes ->
                var hourOfDay = hourOfDay
                //var amPm : String

                if (hourOfDay >= 12) {
                    EamPm = "PM"
                    if (hourOfDay > 12) {
                        hourOfDay = hourOfDay - 12
                    } else {
                        hourOfDay = 12
                    }
                } else {
                    if (hourOfDay == 0) {
                        hourOfDay = 12
                    }
                    EamPm = "AM"
                }

                endtime.setText(String.format("%2d:%02d", hourOfDay, minutes) + " " + EamPm)
            }, currentHour, currentMinute, false)

            timePickerDialog.show()
        })
        val aabttn = alert.findViewById<Button>(R.id.addButtonid)
        //added for addfriendbtt
        val addfrbttn = alert.findViewById<Button>(R.id.addfriendbtt)

        val colorarray2 = arrayOf(getResources().getColor(R.color.redEvent), getResources().getColor(R.color.greenEvent),
            getResources().getColor(R.color.orangeEvent), getResources().getColor(R.color.blueEvent))
        val colorarray3 = arrayOf(R.id.redid, R.id.greenid, R.id.orangeid, R.id.blueid)

        if(ecolor != 0){
            insertEvent?.setText(etitle)
            insertLoca?.setText(elocation)
            if(colorarray2.indexOf(ecolor) != -1){
                radioGroup?.check(colorarray3[colorarray2.indexOf(ecolor)])
            }
            mDisplayData?.setText(edate)
            starttime?.setText(estart)
            endtime?.setText(eEnd)
            aabttn?.setText("Edit")

        }

    addfrbttn?.setOnClickListener{
        val intent = Intent(this, addfriends::class.java)
        startActivity(intent)
    }

        aabttn?.setOnClickListener{


            var day: Int
            var month: Int
            var year: Int
            var shour: Int
            var sminute: Int
            var ehour: Int
            var eminute: Int

            if(starttime?.text.toString() != "Start Time" && mDisplayData?.text.toString().length > 4 && endtime?.text.toString() != "End Time"){
                val uid = Random.nextLong(0,2147483647)
                var addevent = ""

                var date = mDisplayData?.text.toString().split("/")

                day = date[1].toInt()
                month = date[0].toInt()-1
                year = date[2].toInt()

                shour = starttime?.text.toString().split(":")[0].replace(" ","").toInt()
                sminute = starttime?.text.toString().split(":")[1].dropLast(3).toInt()
                if(SamPm == "PM"){shour += 12}

                ehour = endtime?.text.toString().split(":")[0].replace(" ","").toInt()
                eminute = endtime?.text.toString().split(":")[1].dropLast(3).toInt()
                if(EamPm == "PM"){ehour += 12}

                //Making sure start time is less than end time
                if(shour > ehour){
                    endtime?.setError("")
                    errorthing?.setText("End time must be greater than start time.")
                    return@setOnClickListener
                }else {
                    endtime?.setError(null)
                    errorthing?.setText("")
                }
                if(shour == ehour && sminute >= eminute){
                    endtime?.setError("")
                    errorthing?.setText("End time must be greater than start time.")
                    return@setOnClickListener
                }else {
                    endtime?.setError(null)
                    errorthing?.setText("")
                }
                personalevents.add(uid.toString()+"→")
                addevent += uid.toString()+"→"

                        //CHECKING FOR NULL VALUES THEN INSERTING VALUES INTO LIST
                if (insertEvent?.text.toString().length > 0) {
                    personalevents.add(insertEvent?.text.toString()+"→")
                    addevent += insertEvent?.text.toString()+"→"

                } else {
                    insertEvent?.setText("Untitled")
                    personalevents.add("Untitled"+"→")
                    addevent += "Untitled"+"→"

                }
                if (insertLoca?.text.toString().length > 0) {
                    personalevents.add(insertLoca?.text.toString()+"→")
                    addevent += insertLoca?.text.toString()+"→"

                } else {
                    insertLoca?.setText("")
                    personalevents.add(" "+"→")
                    addevent += " "+"→"

                }
                if (colorstr >= 2) {
                    personalevents.add(colorstr.toString()+"→")
                    if(ecolor != 0){
                        addevent +=  colorstr.toString()+"→"
                    }else{
                        addevent += R.color.myEvent.toString()+"→"
                    }
                } else {
                    colorstr = R.color.greyEvent
                    personalevents.add(colorstr.toString()+"→")
                    addevent += R.color.myEvent.toString()+"→"

                }

                    personalevents.add(
                        year.toString() + "→" + month.toString() + "→" + day.toString() + "→" +
                                shour.toString() + "→" + sminute.toString() + "→" + ehour.toString() + "→" + eminute.toString() + "→"
                    )

                    addevent += year.toString() + "→" + month.toString() + "→" + day.toString() + "→" +
                            shour.toString() + "→" + sminute.toString() + "→" + ehour.toString() + "→" + eminute.toString() + "→"

                    val startTime = Calendar.getInstance()
                    //year,month,day,hour,minute
                    startTime.set(year, month, day, shour, sminute)
                    val endTime = startTime.clone() as Calendar
                    endTime.set(year, month, day, ehour, eminute)



                if(ecolor == 0) {
                    val event = WeekViewEvent(
                        uid,
                        insertEvent?.text.toString(),
                        insertLoca?.text.toString(),
                        startTime,
                        endTime
                    )
                    event.color = getResources().getColor(colorstr)


                    events.add(event)

                    AddEventstoDatabase()

                    updateGroups(addevent)

                    mWeekView!!.notifyDatasetChanged()
                }
                else{

                    passedEvent.name = insertEvent?.text.toString()
                    passedEvent.location = insertLoca?.text.toString()
                    passedEvent.color = getResources().getColor(colorstr)
                    passedEvent.startTime = startTime
                    passedEvent.endTime = endTime

                    mWeekView!!.notifyDatasetChanged()
                    editor(passedEvent, addevent, "edit")
                }

                alert.cancel()


            }
            else {
                if (mDisplayData?.text.toString() == "Date") {
                    mDisplayData?.setError("Error")
                    return@setOnClickListener
                } else mDisplayData?.setError(null)

                if (starttime?.text.toString() == "Start Time") {
                    starttime?.setError("Error")
                    return@setOnClickListener
                }else starttime?.setError(null)

                if (endtime?.text.toString() == "End Time") {
                    endtime?.setError("Error")
                    return@setOnClickListener
                }else endtime?.setError(null)
            }

        }
    }
    fun AddEventstoDatabase(){
        val dataString = personalevents.joinToString(separator = "")

        val ref = FirebaseDatabase.getInstance().getReference("/users/$emailer/events")
        ref.setValue(dataString)
            .addOnSuccessListener {
                Log.d("datastring", "saved to firebase database")
            }
            .addOnFailureListener{
                Log.d("datastring", "failed to save into database, ${it.message}")
            }

    }
    private fun populate2(passedlist: kotlin.collections.List<String>) {


        var i = 0
        while (i < passedlist.size && i < passedlist.size-1) {

            val startTime = Calendar.getInstance()
            //year,month,day,hour,minute
            startTime.set(passedlist[i + 4].dropLast(1).toInt(), passedlist[i + 5].dropLast(1).toInt(),
                passedlist[i + 6].dropLast(1).toInt(), passedlist[i + 7].dropLast(1).toInt(), passedlist[i + 8].dropLast(1).toInt())
            val endTime = startTime.clone() as Calendar
            endTime.set(passedlist[i + 4].dropLast(1).toInt(), passedlist[i + 5].dropLast(1).toInt(),
                passedlist[i + 6].dropLast(1).toInt(), passedlist[i + 9].dropLast(1).toInt(), passedlist[i + 10].dropLast(1).toInt())
            val event = WeekViewEvent(passedlist[i].dropLast(1).toLong(), passedlist[i + 1].dropLast(1),
                "",startTime, endTime)
            event.color = getResources().getColor(passedlist[i + 3].dropLast(1).toInt())


            events.add(event)

            i += 11
        }
        mWeekView!!.notifyDatasetChanged()
    }
    fun GetandSet(){
        val ref1 = FirebaseDatabase.getInstance().getReference("/users/$emailer")

        val valueEvent = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                //Convert string to list, then populate the calendar with the events

                if(user!!.events.length > 1){

                    Log.d("checker01", groupcheck.toString())

                    personalevents = user!!.events.replace("→","→*").split("*") as MutableList<String>

                    events.clear()

                    populate2(personalevents) //this visualized the events

                }
                if(user!!.group1.length > 1 ){

                    group1events = user!!.group1.replace("→","→*").split("*") as MutableList<String>

                    if(groupcheck == 1) {

                        events.clear()

                        populate2(group1events) //this visualized the events
                    }

                }
                if(user!!.group2.length > 1 ){

                    group2events = user!!.group2.replace("→","→*").split("*") as MutableList<String>

                    if(groupcheck == 2) {

                        events.clear()

                        populate2(group2events) //this visualized the events
                    }

                }

            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("datastring", databaseError.message) //Don't ignore errors!
            }
        }
        ref1.addListenerForSingleValueEvent(valueEvent)

    }
    private fun editor (passedEvent: WeekViewEvent, addevent : String, editORdelete: String) {


        val ref1 = FirebaseDatabase.getInstance().getReference("/users/$emailer")

        val valueEvent = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                //Convert string to list, then populate the calendar with the events

                personalevents = user!!.events.replace("→","→*").split("*") as MutableList<String>
                group1events = user!!.group1.replace("→","→*").split("*") as MutableList<String>
                group2events = user!!.group2.replace("→","→*").split("*") as MutableList<String>

                var fireList = listOf<String>("events", "group1", "group2")
                var superList = listOf<MutableList<String>>(personalevents, group1events, group2events)

                var newvalues = mutableListOf<String>()

                if(addevent.length > 2) {
                    newvalues = addevent.replace("→", "→*").split("*") as MutableList<String>
                }
                //Loop Through superList and update event
                for (x in superList) {
                    var y = 0
                    while (y < x.size) {

                        if (passedEvent.id.toString() == x[y].dropLast(1)) {
                            break;
                        }
                        y += 11

                    }
                    for (i in y..y + 10) {
                        if(editORdelete == "edit") {
                            if (superList.indexOf(x) != 0) {

                                if (i % 11 != 3) {
                                    x[i] = newvalues[i % 11]
                                }
                            } else {
//                            Log.d("didItwork", "Original ${x[i]}")
                                x[i] = newvalues[i % 11]
                            }
                        }
                        else{
                            x[i] = ""
                        }
                    }
                    var newPersonal = x.joinToString(separator = "")
                    ref1.child(fireList[superList.indexOf(x)]).setValue(newPersonal)
                }

                //Check if group1/2members are not empty, if they are loop throught their group and update the value
                var listOfmembersLists = listOf<String>(user!!.group1members, user!!.group2members)

                for (x in listOfmembersLists) { //This loops twice to check group 1 and 2 sizes

                    if (x.length > 1) {
                        //convert group1members to list
                        var newMembersGroup =
                            ((user!!.group1members).split(" ") as MutableList<String>).dropLast(1)

                        //loop through list updating each members group 1
                        for (z in newMembersGroup) {
                            val reff = FirebaseDatabase.getInstance().getReference("/users/$z")
                            val valueEventListener = object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {

                                    val userr = dataSnapshot.getValue(User::class.java)
                                    var listOfgroupEvents = listOf<String>(userr!!.group1, userr!!.group2)

                                    var groupEvents = listOfgroupEvents[listOfmembersLists.indexOf(x)].replace("→","→*").split("*") as MutableList<String>

                                    var y = 0
                                    while (y < groupEvents.size) {

                                        if (passedEvent.id.toString() == groupEvents[y].dropLast(1)) {
                                            break;
                                        }
                                        y += 11

                                    }
                                    for (i in y..y + 10) {
                                        if(editORdelete == "edit") {
                                            if (i % 11 != 3) {
                                                groupEvents[i] =
                                                    newvalues[i % 11] //change this to personalevents/x
                                            }
                                        }
                                        else{
                                            groupEvents[i] = ""
                                        }

                                    }
                                    var newPersonal = groupEvents.joinToString(separator = "")
                                    reff.child(fireList[listOfmembersLists.indexOf(x)+1]).setValue(newPersonal)
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    Log.d("msg0", databaseError.message) //Don't ignore errors!
                                }
                            }
                            reff.addListenerForSingleValueEvent(valueEventListener)
                        }
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("datastring", databaseError.message) //Don't ignore errors!
            }
        }
        ref1.addListenerForSingleValueEvent(valueEvent)
    }
        private fun updateGroups(newevent: String){
        //get the value of group1 and group 2
        val ref1 = FirebaseDatabase.getInstance().getReference("/users/$emailer")

        var templist = newevent.replace("→", "→*").split("*") as MutableList<String>

        val valueEvent = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)

                ref1.child("group1").setValue(user!!.group1 + newevent)
                ref1.child("group2").setValue(user!!.group2 + newevent)
                if (user!!.group1members.length > 1){
                    //convert group1members to list
                    var newMembersGroup = ((user!!.group1members).split(" ") as MutableList<String>).dropLast(1)
                    Log.d("who4", newMembersGroup.size.toString())

                    //loop through list updating each members group 1
                    for(x in newMembersGroup){
                        val reff = FirebaseDatabase.getInstance().getReference("/users/$x")

                        val valueEventListener = object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {

                                val userr = dataSnapshot.getValue(User::class.java)


                                val theirMembers = userr!!.group1members.split(" ") as MutableList<String>
                                templist[3] = colorarray[theirMembers.indexOf(emailer)].toString() + "→"


                                var newgroup1 = userr!!.group1 + templist.joinToString(separator = "")
                                reff.child("group1").setValue(newgroup1)

                            }
                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.d("msg0", databaseError.message) //Don't ignore errors!
                            }
                        }
                        reff.addListenerForSingleValueEvent(valueEventListener)
                    }
                }
                if (user!!.group2members.length > 1){
                    //convert group1members to list
                    var newMembersGroup = ((user!!.group2members).split(" ") as MutableList<String>).dropLast(1)

                    //loop through list updating each members group 1
                    for(x in newMembersGroup){
                        val reff = FirebaseDatabase.getInstance().getReference("/users/$x")

                        val valueEventListener = object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {

                                val userr = dataSnapshot.getValue(User::class.java)

                                val theirMembers = userr!!.group2members.split(" ") as MutableList<String>
                                templist[3] = colorarray[theirMembers.indexOf(emailer)].toString() + "→"

                                var newgroup1 = userr!!.group2 + templist.joinToString(separator = "")
                                reff.child("group2").setValue(newgroup1)

                            }
                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.d("msg0", databaseError.message) //Don't ignore errors!
                            }
                        }
                        reff.addListenerForSingleValueEvent(valueEventListener)
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("datastring", databaseError.message) //Don't ignore errors!
            }
        }
        ref1.addListenerForSingleValueEvent(valueEvent)
        //check if there's anyon in the groups then update them too but with alt colors

    }
    fun setRadio(){
        val radioGroup2 = findViewById<RadioGroup>(R.id.invites)
        radioGroup2?.setOnCheckedChangeListener { group, checkedId ->
            if (R.id.radioRinvites == checkedId) {

                val intent = Intent(this, Invites::class.java)
                intent.putExtra("passemail",emailer)
                radioGroup2?.clearCheck()
                startActivity(intent)

            }
            if (R.id.radioSinvites == checkedId){

                val dialogBuilder2 = AlertDialog.Builder(this)
                dialogBuilder2.setView(R.layout.invite_popup)
                val alert2 = dialogBuilder2.create()
                alert2.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
                alert2.show()

                val errormsg = alert2?.findViewById<Button>(R.id.informer3)
                val emailtxtbox = alert2.findViewById<EditText>(R.id.invite_email)
                val groupradio = alert2?.findViewById<RadioGroup>(R.id.invitewindowradiogroup)
                var groupnum = ""

                groupradio?.setOnCheckedChangeListener { group, checkedId ->
                    if (R.id.group1 == checkedId) {
                        groupnum = "group1"
                    }
                    else if (R.id.group2 == checkedId){
                        groupnum = "group2"
                    }
                }
                //this is the send invite buttone
                alert2.findViewById<Button>(R.id.invite_send)?.setOnClickListener {
                    //prevents user from pressing send if they haven't filled everything out
                    if (emailtxtbox?.text.toString().length < 1) {
                        emailtxtbox?.setError("")
                        return@setOnClickListener
                    } else {
                        emailtxtbox?.setError(null)
                    }
                    if (groupnum.length == 0) {
                        errormsg?.setText("You must choose a group.")
                        return@setOnClickListener
                    } else {
                        errormsg?.setText("")
                    }

                    val re = Regex("[^A-Za-z0-9 ]")
                    val otheremail = re.replace(emailtxtbox?.text.toString(), "")



                    val ref0 = FirebaseDatabase.getInstance().getReference("/users/$otheremail")
                    val valueEvent0 = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val user = dataSnapshot.getValue(User::class.java)
                            if(user != null){
                                val ref1 = FirebaseDatabase.getInstance().getReference("/users/$emailer")
                                Log.d("msg22", "$otheremail to group number: $groupnum")

                                val valueEvent = object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        val user2 = dataSnapshot.getValue(User::class.java)

                                        if (groupnum == "group1") {
                                            if (user2!!.group1members.contains(otheremail) == false && otheremail != emailer) {

                                                var newMembersGroup = ((user2!!.group1members).split(" ") as MutableList<String>).dropLast(1)

                                                ///////////////////////////////////////CHECK THE LENGTH OF MemberGroup////////////////////////////////////
                                                Log.d("msg22", "group size is "+newMembersGroup.size.toString()+" colorarray size is ${colorarray.size}")

                                                if(newMembersGroup.size != colorarray.size) {

                                                    if(user2!!.group1members != "") {
                                                        for (x in newMembersGroup) {

                                                            val reff = FirebaseDatabase.getInstance().getReference("/users/$x")

                                                            val valueEventListener = object : ValueEventListener {
                                                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                                    val userr = dataSnapshot.getValue(User::class.java)
                                                                    var theirmembers = userr!!.group1members + otheremail + " "
                                                                    reff.child("group1members").setValue(theirmembers)

                                                                }

                                                                override fun onCancelled(databaseError: DatabaseError) {
                                                                    Log.d("msg0", databaseError.message) //Don't ignore errors!
                                                                }
                                                            }
                                                            reff.addListenerForSingleValueEvent(valueEventListener)
                                                        }
                                                    }
                                                    //update their group1members
                                                    var invited = user2!!.group1members + emailer + " "
                                                    ref0.child("group1members").setValue(invited)
                                                    //update your group1members
                                                    var yourmembers = user2!!.group1members + otheremail + " "
                                                    ref1.child("group1members").setValue(yourmembers)

           ///////////////////////////////////////////////////////////////////////////////////////
                                                    MergeEvents3(otheremail, emailer, groupnum)

                                                    //deals with exiting the pop up
                                                    alert2.dismiss()
                                                    radioGroup2?.setOnCheckedChangeListener(null);
                                                    radioGroup2?.clearCheck()
                                                    setRadio()
                                                }else{
                                                    errormsg?.setText("This group is full.")
                                                }
                                            } else {
                                                errormsg?.setText("This person is already in group 1.")
                                            }
                                        }
                                        if (groupnum == "group2") {
                                            if (user2!!.group2members.contains(otheremail) == false && otheremail != emailer) {

                                                var newMembersGroup = ((user2!!.group2members).split(" ") as MutableList<String>).dropLast(1)

                                                if(newMembersGroup.size != colorarray.size) {

                                                    if(user2!!.group2members != "") {

                                                        for (x in newMembersGroup) {

                                                            val reff = FirebaseDatabase.getInstance().getReference("/users/$x")

                                                            val valueEventListener = object : ValueEventListener {
                                                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                                    val userr = dataSnapshot.getValue(User::class.java)
                                                                    var theirmembers = userr!!.group2members + otheremail + " "
                                                                    reff.child("group2members").setValue(theirmembers)

                                                                }

                                                                override fun onCancelled(databaseError: DatabaseError) {
                                                                    Log.d("msg0", databaseError.message) //Don't ignore errors!
                                                                }
                                                            }
                                                            reff.addListenerForSingleValueEvent(valueEventListener)
                                                        }
                                                    }
                                                    //update their group1members
                                                    var invited = user2!!.group2members + emailer + " "
                                                    ref0.child("group2members").setValue(invited)
                                                    //update your group1members
                                                    var yourmembers = user2!!.group2members + otheremail + " "
                                                    ref1.child("group2members").setValue(yourmembers)

   ///////////////////////////////////////////////////////////////////////////////////////////////
                                                    MergeEvents3(otheremail, emailer, groupnum)

                                                    //deals with exiting the pop up
                                                    alert2.dismiss()
                                                    radioGroup2?.setOnCheckedChangeListener(null);
                                                    radioGroup2?.clearCheck()
                                                    setRadio()
                                                }else{
                                                    errormsg?.setText("This group is full.")
                                                }
                                            } else {
                                                errormsg?.setText("This person is already in group 2.")
                                            }
                                        }

                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {
                                        Log.d("datastring", databaseError.message) //Don't ignore errors!
                                    }
                                }
                                ref1.addListenerForSingleValueEvent(valueEvent)

                            }else{
                                errormsg?.setText("${emailtxtbox?.text.toString()} is not in the server.")

                            }
                        }
                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.d("datastring", databaseError.message) //Don't ignore errors!
                        }
                    }
                    ref0.addListenerForSingleValueEvent(valueEvent0)



                }
                alert2.setOnDismissListener {
                    radioGroup2?.setOnCheckedChangeListener(null);
                    radioGroup2?.clearCheck()
                    setRadio()
                }

            }
        }
    }
    private fun MergeEvents3(invitedemail : String, inviteremail : String, group: String){

        val ref = FirebaseDatabase.getInstance().getReference("/users/$inviteremail")
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val youuser = dataSnapshot.getValue(User::class.java)

                val ref2 = FirebaseDatabase.getInstance().getReference("/users/$invitedemail")
                val valueEventListener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val inviteduser = dataSnapshot.getValue(User::class.java)
                        var templist = inviteduser!!.events.replace("→", "→*").split("*") as MutableList<String>

                        if(group == "group1"){
                            var ourMembersGroup = ((youuser!!.group1members).split(" ") as MutableList<String>).dropLast(1)
                            val index = ourMembersGroup.indexOf(invitedemail)

                            var x = 3

                            while (x < templist.size && x < templist.size - 1) {
                                templist[x] = colorarray[index].toString() + "→"
                                x += 11
                            }
                            x = 3
                            val addedevents = templist.joinToString(separator = "")
                            val groupstring = youuser!!.group1 + addedevents
                            //This sets your group1 to equal what it was plus theirs events
                            ref.child("group1").setValue(groupstring)
                                .addOnSuccessListener {
                                    GetandSet()
                                }


                            //this should add the invited person's events to the group events of everyone else in the group
                            if (ourMembersGroup.size > 1) {

                                for (x in ourMembersGroup.dropLast(1)) {

                                    Log.d("who1", "member $x length is ${ourMembersGroup.size}")

                                    val othermember = FirebaseDatabase.getInstance().getReference("/users/$x")

                                    val valueEventListener = object : ValueEventListener {
                                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                            val otherusers = dataSnapshot.getValue(User::class.java)

                                            val groupstring = otherusers!!.group1 + addedevents
                                            othermember.child("group1").setValue(groupstring)


                                        }

                                        override fun onCancelled(databaseError: DatabaseError) {
                                            Log.d("msg0", databaseError.message) //Don't ignore errors!
                                        }
                                    }
                                    othermember.addListenerForSingleValueEvent(valueEventListener)

                                }

                            }

                            val allusers = arrayListOf<String>()

                            var theirMembersGroup = ((inviteduser!!.group1members).split(" ") as MutableList<String>).dropLast(1)

                            val ref = FirebaseDatabase.getInstance().getReference("users")
                            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {

                                    val children = dataSnapshot.children
                                    children.forEach {
                                        if (inviteduser!!.group1members.contains(it.key.toString())) {
                                            val user0 = it.getValue(User::class.java)
                                            Log.d("who4", "index of ${it.key} is ${theirMembersGroup.indexOf(it.key.toString())}")

                                            allusers.add(theirMembersGroup.indexOf(it.key.toString()).toString())
                                            allusers.add(user0!!.events)


                                        }
                                    }

                                    var addedevents = ""
                                    for (x in 1 until allusers.size step 2) {
                                        var y = 3
                                        val templist = allusers[x].replace("→", "→*").split("*") as MutableList<String>
                                        while (y < templist.size && y < templist.size - 1) {
                                            templist[y] = colorarray[allusers[x - 1].toInt()].toString() + "→"
                                            y += 11

                                        }
                                        addedevents = addedevents + templist.joinToString(separator = "")
                                    }
                                    Log.d("who4", addedevents)
                                    ref2.child("group1").setValue(inviteduser!!.group1 + addedevents)

                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    throw databaseError.toException()
                                }
                            })

                        }



                        if(group == "group2"){
                            var ourMembersGroup = ((youuser!!.group2members).split(" ") as MutableList<String>).dropLast(1)
                            val index = ourMembersGroup.indexOf(invitedemail)

                            var x = 3

                            Log.d("who1", "index at $index, color shoulc be ${colorarray[index]}")

                            while (x < templist.size && x < templist.size - 1) {
                                templist[x] = colorarray[index].toString() + "→"
                                x += 11
                            }
                            x = 3
                            val addedevents = templist.joinToString(separator = "")
                            val groupstring = youuser!!.group2 + addedevents
                            //This sets your group1 to equal what it was plus theirs events
                            ref.child("group2").setValue(groupstring)
                                .addOnSuccessListener {
                                    GetandSet()
                                }


                            //this should add the invited person's events to the group events of everyone else in the group
                            if (ourMembersGroup.size > 1) {

                                for (x in ourMembersGroup.dropLast(1)) {

                                    Log.d("who1", "member $x length is ${ourMembersGroup.size}")

                                    val othermember = FirebaseDatabase.getInstance().getReference("/users/$x")

                                    val valueEventListener = object : ValueEventListener {
                                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                            val otherusers = dataSnapshot.getValue(User::class.java)

                                            val groupstring = otherusers!!.group2 + addedevents
                                            othermember.child("group2").setValue(groupstring)


                                        }

                                        override fun onCancelled(databaseError: DatabaseError) {
                                            Log.d("msg0", databaseError.message) //Don't ignore errors!
                                        }
                                    }
                                    othermember.addListenerForSingleValueEvent(valueEventListener)

                                }

                            }


                            val allusers = arrayListOf<String>()


                            var theirMembersGroup = ((inviteduser!!.group2members).split(" ") as MutableList<String>).dropLast(1)

                            val ref = FirebaseDatabase.getInstance().getReference("users")
                            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {

                                    val children = dataSnapshot.children
                                    children.forEach {
                                        if (inviteduser!!.group2members.contains(it.key.toString())) {
                                            val user0 = it.getValue(User::class.java)
                                            Log.d("who4", "index of ${it.key} is ${theirMembersGroup.indexOf(it.key.toString())}")

                                            allusers.add(theirMembersGroup.indexOf(it.key.toString()).toString())
                                            allusers.add(user0!!.events)


                                        }
                                    }

                                    var addedevents = ""
                                    for (x in 1 until allusers.size step 2) {
                                        var y = 3
                                        val templist = allusers[x].replace("→", "→*").split("*") as MutableList<String>
                                        while (y < templist.size && y < templist.size - 1) {
                                            templist[y] = colorarray[allusers[x - 1].toInt()].toString() + "→"
                                            y += 11

                                        }
                                        addedevents = addedevents + templist.joinToString(separator = "")
                                    }
                                    Log.d("who4", addedevents)
                                    ref2.child("group2").setValue(inviteduser!!.group2 + addedevents)

                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    throw databaseError.toException()
                                }
                            })

                        }



                    }
                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.d("msg0", databaseError.message) //Don't ignore errors!
                    }
                }
                ref2.addListenerForSingleValueEvent(valueEventListener)

            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("msg0", databaseError.message) //Don't ignore errors!
            }
        }
        ref.addListenerForSingleValueEvent(valueEventListener)
    }
    private fun leaveGroup (groupnum : Int){

        val ref1 = FirebaseDatabase.getInstance().getReference("/users/$emailer")

        val valueEvent = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)

                var listofGroupmembers = listOf<String>(user!!.group1members, user!!.group2members)
                var fireList = listOf<String>("group1", "group2", "group1members", "group2members")

                var MembersGroup = ((listofGroupmembers[groupnum-1]).split(" ") as MutableList<String>).dropLast(1)

                //This sets their group members to empty
                ref1.child(fireList[groupnum+1]).setValue("")

                //Reset their group1/2 events
                var tempList = user!!.events.replace("→","→*").split("*") as MutableList<String>
                var y = 3
                while (y < tempList.size && y < tempList.size-1) {

                    tempList[y] = R.color.myEvent.toString()+"→"
                    y += 11

                }
                var tempString = tempList.joinToString(separator = "")
                ref1.child(fireList[groupnum-1]).setValue(tempString)
                Log.d("safetyTest1", tempString)

                //Remove name from everyone elses groupmembers
                for(x in MembersGroup){
                    val reff = FirebaseDatabase.getInstance().getReference("/users/$x")

                    val valueEventListener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {

                            val userr = dataSnapshot.getValue(User::class.java)

                            var tempListofGroupmembers = listOf<String>(userr!!.group1members, userr!!.group2members)

                            var theirMembers = tempListofGroupmembers[groupnum-1].replace("$emailer ", "")

                            Log.d("safetyTest2", "${fireList[groupnum+1]} members are $theirMembers")
                            reff.child(fireList[groupnum+1]).setValue("")

                            var tempEvents = userr!!.events.replace("→","→*").split("*") as MutableList<String>

                            //Reset their group1/2 events
                            var y = 3
                            while (y < tempEvents.size && y < tempEvents.size-1) {

                                tempEvents[y] = R.color.myEvent.toString()+"→"
                                y += 11

                            }
                            var tempString = tempEvents.joinToString(separator = "")
                            reff.child(fireList[groupnum-1]).setValue(tempString)
                            Log.d("safetyTest2or3", "${fireList[groupnum-1]}'s group is $tempString")
                        }
                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.d("msg0", databaseError.message) //Don't ignore errors!
                        }
                    }
                    reff.addListenerForSingleValueEvent(valueEventListener)
                }
                //Re-murge the remaining group members
//                for(x in 1..MembersGroup.size-1){
//                    //Log.d("safetyTest4444", "Sending to Merge(${MembersGroup[0]}, ${MembersGroup[x]}, ${fireList[groupnum-1]})")
//                    MergeEvents3(MembersGroup[x], MembersGroup[0], fireList[groupnum-1])
//                }

            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("datastring", databaseError.message) //Don't ignore errors!
            }
        }
        ref1.addListenerForSingleValueEvent(valueEvent)

    }
    companion object {

        private val TYPE_DAY_VIEW = 1
        private val TYPE_THREE_DAY_VIEW = 2
        private val TYPE_WEEK_VIEW = 3
    }
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location : Location? ->

                        var myAddrInfo = location.toString().split(" ")
                        var googleSTR = ""

                        Toast.makeText(this, "location is ${myAddrInfo[1]}", Toast.LENGTH_LONG).show()
                        Log.d("checker", "location is ${myAddrInfo}")

                        //myAddrInfo[1] is you lat/long
                        //Gets number of addresses
                        var addressList = addressesSTRING.split("<br/>").dropLast(1)
                        Log.d("checker", "number of addresses = ${addressList.size}")

                        try {
                            if (addressList.size == 1) {

                                var otherAddr =
                                    getLocationFromAddress(this, addressList[0]).toString()
                                        .substring(10).dropLast(1).split(",")
                                Log.d("checker", "location is ${otherAddr[0]} ${otherAddr[1]}")

                                googleSTR = "https://www.google.com/maps/dir/?api=1&" +
                                        "origin=${myAddrInfo[1]}&destination=${otherAddr[0]},${otherAddr[1]}&travelmode=driving"
                            } else {

                                var shortestDist = 234342342345.9F
                                var shortestLatLng = mutableListOf<String>("", "")

                                var nextAddr = myAddrInfo[1].split(",")
                                var latlngList = mutableListOf<String>()
                                var tempList = mutableListOf<String>()

                                for (x in addressList) {

                                    var latlong =
                                        getLocationFromAddress(this, x).toString().substring(10)
                                            .dropLast(1).split(",")

                                    latlngList.add(latlong[0])
                                    latlngList.add(latlong[1])

                                }
                                Log.d("checker", "list of lat longs is = ${latlngList}")

                                var templatlng = latlngList

                                while (templatlng.size != 0) {

                                    for (x in 0..templatlng.size - 1 step 2) {

                                        val loc1 = Location("")
                                        loc1.latitude = nextAddr[0].toDouble()
                                        loc1.longitude = nextAddr[1].toDouble()

                                        val loc2 = Location("")
                                        loc2.latitude = templatlng[x].toDouble()
                                        loc2.longitude = templatlng[x + 1].toDouble()

                                        var distance = loc1.distanceTo(loc2)

                                        if (distance < shortestDist) {
                                            shortestDist = distance
                                            shortestLatLng[0] = templatlng[x]
                                            shortestLatLng[1] = templatlng[x + 1]
                                        }

                                        if (x == templatlng.size - 2) {
                                            shortestDist = 234342342345.9F
                                            //nextAddr = shortestLatLng
                                            tempList.add(shortestLatLng[0])
                                            tempList.add(shortestLatLng[1])
                                            templatlng.removeAt(templatlng.indexOf(shortestLatLng[0]))
                                            templatlng.removeAt(templatlng.indexOf(shortestLatLng[1]))
                                        }
                                    }
                                }
                                Log.d("checker", "our sorted list is = ${tempList}")

                                var temp2 = tempList.dropLast(2)
                                var middleSTR = ""

                                for (x in 1..temp2.size) {
                                    middleSTR += temp2[x - 1]
                                    if (x % 2 != 0) {
                                        middleSTR += ","
                                    }
                                    if (x % 2 == 0 && x < temp2.size) {
                                        middleSTR += "|"
                                    }
                                }

                                googleSTR = "https://www.google.com/maps/dir/?api=1&" +
                                        "origin=${myAddrInfo[1]}&destination=${tempList[tempList.size - 2]},${tempList[tempList.size - 1]}" +
                                        "&waypoints=$middleSTR&travelmode=driving"

                            }
                        }
                        catch (ex: Exception) {
                            val dialogBuilder = AlertDialog.Builder(this)

                            // set message of alert dialog
                            dialogBuilder.setMessage("One or more of your addresses are incorrect.")
                                // if the dialog is cancelable
                                .setCancelable(true)

                            val alert = dialogBuilder.create()
                            alert.setTitle("Error")
                            alert.show()

                            return@addOnSuccessListener
                        }
                        val gmmIntentUri =
                            Uri.parse("$googleSTR")

                        val intent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        intent.setPackage("com.google.android.apps.maps")
                        try {
                            startActivity(intent)
                        } catch (ex: ActivityNotFoundException) {
                            try {
                                val unrestrictedIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                startActivity(unrestrictedIntent)
                            } catch (innerEx: ActivityNotFoundException) {
                                Toast.makeText(this, "Please install a maps application", Toast.LENGTH_LONG)
                                    .show()
                            }

                        }
                    }
            }
            else{
                Toast.makeText(this, "You must turn on location for this feature", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getLocationFromAddress(context : Context, strAddress: String): LatLng? {

        val coder = Geocoder(context)
        val address: kotlin.collections.List<Address>?
        var p1: LatLng? = null

        try {
            // May throw an IOException
            address = coder.getFromLocationName(strAddress, 5)
            if (address == null) {
                return null
            }

            try {
                val location = address[0]
                p1 = LatLng(location.latitude, location.longitude)
            } catch (ex: Exception) {
                Log.d("checker", "one of more addresses are incorrect.")

            }

        } catch (ex: IOException) {

            ex.printStackTrace()
        }


        return p1
    }
}
