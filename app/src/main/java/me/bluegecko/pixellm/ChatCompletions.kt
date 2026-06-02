package me.bluegecko.pixellm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatCompletions(
    val messages: List<ChatCompletionMessageParam>,
    val model: String,
    val audio: ChatCompletionAudioParam? = null,
    @SerialName("frequency_penalty")
    val frequencyPenalty: Double? = null,
    @SerialName("function_call")
    val functionCall: JsonElement? = null,
    val functions: List<ChatCompletionFunctionParam>? = null,
    @SerialName("logit_bias")
    val logitBias: Map<String, Int>? = null,
    val logprobs: Boolean? = null,
    @SerialName("max_completion_tokens")
    val maxCompletionTokens: Int? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val metadata: Map<String, String>? = null,
    val modalities: List<String>? = null,
    val n: Int? = null,
    @SerialName("parallel_tool_calls")
    val parallelToolCalls: Boolean? = null,
    @SerialName("prediction")
    val prediction: ChatCompletionPredictionContentParam? = null,
    @SerialName("presence_penalty")
    val presencePenalty: Double? = null,
    @SerialName("reasoning_effort")
    val reasoningEffort: String? = null,
    @SerialName("response_format")
    val responseFormat: JsonElement? = null,
    val seed: Int? = null,
    @SerialName("service_tier")
    val serviceTier: String? = null,
    val stop: JsonElement? = null,
    val store: Boolean? = null,
    val stream: Boolean? = null,
    @SerialName("stream_options")
    val streamOptions: ChatCompletionStreamOptionsParam? = null,
    val temperature: Double? = null,
    @SerialName("tool_choice")
    val toolChoice: JsonElement? = null,
    val tools: List<ChatCompletionToolParam>? = null,
    @SerialName("top_logprobs")
    val topLogprobs: Int? = null,
    @SerialName("top_p")
    val topP: Double? = null,
    val user: String? = null,
    @SerialName("web_search_options")
    val webSearchOptions: JsonElement? = null,
)

@Serializable
data class ChatCompletionMessageParam(
    val role: String,
    val content: JsonElement? = null,
    val name: String? = null,
    @SerialName("tool_call_id")
    val toolCallId: String? = null,
    @SerialName("tool_calls")
    val toolCalls: List<ChatCompletionMessageToolCallParam>? = null,
    @SerialName("function_call")
    val functionCall: ChatCompletionMessageFunctionCallParam? = null,
    val refusal: String? = null,
    val audio: ChatCompletionAssistantMessageAudioParam? = null,
)

@Serializable
data class ChatCompletionMessageToolCallParam(
    val id: String,
    val type: String,
    val function: ChatCompletionFunctionCallParam,
)

@Serializable
data class ChatCompletionMessageFunctionCallParam(
    val name: String,
    val arguments: String,
)

@Serializable
data class ChatCompletionFunctionCallParam(
    val name: String,
    val arguments: String,
)

@Serializable
data class ChatCompletionAssistantMessageAudioParam(
    val id: String,
)

@Serializable
data class ChatCompletionAudioParam(
    val format: String,
    val voice: String,
)

@Serializable
data class ChatCompletionFunctionParam(
    val name: String,
    val description: String? = null,
    val parameters: JsonElement? = null,
)

@Serializable
data class ChatCompletionToolParam(
    val type: String,
    val function: ChatCompletionFunctionParam,
)

@Serializable
data class ChatCompletionPredictionContentParam(
    val type: String,
    val content: JsonElement,
)

@Serializable
data class ChatCompletionStreamOptionsParam(
    @SerialName("include_usage")
    val includeUsage: Boolean? = null,
)
