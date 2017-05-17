package edu.ucsc.sites.microreport;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * A custom listview/recyclerview adapter to show Good News items.
 */
public class MyNewsAdapter extends RecyclerView.Adapter<MyNewsAdapter.ViewHolder> {

    private ArrayList<BulletinBoard.NewsItem> mDataset;

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView mHeading;
        public TextView mDescription;
        public TextView mTimestamp;
        public String mLink;

        public ViewHolder(View v) {
            super(v);
            mHeading = (TextView) v.findViewById(R.id.heading);
            mDescription = (TextView) v.findViewById(R.id.description);
            mTimestamp = (TextView) v.findViewById(R.id.timestamp);
            mHeading.setOnClickListener(this);
            v.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
                Context context = v.getContext();
                //todo: set pressed color to give feedback when clicking views
                //v.setBackgroundColor(v.getResources().getColor(R.color.ucsc_gold));
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mLink));
                context.startActivity(intent);
        }
    }

    public MyNewsAdapter(ArrayList<BulletinBoard.NewsItem> myDataset) {
        mDataset = myDataset;
    }

    /** Create new views (invoked by the layout manager) */
    @Override
    public MyNewsAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup,
                                                   int i) {
        // create a new view
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.card_layout, viewGroup, false);

        return new ViewHolder(itemView);
    }

    /**Replace the contents of a view (invoked by the layout manager) */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        BulletinBoard.NewsItem item = mDataset.get(position);
        holder.mHeading.setText(item.getTitle());
        holder.mDescription.setText(item.getText());
        holder.mTimestamp.setText(item.getTimestamp().toString());
        holder.mLink = item.getLink();
    }

    /** Return the size of your dataset (invoked by the layout manager) */
    @Override
    public int getItemCount() {
            return mDataset.size();
    }
}
