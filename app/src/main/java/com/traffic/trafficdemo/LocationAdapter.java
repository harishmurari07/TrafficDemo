package com.traffic.trafficdemo;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class LocationAdapter extends ArrayAdapter<LocationEntity> {

    private ArrayList<LocationEntity> items;
    private Activity activity;
    private LayoutInflater mInflater;

    public LocationAdapter(Activity activity, int resourceId,
                           ArrayList<LocationEntity> itemsList) {
        super(activity, resourceId, itemsList);
        this.activity = activity;
        items = itemsList;
        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View view, ViewGroup parent) {
        DataViewHolder holder;
        if (view != null) {
            holder = (DataViewHolder) view.getTag();
        } else {
            view = mInflater.inflate(R.layout.list_item, parent, false);
            holder = new DataViewHolder(view);
            view.setTag(holder);
        }
        LocationEntity entity = items.get(position);
        view.setId(position);
        if (holder.address != null)
            holder.address.setText(entity.address);

        return view;
    }

    public void setItems(ArrayList<LocationEntity> itemsList) {
        clearItems();
        items.addAll(itemsList);
        notifyDataSetChanged();
    }

    private void clearItems() {
        this.items.clear();
    }

    static class DataViewHolder{

        TextView address;

        DataViewHolder(View view) {
            address = (TextView) view.findViewById(R.id.address);
        }
    }

}
