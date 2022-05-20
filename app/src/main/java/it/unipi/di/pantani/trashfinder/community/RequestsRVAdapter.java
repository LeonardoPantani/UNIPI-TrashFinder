package it.unipi.di.pantani.trashfinder.community;

import static it.unipi.di.pantani.trashfinder.data.marker.POIMarker.getMarkerTypeName;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;
import it.unipi.di.pantani.trashfinder.data.requests.POIRequest;

public class RequestsRVAdapter extends RecyclerView.Adapter<RequestsRVAdapter.ViewHolder> {
    private final Context context;
    private final RecyclerView mRV;
    private final LayoutInflater mInflater;
    private final ArrayList<POIRequest> requestsList;

    private int mExpandedPosition = -1;
    private int mPreviousExpandedPosition = -1;

    // data is passed into the constructor
    RequestsRVAdapter(Context context, RecyclerView rv) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
        this.mRV = rv;
        requestsList = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = mInflater.inflate(R.layout.fragment_requests_item, parent, false);
        return new ViewHolder(root);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        POIRequest current = requestsList.get(position);
        POIMarker element = current.getElement();
        String imageLink = current.getImageLink();

        final boolean isExpanded = holder.getAbsoluteAdapterPosition() == mExpandedPosition;
        holder.mExpanded.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.itemView.setActivated(isExpanded);

        if (isExpanded)
            mPreviousExpandedPosition = holder.getAbsoluteAdapterPosition();

        holder.itemView.setOnClickListener(v -> {
            mExpandedPosition = isExpanded ? -1 : holder.getAbsoluteAdapterPosition();
            TransitionManager.beginDelayedTransition(mRV);

            notifyItemChanged(mPreviousExpandedPosition);
            notifyItemChanged(holder.getAbsoluteAdapterPosition());
        });

        holder.mContentTitle.setText(context.getResources().getString(R.string.requests_item_title, current.getId(), POIMarker.getTitleFromMarker(context, element)));

        Set<POIMarker.MarkerType> types = element.getTypes();
        types.remove(POIMarker.MarkerType.recyclingdepot);
        String s = types.stream()
                .map(t -> getMarkerTypeName(context, t))
                .collect(Collectors.joining(", "));

        holder.mContentDesc.setText(s);

        if(imageLink != null)
            holder.mImage.setImageURI(Uri.parse(imageLink));
        else
            holder.mImage.setImageURI(null);
        /*
        TODO far funzionare picasso o usare altra libreria
        Picasso p = Picasso.with(context);
        p.load(current.getImageLink()).placeholder(R.drawable.default_user_icon).fit().centerInside().error(R.drawable.ic_baseline_swipe_24).into(holder.mImage);
        */
    }

    @Override
    public int getItemCount() {
        return requestsList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<POIRequest> newList) {
        requestsList.clear();
        requestsList.addAll(newList);

        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final LinearLayout mExpanded;
        public final TextView mContentTitle;
        public final TextView mContentDesc;
        public final ImageView mImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mExpanded = itemView.findViewById(R.id.rv_itemexpanded);
            mContentTitle = itemView.findViewById(R.id.rv_item_title);
            mContentDesc = itemView.findViewById(R.id.rv_item_desc);
            mImage = itemView.findViewById(R.id.rv_item_img);
        }
    }
}