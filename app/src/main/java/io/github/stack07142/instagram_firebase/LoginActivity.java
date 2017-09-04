package io.github.stack07142.instagram_firebase;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;

import io.github.stack07142.instagram_firebase.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    // Data Binding
    private ActivityLoginBinding binding;

    // Sign In - Google
    private static final int RC_SIGN_IN = 9001; // Intent Request ID
    private FirebaseAuth auth;
    private GoogleApiClient mGoogleApiClient;

    // Sign In - Facebook
    private CallbackManager callbackManager;

    // Login Listener
    FirebaseAuth.AuthStateListener authListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        // Firebase 로그인 통합 관리하는 Object 만들기
        auth = FirebaseAuth.getInstance();

        // 구글 로그인 옵션 설정(요청 토큰, 요청 권한 등)
        GoogleSignInOptions gso = new GoogleSignInOptions
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
        binding.googleSignInButton.setOnClickListener(this);

        // Facebook Login
        callbackManager = CallbackManager.Factory.create();

        // Email 로그인
        binding.emailLoginButton.setOnClickListener(this);

        // Login Listener
        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();
                // User is signed in
                if (user != null) {

                    Toast.makeText(LoginActivity.this, getString(R.string.signin_complete), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);

                    finish();

                    binding.progressBar.setVisibility(View.GONE);
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();

        auth.addAuthStateListener(authListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (authListener != null) {

            auth.removeAuthStateListener(authListener);
        }
    }

    /*
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

                binding.progressBar.setVisibility(View.VISIBLE);

                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
                break;

            // 페이스북 로그인 버튼
            case R.id.facebook_login_button:

                LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"));
                LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

                    @Override
                    public void onSuccess(LoginResult loginResult) {

                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {

                        // TODO
                        binding.progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(FacebookException error) {

                        // TODO
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
                break;
            // 이메일 로그인 버튼
            case R.id.email_login_button:

                binding.progressBar.setVisibility(View.VISIBLE);

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

                            // TODO
                            binding.progressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }

    /*
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

                            // TODO
                            binding.progressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }

    /*
     * Email 로그인
     */

    //이메일 회원가입 및 로그인 메소드
    private void createAndLoginEmail() {
        auth.createUserWithEmailAndPassword(binding.emailEdittext.getText().toString(), binding.passwordEdittext.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            Toast.makeText(LoginActivity.this,
                                    getString(R.string.signup_complete), Toast.LENGTH_SHORT).show();
                        }
                        //회원가입 에러 - 1. 비밀번호가 6자리 이상 입력이 안됐을 경우
                        else if (binding.passwordEdittext.getText().toString().length() < 6) {

                            binding.progressBar.setVisibility(View.GONE);

                            Toast.makeText(LoginActivity.this,
                                    task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        //회원가입 에러 - 2. 아이디가 있을 경우이며 에러를 발생시키지 않고 바로 로그인 코드로 넘어간다
                        else {

                            signinEmail();
                        }
                    }
                });
    }

    //로그인 메소드
    private void signinEmail() {

        auth.signInWithEmailAndPassword(binding.emailEdittext.getText().toString(), binding.passwordEdittext.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        //로그인 에러 발생 - 1. 비밀번호가 틀릴 경우
                        if (!task.isSuccessful()) {

                            binding.progressBar.setVisibility(View.GONE);

                            Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}