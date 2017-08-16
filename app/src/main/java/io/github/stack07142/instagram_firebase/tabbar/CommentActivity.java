package io.github.stack07142.instagram_firebase.tabbar;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import io.github.stack07142.instagram_firebase.R;
import io.github.stack07142.instagram_firebase.model.ContentDTO;

public class CommentActivity extends AppCompatActivity {

    private EditText message;
    private Button sendButton;

    private String imageUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        imageUid = getIntent().getStringExtra("imageUid");

        message = (EditText) findViewById(R.id.commentactivity_edittext_message);
        sendButton = (Button) findViewById(R.id.commentactivity_button_send);

        sendButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                ContentDTO.Comment comment = new ContentDTO.Comment();

                comment.userId = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                comment.comment = message.getText().toString();
                comment.uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                FirebaseDatabase.getInstance()
                        .getReference("images")
                        .child(imageUid)
                        .child("comments")
                        .push()
                        .setValue(comment);
            }
        });

    }
}
