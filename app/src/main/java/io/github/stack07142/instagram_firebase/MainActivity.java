package io.github.stack07142.instagram_firebase;

import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.github.stack07142.instagram_firebase.databinding.ActivityMainBinding;
import io.github.stack07142.instagram_firebase.tabbar.AddPhotoActivity;
import io.github.stack07142.instagram_firebase.tabbar.AlarmFragment;
import io.github.stack07142.instagram_firebase.tabbar.DetailViewFragment;
import io.github.stack07142.instagram_firebase.tabbar.GridFragment;
import io.github.stack07142.instagram_firebase.tabbar.UserFragment;

import static io.github.stack07142.instagram_firebase.util.StatusCode.PICK_IMAGE_FROM_ALBUM;
import static io.github.stack07142.instagram_firebase.util.StatusCode.PICK_PROFILE_FROM_ALBUM;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    // Data Binding
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.progressBar.setVisibility(View.VISIBLE);

        // Bottom Navigation View
        binding.bottomNavigation.setOnNavigationItemSelectedListener(this);
        binding.bottomNavigation.setSelectedItemId(R.id.action_home);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_home:
                getFragmentManager().beginTransaction()
                        .replace(R.id.main_content, new DetailViewFragment())
                        .commit();
                return true;

            case R.id.action_search:

                Log.d("MainActivity", "action_search");
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_content, new GridFragment())
                        .commit();
                return true;

            case R.id.action_add_photo:
                startActivityForResult(new Intent(MainActivity.this, AddPhotoActivity.class), PICK_IMAGE_FROM_ALBUM);

                return true;

            case R.id.action_favorite_alarm:

                getFragmentManager().beginTransaction().replace(R.id.main_content, new AlarmFragment()).commit();

                return true;

            case R.id.action_account:

                Fragment fragment = new UserFragment();

                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                Bundle bundle = new Bundle();
                bundle.putString("destinationUid", uid);

                fragment.setArguments(bundle);
                getFragmentManager().beginTransaction()
                        .replace(R.id.main_content, fragment)
                        .commit();

                return true;
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 앨범에서 Profile Image 사진 선택시 호출 되는 부분분
        if (requestCode == PICK_PROFILE_FROM_ALBUM && resultCode == RESULT_OK) {

            String[] proj = {MediaStore.Images.Media.DATA};
            CursorLoader cursorLoader = new CursorLoader(this, data.getData(), proj, null, null, null);
            Cursor cursor = cursorLoader.loadInBackground();
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();

            //이미지 경로
            String photoPath = cursor.getString(column_index);

            //유저 Uid
            final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid(); //파일 업로드
            File f = new File(photoPath);
            FirebaseStorage
                    .getInstance()
                    .getReference()
                    .child("userProfileImages")
                    .child(uid)
                    .putFile(Uri.fromFile(f))
                    .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {

                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                            @SuppressWarnings("VisibleForTests")
                            String url = task.getResult().getDownloadUrl().toString();
                            Map<String, Object> map = new HashMap<String, Object>();
                            map.put(uid, url);
                            FirebaseDatabase.getInstance().getReference().child("profileImages").updateChildren(map);
                        }
                    });
        } else if (requestCode == PICK_IMAGE_FROM_ALBUM && resultCode == RESULT_OK) {

            binding.bottomNavigation.setSelectedItemId(R.id.action_account);
        }
    }

    public ActivityMainBinding getBinding() {

        return binding;
    }
}