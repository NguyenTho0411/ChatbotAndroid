package hcmute.edu.vn.bookappandroid.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.adapters.ChatAdapter;
import hcmute.edu.vn.bookappandroid.models.MessageModel;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatbotActivity extends AppCompatActivity {

    private EditText editTextMessage;
    private ImageView btnSend, btnImage;
    private RecyclerView recyclerView;
    private LinearLayout typingIndicator;
    private String apiKey;

    private ChatAdapter chatAdapter;
    private List<MessageModel> messageList;

    private static final int REQUEST_IMAGE_PICK = 100;
    private static final String TAG = "ChatbotActivity";

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        apiKey = getString(R.string.API_KEY);
        editTextMessage = findViewById(R.id.editTextMessage);
        btnSend = findViewById(R.id.btnSend);
        btnImage = findViewById(R.id.btnImage);
        recyclerView = findViewById(R.id.recyclerViewMessages);
        typingIndicator = findViewById(R.id.typingIndicator);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messageList, "USER");
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSend.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSend.setOnClickListener(v -> {
            String question = editTextMessage.getText().toString().trim();
            if (!question.isEmpty()) {
                sendUserMessage(question);
                fetchDataFromRealtimeDatabase(question);
                editTextMessage.setText("");
            }
        });

        btnImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                sendUserImage(imageUri.toString());
                recognizeTextFromImage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                sendBotMessage("Không thể xử lý ảnh.");
            }
        }
    }

    private void recognizeTextFromImage(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        typingIndicator.setVisibility(View.VISIBLE);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String recognizedText = visionText.getText();
                    if (!recognizedText.isEmpty()) {
                        String cleanedText = recognizedText.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " ").trim();
                        if (cleanedText.length() < 3) {
                            sendBotMessage("Không nhận diện đủ văn bản để tìm sách.");
                        } else {
                            searchBookFromText(cleanedText);
                        }
                    } else {
                        sendBotMessage("Không nhận diện được văn bản nào từ ảnh.");
                    }
                    typingIndicator.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    sendBotMessage("Lỗi khi nhận diện văn bản.");
                    typingIndicator.setVisibility(View.GONE);
                });
    }

    private void searchBookFromText(String recognizedText) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference booksRef = database.getReference("Books");

        booksRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> keywords = new ArrayList<>();
                for (String word : recognizedText.split(" ")) {
                    if (word.length() >= 4) keywords.add(word);
                }
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    String title = snapshot.child("title").getValue(String.class);
                    String pdfUrl = snapshot.child("url").getValue(String.class);
                    if (title != null && pdfUrl != null) {
                        for (String keyword : keywords) {
                            if (title.toLowerCase().contains(keyword)) {
                                // Lấy thêm thông tin chi tiết
                                String author = snapshot.child("author").getValue(String.class);
                                String description = snapshot.child("description").getValue(String.class);
                                Long views = snapshot.child("viewsCount").getValue(Long.class);
                                Long downloads = snapshot.child("downloadsCount").getValue(Long.class);

                                StringBuilder detail = new StringBuilder();
                                detail.append("📘 Tên sách: ").append(title).append("\n");
                                if (author != null) detail.append("✍️ Tác giả: ").append(author).append("\n");
                                if (description != null) detail.append("📖 Mô tả: ").append(description).append("\n");
                                if (views != null) detail.append("👁️ Lượt xem: ").append(views).append("\n");
                                if (downloads != null) detail.append("⬇️ Lượt tải: ").append(downloads);
                                sendBotMessage("Đây là sách như bạn cần tìm có trong app cua tui:");
                                renderPdfThumbnailFromFirebase(pdfUrl);
                                sendBotMessage(detail.toString());

                                return;
                            }
                        }
                    }
                }
                sendBotMessage("Không tìm thấy sách nào khớp.");
            } else {
                sendBotMessage("Lỗi khi truy vấn dữ liệu sách.");
            }
        });
    }

    private void renderPdfThumbnailFromFirebase(String pdfUrl) {
        StorageReference pdfRef = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        try {
            File localFile = File.createTempFile("tempPdf", ".pdf");
            pdfRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> renderPdfToBitmap(localFile))
                    .addOnFailureListener(e -> Log.e("PDF_DOWNLOAD", "Lỗi tải PDF", e));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderPdfToBitmap(File pdfFile) {
        try {
            ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(fileDescriptor);
            PdfRenderer.Page page = renderer.openPage(0);

            Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            renderer.close();

            // Lưu bitmap thành file ảnh tạm thời để gửi cho adapter
            File imageFile = new File(getCacheDir(), "pdf_thumbnail_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            Uri imageUri = Uri.fromFile(imageFile);  // đảm bảo là file:// URI
            sendBotImage(imageUri.toString());

        } catch (IOException e) {
            e.printStackTrace();
            sendBotMessage("Không thể hiển thị ảnh thumbnail.");
        }
    }


    private void sendUserMessage(String message) {
        messageList.add(new MessageModel("USER", "BOT", message, System.currentTimeMillis()));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void sendUserImage(String imageUri) {
        messageList.add(new MessageModel("USER", "BOT", null, System.currentTimeMillis(), imageUri));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void sendBotImage(String imageUri) {
        messageList.add(new MessageModel("BOT", "USER", null, System.currentTimeMillis(), imageUri));
        runOnUiThread(() -> {
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);
        });
    }

    private void sendBotMessage(String message) {
        if (message != null && !message.trim().isEmpty()) {
            messageList.add(new MessageModel("BOT", "USER", message.trim(), System.currentTimeMillis()));
            runOnUiThread(() -> {
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                recyclerView.scrollToPosition(messageList.size() - 1);
            });
        }
    }

    private void fetchDataFromRealtimeDatabase(String userQuestion) {
        typingIndicator.setVisibility(View.VISIBLE);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference booksRef = database.getReference("Books");
        DatabaseReference categoriesRef = database.getReference("Categories");

        // Step 1: Search for a matching book in the database
        booksRef.get().addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                List<String> keywords = new ArrayList<>();
                // Split user question into keywords (words with length >= 4)
                for (String word : userQuestion.toLowerCase().split(" ")) {
                    if (word.length() >= 4) keywords.add(word);
                }

                // Search for a book that matches the keywords
                for (DataSnapshot snapshot : task1.getResult().getChildren()) {
                    String title = snapshot.child("title").getValue(String.class);
                    String pdfUrl = snapshot.child("url").getValue(String.class);
                    if (title != null && pdfUrl != null) {
                        for (String keyword : keywords) {
                            if (title.toLowerCase().contains(keyword)) {
                                // Book found, extract details and render PDF thumbnail
                                String author = snapshot.child("author").getValue(String.class);
                                String description = snapshot.child("description").getValue(String.class);
                                Long views = snapshot.child("viewsCount").getValue(Long.class);
                                Long downloads = snapshot.child("downloadsCount").getValue(Long.class);

                                StringBuilder detail = new StringBuilder();
                                detail.append("📘 Tên sách: ").append(title).append("\n");
                                if (author != null) detail.append("✍️ Tác giả: ").append(author).append("\n");
                                if (description != null) detail.append("📖 Mô tả: ").append(description).append("\n");
                                if (views != null) detail.append("👁️ Lượt xem: ").append(views).append("\n");
                                if (downloads != null) detail.append("⬇️ Lượt tải: ").append(downloads);

                                runOnUiThread(() -> {
                                    typingIndicator.setVisibility(View.GONE);
                                    sendBotMessage("Đây là sách như bạn cần tìm có trong app của tui:");
                                    renderPdfThumbnailFromFirebase(pdfUrl);
                                    sendBotMessage(detail.toString());
                                });
                                return; // Exit after finding and displaying the first match
                            }
                        }
                    }
                }

                // Step 2: If no book is found, proceed with Gemini API
                List<String> contextList = new ArrayList<>();
                for (DataSnapshot book : task1.getResult().getChildren()) {
                    String title = book.child("title").getValue(String.class);
                    if (title != null) {
                        // Include only textual data, exclude URLs
                        contextList.add("Book title: " + title);
                    }
                }

                categoriesRef.get().addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful()) {
                        for (DataSnapshot cat : task2.getResult().getChildren()) {
                            contextList.add("Category: " + cat.toString());
                        }
                    }

                    StringBuilder contextBuilder = new StringBuilder();
                    for (String item : contextList) {
                        contextBuilder.append(item).append("\n\n");
                    }

                    String prompt = "Bạn là chatbot Kajima. Dựa vào dữ liệu dưới đây, hãy trả lời người dùng một cách tự nhiên:\n\n"
                            + contextBuilder + "\nCâu hỏi: " + userQuestion;

                    askGemini(prompt);
                });
            } else {
                runOnUiThread(() -> {
                    typingIndicator.setVisibility(View.GONE);
                    sendBotMessage("Lỗi khi truy vấn dữ liệu sách.");
                });
            }
        });
    }

    private void askGemini(String prompt) {
        try {
            JSONObject messageObj = new JSONObject();
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", prompt));
            messageObj.put("contents", new JSONArray()
                    .put(new JSONObject().put("role", "user").put("parts", parts)));

            RequestBody body = RequestBody.create(messageObj.toString(), MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        typingIndicator.setVisibility(View.GONE);
                        sendBotMessage("Lỗi khi kết nối Gemini.");
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> {
                            typingIndicator.setVisibility(View.GONE);
                            sendBotMessage("Gemini phản hồi lỗi.");
                        });
                        return;
                    }

                    String resBody = response.body().string();
                    try {
                        JSONObject json = new JSONObject(resBody);
                        String botReply = json.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        Pattern pattern = Pattern.compile("(https://firebasestorage\\.googleapis\\.com/[^\\s\\)\\]]+\\.pdf[^\\s\\)\\]]*)");
                        Matcher matcher = pattern.matcher(botReply);

                        String pdfUrl = null;
                        if (matcher.find()) {
                            pdfUrl = matcher.group(1);
                            botReply = botReply.replace(pdfUrl, "");
                        }

                        // Process **text** to uppercase and remove **
                        Pattern boldPattern = Pattern.compile("\\*\\*([^\\*\\*]+)\\*\\*");
                        Matcher boldMatcher = boldPattern.matcher(botReply);
                        StringBuilder processedText = new StringBuilder();
                        int lastEnd = 0;

                        while (boldMatcher.find()) {
                            // Append text before the match
                            processedText.append(botReply, lastEnd, boldMatcher.start());
                            // Append the text inside ** ** in uppercase
                            String boldText = boldMatcher.group(1);
                            processedText.append(boldText.toUpperCase());
                            lastEnd = boldMatcher.end();
                        }
                        // Append remaining text after the last match
                        processedText.append(botReply.substring(lastEnd));

                        String finalText = processedText.toString().trim();
                        String finalPdfUrl = pdfUrl;

                        runOnUiThread(() -> {
                            typingIndicator.setVisibility(View.GONE);

                            if (!finalText.isEmpty()) {
                                sendBotMessage(finalText);
                            }

                            if (finalPdfUrl != null) {
                                sendBotMessage("Đây là hình ảnh sách bạn cần tìm:");
                                renderPdfThumbnailFromFirebase(finalPdfUrl);
                            }
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            typingIndicator.setVisibility(View.GONE);
                            sendBotMessage("Không thể phân tích phản hồi.");
                        });
                    }
                }
            });

        } catch (Exception e) {
            typingIndicator.setVisibility(View.GONE);
            sendBotMessage("Lỗi tạo prompt.");
        }
    }
}
