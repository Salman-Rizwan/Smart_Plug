package lk.salman.smart_plug

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.database.*
import lk.salman.smart_plug.databinding.ActivityHomeBinding
import org.json.JSONObject
import java.net.URL
import java.util.*
import kotlin.math.roundToInt


class Home : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var database: DatabaseReference
    val city: String = "Colombo,lk"
    val api: String = "c4953fb71ad8c0597628bb7dd4b0e146"
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    // Declare bottomSheetView as a member variable
    private lateinit var bottomSheetView: View
    private var selectedStartYear = 0
    private var selectedStartMonth = 0
    private var selectedStartDay = 0
    private var selectedEndYear = 0
    private var selectedEndMonth = 0
    private var selectedEndDay = 0
    private var selectedStartHour = 0
    private var selectedStartMinute = 0
    private var selectedEndHour = 0
    private var selectedEndMinute = 0

    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //hide app name displaying
        supportActionBar?.hide()


        //open bottom sheet
        val start1 = findViewById<CardView>(R.id.schedule)
        start1.setOnClickListener {
            showCreateScheduleDialog()
            showBottomSheet()
        }

        //go to detailed window
        val start = findViewById<CardView>(R.id.energy)
        start.setOnClickListener {
            val intent = Intent(this, Detailed_window::class.java)
            startActivity(intent)
        }


        //toggle
        val toggle_btn = findViewById<SwitchMaterial>(R.id.on_off)
        toggle_btn.setOnCheckedChangeListener { buttonView, isChecked ->
            val value = if (isChecked) 1 else 0
            controlBulb(value)
        }

        //read current humid data function
        readData()
        //weather data function
        GetWeather().execute()

    }

    //bulb controlling
    private fun controlBulb(value: Int) {
        //main node
        database = FirebaseDatabase.getInstance().getReference("HOUSES")
        //database path
        val adaptor1Ref = database.child("HOUSES_1").child("ADAPTORS").child("ADAPTOR_1")
        //string where the value need to change
        adaptor1Ref.child("OUTPUT_STATUS").setValue(value)
            .addOnSuccessListener {
                if (value == 1) {
                    Toast.makeText(this@Home, "Bulb is On", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@Home, "Bulb is Off", Toast.LENGTH_SHORT).show()

                }
            }
            .addOnFailureListener {
                Toast.makeText(this@Home, "Failed", Toast.LENGTH_LONG).show()
            }
    }


    //realtime read data power/humidity
    private fun readData() {
        val adapter1Ref =
            FirebaseDatabase.getInstance().getReference("HOUSES/HOUSES_1/ADAPTORS/ADAPTOR_1")

        adapter1Ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val humidity =
                        snapshot.child("HUMIDITY/CURRENT").getValue(Float::class.java)?.toInt()
                    val current = snapshot.child("POWER/WATTS").getValue(Float::class.java)?.toInt()

                    binding.txtHumid.text = humidity?.let { "$it g.m-3" } ?: "Data not found"
                    binding.txtEnergy.text = current?.let { "$it V" } ?: "Data not found"

                    Toast.makeText(
                        this@Home,
                        "Successfully read humidity and current",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    binding.txtHumid.text = "Data not found"
                    binding.txtEnergy.text = "Data not found"
                    Toast.makeText(this@Home, "Data not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Home, "Database reference failed", Toast.LENGTH_LONG).show()
            }
        })
    }


    //Open-weather retrieve data
    inner class GetWeather() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg p0: String): String? {
            var response: String?
            try {
                response =
                    URL("https://api.openweathermap.org/data/2.5/weather?q=${city}&units=metric&appid=${api}").readText(
                        Charsets.UTF_8
                    )
            } catch (e: Exception) {
                response = null
            }
            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {
                val jsonObj = JSONObject(result)
                val main = jsonObj.getJSONObject("main")
//                val sys = jsonObj.getJSONObject("sys")
//                val wind = jsonObj.getJSONObject("wind")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

                val temp = main.getDouble("temp").roundToInt()
                val mainDesc = weather.getString("main")
                val description = weather.getString("description")

                findViewById<TextView>(R.id.txt_celcius).text = "${temp}°C"
                findViewById<TextView>(R.id.txt_main).text = mainDesc
                findViewById<TextView>(R.id.txt_desc).text = description
            } catch (e: Exception) {
                findViewById<TextView>(R.id.errortext).visibility = View.VISIBLE
            }
        }
    }


    //bottom sheet function
    private fun showBottomSheet() {
        bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()

        //Date selection
        val start_date = bottomSheetView.findViewById<ImageButton>(R.id.start_date)
        start_date.setOnClickListener {
            showDatePickerDialog(true)
        }

        //End date selection
        val end_date = bottomSheetView.findViewById<ImageButton>(R.id.end_date)
        end_date.setOnClickListener {
            showDatePickerDialog(false)
        }

        // Start time selection
        val start_time = bottomSheetView.findViewById<ImageButton>(R.id.start_time)
        start_time.setOnClickListener {
            showTimePickerDialog(true)
        }

        // End time selection
        val end_time = bottomSheetView.findViewById<ImageButton>(R.id.end_time)
        end_time.setOnClickListener {
            showTimePickerDialog(false)
        }

        // Edit button
        val editButton = bottomSheetView.findViewById<Button>(R.id.edit_btn)
        editButton.visibility = View.GONE

        val scheduleButton = bottomSheetView.findViewById<Button>(R.id.schedule_btn)
        scheduleButton.setOnClickListener {

        }
    }




    //check and save end and start dates/ time
    private fun showDatePickerDialog(isStartDate: Boolean) {
        val datePicker = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                val selectedDate = "$dayOfMonth-${monthOfYear + 1}-$year"
                val textView = if (isStartDate) {
                    this.bottomSheetView.findViewById<TextView>(R.id.start_date_dis)
                } else {
                    this.bottomSheetView.findViewById<TextView>(R.id.end_date_dis)
                }
                textView.text = selectedDate

                // Save the selected date to the corresponding variables
                if (isStartDate) {
                    selectedStartMonth = monthOfYear + 1
                    selectedStartYear = year
                    selectedStartDay = dayOfMonth
                } else {
                    selectedEndMonth = monthOfYear + 1
                    selectedEndYear = year
                    selectedEndDay = dayOfMonth
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    //check and save end and start dates/ time
    private fun showTimePickerDialog(isStartTime: Boolean) {
        val timePicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                val textView = if (isStartTime) {
                    this.bottomSheetView.findViewById<TextView>(R.id.start_time_dis)
                } else {
                    this.bottomSheetView.findViewById<TextView>(R.id.end_time_dis)
                }
                textView.text = selectedTime

                // Save selected time to the corresponding variables
                if (isStartTime) {
                    selectedStartHour = hourOfDay
                    selectedStartMinute = minute
                } else {
                    selectedEndHour = hourOfDay
                    selectedEndMinute = minute
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePicker.show()
    }


    private fun scheduleAction() {
        // Check if an existing schedule is present
        val database = FirebaseDatabase.getInstance().getReference("HOUSES")
        val adaptor1Ref = database.child("HOUSES_1").child("ADAPTORS").child("ADAPTOR_1").child("SCHEDULE")

        adaptor1Ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Existing schedule found user can edit
                    showEditScheduleDialog()
                } else {
                    // No existing schedule, assign values and set SCHEDULE_STATUS
                    showCreateScheduleDialog()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle onCancelled event, if needed
            }
        })
    }

    private fun showEditScheduleDialog() {
        val database = FirebaseDatabase.getInstance().getReference("HOUSES")
        val adaptor1Ref = database.child("HOUSES_1").child("ADAPTORS").child("ADAPTOR_1").child("SCHEDULE")
        val dialog = BottomSheetDialog(this@Home)

        val bottomSheetView = LayoutInflater.from(this@Home).inflate(R.layout.bottom_sheet, null)

        val startDate = bottomSheetView.findViewById<ImageButton>(R.id.start_date)
        val endDate = bottomSheetView.findViewById<ImageButton>(R.id.end_date)
        val startTime = bottomSheetView.findViewById<ImageButton>(R.id.start_time)
        val endTime = bottomSheetView.findViewById<ImageButton>(R.id.end_time)
        val startDateTextView = bottomSheetView.findViewById<TextView>(R.id.start_date_dis)
        val startTimeTextView = bottomSheetView.findViewById<TextView>(R.id.start_time_dis)
        val endDateTextView = bottomSheetView.findViewById<TextView>(R.id.end_date_dis)
        val endTimeTextView = bottomSheetView.findViewById<TextView>(R.id.end_time_dis)
        val scheduleButton = bottomSheetView.findViewById<Button>(R.id.schedule_btn)
        val editButton = bottomSheetView.findViewById<Button>(R.id.edit_btn)

        // Retrieve the values from the database and set them in the dialog
        adaptor1Ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val selectedStartYear = dataSnapshot.child("START_YEAR").getValue(Int::class.java)
                    val selectedStartMonth = dataSnapshot.child("START_MONTH").getValue(Int::class.java)
                    val selectedStartDay = dataSnapshot.child("START_DAY").getValue(Int::class.java)
                    val selectedStartHour = dataSnapshot.child("START_HOUR").getValue(Int::class.java)
                    val selectedStartMinute = dataSnapshot.child("START_MINUTE").getValue(Int::class.java)
                    val selectedEndYear = dataSnapshot.child("END_YEAR").getValue(Int::class.java)
                    val selectedEndMonth = dataSnapshot.child("END_MONTH").getValue(Int::class.java)
                    val selectedEndDay = dataSnapshot.child("END_DAY").getValue(Int::class.java)
                    val selectedEndHour = dataSnapshot.child("END_HOUR").getValue(Int::class.java)
                    val selectedEndMinute = dataSnapshot.child("END_MINUTE").getValue(Int::class.java)

                    // Set the retrieved values in the dialog TextViews
                    startDateTextView.text = "$selectedStartDay/$selectedStartMonth/$selectedStartYear"
                    startTimeTextView.text = "$selectedStartHour:$selectedStartMinute"
                    endDateTextView.text = "$selectedEndDay/$selectedEndMonth/$selectedEndYear"
                    endTimeTextView.text = "$selectedEndHour:$selectedEndMinute"
                }

                // Make the date/time pickers not clickable
                startDate.isEnabled = false
                endDate.isEnabled = false
                startTime.isEnabled = false
                endTime.isEnabled = false
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle any errors that occur during the data retrieval
                Toast.makeText(
                    this@Home,
                    "Failed to retrieve from Database" + databaseError.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        scheduleButton.setOnClickListener {
            // Retrieve the updated values from the dialog TextViews
            val updatedStartYear = selectedStartYear
            val updatedStartMonth = selectedStartMonth
            val updatedStartDay = selectedStartDay
            val updatedStartHour = selectedStartHour
            val updatedStartMinute = selectedStartMinute
            val updatedEndYear = selectedEndYear
            val updatedEndMonth = selectedEndMonth
            val updatedEndDay = selectedEndDay
            val updatedEndHour = selectedEndHour
            val updatedEndMinute = selectedEndMinute

            val scheduleData = HashMap<String, Any>()
            scheduleData["START_YEAR"] = updatedStartYear
            scheduleData["START_MONTH"] = updatedStartMonth
            scheduleData["START_DAY"] = updatedStartDay
            scheduleData["START_HOUR"] = updatedStartHour
            scheduleData["START_MINUTE"] = updatedStartMinute
            scheduleData["END_YEAR"] = updatedEndYear
            scheduleData["END_MONTH"] = updatedEndMonth
            scheduleData["END_DAY"] = updatedEndDay
            scheduleData["END_HOUR"] = updatedEndHour
            scheduleData["END_MINUTE"] = updatedEndMinute

            adaptor1Ref.updateChildren(scheduleData)
                .addOnSuccessListener {
                    Toast.makeText(this@Home, "Schedule updated successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss() // Close the bottom sheet dialog
                }
                .addOnFailureListener {
                    Toast.makeText(this@Home, "Failed to update schedule", Toast.LENGTH_SHORT).show()
                }

        }

        editButton.setOnClickListener {
            // Make the date/time pickers clickable
            startDate.isEnabled = true
            endDate.isEnabled = true
            startTime.isEnabled = true
            endTime.isEnabled = true
            showBottomSheet()

        }

        // Set the custom view for the dialog and show it
        dialog.setContentView(bottomSheetView)
        dialog.show()
    }

    private fun showCreateScheduleDialog() {
        val database = FirebaseDatabase.getInstance().getReference("HOUSES")
        val adaptor1Ref = database.child("HOUSES_1").child("ADAPTORS").child("ADAPTOR_1").child("SCHEDULE")
        val dialog = BottomSheetDialog(this@Home)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet, null)


        val startDate = bottomSheetView.findViewById<ImageButton>(R.id.start_date)
        val endDate = bottomSheetView.findViewById<ImageButton>(R.id.end_date)
        val startTime = bottomSheetView.findViewById<ImageButton>(R.id.start_time)
        val endTime = bottomSheetView.findViewById<ImageButton>(R.id.end_time)
//        val startDateTextView = bottomSheetView.findViewById<TextView>(R.id.start_date_dis)
//        val startTimeTextView = bottomSheetView.findViewById<TextView>(R.id.start_time_dis)
//        val endDateTextView = bottomSheetView.findViewById<TextView>(R.id.end_date_dis)
//        val endTimeTextView = bottomSheetView.findViewById<TextView>(R.id.end_time_dis)
        val scheduleButton = bottomSheetView.findViewById<Button>(R.id.schedule_btn)
        val editButton = bottomSheetView.findViewById<Button>(R.id.edit_btn)

        // Make the date/time pickers clickable
        startDate.isEnabled = true
        endDate.isEnabled = true
        startTime.isEnabled = true
        endTime.isEnabled = true

        scheduleButton.setOnClickListener {
            // Retrieve the selected values from the dialog TextViews
            val selectedStartYear = this.selectedStartYear
            val selectedStartMonth = this.selectedStartMonth
            val selectedStartDay = this.selectedStartDay
            val selectedStartHour = this.selectedStartHour
            val selectedStartMinute = this.selectedStartMinute
            val selectedEndYear = this.selectedEndYear
            val selectedEndMonth = this.selectedEndMonth
            val selectedEndDay = this.selectedEndDay
            val selectedEndHour = this.selectedEndHour
            val selectedEndMinute = this.selectedEndMinute

            val scheduleData = HashMap<String, Any>()
            scheduleData["START_YEAR"] = selectedStartYear
            scheduleData["START_MONTH"] = selectedStartMonth
            scheduleData["START_DAY"] = selectedStartDay
            scheduleData["START_HOUR"] = selectedStartHour
            scheduleData["START_MINUTE"] = selectedStartMinute
            scheduleData["END_YEAR"] = selectedEndYear
            scheduleData["END_MONTH"] = selectedEndMonth
            scheduleData["END_DAY"] = selectedEndDay
            scheduleData["END_HOUR"] = selectedEndHour
            scheduleData["END_MINUTE"] = selectedEndMinute

            adaptor1Ref.updateChildren(scheduleData)
                .addOnSuccessListener {
                    Toast.makeText(this@Home, "Schedule created successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss() // Close the bottom sheet dialog
                }
                .addOnFailureListener {
                    Toast.makeText(this@Home, "Failed to create schedule", Toast.LENGTH_SHORT).show()
                }
        }

        editButton.visibility = View.GONE // Hide the edit button since it's a new schedule

        // Set the custom view for the dialog and show it
        dialog.setContentView(bottomSheetView)
        dialog.show()
        dialog.dismiss()
    }

}