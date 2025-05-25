package hcmute.edu.vn.bookappandroid.filters;

import android.widget.Filter;
import java.util.ArrayList;
import hcmute.edu.vn.bookappandroid.adapters.AdapterPdfUser;
import hcmute.edu.vn.bookappandroid.models.ModelPdf;

public class FilterPdfUser extends Filter {

    private final ArrayList<ModelPdf> filterList;
    private final AdapterPdfUser adapterPdfUser;

    public FilterPdfUser(ArrayList<ModelPdf> filterList, AdapterPdfUser adapterPdfUser) {
        this.filterList = filterList;
        this.adapterPdfUser = adapterPdfUser;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();

        if (constraint != null && constraint.length() > 0) {
            constraint = constraint.toString().toUpperCase();
            ArrayList<ModelPdf> filteredModels = new ArrayList<>();

            for (ModelPdf pdf : filterList) {
                if (pdf.getTitle().toUpperCase().contains(constraint)) {
                    filteredModels.add(pdf);
                }
            }

            results.count = filteredModels.size();
            results.values = filteredModels;
        } else {
            results.count = filterList.size();
            results.values = filterList;
        }

        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        adapterPdfUser.updateList((ArrayList<ModelPdf>) results.values);
    }
}

