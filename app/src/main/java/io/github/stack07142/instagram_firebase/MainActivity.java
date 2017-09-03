package io.github.stack07142.instagram_firebase;

import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.HashMap;

import io.github.stack07142.instagram_firebase.tabbar.AddPhotoActivity;
import io.github.stack07142.instagram_firebase.tabbar.AlarmFragment;
import io.github.stack07142.instagram_firebase.tabbar.DetailViewFragment;
import io.github.stack07142.instagram_firebase.tabbar.GridFragment;
import io.github.stack07142.instagram_firebase.tabbar.UserFragment;

/**
 * 로그인 이후 작동되는 Activity
 */
public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private static final int PICK_FROM_ALBUM = 10;
    private String photoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // BottomNavigationView를 불러오는 코드
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.mainactivity_bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            //AddPhotoActivity를 호출 하는 코드
            case R.id.action_add_photo:
                startActivity(new Intent(MainActivity.this, AddPhotoActivity.class));

                return true;
            case R.id.action_home:
                getFragmentManager().beginTransaction()
                        .replace(R.id.mainactivity_framelayout, new DetailViewFragment())
                        .commit();
                return true;

            case R.id.action_search:

                Log.d("MainActivity", "action_search");
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.mainactivity_framelayout, new GridFragment())
                        .commit();
                return true;

            case R.id.action_account:

                Fragment fragment = new UserFragment();

                Bundle bundle = new Bundle();

                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                bundle.putString("destinationUid", uid);

                fragment.setArguments(bundle);

                getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, fragment).commit();

                return true;

            case R.id.action_favorite_alarm:

                getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new AlarmFragment()).commit();

                return true;
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 앨범에서 사진 선택시 호출 되는 부분분
        if (requestCode == PICK_FROM_ALBUM && resultCode == RESULT_OK) {

            String[] proj = {MediaStore.Images.Media.DATA};
            CursorLoader cursorLoader = new CursorLoader(this, data.getData(), proj, null, null, null);
            Cursor cursor = cursorLoader.loadInBackground();
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();

            //이미지 경로
            photoPath = cursor.getString(column_index);

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
                            HashMap<String,String> map = new HashMap<String, String>(); map.put(uid,url);
                            FirebaseDatabase.getInstance().getReference().child("profileImages").setValue(map); }
                    });
        }
    }
}