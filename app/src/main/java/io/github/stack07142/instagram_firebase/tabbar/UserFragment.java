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
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import io.github.stack07142.instagram_firebase.R;
import io.github.stack07142.instagram_firebase.databinding.FragmentUserBinding;
import io.github.stack07142.instagram_firebase.model.AlarmDTO;
import io.github.stack07142.instagram_firebase.model.ContentDTO;
import io.github.stack07142.instagram_firebase.model.FollowDTO;

public class UserFragment extends Fragment {

    private static final int PICK_FROM_ALBUM = 10;

    // Data Binding
    private FragmentUserBinding binding;

    // Firebase
    private DatabaseReference dbRef;

    private String destinationUid;
    private String uid;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user, container, false);

        // Firebase
        dbRef = FirebaseDatabase.getInstance().getReference();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        return view;
    }

    // UI 변경 작업
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        binding = FragmentUserBinding.bind(getView());

        /*
         * UI Setting
         */

        // 버튼 - Follow or SignOut
        if (getArguments() != null) {

            destinationUid = getArguments().getString("destinationUid");

            // 본인 계정인 경우 -> 로그아웃
            if (destinationUid != null && destinationUid.equals(uid)) {

                binding.accountBtnFollowSignout.setText(getString(R.string.signout));
                binding.accountBtnFollowSignout.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        signOut();
                    }
                });
            }
            // 본인 계정이 아닌 경우 -> 팔로우
            else {

                binding.accountBtnFollowSignout.setText(getString(R.string.follow));
                binding.accountBtnFollowSignout.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        requestFollow();
                    }
                });
            }
        }

        // Profile Image Click Listener
        binding.accountIvProfile.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                //권한 요청 하는 부분
                ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                //앨범 오픈
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                getActivity().startActivityForResult(photoPickerIntent, PICK_FROM_ALBUM);
            }
        });

        /*
         * Get Data
         */
        getProfileImage();
        getFollower();
        getFollowing();

        // Recycler View
        binding.accountRecyclerview.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        binding.accountRecyclerview.setAdapter(new UserFragmentRecyclerViewAdapter());
    }

    /* ------------------------------------------------------------------------------------------ */

    /**
     * RecyclerView Adapter
     */

    private class UserFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<ContentDTO> contentDTOs;

        UserFragmentRecyclerViewAdapter() {

            contentDTOs = new ArrayList<>();

            dbRef.child("images").addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    contentDTOs.clear();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        contentDTOs.add(snapshot.getValue(ContentDTO.class));
                    }

                    binding.accountTvPostCount.setText(String.valueOf(contentDTOs.size()));
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

        // RecyclerView Adapter - View Holder
        private class CustomViewHolder extends RecyclerView.ViewHolder {

            ImageView imageView;

            CustomViewHolder(ImageView imageView) {
                super(imageView);
                this.imageView = imageView;
            }
        }
    }

    /* ------------------------------------------------------------------------------------------ */

    /**
     * Get - Profile Image, Follwer Count, Following Count, (Post Count <- ContentsDTO's Size)
     */

    void getProfileImage() {

        dbRef.child("profileImages").child(destinationUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()) {

                            @SuppressWarnings("VisibleForTests")
                            String url = dataSnapshot.getValue().toString();
                            Glide.with(getActivity())
                                    .load(url)
                                    .apply(new RequestOptions().circleCrop()).into(binding.accountIvProfile);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    void getFollower() {

        dbRef.child("users").child(destinationUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FollowDTO followDTO = dataSnapshot.getValue(FollowDTO.class);
                        try {
                            binding.accountTvFollowerCount.setText(String.valueOf(followDTO.followerCount));
                            if (followDTO.followers.containsKey(uid)) {
                                binding.accountBtnFollowSignout
                                        .getBackground()
                                        .setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
                            } else {
                                binding.accountBtnFollowSignout
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

    void getFollowing() {

        dbRef.child("users").child(destinationUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FollowDTO followDTO = dataSnapshot.getValue(FollowDTO.class);
                        try {
                            binding.accountTvFollowingCount.setText(String.valueOf(followDTO.followingCount));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    /* ------------------------------------------------------------------------------------------ */

    /**
     * Request Follower, Follow Alarm
     */

    public void requestFollow() {

        dbRef.child("users").child(uid)
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
                            followerAlarm(destinationUid);
                        }
                        // Set value and report transaction success
                        mutableData.setValue(followDTO);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                    }
                });

        dbRef.child("users").child(destinationUid)
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

        dbRef.child("alarms").push().setValue(alarmDTO);
    }


    /* ------------------------------------------------------------------------------------------ */

    /**
     * Sign Out
     */

    private void signOut() {

    }

}
