package hcmute.edu.vn.bookappandroid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.facebook.login.LoginManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;

import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.adapters.AdapterPdfUser;
import hcmute.edu.vn.bookappandroid.databinding.ActivityDashboardUserBinding;
import hcmute.edu.vn.bookappandroid.models.ModelPdf;

public class DashboardUserActivity extends AppCompatActivity {

    private ActivityDashboardUserBinding binding;
    private FirebaseAuth firebaseAuth;

    private AdapterPdfUser adapterAll, adapterViewed, adapterDownloaded, adapterNew;
    private ArrayList<ModelPdf> listAll = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        checkUser();
        setSupportActionBar(binding.toolbar); // Gắn toolbar để hiển thị menu logout

        setupRecyclerViews();
        setupBottomNavigation();
        loadAllBooksFromFirebase();
    }

    private void checkUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            binding.toolbar.setTitle("Chưa đăng nhập");
        } else {
            binding.toolbar.setTitle(user.getEmail());
        }
    }

    private void setupRecyclerViews() {
        binding.rvRecommendedBooks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvMostViewedBooks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvMostDownloadedBooks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvNewReleases.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void loadAllBooksFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                listAll.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelPdf model = ds.getValue(ModelPdf.class);
                    listAll.add(model);
                }
                showFilteredLists();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(DashboardUserActivity.this, "Lỗi tải sách", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFilteredLists() {
        // Tất cả sách
        adapterAll = new AdapterPdfUser(this, new ArrayList<>(listAll));
        binding.rvRecommendedBooks.setAdapter(adapterAll);

        // Sách được xem nhiều nhất
        ArrayList<ModelPdf> viewedList = new ArrayList<>(listAll);
        Collections.sort(viewedList, (a, b) -> Long.compare(b.getViewsCount(), a.getViewsCount()));
        adapterViewed = new AdapterPdfUser(this, new ArrayList<>(viewedList.subList(0, Math.min(10, viewedList.size()))));
        binding.rvMostViewedBooks.setAdapter(adapterViewed);

        // Sách được tải nhiều nhất
        ArrayList<ModelPdf> downloadedList = new ArrayList<>(listAll);
        Collections.sort(downloadedList, (a, b) -> Long.compare(b.getDownloadsCount(), a.getDownloadsCount()));
        adapterDownloaded = new AdapterPdfUser(this, new ArrayList<>(downloadedList.subList(0, Math.min(10, downloadedList.size()))));
        binding.rvMostDownloadedBooks.setAdapter(adapterDownloaded);

        // Sách phát hành mới nhất
        ArrayList<ModelPdf> newList = new ArrayList<>(listAll);
        Collections.sort(newList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        adapterNew = new AdapterPdfUser(this, new ArrayList<>(newList.subList(0, Math.min(10, newList.size()))));
        binding.rvNewReleases.setAdapter(adapterNew);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = binding.bottomNavigation;

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Toast.makeText(this, "Trang chủ", Toast.LENGTH_SHORT).show();
                return true;

            } else if (id == R.id.nav_library) {
                Toast.makeText(this, "Thư viện", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, ChatbotActivity.class));
                return true;

            } else if (id == R.id.nav_search) {
                Toast.makeText(this, "Tìm kiếm", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, SearchActivity.class));
                return true;

            } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "Hồ sơ", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, ProfileActivity.class));
                return true;

            } else if (id == R.id.nav_chat) {
                Toast.makeText(this, "Hỗ trợ", Toast.LENGTH_SHORT).show();

                // Lấy UID của admin để mở ChatActivity
                FirebaseDatabase.getInstance().getReference("Users")
                        .orderByChild("userType").equalTo("admin")
                        .limitToFirst(1)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                for (DataSnapshot ds : snapshot.getChildren()) {
                                    String adminUid = ds.getKey();
                                    Intent intent = new Intent(DashboardUserActivity.this, ChatActivity.class);
                                    intent.putExtra("otherUid", adminUid);
                                    startActivity(intent);
                                    break;
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Toast.makeText(DashboardUserActivity.this, "Không thể mở chat", Toast.LENGTH_SHORT).show();
                            }
                        });

                return true;

            } else {
                return false;
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void logout() {
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut(); // Facebook
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }


}
