package com.example.data

import com.example.network.TrainApiService
import com.example.network.TrainResponse
import kotlinx.coroutines.flow.Flow

data class TrainCatalogItem(
    val trainNo: String,
    val trainName: String,
    val source: String,
    val destination: String,
    val fromTime: String,
    val toTime: String,
    val duration: String,
    val runsOn: String,
    val stations: List<String>
)

private val TRAIN_CATALOG = listOf(
    TrainCatalogItem(
        trainNo = "12301",
        trainName = "Howrah Rajdhani Express",
        source = "Howrah Jn (HWH)",
        destination = "New Delhi (NDLS)",
        fromTime = "16:55",
        toTime = "09:55",
        duration = "17h 00m",
        runsOn = "Mon, Tue, Wed, Thu, Fri, Sat",
        stations = listOf("HWH", "Howrah", "ASN", "Asansol", "DHN", "Dhanbad", "GAYA", "Gaya", "PRYJ", "Prayagraj", "CNB", "Kanpur", "NDLS", "New Delhi")
    ),
    TrainCatalogItem(
        trainNo = "12260",
        trainName = "New Delhi - Howrah Duronto Express",
        source = "New Delhi (NDLS)",
        destination = "Howrah Jn (HWH)",
        fromTime = "12:20",
        toTime = "06:15",
        duration = "17h 55m",
        runsOn = "Daily",
        stations = listOf("NDLS", "New Delhi", "CNB", "Kanpur", "DDU", "Mughalsarai", "PNBE", "Patna", "HWH", "Howrah")
    ),
    TrainCatalogItem(
        trainNo = "12626",
        trainName = "Kerala Express",
        source = "New Delhi (NDLS)",
        destination = "Trivandrum Central (TVC)",
        fromTime = "20:10",
        toTime = "21:50",
        duration = "49h 40m",
        runsOn = "Daily",
        stations = listOf("NDLS", "New Delhi", "GWL", "Gwalior", "BPL", "Bhopal", "NGP", "Nagpur", "BZA", "Vijayawada", "TPTY", "Tirupati", "CBE", "Coimbatore", "TVC", "Trivandrum")
    ),
    TrainCatalogItem(
        trainNo = "11040",
        trainName = "Maharashtra Express",
        source = "Gondia Jn (G)",
        destination = "Kolhapur (KOP)",
        fromTime = "08:15",
        toTime = "12:25",
        duration = "28h 10m",
        runsOn = "Daily",
        stations = listOf("G", "Gondia", "TMR", "Tumsar", "BRD", "Bhandara", "KP", "Kamptee", "NGP", "Nagpur", "WR", "Wardha", "BD", "Badnera", "AK", "Akola", "BSL", "Bhusaval", "MMR", "Manmad", "PUNE", "Pune", "MRJ", "Miraj", "KOP", "Kolhapur")
    ),
    TrainCatalogItem(
        trainNo = "12102",
        trainName = "Jnaneswari Super Deluxe Express",
        source = "Lokmanya Tilak Terminus (LTT)",
        destination = "Shalimar (SHM)",
        fromTime = "20:35",
        toTime = "03:40",
        duration = "31h 05m",
        runsOn = "Mon, Wed, Thu, Sat",
        stations = listOf("LTT", "Mumbai", "KYN", "Kalyan", "BSL", "Bhusaval", "AK", "Akola", "BD", "Badnera", "NGP", "Nagpur", "G", "Gondia", "R", "Raipur", "BSP", "Bilaspur", "TATA", "Tatanagar", "SHM", "Shalimar")
    ),
    TrainCatalogItem(
        trainNo = "12951",
        trainName = "Mumbai Rajdhani Express",
        source = "Mumbai Central (MMCT)",
        destination = "New Delhi (NDLS)",
        fromTime = "17:00",
        toTime = "08:32",
        duration = "15h 32m",
        runsOn = "Daily",
        stations = listOf("MMCT", "Mumbai", "BVI", "Borivali", "ST", "Surat", "BRC", "Vadodara", "RTM", "Ratlam", "KOTA", "Kota", "NDLS", "New Delhi")
    ),
    TrainCatalogItem(
        trainNo = "12002",
        trainName = "New Delhi Shatabdi Express",
        source = "New Delhi (NDLS)",
        destination = "Habibganj (RKMP)",
        fromTime = "06:00",
        toTime = "14:40",
        duration = "8h 40m",
        runsOn = "Daily",
        stations = listOf("NDLS", "New Delhi", "MTJ", "Mathura", "AGC", "Agra", "GWL", "Gwalior", "VGLJ", "Jhansi", "Lalitpur", "LAR", "BPL", "Bhopal", "RKMP", "Habibganj")
    )
)

class TrainRepository(
    private val trainDao: TrainDao,
    private val apiService: TrainApiService
) {
    val savedTrains: Flow<List<SavedTrain>> = trainDao.getSavedTrains()

    suspend fun getTrainStatus(trainNo: String): TrainResponse {
        return apiService.getTrainRunningStatus(trainNo)
    }

    suspend fun saveTrain(trainNo: String, trainName: String) {
        val savedTrain = SavedTrain(
            trainNo = trainNo,
            trainName = trainName,
            lastSearched = System.currentTimeMillis(),
            isFavorite = false
        )
        trainDao.insertTrain(savedTrain)
    }

    suspend fun toggleFavorite(train: SavedTrain) {
        trainDao.updateTrain(train.copy(isFavorite = !train.isFavorite))
    }

    suspend fun deleteTrain(trainNo: String) {
        trainDao.deleteByNo(trainNo)
    }

    fun getTrainCatalog(): List<TrainCatalogItem> {
        return TRAIN_CATALOG
    }

    fun searchTrainsBetweenStations(from: String, to: String): List<TrainCatalogItem> {
        val cleanFrom = from.trim().lowercase()
        val cleanTo = to.trim().lowercase()
        if (cleanFrom.isEmpty() || cleanTo.isEmpty()) return emptyList()

        return TRAIN_CATALOG.filter { train ->
            val fromIndices = train.stations.mapIndexedNotNull { index, station ->
                if (station.lowercase().contains(cleanFrom)) index else null
            }
            val toIndices = train.stations.mapIndexedNotNull { index, station ->
                if (station.lowercase().contains(cleanTo)) index else null
            }

            if (fromIndices.isNotEmpty() && toIndices.isNotEmpty()) {
                fromIndices.any { fromIdx ->
                    toIndices.any { toIdx ->
                        fromIdx < toIdx
                    }
                }
            } else {
                false
            }
        }
    }
}
