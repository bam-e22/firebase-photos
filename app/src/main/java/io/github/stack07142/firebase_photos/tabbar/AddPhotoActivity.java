package io.github.stack07142.firebase_photos.tabbar;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.github.stack07142.firebase_photos.R;
import io.github.stack07142.firebase_photos.databinding.ActivityAddPhotoBinding;
import io.github.stack07142.firebase_photos.model.ContentDTO;

import static io.github.stack07142.firebase_photos.util.StatusCode.PICK_IMAGE_FROM_ALBUM;

public class AddPhotoActivity extends AppCompatActivity implements View.OnClickListener {

    // Data Binding
    private ActivityAddPhotoBinding binding;

    private String photoUrl;

    // Firebase Storage, Database, Auth
    private FirebaseStorage firebaseStorage;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_photo);

        //ImageView Button EditText 찾아오고 버튼 세팅하기
        binding.addphotoBtnUpload.setOnClickListener(this);

        //권한 요청 하는 부분
        ActivityCompat.requestPermissions
                (this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        //앨범 오픈
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM);

        // Firebase storage
        firebaseStorage = FirebaseStorage.getInstance();

        // Firebase Database
        firebaseDatabase = FirebaseDatabase.getInstance();

        // Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        binding.addphotoImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // 앨범에서 사진 선택시 호출 되는 부분
        if (requestCode == PICK_IMAGE_FROM_ALBUM && resultCode == RESULT_OK) {

            String[] proj = {MediaStore.Images.Media.DATA};
            CursorLoader cursorLoader = new CursorLoader(this, data.getData(), proj,
                    null, null, null);
            Cursor cursor = cursorLoader.loadInBackground();
            int column_index =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();

            //이미지 경로
            photoUrl = cursor.getString(column_index);

            //이미지뷰에 이미지 세팅
            binding.addphotoImage.setImageURI(data.getData());
        }
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.addphoto_btn_upload && photoUrl != null) {

            binding.progressBar.setVisibility(View.VISIBLE);

            File file = new File(photoUrl);
            Uri contentUri = Uri.fromFile(file);
            StorageReference storageRef =
                    firebaseStorage.getReferenceFromUrl("gs://fir-project-abb40.appspot.com/").child("images").child(contentUri.getLastPathSegment());
            UploadTask uploadTask = storageRef.putFile(contentUri);
            uploadTask
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            binding.progressBar.setVisibility(View.GONE);

                            Toast.makeText(AddPhotoActivity.this, getString(R.string.upload_success),
                                    Toast.LENGTH_SHORT).show();

                            @SuppressWarnings("VisibleForTests")
                            Uri uri = taskSnapshot.getDownloadUrl();
                            //디비에 바인딩 할 위치 생성 및 컬렉션(테이블)에 데이터 집합 생성
                            DatabaseReference images = firebaseDatabase.getReference().child("images").push();

                            //시간 생성
                            Date date = new Date();
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            ContentDTO contentDTO = new ContentDTO();

                            //이미지 주소
                            contentDTO.imageUrl = uri.toString();
                            //유저의 UID
                            contentDTO.uid = firebaseAuth.getCurrentUser().getUid();
                            //게시물의 설명
                            contentDTO.explain = binding.addphotoEditExplain.getText().toString();
                            //유저의 아이디
                            contentDTO.userId = firebaseAuth.getCurrentUser().getEmail();
                            //게시물 업로드 시간
                            contentDTO.timestamp = simpleDateFormat.format(date);

                            //게시물을 데이터를 생성 및 엑티비티 종료
                            images.setValue(contentDTO);

                            setResult(RESULT_OK);
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            binding.progressBar.setVisibility(View.GONE);

                            Toast.makeText(AddPhotoActivity.this, getString(R.string.upload_fail),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
