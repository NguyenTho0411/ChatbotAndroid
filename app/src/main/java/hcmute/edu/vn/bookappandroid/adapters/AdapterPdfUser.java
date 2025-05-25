package hcmute.edu.vn.bookappandroid.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import hcmute.edu.vn.bookappandroid.MyApplication;
import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.activities.PdfDetailActivity;
import hcmute.edu.vn.bookappandroid.databinding.ItemBookBinding;
import hcmute.edu.vn.bookappandroid.filters.FilterPdfUser;
import hcmute.edu.vn.bookappandroid.models.ModelPdf;

public class AdapterPdfUser extends RecyclerView.Adapter<AdapterPdfUser.ViewHolder> implements Filterable {

    private final Context context;
    public ArrayList<ModelPdf> pdfList;
    private final ArrayList<ModelPdf> filterList;
    private FilterPdfUser filter;

    public AdapterPdfUser(Context context, ArrayList<ModelPdf> pdfList) {
        this.context = context;
        this.pdfList = pdfList;
        this.filterList = new ArrayList<>(pdfList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBookBinding binding = ItemBookBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ModelPdf model = pdfList.get(position);

        holder.binding.tvBookTitle.setText(model.getTitle());
        holder.binding.tvBookAuthor.setText(model.getDescription());

        // Load trang đầu tiên của PDF như ảnh thumbnail
        MyApplication.loadPdfFromUrlSinglePage(
                model.getUrl(),
                model.getTitle(),
                holder.binding.pdfView,
                holder.binding.progressBar,
                null
        );


        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PdfDetailActivity.class);
            intent.putExtra("bookId", model.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return pdfList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemBookBinding binding;

        public ViewHolder(@NonNull ItemBookBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new FilterPdfUser(filterList, this);
        }
        return filter;
    }

    public void updateList(ArrayList<ModelPdf> newList) {
        this.pdfList = newList;
        notifyDataSetChanged();
    }
}
