package hcmute.edu.vn.bookappandroid.activities;

import android.os.Bundle;
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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private ImageView btnSend;
    private RecyclerView recyclerView;
    private LinearLayout typingIndicator;
    private String apiKey;

    private ChatAdapter chatAdapter;
    private List<MessageModel> messageList;

    private FirebaseFirestore firestore;
    private static final String TAG = "ChatbotActivity";

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);
        apiKey = getString(R.string.API_KEY);

        firestore = FirebaseFirestore.getInstance();

        editTextMessage = findViewById(R.id.editTextMessage);
        btnSend = findViewById(R.id.btnSend);
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
    }

    private void sendUserMessage(String message) {
        messageList.add(new MessageModel("USER", "BOT", message, System.currentTimeMillis()));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void sendBotMessage(String message) {
        messageList.add(new MessageModel("BOT", "USER", message, System.currentTimeMillis()));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void fetchDataFromRealtimeDatabase(String userQuestion) {
        typingIndicator.setVisibility(View.VISIBLE);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference booksRef = database.getReference("Books");
        DatabaseReference categoriesRef = database.getReference("Categories");

        List<String> contextList = new ArrayList<>();

        booksRef.get().addOnCompleteListener(task1 -> {
            if (task1.isSuccessful() && task1.getResult().exists()) {
                for (DataSnapshot bookSnapshot : task1.getResult().getChildren()) {
                    Object bookData = bookSnapshot.getValue();
                    contextList.add("Book: " + bookData.toString());
                }
            }

            categoriesRef.get().addOnCompleteListener(task2 -> {
                if (task2.isSuccessful() && task2.getResult().exists()) {
                    for (DataSnapshot catSnapshot : task2.getResult().getChildren()) {
                        Object categoryData = catSnapshot.getValue();
                        contextList.add("Category: " + categoryData.toString());
                    }
                }

                StringBuilder contextBuilder = new StringBuilder();
                for (String item : contextList) {
                    contextBuilder.append(item).append("\n\n");
                }

                String prompt = "Bạn đang là chatbot Kajima, dựa vào dữ liệu dưới đây (không ghi dựa vao dữ liệu), trả lời như một chatbot tự nhiên: \n\n"
                        + contextBuilder + " Hãy trả lời câu hỏi: " + userQuestion;

                askGemini(prompt);
            });
        });
    }



    private void askGemini(String prompt) {
        try {
            JSONObject messageObj = new JSONObject();
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", prompt));

            messageObj.put("contents", new JSONArray()
                    .put(new JSONObject().put("role", "user").put("parts", parts)));

            RequestBody body = RequestBody.create(
                    messageObj.toString(),
                    MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" +apiKey)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        typingIndicator.setVisibility(View.GONE);
                        sendBotMessage("Có lỗi xảy ra khi kết nối Gemini.");
                    });
                }

                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
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
                        JSONArray candidates = json.getJSONArray("candidates");
                        String botReply = candidates.getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        runOnUiThread(() -> {
                            typingIndicator.setVisibility(View.GONE);
                            sendBotMessage(botReply.trim());
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            typingIndicator.setVisibility(View.GONE);
                            sendBotMessage("Không thể phân tích phản hồi từ Gemini.");
                        });
                    }
                }
            });

        } catch (Exception e) {
            typingIndicator.setVisibility(View.GONE);
            Log.e(TAG, "Lỗi tạo prompt hoặc gửi request", e);
            sendBotMessage("Lỗi xử lý câu hỏi.");
        }
    }
}
