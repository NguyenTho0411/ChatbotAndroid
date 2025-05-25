package hcmute.edu.vn.bookappandroid;

import static hcmute.edu.vn.bookappandroid.Constants.MAX_BYTES_PDF;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class MyApplication extends Application {

    private static final String TAG_DOWNLOAD = "DOWNLOAD_TAG";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static final String formatTimestamp(long timestamp) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp);
        return DateFormat.format("dd/MM/yyyy", cal).toString();
    }

    public static void deleteBook(Context context, String bookId, String bookUrl, String bookTitle) {
        String TAG = "DELETE_BOOK_TAG";

        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Deleting " + bookTitle + "....");
        progressDialog.show();

        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
        storageReference.delete()
                .addOnSuccessListener(unused -> {
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books");
                    reference.child(bookId).removeValue()
                            .addOnSuccessListener(unused1 -> {
                                progressDialog.dismiss();
                                Toast.makeText(context, "Book Deleted successfully...", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public static void loadPdfSize(String pdfUrl, String pdfTitle, TextView sizeTv) {
        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        ref.getMetadata().addOnSuccessListener(storageMetadata -> {
            double bytes = storageMetadata.getSizeBytes();
            double kb = bytes / 1024;
            double mb = kb / 1024;
            String sizeText;
            if (mb >= 1) {
                sizeText = String.format("%.2f", mb) + " MB";
            } else if (kb >= 1) {
                sizeText = String.format("%.2f", kb) + " KB";
            } else {
                sizeText = String.format("%.2f", bytes) + " bytes";
            }

            if (sizeTv != null) {
                sizeTv.setText(sizeText);
            }

        });
    }

    public static void loadPdfPageCount(Context context, String pdfUrl, TextView pagesTv) {
        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        ref.getBytes(MAX_BYTES_PDF).addOnSuccessListener(bytes -> {
            PDFView dummyPdfView = new PDFView(context, null);
            dummyPdfView.fromBytes(bytes)
                    .enableSwipe(false)
                    .onLoad(nbPages -> {
                        pagesTv.setText(String.valueOf(nbPages));
                    })
                    .onError(t -> Log.e("PDF_PAGE_COUNT", "Load error: " + t.getMessage()))
                    .load();
        }).addOnFailureListener(e -> Log.e("PDF_PAGE_COUNT", "Failed: " + e.getMessage()));
    }

    public static void loadPdfFromUrlSinglePage(String pdfUrl, String pdfTitle, PDFView pdfView, ProgressBar progressBar, TextView pagesTv) {
        String TAG = "PDF_LOAD_SINGLE_TAG";

        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        ref.getBytes(MAX_BYTES_PDF)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Log.d(TAG, "onSuccess: " + pdfTitle + " successfully got the file");

                        pdfView.fromBytes(bytes)
                                .pages(0)
                                .spacing(0)
                                .swipeHorizontal(false)
                                .enableSwipe(false)
                                .onError(new OnErrorListener() {
                                    @Override
                                    public void onError(Throwable t) {
                                        progressBar.setVisibility(View.INVISIBLE);
                                        Log.d(TAG, "onError: " + t.getMessage());
                                    }
                                })
                                .onPageError(new OnPageErrorListener() {
                                    @Override
                                    public void onPageError(int page, Throwable t) {
                                        progressBar.setVisibility(View.INVISIBLE);
                                        Log.d(TAG, "onPageError: " + t.getMessage());
                                    }
                                })
                                .onLoad(new OnLoadCompleteListener() {
                                    @Override
                                    public void loadComplete(int nbPages) {
                                        progressBar.setVisibility(View.INVISIBLE);
                                        Log.d(TAG, "loadComplete: pdf loaded");

                                        if (pagesTv != null) {
                                            pagesTv.setText("" + nbPages);
                                        }
                                    }
                                })
                                .load();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.INVISIBLE);
                        Log.d(TAG, "onFailure: failed getting file from url due to " + e.getMessage());
                    }
                });
    }

    public static void loadCategory(String categoryId, TextView categoryTv) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Categories");
        ref.child(categoryId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String category = "" + snapshot.child("category").getValue();
                categoryTv.setText(category);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void incrementBookViewCount(String bookId) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String viewsCount = "" + snapshot.child("viewCount").getValue();
                if (viewsCount.equals("") || viewsCount.equals("null")) {
                    viewsCount = "0";
                }
                long newViewsCount = Long.parseLong(viewsCount) + 1;

                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("viewCount", newViewsCount);

                ref.child(bookId).updateChildren(hashMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void downloadBook(Context context, String bookId, String bookTitle, String bookUrl) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Downloading " + bookTitle + ".pdf...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
        storageReference.getBytes(MAX_BYTES_PDF)
                .addOnSuccessListener(bytes -> saveDownloadedBook(context, progressDialog, bytes, bookTitle + ".pdf", bookId))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Failed to download due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private static void saveDownloadedBook(Context context, ProgressDialog progressDialog, byte[] bytes, String nameWithExtension, String bookId) {
        try {
            File downloadsFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (downloadsFolder != null && !downloadsFolder.exists()) {
                downloadsFolder.mkdirs();
            }

            String filePath = downloadsFolder.getPath() + "/" + nameWithExtension;
            FileOutputStream out = new FileOutputStream(filePath);
            out.write(bytes);
            out.close();

            Toast.makeText(context, "Saved to: " + filePath, Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            incrementBookDownloadCount(bookId);
        } catch (Exception e) {
            Toast.makeText(context, "Failed saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }
    }


    private static void incrementBookDownloadCount(String bookId) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String downloadsCount = "" + snapshot.child("downloadsCount").getValue();
                if (downloadsCount.equals("") || downloadsCount.equals("null")) {
                    downloadsCount = "0";
                }
                long newDownloadsCount = Long.parseLong(downloadsCount) + 1;

                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("downloadsCount", newDownloadsCount);

                ref.child(bookId).updateChildren(hashMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void addToFavorite(Context context, String bookId) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(context, "You're not logged in", Toast.LENGTH_SHORT).show();
        } else {
            long timestamp = System.currentTimeMillis();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("bookId", bookId);
            hashMap.put("timestamp", timestamp);

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorites").child(bookId)
                    .setValue(hashMap)
                    .addOnSuccessListener(unused -> Toast.makeText(context, "Added to your favourites list...", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to add to favorite due to " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    public static void removeFromFavorite(Context context, String bookId) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(context, "You're not logged in", Toast.LENGTH_SHORT).show();
        } else {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorites").child(bookId)
                    .removeValue()
                    .addOnSuccessListener(unused -> Toast.makeText(context, "Removed from your favorites list...", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to remove from favorite due to " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}
