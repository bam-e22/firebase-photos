package io.github.stack07142.instagram_firebase.tabbar;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import io.github.stack07142.instagram_firebase.MainActivity;
import io.github.stack07142.instagram_firebase.R;
import io.github.stack07142.instagram_firebase.model.AlarmDTO;
import io.github.stack07142.instagram_firebase.model.ContentDTO;

public class DetailViewFragment extends Fragment {

    private FirebaseUser user;

    public DetailViewFragment() {

        user = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_detailview, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.detailviewfragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(new DetailRecyclerViewAdapter());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        ((MainActivity) getActivity()).getBinding().progressBar.setVisibility(View.GONE);
    }

    private class DetailRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<ContentDTO> contentDTOs;
        private ArrayList<String> contentUidList;

        DetailRecyclerViewAdapter() {

            contentDTOs = new ArrayList<>();
            contentUidList = new ArrayList<>();

            FirebaseDatabase.getInstance().getReference().child("images").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    contentDTOs.clear();
                    contentUidList.clear();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        contentDTOs.add(snapshot.getValue(ContentDTO.class));
                        contentUidList.add(snapshot.getKey());
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
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {

            CustomViewHolder customViewHolder = (CustomViewHolder) holder;

            // Profile Image
            FirebaseDatabase.getInstance()
                    .getReference()
                    .child("profileImages").child(contentDTOs.get(position).uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            if (dataSnapshot.exists()) {

                                @SuppressWarnings("VisibleForTests")
                                String url = dataSnapshot.getValue().toString();

                                ImageView profileImageView = ((CustomViewHolder) holder).profileImageView;
                                Glide.with(holder.itemView.getContext())
                                        .load(url)
                                        .apply(new RequestOptions().circleCrop()).into(profileImageView);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

            // 유저 아이디
            customViewHolder.profileTextView.setText(contentDTOs.get(position).userId);

            // 가운데 이미지
            Glide.with(holder.itemView.getContext())
                    .load(contentDTOs.get(position).imageUrl)
                    .into(customViewHolder.contentImageView);

            // 설명 텍스트
            customViewHolder.explainTextView.setText(contentDTOs.get(position).explain);


            final int finalPosition = position;
            customViewHolder.favoriteImageView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    favoriteEvent(finalPosition);
                }
            });

            if (contentDTOs.get(position)
                    .favorites.containsKey(FirebaseAuth.getInstance().getCurrentUser().getUid())) {

                customViewHolder.favoriteImageView.setImageResource(R.drawable.ic_favorite);
            } else {

                customViewHolder.favoriteImageView.setImageResource(R.drawable.ic_favorite_border);
            }

            customViewHolder.favoriteCounterTextView.setText("좋아요 " + contentDTOs.get(position).favoriteCount + "개");

            customViewHolder.commentImageView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(v.getContext(), CommentActivity.class);
                    intent.putExtra("imageUid", contentUidList.get(finalPosition));
                    intent.putExtra("destinationUid", contentDTOs.get(finalPosition).uid);
                    Log.d("DetailViewFragment", contentUidList.get(finalPosition) == null ? "NULL" : contentUidList.get(finalPosition));
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {

            return contentDTOs.size();
        }

        private void favoriteEvent(int position) {

            final int finalPosition = position;

            FirebaseDatabase.getInstance().getReference("images").child(contentUidList.get(position))
                    .runTransaction(new Transaction.Handler() {

                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {

                            ContentDTO contentDTO = mutableData.getValue(ContentDTO.class);

                            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                            if (contentDTO == null) {
                                return Transaction.success(mutableData);
                            }
                            if (contentDTO.favorites.containsKey(uid)) {

                                // Unstar the post and remove self from stars
                                contentDTO.favoriteCount = contentDTO.favoriteCount - 1;
                                contentDTO.favorites.remove(uid);
                            } else {

                                // Star the post and add self to stars
                                contentDTO.favoriteCount = contentDTO.favoriteCount + 1;
                                contentDTO.favorites.put(uid, true);
                                favoriteAlarm(contentDTOs.get(finalPosition).uid);
                            }

                            // Set value and report transaction success
                            mutableData.setValue(contentDTO);

                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                        }
                    });
        }

        public void favoriteAlarm(String destinationUid) {

            AlarmDTO alarmDTO = new AlarmDTO();

            alarmDTO.destinationUid = destinationUid;
            alarmDTO.userId = user.getEmail();
            alarmDTO.uid = user.getUid();
            alarmDTO.kind = 0; // TODO : TypeDef
            FirebaseDatabase.getInstance().getReference().child("alarms").push().setValue(alarmDTO);
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
