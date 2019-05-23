package com.zen.noisyai.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zen.noisyai.Constant;
import com.zen.noisyai.R;
import com.zen.noisyai.database.model.Record;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.MyViewHolder> {

    private Context context;
    private List<Record> recsList;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public TextView dot;
        public TextView timestamp;
        public TextView percentage;

        public MyViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.record);
            dot = view.findViewById(R.id.dot);
            timestamp = view.findViewById(R.id.timestamp);
            percentage = view.findViewById(R.id.percentage);
        }
    }


    public RecordAdapter(Context context, List<Record> recsList) {
        this.context = context;
        this.recsList = recsList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.record_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Record rec = recsList.get(position);

        holder.name.setText(rec.getName());
        // Displaying dot from HTML character code
        //holder.dot.setText(Html.fromHtml("&#8226;"));
        if(rec.getType() == 0){
            holder.dot.setText(Html.fromHtml("&#8226;"));
            holder.dot.setTextColor(ContextCompat.getColor(context, R.color.materail_red));
        }
        else if(rec.getType() >= 1){
            holder.dot.setText(Html.fromHtml("&#8226;") + String.valueOf(rec.getType()));
            holder.dot.setTextColor(ContextCompat.getColor(context, R.color.materail_green));
        }
        // Formatting and displaying timestamp
        holder.timestamp.setText(formatDate(rec.getTimestamp()));
        if(rec.getPercentage() > 0) holder.percentage.setText("%"+(int)rec.getPercentage());
    }

    @Override
    public int getItemCount() {
        return recsList.size();
    }

    /**
     * Formatting timestamp to `MMM d` format
     * Input: 2018-02-21 00:15:42
     * Output: Feb 21
     */
    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat(Constant.DATE_FORMAT);
            Date date = fmt.parse(dateStr);
            SimpleDateFormat fmtOut = new SimpleDateFormat("MMM d");
            return fmtOut.format(date);
        } catch (ParseException e) {

        }

        return "";
    }
}
