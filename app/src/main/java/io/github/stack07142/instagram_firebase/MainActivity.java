package io.github.stack07142.instagram_firebase;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;

import io.github.stack07142.instagram_firebase.tabbar.AddPhotoActivity;
import io.github.stack07142.instagram_firebase.tabbar.AlarmFragment;
import io.github.stack07142.instagram_firebase.tabbar.DetailViewFragment;
import io.github.stack07142.instagram_firebase.tabbar.GridFragment;
import io.github.stack07142.instagram_firebase.tabbar.UserFragment;

/**
 * 로그인 이후 작동되는 Activity
 */
public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

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

}
