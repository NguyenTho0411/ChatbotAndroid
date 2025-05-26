package hcmute.edu.vn.bookappandroid.activities;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import hcmute.edu.vn.bookappandroid.MyApplication;
import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.adapters.AdapterPdfFavorite;
import hcmute.edu.vn.bookappandroid.databinding.ActivityFragmentProfileBinding;
import hcmute.edu.vn.bookappandroid.models.ModelPdf;

public class ProfileActivity extends AppCompatActivity {

    private ActivityFragmentProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    private ArrayList<ModelPdf> pdfArrayList;
    private AdapterPdfFavorite adapterPdfFavorite;

    private static final String TAG = "PROFILE_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFragmentProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        // Load info
        loadUserInfo();
        loadFavoriteBooks();

        // Mở màn hình settings (sửa hồ sơ)
        binding.settingsButton.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileEditActivity.class))
        );
    }

    private void loadUserInfo() {
        Log.d(TAG, "loadUserInfo: uid=" + firebaseAuth.getUid());

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String email = "" + snapshot.child("email").getValue();
                String name = "" + snapshot.child("name").getValue();
                String profileImage = "" + snapshot.child("profileImage").getValue();
                String timestamp = "" + snapshot.child("timestamp").getValue();

                String formattedDate = MyApplication.formatTimestamp(Long.parseLong(timestamp));

                binding.profileName.setText(name);
                binding.profileBio.setText("Member since: " + formattedDate);

                if (!profileImage.isEmpty()) {
                    Glide.with(ProfileActivity.this)
                            .load(profileImage)
                            .placeholder(R.drawable.ic_personal_gray)
                            .into(binding.profileImage);
                } else {
                    binding.profileImage.setImageResource(R.drawable.ic_personal_gray);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Load info failed", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loadFavoriteBooks() {
        pdfArrayList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Favorites")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        pdfArrayList.clear();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String bookId = "" + ds.child("bookId").getValue();
                            ModelPdf modelPdf = new ModelPdf();
                            modelPdf.setId(bookId);
                            pdfArrayList.add(modelPdf);
                        }

                        adapterPdfFavorite = new AdapterPdfFavorite(ProfileActivity.this, pdfArrayList);
                        binding.rvUserBooks.setLayoutManager(new GridLayoutManager(ProfileActivity.this, 2));
                        binding.rvUserBooks.setAdapter(adapterPdfFavorite);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ProfileActivity.this, "Load favorites failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
