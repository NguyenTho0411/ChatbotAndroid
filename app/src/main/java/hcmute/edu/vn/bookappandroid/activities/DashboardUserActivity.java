// DashboardUserActivity.java (FULL SAU KHI SỬA)
package hcmute.edu.vn.bookappandroid.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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

        setupRecyclerViews();
        loadAllBooksFromFirebase();
    }

    private void checkUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            binding.toolbar.setTitle("Not logged in");
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
}