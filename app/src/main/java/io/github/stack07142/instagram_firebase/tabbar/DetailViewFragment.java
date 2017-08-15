package io.github.stack07142.instagram_firebase.tabbar;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import io.github.stack07142.instagram_firebase.R;
import io.github.stack07142.instagram_firebase.model.ContentDTO;

public class DetailViewFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_detailview, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.detailviewfragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(new DetailRecyclerViewAdapter());

        return view;
    }

    class DetailRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<ContentDTO> contentDTOs;

        public DetailRecyclerViewAdapter() {

            contentDTOs = new ArrayList<>();

            FirebaseDatabase.getInstance().getReference().child("images").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    contentDTOs.clear();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        contentDTOs.add(snapshot.getValue(ContentDTO.class));
                    }

                    notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detailview, parent, false);

            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            CustomViewHolder customViewHolder = (CustomViewHolder) holder;

            // 유저 아이디
            customViewHolder.profileTextView.setText(contentDTOs.get(position).userId);

            // 가운데 이미지
            Glide.with(holder.itemView.getContext())
                    .load(contentDTOs.get(position).imageUrl)
                    .into(customViewHolder.contentImageView);

            // 설명 텍스트
            customViewHolder.explainTextView.setText(contentDTOs.get(position).explain);

        }

        @Override
        public int getItemCount() {

            return contentDTOs.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {

            ImageView profileImageView;
            TextView profileTextView;
            ImageView contentImageView;
            ImageView favoriteImageView;
            ImageView commentImageView;
            TextView favoriteCounterTextView;
            TextView explainTextView;

            CustomViewHolder(View itemView) {
                super(itemView);

                profileImageView = (ImageView) itemView.findViewById(R.id.detailviewitem_profile_image);
                profileTextView = (TextView) itemView.findViewById(R.id.detailviewitem_profile_textview);

                contentImageView = (ImageView) itemView.findViewById(R.id.detailviewitem_imageview_content);

                favoriteImageView = (ImageView) itemView.findViewById(R.id.detailviewitem_favorite_imageview);
                commentImageView = (ImageView) itemView.findViewById(R.id.detailviewitem_comment_imageview);

                favoriteCounterTextView = (TextView) itemView.findViewById(R.id.detailviewitem_favoritecounter_textview);
                explainTextView = (TextView) itemView.findViewById(R.id.detailviewitem_explain_textview);
            }
        }
    }
}
