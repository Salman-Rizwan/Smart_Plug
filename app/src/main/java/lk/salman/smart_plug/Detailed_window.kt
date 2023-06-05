package lk.salman.smart_plug

import android.icu.util.Calendar
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class Detailed_window : AppCompatActivity() {

    private lateinit var summaryRecyclerView: RecyclerView
    private lateinit var summaryAdapter: SummaryAdapter
    private val monthlyReadingsMap = HashMap<String, String>()
    private var currentYear: Int = 0
    private var currentMonth: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_window)

        //hide app name displaying
        supportActionBar?.hide()

        // Initialize RecyclerView and its adapter
        summaryRecyclerView = findViewById(R.id.summary_view)
        summaryAdapter = SummaryAdapter()
        summaryRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false)
        summaryRecyclerView.adapter = summaryAdapter

        // Call the function to calculate and update monthly consumption
        calculateMonthlyCurrent()
    }

    private fun calculateMonthlyCurrent() {
        // Get a reference to the Firebase database
        val database = FirebaseDatabase.getInstance().reference
        // Specify the database path to the relevant node
        val adaptor1Ref = database.child("HOUSES").child("HOUSES_1").child("ADAPTORS").child("ADAPTOR_1")

        // Get the current month and year
        val calendar = Calendar.getInstance()
        currentMonth = calendar.get(Calendar.MONTH) + 1 // Get the current month (1-12)
        currentYear = calendar.get(Calendar.YEAR) // Get the current year

        // Add a ValueEventListener to retrieve the monthly consumption data
        adaptor1Ref.child("MonthlyReadings").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                monthlyReadingsMap.clear() // Clear the previous data

                // Iterate through the monthly readings and retrieve the consumption values
                for (monthSnapshot in snapshot.children) {
                    val month = monthSnapshot.key ?: ""
                    val current = monthSnapshot.child("current").getValue(Float::class.java) ?: 0f
                    monthlyReadingsMap[month] = String.format("%.2f", current)
                }

                // Notify the adapter that the data has changed
                summaryAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Detailed_window, "Database reference failed", Toast.LENGTH_LONG).show()
            }
        })
    }




    private inner class SummaryAdapter : RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder>(){

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
            // Inflate the layout for the summary card item
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.summery_item, parent, false)
            return SummaryViewHolder(itemView)
        }

        override fun getItemCount(): Int {
            return 12 // Return the total number of months (12)
        }

        override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
            // Bind the data to the views of the summary card item
            val currentMonth = getMonthName(position + 1) // position starts from 0, so add 1
            val currentConsumption = monthlyReadingsMap[currentMonth] ?: "0"

            holder.bindData(currentMonth, currentConsumption)
        }




        inner class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
            private val currentSumTextView: TextView = itemView.findViewById(R.id.current_sum)
            private val yearTextView: TextView = itemView.findViewById(R.id.txt_year)
            private val monthTextView: TextView = itemView.findViewById(R.id.txt_month)

            // Bind the data to the views
            fun bindData(month: String, consumption: String) {
                currentSumTextView.text = consumption
                yearTextView.text = currentYear.toString()
                monthTextView.text = getMonthDateRangeText(month)


                // Add any additional styling or functionality to the card as needed
                // For example, you can add click listeners or customize the card appearance
                // based on the consumption value or other criteria.
            }
        }

        }

    private fun getMonthName(month: Int): String {
        // Convert the month number to a month name string (e.g., 1 -> "January")
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, month - 1) // Subtract 1 as month is zero-based
        val monthDateFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        return monthDateFormat.format(calendar.time)
    }


    //get range to display in month textview
    private fun getMonthDateRangeText(month: String): String {
        // Get the date range for the given month (e.g., "Jan 01 - Jan 31")
        val calendar = Calendar.getInstance()
        val monthDateFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val formattedMonth = monthDateFormat.parse(month)

        val startDate = calendar.getActualMinimum(Calendar.DAY_OF_MONTH)
        val endDate = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        calendar.time = formattedMonth
//        val startDateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
//        val endDateFormat = SimpleDateFormat("dd", Locale.getDefault())
//
//        val startDateString = startDateFormat.format(calendar.time)
//        calendar.set(Calendar.DAY_OF_MONTH, endDate)
//        val endDateString = endDateFormat.format(calendar.time)

        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val startDateString = dateFormat.format(calendar.time)
        calendar.set(Calendar.DAY_OF_MONTH, endDate)
        val endDateString = dateFormat.format(calendar.time)

        return "$startDateString - $endDateString"
    }

}

