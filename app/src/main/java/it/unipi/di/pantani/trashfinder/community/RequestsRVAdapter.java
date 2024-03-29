/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.community;

import static it.unipi.di.pantani.trashfinder.data.marker.POIMarker.getMarkerTypeName;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.Utils;
import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;
import it.unipi.di.pantani.trashfinder.data.requests.POIRequest;

public class RequestsRVAdapter extends RecyclerView.Adapter<RequestsRVAdapter.ViewHolder> {
    private final Context mContext;
    private final RecyclerView mRV;
    private final LayoutInflater mInflater;
    private final ArrayList<POIRequest> mRequestsList;

    private int mExpandedPosition = -1;
    private int mPreviousExpandedPosition = -1;

    // data is passed into the constructor
    RequestsRVAdapter(Context mContext, RecyclerView rv) {
        this.mContext = mContext;
        this.mInflater = LayoutInflater.from(mContext);
        this.mRV = rv;
        mRequestsList = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = mInflater.inflate(R.layout.fragment_requests_item, parent, false);
        return new ViewHolder(root);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        POIRequest currentReq = mRequestsList.get(position);
        POIMarker currentElem = currentReq.getElement();
        String imageLink = currentReq.getImageLink();

        if(currentElem == null) return;

        // se l'attuale richiesta non è una eliminazione ed esiste un'app che sa gestire gli intent di navigazione
        if(holder.mCanNavigate && !currentReq.getDeletion()) {
                holder.mDirections.setOnClickListener(v -> {
                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + currentElem.getLatitude() + "," + currentElem.getLongitude());
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");

                    // mostra avviso
                    AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
                    alertDialog.setTitle(mContext.getResources().getString(R.string.requests_navigation_title));
                    alertDialog.setMessage(mContext.getResources().getString(R.string.requests_navigation_desc));
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, mContext.getResources().getString(R.string.button_cancel),
                            (dialog, which) -> dialog.dismiss());
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, mContext.getResources().getString(R.string.button_ok),
                            (dialog, which) -> mContext.startActivity(mapIntent));
                    alertDialog.show();
                });
            holder.mDirections.setVisibility(View.VISIBLE);
        } else {
            holder.mDirections.setVisibility(View.GONE);
        }

        /*
            questa parte si occupa di mostrare e neascondere parti inizialmente visibili di ogni
            oggetto della recycler view.
         */
        final boolean isExpanded = holder.getAbsoluteAdapterPosition() == mExpandedPosition;
        holder.mExpanded.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.itemView.setActivated(isExpanded);
        if (isExpanded) mPreviousExpandedPosition = holder.getAbsoluteAdapterPosition();

        holder.itemView.setOnClickListener(v -> {
            mExpandedPosition = isExpanded ? -1 : holder.getAbsoluteAdapterPosition();
            TransitionManager.beginDelayedTransition(mRV);

            // più efficiente di aggiornare l'intera lista
            notifyItemChanged(mPreviousExpandedPosition);
            notifyItemChanged(holder.getAbsoluteAdapterPosition());
        });

        // id richiesta e titolo marker
        String typeRequest;
        if(currentReq.getDeletion()) {
            // eliminazione
            typeRequest = mContext.getResources().getString(R.string.requests_action_deletion);
        } else if(currentReq.getImageLink() != null && !currentReq.getImageLink().equals("")) {
            // creazione
            typeRequest = mContext.getResources().getString(R.string.requests_action_creation);
        } else {
            // modifica
            typeRequest = mContext.getResources().getString(R.string.requests_action_update);
        }

        holder.mContentTitle.setText(mContext.getResources().getString(R.string.requests_item_title, currentReq.getId(), POIMarker.getTitleFromMarker(mContext, currentElem), typeRequest));
        Date d = new Date(currentReq.getDate());
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault());
        holder.mContentDate.setText(sdf.format(d));
        // tipo marker
        HashSet<POIMarker.MarkerType> types = new HashSet<>(currentElem.getTypes());
        types.remove(POIMarker.MarkerType.recyclingdepot);
        String s = types.stream()
                .map(t -> getMarkerTypeName(mContext, t))
                .collect(Collectors.joining(", "));
        holder.mContentDesc.setText(s);

        // immagine marker
        if(currentReq.getImageLink() != null && !currentReq.getImageLink().equals("")) {
            Glide.with(this.mContext)
                    .load(imageLink)
                    .error(R.drawable.ic_baseline_running_with_errors_24)
                    .placeholder(R.drawable.ic_baseline_downloading_24)
                    .centerCrop()
                    .into(holder.mImage);
            holder.mBinImageHeader.setVisibility(View.VISIBLE);
            holder.mImage.setVisibility(View.VISIBLE);
        } else {
            holder.mBinImageHeader.setVisibility(View.GONE);
            holder.mImage.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mRequestsList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<POIRequest> newList) {
        mRequestsList.clear();
        mRequestsList.addAll(newList);

        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final LinearLayout mExpanded;

        public final TextView mContentTitle;
        public final TextView mContentDate;
        public final TextView mContentDesc;
        public final ImageView mImage;

        public final TextView mBinTypesHeader;
        public final TextView mBinImageHeader;

        public final ImageView mDirections;
        public boolean mCanNavigate = true;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mExpanded = itemView.findViewById(R.id.rv_itemexpanded);
            mContentTitle = itemView.findViewById(R.id.rv_item_title);
            mContentDate = itemView.findViewById(R.id.rv_item_date);
            mContentDesc = itemView.findViewById(R.id.rv_item_desc);
            mImage = itemView.findViewById(R.id.rv_item_img);

            mBinTypesHeader = itemView.findViewById(R.id.rv_item_bintypesheader);
            mBinImageHeader = itemView.findViewById(R.id.rv_item_binimageheader);

            mDirections = itemView.findViewById(R.id.rv_item_directions);
            if(!Utils.canNavigate(itemView.getContext())) {
                mCanNavigate = false;
            }
        }
    }
}