package hcmute.edu.vn.bookappandroid.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.adapters.AdapterPdfUser;
import hcmute.edu.vn.bookappandroid.databinding.ActivitySearchBinding;
import hcmute.edu.vn.bookappandroid.models.ModelPdf;

public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private AdapterPdfUser bookAdapter;
    private List<ModelPdf> bookList;
    private DatabaseReference booksRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        booksRef = FirebaseDatabase.getInstance().getReference("Books");
        bookList = new ArrayList<>();
        bookAdapter = new AdapterPdfUser(this, new ArrayList<>(bookList));
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(bookAdapter);

        binding.editTextSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        binding.editTextSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = v.getText().toString().trim();
                if (!query.isEmpty()) performSearch(query);
                return true;
            }
            return false;
        });

        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 1) performSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnClear.setOnClickListener(v -> binding.editTextSearch.setText(""));

        // Optional: Show chipGroup if needed
        binding.btnFilter.setOnClickListener(v -> {
            if (binding.scrollViewFilters.getVisibility() == View.GONE) {
                binding.scrollViewFilters.setVisibility(View.VISIBLE);
            } else {
                binding.scrollViewFilters.setVisibility(View.GONE);
            }
        });

        binding.chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1 && checkedId != R.id.chipAll) {
                Chip chip = group.findViewById(checkedId);
                if (chip != null) {
                    String chipText = chip.getText().toString();
                    performSearch(chipText);
                }
            } else if (checkedId == R.id.chipAll) {
                performSearch("");
            }
        });
    }

    private void performSearch(String keyword) {
        binding.progressLoading.setVisibility(View.VISIBLE);
        booksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                bookList.clear();
                String keywordLower = keyword.toLowerCase();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelPdf model = ds.getValue(ModelPdf.class);
                    if (model != null && model.getTitle() != null &&
                            model.getTitle().toLowerCase().contains(keywordLower)) {
                        bookList.add(model);
                    }
                }

                bookAdapter.updateList(new ArrayList<>(bookList));
                binding.progressLoading.setVisibility(View.GONE);
                binding.layoutEmptyState.setVisibility(bookList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(SearchActivity.this, "Lỗi tìm kiếm: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                binding.progressLoading.setVisibility(View.GONE);
            }
        });
    }

}
