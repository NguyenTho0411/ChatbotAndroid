package hcmute.edu.vn.bookappandroid.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import hcmute.edu.vn.bookappandroid.R;

public class CategoryAddActivity extends AppCompatActivity {

    private EditText etCategoryName, etCategoryDescription;
    private View selectedColorView;
    private String selectedColor = "#FF0000"; // Default red

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_add);

        // Ánh xạ view
        etCategoryName = findViewById(R.id.et_category_name);
        etCategoryDescription = findViewById(R.id.et_category_description);
        ImageView btnBack = findViewById(R.id.btn_back);

        Button btnCancel = findViewById(R.id.btn_cancel);
        Button btnSave = findViewById(R.id.btn_save_category);

        // Khởi tạo Firebase
        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        // Quay lại
        btnBack.setOnClickListener(v -> onBackPressed());
        btnCancel.setOnClickListener(v -> finish());

        // Lưu
        btnSave.setOnClickListener(v -> validateData());

        // Chọn màu
        int[] colorIds = new int[]{
                R.id.color_red, R.id.color_blue, R.id.color_green,
                R.id.color_orange, R.id.color_purple
        };

        for (int id : colorIds) {
            View colorView = findViewById(id);
            colorView.setOnClickListener(v -> {
                selectedColorView = v;

                int viewId = v.getId();

                if (viewId == R.id.color_red) {
                    selectedColor = "#FF0000";
                } else if (viewId == R.id.color_blue) {
                    selectedColor = "#2196F3";
                } else if (viewId == R.id.color_green) {
                    selectedColor = "#4CAF50";
                } else if (viewId == R.id.color_orange) {
                    selectedColor = "#FF9800";
                } else if (viewId == R.id.color_purple) {
                    selectedColor = "#9C27B0";
                }

                highlightSelectedColor();
            });

        }
    }

    private void highlightSelectedColor() {
        int[] colorIds = new int[]{
                R.id.color_red, R.id.color_blue, R.id.color_green,
                R.id.color_orange, R.id.color_purple
        };

        for (int id : colorIds) {
            View v = findViewById(id);
            v.setAlpha(0.5f);
        }

        if (selectedColorView != null) {
            selectedColorView.setAlpha(1.0f);
        }
    }

    private void validateData() {
        String categoryName = etCategoryName.getText().toString().trim();
        String description = etCategoryDescription.getText().toString().trim();

        if (TextUtils.isEmpty(categoryName)) {
            Toast.makeText(this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
        } else {
            addCategoryToFirebase(categoryName, description);
        }
    }

    private void addCategoryToFirebase(String name, String description) {
        progressDialog.setMessage("Đang thêm danh mục...");
        progressDialog.show();

        long timestamp = System.currentTimeMillis();
        String id = String.valueOf(timestamp);

        HashMap<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("category", name);
        data.put("description", description);
        data.put("color", selectedColor);
        data.put("timestamp", timestamp);
        data.put("uid", firebaseAuth.getUid());

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Categories");
        ref.child(id)
                .setValue(data)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Thêm danh mục thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
