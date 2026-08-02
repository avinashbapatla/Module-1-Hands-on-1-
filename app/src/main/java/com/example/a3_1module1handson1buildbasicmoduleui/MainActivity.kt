package com.example.a3_1module1handson1buildbasicmoduleui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var promptEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var userQuestion: TextView
    private lateinit var aiResponse: TextView
    private lateinit var userLabel: TextView
    private lateinit var aiLabel: TextView
    private lateinit var chatScrollView: ScrollView

    // ==========================================
    // PASTE YOUR NEW GROQ API KEY BELOW
    // Example format: gsk_xxxxxxxxxxxxxxxxx
    // ==========================================
    private val groqApiKey = "PASTE_YOUR_GROQ_API_KEY_HERE"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Connect Kotlin variables to XML
        promptEditText = findViewById(R.id.promptEditText)
        sendButton = findViewById(R.id.sendButton)
        userQuestion = findViewById(R.id.userQuestion)
        aiResponse = findViewById(R.id.aiResponse)
        userLabel = findViewById(R.id.userLabel)
        aiLabel = findViewById(R.id.aiLabel)
        chatScrollView = findViewById(R.id.chatScrollView)

        // Suggestion cards
        val suggestion1 =
            findViewById<TextView>(R.id.suggestion1)

        val suggestion2 =
            findViewById<TextView>(R.id.suggestion2)

        val suggestion3 =
            findViewById<TextView>(R.id.suggestion3)

        // Suggestion 1
        suggestion1.setOnClickListener {

            promptEditText.setText(
                "Explain artificial intelligence in simple words"
            )

            promptEditText.setSelection(
                promptEditText.text.length
            )

            promptEditText.requestFocus()
        }

        // Suggestion 2
        suggestion2.setOnClickListener {

            promptEditText.setText(
                "Help me write clean Kotlin code"
            )

            promptEditText.setSelection(
                promptEditText.text.length
            )

            promptEditText.requestFocus()
        }

        // Suggestion 3
        suggestion3.setOnClickListener {

            promptEditText.setText(
                "Suggest a creative AI project idea for a college student"
            )

            promptEditText.setSelection(
                promptEditText.text.length
            )

            promptEditText.requestFocus()
        }

        // Send button
        sendButton.setOnClickListener {

            val prompt = promptEditText
                .text
                .toString()
                .trim()

            if (prompt.isEmpty()) {

                Toast.makeText(
                    this,
                    "Type a message first",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            sendPrompt(prompt)
        }
    }

    private fun sendPrompt(prompt: String) {

        // Check whether API key was added
        if (
            groqApiKey.isBlank() ||
            groqApiKey.contains("PASTE_YOUR")
        ) {
            Toast.makeText(
                this,
                "Please add your Groq API key",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // Show user question
        userQuestion.text = prompt
        userQuestion.visibility = View.VISIBLE
        userLabel.visibility = View.VISIBLE

        // Show AI loading state
        aiLabel.visibility = View.VISIBLE
        aiResponse.visibility = View.VISIBLE

        aiResponse.text = "Thinking..."

        // Clear input
        promptEditText.text.clear()

        // Disable button while request is running
        sendButton.isEnabled = false
        sendButton.alpha = 0.5f

        // Hide keyboard
        hideKeyboard()

        // Scroll to latest message
        scrollToBottom()

        // Send request
        callGroqAPI(prompt)
    }

    private fun callGroqAPI(prompt: String) {

        val url =
            "https://api.groq.com/openai/v1/chat/completions"

        // ==========================================
        // CREATE MESSAGES
        // ==========================================

        val messagesArray = JSONArray()

        // System message
        val systemMessage = JSONObject()

        systemMessage.put(
            "role",
            "system"
        )

        systemMessage.put(
            "content",
            """
            You are Pulse AI, a helpful AI assistant.
            
            Give clear and useful answers.
            Explain programming concepts simply.
            Format code clearly.
            Be concise unless the user asks for details.
            """.trimIndent()
        )

        messagesArray.put(systemMessage)

        // User message
        val userMessage = JSONObject()

        userMessage.put(
            "role",
            "user"
        )

        userMessage.put(
            "content",
            prompt
        )

        messagesArray.put(userMessage)

        // ==========================================
        // CREATE REQUEST JSON
        // ==========================================

        val requestJson = JSONObject()

        requestJson.put(
            "model",
            "llama-3.3-70b-versatile"
        )

        requestJson.put(
            "messages",
            messagesArray
        )

        requestJson.put(
            "temperature",
            0.7
        )

        requestJson.put(
            "max_completion_tokens",
            1024
        )

        // ==========================================
        // CREATE REQUEST BODY
        // ==========================================

        val mediaType =
            "application/json; charset=utf-8"
                .toMediaType()

        val requestBody =
            requestJson
                .toString()
                .toRequestBody(mediaType)

        // ==========================================
        // BUILD HTTP REQUEST
        // ==========================================

        val request = Request.Builder()
            .url(url)

            .addHeader(
                "Authorization",
                "Bearer $groqApiKey"
            )

            .addHeader(
                "Content-Type",
                "application/json"
            )

            .post(requestBody)

            .build()

        // ==========================================
        // CALL GROQ API
        // ==========================================

        client
            .newCall(request)
            .enqueue(

                object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                        runOnUiThread {

                            aiResponse.text =
                                """
                                Unable to connect to Pulse AI.
                                
                                Please check your internet connection and try again.
                                """.trimIndent()

                            enableSendButton()

                            scrollToBottom()
                        }
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {

                        val responseBody =
                            response.body?.string()

                        // API returned an error
                        if (!response.isSuccessful) {

                            runOnUiThread {

                                aiResponse.text =
                                    getFriendlyError(
                                        response.code,
                                        responseBody
                                    )

                                enableSendButton()

                                scrollToBottom()
                            }

                            return
                        }

                        try {

                            // Parse JSON response
                            val jsonResponse =
                                JSONObject(
                                    responseBody ?: ""
                                )

                            val choices =
                                jsonResponse
                                    .getJSONArray("choices")

                            val firstChoice =
                                choices
                                    .getJSONObject(0)

                            val message =
                                firstChoice
                                    .getJSONObject("message")

                            val answer =
                                message
                                    .getString("content")

                            // Display AI response
                            runOnUiThread {

                                aiResponse.text =
                                    answer.trim()

                                enableSendButton()

                                scrollToBottom()
                            }

                        } catch (e: Exception) {

                            runOnUiThread {

                                aiResponse.text =
                                    """
                                    Pulse AI received a response but could not process it correctly.
                                    
                                    Please try again.
                                    """.trimIndent()

                                enableSendButton()

                                scrollToBottom()
                            }
                        }
                    }
                }
            )
    }

    private fun getFriendlyError(
        errorCode: Int,
        responseBody: String?
    ): String {

        return when (errorCode) {

            400 ->
                """
                The AI request could not be processed.
                
                Please try another message.
                """.trimIndent()

            401 ->
                """
                API authentication failed.
                
                Please create a new Groq API key and check that it was copied correctly.
                """.trimIndent()

            403 ->
                """
                Access to the AI service was denied.
                
                Please check your Groq API key permissions.
                """.trimIndent()

            404 ->
                """
                The selected AI model is currently unavailable.
                
                Please check the model name.
                """.trimIndent()

            429 ->
                """
                The API usage limit has been reached.
                
                Please wait for a while and try again.
                """.trimIndent()

            500, 502, 503, 504 ->
                """
                The AI service is temporarily unavailable.
                
                Please try again later.
                """.trimIndent()

            else ->
                """
                Something went wrong while contacting the AI service.
                
                Error code: $errorCode
                """.trimIndent()
        }
    }

    private fun enableSendButton() {

        sendButton.isEnabled = true
        sendButton.alpha = 1f
    }

    private fun hideKeyboard() {

        val inputMethodManager =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        inputMethodManager.hideSoftInputFromWindow(
            promptEditText.windowToken,
            0
        )
    }

    private fun scrollToBottom() {

        chatScrollView.post {

            chatScrollView.fullScroll(
                View.FOCUS_DOWN
            )
        }
    }
}