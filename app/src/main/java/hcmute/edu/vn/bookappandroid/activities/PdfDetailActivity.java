package hcmute.edu.vn.bookappandroid.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;

import hcmute.edu.vn.bookappandroid.MyApplication;
import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.adapters.AdapterComment;
import hcmute.edu.vn.bookappandroid.databinding.ActivityDetailBookBinding;
import hcmute.edu.vn.bookappandroid.databinding.DialogCommentAddBinding;
import hcmute.edu.vn.bookappandroid.models.ModelComment;

public class PdfDetailActivity extends AppCompatActivity {

    private ActivityDetailBookBinding binding;
    private FirebaseAuth firebaseAuth;

    private String bookId, bookTitle, bookUrl;
    private boolean isInMyFavorite = false;
    private ProgressDialog progressDialog;
    private ArrayList<ModelComment> commentArrayList;
    private AdapterComment adapterComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBookBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bookId = getIntent().getStringExtra("bookId");

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        if (firebaseAuth.getCurrentUser() != null) checkIsFavorite();

        loadBookDetails();
        loadComments();
        MyApplication.incrementBookViewCount(bookId);

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.btnRead.setOnClickListener(v -> {
            Intent intent = new Intent(PdfDetailActivity.this, PdfViewActivity.class);
            intent.putExtra("bookId", bookId);
            startActivity(intent);
        });

        binding.btnDownload.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                MyApplication.downloadBook(this, bookId, bookTitle, bookUrl);
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        });

        binding.fabFavorite.setOnClickListener(v -> {
            if (firebaseAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            } else {
                if (isInMyFavorite) {
                    MyApplication.removeFromFavorite(this, bookId);
                } else {
                    MyApplication.addToFavorite(this, bookId);
                }
            }
        });

        binding.addCommentBtn.setOnClickListener(v -> {
            if (firebaseAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            } else {
                showAddCommentDialog();
            }
        });
    }

    private void loadComments() {
        commentArrayList = new ArrayList<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId).child("Comments").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentArrayList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelComment model = ds.getValue(ModelComment.class);
                    commentArrayList.add(model);
                }
                adapterComment = new AdapterComment(PdfDetailActivity.this, commentArrayList);
                binding.commentsRv.setAdapter(adapterComment);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showAddCommentDialog() {
        DialogCommentAddBinding commentBinding = DialogCommentAddBinding.inflate(LayoutInflater.from(this));
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);
        builder.setView(commentBinding.getRoot());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        commentBinding.backBtn.setOnClickListener(v -> alertDialog.dismiss());
        commentBinding.submitBtn.setOnClickListener(v -> {
            String comment = commentBinding.commentEt.getText().toString().trim();
            if (TextUtils.isEmpty(comment)) {
                Toast.makeText(this, "Nhập bình luận", Toast.LENGTH_SHORT).show();
            } else {
                alertDialog.dismiss();
                submitComment(comment);
            }
        });
    }

    private void submitComment(String comment) {
        progressDialog.setMessage("Đang gửi bình luận...");
        progressDialog.show();

        String timestamp = "" + System.currentTimeMillis();
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", timestamp);
        map.put("bookId", bookId);
        map.put("timestamp", timestamp);
        map.put("uid", firebaseAuth.getUid());
        map.put("comment", comment);

        FirebaseDatabase.getInstance().getReference("Books").child(bookId)
                .child("Comments").child(timestamp).setValue(map)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã thêm bình luận", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                });
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    MyApplication.downloadBook(this, bookId, bookTitle, bookUrl);
                } else {
                    Toast.makeText(this, "Bị từ chối quyền", Toast.LENGTH_SHORT).show();
                }
            });

    private void loadBookDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookTitle = "" + snapshot.child("title").getValue();
                String description = "" + snapshot.child("description").getValue();
                String categoryId = "" + snapshot.child("categoryId").getValue();
                String views = "" + snapshot.child("viewCount").getValue();
                String downloads = "" + snapshot.child("downloadsCount").getValue();
                bookUrl = "" + snapshot.child("url").getValue();
                String timestamp = "" + snapshot.child("timestamp").getValue();
                String publisher = "" + snapshot.child("publisher").getValue();
                String publishDate = "" + snapshot.child("publishDate").getValue();
                String pages = "" + snapshot.child("pages").getValue();
                String isbn = "" + snapshot.child("isbn").getValue();
                String language = "" + snapshot.child("language").getValue();
                String author = "" + snapshot.child("author").getValue();

                String date = MyApplication.formatTimestamp(Long.parseLong(timestamp));

                binding.bookTitle.setText(bookTitle);
                binding.bookDescription.setText(description);
                binding.bookPublisher.setText(publisher);
                binding.bookPublishDate.setText(publishDate);
                binding.bookPages.setText(pages);
                binding.bookIsbn.setText(isbn);
                binding.bookLanguage.setText(language);
                binding.bookAuthor.setText(author);
                binding.viewsTv.setText(views.replace("null", "0"));
                binding.downloadsTv.setText(downloads.replace("null", "0"));

                MyApplication.loadCategory(categoryId, binding.categoryTv);
                MyApplication.loadPdfFromUrlSinglePage(bookUrl, bookTitle, binding.pdfView, binding.progressBar, null);
                MyApplication.loadPdfSize(bookUrl, bookTitle, null);
                MyApplication.loadPdfPageCount(PdfDetailActivity.this, bookUrl, null);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkIsFavorite() {
        FirebaseDatabase.getInstance().getReference("Users")
                .child(firebaseAuth.getUid()).child("Favorites").child(bookId)
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isInMyFavorite = snapshot.exists();
                        if (isInMyFavorite) {
                            binding.fabFavorite.setImageResource(R.drawable.ic_favorite_white);
                        } else {
                            binding.fabFavorite.setImageResource(R.drawable.ic_favorite_border);
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
