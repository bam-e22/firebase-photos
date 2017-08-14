package io.github.stack07142.instagram_firebase;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private final String TAG = LoginActivity.class.getSimpleName();

    // Google Sign In
    private static final int RC_SIGN_IN = 1000; // Intent Request ID
    private FirebaseAuth auth;
    private GoogleSignInOptions gso;
    private GoogleApiClient mGoogleApiClient;
    private SignInButton googleSignInButton;

    // Facebook Sign In
    private LoginButton facebookSignInButton;
    private CallbackManager callbackManager;

    // Email Sign In
    private EditText email;
    private EditText password;
    private Button emailLoginButton;

    // Login Listener
    FirebaseAuth.AuthStateListener mAuthListener;

    // Progressbar
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        // Firebase 로그인 통합 관리하는 Object 만들기
        auth = FirebaseAuth.getInstance();

        // 구글 로그인 옵션 설정(요청 토큰, 요청 권한 등)
        gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // 구글 로그인 API 만들기
        mGoogleApiClient =
                new GoogleApiClient.Builder(this)
                        .enableAutoManage(this, this)
                        .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                        .build();

        // 구글 로그인 버튼 가져오기
        googleSignInButton = (SignInButton) findViewById(R.id.google_sign_in_button);
        googleSignInButton.setOnClickListener(this);

        // Facebook Login 추가
        callbackManager = CallbackManager.Factory.create();
        facebookSignInButton = (LoginButton) findViewById(R.id.facebook_login_button);
        facebookSignInButton.setReadPermissions("email", "public_profile");
        facebookSignInButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {

                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });

        // Email 로그인
        email = (EditText) findViewById(R.id.email_edittext);
        password = (EditText) findViewById(R.id.password_edittext);
        emailLoginButton = (Button) findViewById(R.id.email_login_button);
        emailLoginButton.setOnClickListener(this);

        // Login Listener
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();
                // User is signed in
                if (user != null) {

                    Toast.makeText(LoginActivity.this, "로그인 완료 됬습니다.", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);

                    finish();

                    progressBar.setVisibility(View.GONE);
                }
                // User is signed out
                else {

                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();

        auth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mAuthListener != null) {

            auth.removeAuthStateListener(mAuthListener);
        }
    }

    /**
     * Google Sign In
     */

    /**
     * GoogleApiClient.OnConnectionFailedListener
     * Provides callbacks for scenarios that result in a failed attempt to connect the client to the service.
     * See ConnectionResult for a list of error codes and suggestions for resolution.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        // Firebase 접속 실패 시 호출
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            // 구글 로그인 버튼
            case R.id.google_sign_in_button:

                progressBar.setVisibility(View.VISIBLE);

                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
                break;

            // 이메일 로그인 버튼
            case R.id.email_login_button:

                progressBar.setVisibility(View.VISIBLE);

                createAndLoginEmail();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Facebook SDK로 값 넘겨주기
        callbackManager.onActivityResult(requestCode, resultCode, data);

        // 구글에서 승인된 정보를 가지고 오기
        if (requestCode == RC_SIGN_IN) {

            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            if (result.isSuccess()) {

                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (!task.isSuccessful()) {

                            // 에러 발생 시 호출
                        }
                    }
                });
    }

    /**
     * Facebook Sign In
     */

    // Facebook 토큰을 Firebase로 넘겨주는 코드
    private void handleFacebookAccessToken(AccessToken token) {

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());

        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (!task.isSuccessful()) {

                            // 에러 발생 시
                        }
                    }
                });
    }

    /**
     * Email 로그인
     */

    //이메일 회원가입 및 로그인 메소드
    private void createAndLoginEmail() {
        auth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            Log.d(TAG, "createAndLoginEmail - successful");

                            Toast.makeText(LoginActivity.this,
                                    "회원가입에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                        }
                        //회원가입 에러 - 1. 비밀번호가 6자리 이상 입력이 안됬을 경우
                        else if (password.getText().toString().length() < 6) {

                            Log.d(TAG, "createAndLoginEmail - 비밀번호 6자리 이상 입력안된경우");
                            Toast.makeText(LoginActivity.this,
                                    task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        //회원가입 에러 - 2. 아이디가 있을 경우이며 에러를 발생시키지 않고 바로 로그인 코드로 넘어간다
                        else {

                            Log.d(TAG, "createAndLoginEmail - 아이디 있는 경우");
                            signinEmail();
                        }
                    }
                });
    }

    //로그인 메소드
    private void signinEmail() {

        auth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            Log.d(TAG, "signInEmail - successful");
                        }
                        //로그인 에러 발생 - 1. 비밀번호가 틀릴 경우
                        else {

                            Log.d(TAG, "signInEmail - 비밀번호 틀린 경우");
                            Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}