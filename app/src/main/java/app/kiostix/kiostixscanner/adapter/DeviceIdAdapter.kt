package app.kiostix.kiostixscanner.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import app.kiostix.kiostixscanner.model.DeviceIdSpinnerModel

class DeviceIdAdapter(context: Context, resource:Int, private val arrayList: ArrayList<DeviceIdSpinnerModel>) :
        ArrayAdapter<DeviceIdSpinnerModel>(context, resource, arrayList) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: ViewHolder
        val retView: View

        if (convertView == null) {
            holder = ViewHolder()
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            retView = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, null, true)
            holder.deviceName = retView.findViewById(android.R.id.text1)

            retView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
            retView = convertView
        }

        holder.deviceName.text = arrayList[position].deviceName

        return retView
    }

    override fun getItem(position: Int): DeviceIdSpinnerModel {
        return arrayList[position]
    }

    override fun getCount(): Int {
        return arrayList.size
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: ViewHolder
        val retView: View

        if (convertView == null) {
            holder = ViewHolder()
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            retView = inflater.inflate(android.R.layout.simple_spinner_item, null, true)
            holder.deviceNameDropDown = retView.findViewById(android.R.id.text1)

            retView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
            retView = convertView
        }

        holder.deviceNameDropDown.text = arrayList[position].deviceName

        return retView
    }

    class ViewHolder {
        lateinit var deviceName: TextView
        lateinit var deviceNameDropDown: TextView
    }
}