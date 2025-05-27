package hcmute.edu.vn.bookappandroid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.databinding.ActivityDashboardAdminBinding;

public class DashboardAdminActivity extends AppCompatActivity {

    private ActivityDashboardAdminBinding binding;
    private FirebaseAuth firebaseAuth;

    // Các view từ layout tùy chỉnh
    private LinearLayout btnAddCategory, btnAddBook;
    private TextView tvSubTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        // Kiểm tra người dùng hiện tại
        checkUser();

        // Ánh xạ các view
        btnAddCategory = findViewById(R.id.btn_add_category);
        btnAddBook = findViewById(R.id.btn_add_book);
        tvSubTitle = findViewById(R.id.subTitleTv); // Nếu bạn muốn hiển thị email người dùng
        LinearLayout btnChatWithUser = findViewById(R.id.btn_chat_with_user);
        btnChatWithUser.setOnClickListener(v -> {
            startActivity(new Intent(DashboardAdminActivity.this, UserListActivity.class));
        });

        // Gán sự kiện
        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardAdminActivity.this, CategoryAddActivity.class));
            }
        });

        btnAddBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardAdminActivity.this, AddBookActivity.class));
            }
        });
    }

    private void checkUser() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            // Nếu layout bạn có TextView hiển thị email thì set ở đây
            if (tvSubTitle != null) {
                tvSubTitle.setText(firebaseUser.getEmail());
            }
        }
    }
}
