package io.github.stack07142.instagram_firebase;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import io.github.stack07142.instagram_firebase.tabbar.AddPhotoActivity;
import io.github.stack07142.instagram_firebase.tabbar.HomeFragment;

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

                break;
            case R.id.action_home:

                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.mainactivity_framelayout, new HomeFragment())
                        .commit();
                break;
        }

        return false;
    }

}
