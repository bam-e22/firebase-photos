package io.github.stack07142.instagram_firebase.tabbar;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import io.github.stack07142.instagram_firebase.R;
import io.github.stack07142.instagram_firebase.model.AlarmDTO;
import io.github.stack07142.instagram_firebase.model.ContentDTO;
import io.github.stack07142.instagram_firebase.model.FollowDTO;

public class UserFragment extends Fragment {

    private static final int PICK_FROM_ALBUM = 10;

    private TextView contentsCounter;
    private TextView followerCounter;
    private TextView followeringCounter;
    private Button followerButton;
    private String destinationUid;
    private String uid;
    private ImageView profileImage;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user, container, false);

        contentsCounter = (TextView) view.findViewById(R.id.userfragment_textview_contentscounter);
        followerCounter = (TextView) view.findViewById(R.id.userfragment_textview_follower);
        followeringCounter = (TextView) view.findViewById(R.id.userfragment_textview_following);
        followerButton = (Button) view.findViewById(R.id.userfragment_button_follow);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        profileImage = (ImageView) view.findViewById(R.id.userfragment_imageview_profile);
        if (getArguments() != null) {
            destinationUid = getArguments().getString("destinationUid");
            if (destinationUid.equals(uid)) {
                followerButton.setEnabled(false);
                profileImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) { //권한 요청 하는 부분 ActivityCompat.requestPermissions
                        ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                        //앨범 오픈
                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                        photoPickerIntent.setType("image/*");
                        getActivity().startActivityForResult(photoPickerIntent, PICK_FROM_ALBUM);
                    }
                });
            }
        }

        followerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestFollow();
            }
        });
        if (getArguments() != null) {
            destinationUid = getArguments().getString("destinationUid");

            if (destinationUid.equals(uid)) {

                followerButton.setEnabled(false);
            }
        }
        getFollower();
        getFollowing();

        getProfileImage();

        // Recycler View
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.userfragment_recyclerview);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        recyclerView.setAdapter(new UserFragmentRecyclerViewAdapter());

        return view;
    }

    public void getProfileImage() {
        FirebaseDatabase.getInstance().getReference()
                .child("profileImages")
                .child(destinationUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        @SuppressWarnings("VisibleForTests")
                        String url = dataSnapshot.getValue().toString();
                        Glide.with(getActivity())
                                .load(url)
                                .apply(new RequestOptions().circleCrop()).into(profileImage);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    class UserFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<ContentDTO> contentDTOs;

        public UserFragmentRecyclerViewAdapter() {

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

            int width = getResources().getDisplayMetrics().widthPixels / 3;

            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new LinearLayoutCompat.LayoutParams(width, width));

            return new CustomViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            Glide.with(holder.itemView.getContext())
                    .load(contentDTOs.get(position).imageUrl)
                    .apply(new RequestOptions().centerCrop())
                    .into(((CustomViewHolder) holder).imageView);
        }

        @Override
        public int getItemCount() {

            return contentDTOs.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {

            public ImageView imageView;

            public CustomViewHolder(ImageView imageView) {
                super(imageView);
                this.imageView = imageView;
            }
        }
    }

    public void getFollower() {
        FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .child(destinationUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FollowDTO followDTO = dataSnapshot.getValue(FollowDTO.class);
                        try {
                            followerCounter.setText(String.valueOf(followDTO.followerCount));
                            if (followDTO.followers.containsKey(uid)) {
                                followerButton
                                        .getBackground()
                                        .setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
                            } else {
                                followerButton
                                        .getBackground().setColorFilter(null);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    public void getFollowing() {
        FirebaseDatabase.getInstance().getReference().child("users").child(destinationUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FollowDTO followDTO = dataSnapshot.getValue(FollowDTO.class);
                        try {
                            followeringCounter.setText(String.valueOf(followDTO.followingCount));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    public void requestFollow() {
        FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .child(uid)
                .runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        FollowDTO followDTO = mutableData.getValue(FollowDTO.class);
                        if (followDTO == null) {
                            followDTO = new FollowDTO();
                            followDTO.followingCount = 1;
                            followDTO.followings.put(destinationUid, true);
                            mutableData.setValue(followDTO);
                            return Transaction.success(mutableData);
                        }
                        if (followDTO.followings.containsKey(destinationUid)) {
                            // Unstar the post and remove self from stars
                            followDTO.followingCount = followDTO.followingCount - 1;
                            followDTO.followings.remove(destinationUid);
                        } else {
                            // Star the post and add self to stars
                            followDTO.followingCount = followDTO.followingCount + 1;
                            followDTO.followings.put(destinationUid, true);
                        }
                        // Set value and report transaction success
                        mutableData.setValue(followDTO);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                    }
                });

        FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .child(destinationUid)
                .runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        FollowDTO followDTO = mutableData.getValue(FollowDTO.class);
                        if (followDTO == null) {
                            followDTO = new FollowDTO();
                            followDTO.followerCount = 1;
                            followDTO.followers.put(uid, true);
                            mutableData.setValue(followDTO);
                            return Transaction.success(mutableData);
                        }
                        if (followDTO.followers.containsKey(uid)) {
                            // Unstar the post and remove self from stars
                            followDTO.followerCount = followDTO.followerCount - 1;
                            followDTO.followers.remove(uid);
                        } else {
                            // Star the post and add self to stars
                            followDTO.followerCount = followDTO.followerCount + 1;
                            followDTO.followers.put(uid, true);
                        }
                        // Set value and report transaction success
                        mutableData.setValue(followDTO);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                    }
                });
    }

    private void followerAlarm(String destinationUid) {

        AlarmDTO alarmDTO = new AlarmDTO();

        alarmDTO.destinationUid = destinationUid;
        alarmDTO.userId = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        alarmDTO.uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        alarmDTO.kind = 2;

        FirebaseDatabase.getInstance().getReference().child("alarms").push().setValue(alarmDTO);
    }
}
