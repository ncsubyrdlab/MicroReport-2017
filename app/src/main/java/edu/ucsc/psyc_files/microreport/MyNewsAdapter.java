package edu.ucsc.psyc_files.microreport;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Christy on 7/26/2015.
 */
public class MyNewsAdapter extends RecyclerView.Adapter<MyNewsAdapter.ViewHolder> {
    private String[][] mDataset; //0 is header, 1 is description
    private String mLink;   //needs to be an intent or activity?

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mHeading;
        public TextView mDescription;
        public TextView mTimestamp;

        public ViewHolder(View v) {
            super(v);
            mHeading = (TextView) v.findViewById(R.id.heading);
            mDescription = (TextView) v.findViewById(R.id.description);
            mTimestamp = (TextView) v.findViewById(R.id.timestamp);

        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyNewsAdapter(String[][] myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyNewsAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup,
                                                   int i) {
        // create a new view
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.card_layout, viewGroup, false);

        return new ViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.mHeading.setText(mDataset[position][0]);
        holder.mDescription.setText(mDataset[position][1]);
        holder.mTimestamp.setText(mDataset[position][2]);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.length;
    }

    public String getLink(int position) {
        mLink = (mDataset[position][3]);
        return mLink;
    }
}
