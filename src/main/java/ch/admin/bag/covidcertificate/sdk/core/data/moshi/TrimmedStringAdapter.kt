package ch.admin.bag.covidcertificate.sdk.core.data.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * A Moshi json adapter that parses JSON into trimmed strings
 */
class TrimmedStringAdapter : JsonAdapter<String>() {
	override fun fromJson(reader: JsonReader): String? {
		if (reader.peek() == JsonReader.Token.NULL) {
			return reader.nextNull<String>()
		}
		return reader.nextString().trim()
	}

	override fun toJson(writer: JsonWriter, value: String?) {
		value?.let {
			writer.value(it)
		} ?: writer.nullValue()
	}

}