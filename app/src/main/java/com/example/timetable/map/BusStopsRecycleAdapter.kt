package com.example.timetable.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.timetable.R
import com.example.timetable.data.BusStopWithTime


class BusStopsRecycleAdapter(var current: Int, var dataSet: List<BusStopWithTime>)
    : RecyclerView.Adapter<BusStopsRecycleAdapter.ViewHolder>()
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        val View = LayoutInflater.from(parent.context).inflate(R.layout.item_map, parent, false)
        return ViewHolder(View)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        holder.onBind(dataSet[position])
    }

    override fun getItemCount(): Int = dataSet.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        var context = itemView.context
        var name_text = itemView.findViewById<TextView>(R.id.textViewOst)
        var time_text = itemView.findViewById<TextView>(R.id.textViewTime)
        fun onBind(busStopWithTime: BusStopWithTime)
        {
            if (dataSet[current] == busStopWithTime)
            {
                itemView.findViewById<LinearLayout>(R.id.LinearLayout).setBackgroundColor( context.resources.getColor(
                    R.color.purple_700
                ) )
            }


            name_text.text = busStopWithTime.busStop?.name ?: ""
            time_text.text = busStopWithTime.time ?: ""

        }
    }

}