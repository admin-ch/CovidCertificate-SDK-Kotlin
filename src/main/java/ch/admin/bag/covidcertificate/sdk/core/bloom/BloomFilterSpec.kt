package ch.admin.bag.covidcertificate.sdk.core.bloom


data class BloomFilterSpec( val filterBytes:String, val numBits: Int, val numHashes : Int, val seed : Long)