package hcmute.edu.vn.bookappandroid.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;

import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    private String email = "", password = "";

    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;

    private CallbackManager callbackManager; // Facebook

    private final ActivityResultLauncher<IntentSenderRequest> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    try {
                        SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(result.getData());
                        String idToken = credential.getGoogleIdToken();
                        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                        firebaseAuth.signInWithCredential(firebaseCredential)
                                .addOnSuccessListener(authResult -> checkUser())
                                .addOnFailureListener(e -> Toast.makeText(this, "Google login failed!", Toast.LENGTH_SHORT).show());
                    } catch (Exception e) {
                        Toast.makeText(this, "Google login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        callbackManager = CallbackManager.Factory.create(); // Facebook

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        binding.btnLogin.setOnClickListener(v -> validateData());
        binding.tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        binding.tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        binding.btnGoogle.setOnClickListener(v -> googleLogin());
        binding.btnFacebook.setOnClickListener(v -> facebookLogin());
    }

    private void validateData() {
        email = binding.etEmail.getText().toString().trim();
        password = binding.etPassword.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ!", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu!", Toast.LENGTH_SHORT).show();
        } else {
            loginUser();
        }
    }

    private void loginUser() {
        progressDialog.setMessage("Đang đăng nhập...");
        progressDialog.show();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> checkUser())
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkUser() {
        progressDialog.setMessage("Đang kiểm tra vai trò...");
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        FirebaseDatabase.getInstance().getReference("Users")
                .child(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    progressDialog.dismiss();
                    String userType = "" + snapshot.child("userType").getValue();
                    if ("admin".equals(userType)) {
                        startActivity(new Intent(LoginActivity.this, DashboardAdminActivity.class));
                    } else {
                        startActivity(new Intent(LoginActivity.this, DashboardUserActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Không thể kiểm tra loại người dùng!", Toast.LENGTH_SHORT).show();
                });
    }

    private void googleLogin() {
        oneTapClient = Identity.getSignInClient(this);
        signInRequest = new BeginSignInRequest.Builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(getString(R.string.default_web_client_id))
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();

        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(result -> {
                    IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(
                            result.getPendingIntent().getIntentSender()).build();
                    googleLauncher.launch(intentSenderRequest);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Google Sign-In Failed!", Toast.LENGTH_SHORT).show());
    }

    private void facebookLogin() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }
            @Override public void onCancel() {
                Toast.makeText(LoginActivity.this, "Đã hủy đăng nhập Facebook", Toast.LENGTH_SHORT).show();
            }
            @Override public void onError(FacebookException error) {
                Toast.makeText(LoginActivity.this, "Lỗi Facebook: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> checkUser())
                .addOnFailureListener(e -> Toast.makeText(this, "Facebook login failed!", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data); // Facebook
        super.onActivityResult(requestCode, resultCode, data);
    }
}
