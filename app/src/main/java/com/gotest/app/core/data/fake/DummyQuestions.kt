package com.gotest.app.core.data.fake

import com.gotest.app.core.model.Question
import com.gotest.app.core.model.QuestionOption
import com.gotest.app.core.model.QuestionStatement
import com.gotest.app.core.model.QuestionType

private const val ROUTE_TITLE = "Rute Angkutan Pagi di Kampung Melati"
private const val ROUTE_BODY =
    "Dalam pemantauan selama satu pekan, rute A mengangkut 44 penumpang per hari, rute B 39 penumpang, " +
        "dan rute C 30 penumpang. Setiap kendaraan berkapasitas 94 penumpang. Pengelola akan menambah " +
        "satu perjalanan pada rute yang paling sering penuh."
private const val ROUTE_PROMPT =
    "Berdasarkan catatan rute angkutan pagi di Kampung Melati, kesimpulan yang paling kuat adalah ...."
private val routeOptions =
    listOf(
        QuestionOption("A", "Rute A perlu dipantau lebih dulu karena jumlah penumpangnya paling tinggi."),
        QuestionOption("B", "Rute dengan jumlah penumpang paling rendah pasti tidak diperlukan lagi."),
        QuestionOption("C", "Semua rute harus diberi kendaraan dengan kapasitas yang sama tanpa melihat data."),
        QuestionOption("D", "Kapasitas kendaraan tidak berkaitan dengan keputusan menambah perjalanan."),
    )

private fun mc(
    topic: String,
    prompt: String,
    options: List<String>,
    correct: String,
    explanation: String,
    stimulusTitle: String? = null,
    stimulusBody: String? = null,
) = Question(
    id = "",
    number = 0,
    topic = topic,
    type = QuestionType.MultipleChoice,
    stimulusTitle = stimulusTitle,
    stimulusBody = stimulusBody,
    imageUrl = null,
    prompt = prompt,
    instruction = null,
    options = options.mapIndexed { i, text -> QuestionOption(('A' + i).toString(), text) },
    statements = emptyList(),
    correctKeys = setOf(correct),
    correctStatements = emptyMap(),
    explanation = explanation,
)

/**
 * Hand-written question bank, taken from the design where it has content. Packages with more
 * questions than the bank reuse it in order with new numbers.
 */
internal val QuestionBank: List<Question> =
    listOf(
        mc(
            topic = "Matematika Dasar",
            prompt = "Keliling segitiga sama sisi dengan panjang sisi 8 cm adalah?",
            options = listOf("16 cm", "20 cm", "24 cm", "32 cm"),
            correct = "C",
            explanation = "Keliling = 3 × 8 = 24 cm.",
        ),
        mc(
            topic = "Matematika Diskrit",
            prompt = "Banyak susunan huruf berbeda dari kata \"TONI\" adalah ....",
            options = listOf("12", "120", "24", "4"),
            correct = "C",
            explanation =
                "Kata \"TONI\" terdiri dari 4 huruf yang semuanya berbeda. " +
                    "Banyak susunan = 4! = 4 × 3 × 2 × 1 = 24 susunan.",
        ),
        mc(
            topic = "Matematika Dasar",
            prompt = "Keliling persegi panjang dengan panjang 7 cm dan lebar 5 cm adalah?",
            options = listOf("12 cm", "35 cm", "24 cm", "124 cm"),
            correct = "C",
            explanation = "Keliling = 2 × (7 + 5) = 24 cm.",
        ),
        mc(
            topic = "Logaritma",
            prompt = ROUTE_PROMPT,
            options = routeOptions.map { it.text },
            correct = "A",
            explanation =
                "Rute A memiliki penumpang terbanyak (44 per hari) sehingga paling mungkin penuh lebih dulu. " +
                    "Pilihan lain menarik kesimpulan yang tidak didukung data.",
            stimulusTitle = ROUTE_TITLE,
            stimulusBody = ROUTE_BODY,
        ),
        Question(
            id = "",
            number = 0,
            topic = "PG Kompleks",
            type = QuestionType.ComplexMultipleChoice,
            stimulusTitle = ROUTE_TITLE,
            stimulusBody = ROUTE_BODY,
            imageUrl = null,
            prompt = ROUTE_PROMPT,
            instruction =
                "Pilih semua jawaban yang tepat. Skor penuh hanya diberikan bila pilihanmu tepat sama " +
                    "dengan seluruh kunci.",
            options = routeOptions,
            statements = emptyList(),
            correctKeys = setOf("A", "C"),
            correctStatements = emptyMap(),
            explanation =
                "Data menunjukkan rute A paling ramai, dan kapasitas yang seragam memudahkan perbandingan " +
                    "antarrute, sehingga A dan C tepat.",
        ),
        Question(
            id = "",
            number = 0,
            topic = "Benar / Salah",
            type = QuestionType.TrueFalse,
            stimulusTitle = "Tandon Air Gedung",
            stimulusBody =
                "Tandon gedung belajar berkapasitas 840 liter dan terisi 504 liter pada pagi hari. " +
                    "Siang hari air terpakai 110 liter, lalu sore hari ditambah 150 liter.",
            imageUrl = drawableUri("question_water_tank"),
            prompt = "Tentukan kebenaran pernyataan tentang pemodelan tandon pada Tandon Air Gedung.",
            instruction =
                "Pilih satu jawaban (Benar / Salah) untuk setiap pernyataan. Skor penuh hanya diberikan " +
                    "bila seluruh pernyataan tepat.",
            options = emptyList(),
            statements =
                listOf(
                    QuestionStatement(1, "Volume akhir dapat dihitung dengan 504 − 110 + 150."),
                    QuestionStatement(2, "Model tidak membutuhkan satuan liter karena angka sudah cukup."),
                    QuestionStatement(3, "Volume akhir perlu dibandingkan dengan kapasitas tandon."),
                ),
            correctKeys = emptySet(),
            correctStatements = mapOf(1 to true, 2 to false, 3 to true),
            explanation =
                "Volume akhir = 504 − 110 + 150 = 544 liter. Satuan tetap diperlukan agar model bermakna, " +
                    "dan 544 liter perlu dibandingkan dengan kapasitas 840 liter.",
        ),
        mc(
            topic = "Logaritma",
            prompt = "Nilai dari ²log 8 + ³log 9 adalah ....",
            options = listOf("3", "4", "5", "6"),
            correct = "C",
            explanation = "²log 8 = 3 dan ³log 9 = 2, sehingga hasilnya 3 + 2 = 5.",
        ),
        mc(
            topic = "Aljabar",
            prompt = "Jika 3x − 7 = 11, maka nilai x adalah ....",
            options = listOf("4", "6", "18", "3"),
            correct = "B",
            explanation = "3x = 11 + 7 = 18, sehingga x = 18 ÷ 3 = 6.",
        ),
        mc(
            topic = "Aljabar Linear",
            prompt = "Determinan matriks [[2, 3], [1, 4]] adalah ....",
            options = listOf("5", "8", "11", "−5"),
            correct = "A",
            explanation = "det = (2 × 4) − (3 × 1) = 8 − 3 = 5.",
        ),
        mc(
            topic = "Logaritma",
            prompt = "Bentuk sederhana dari log 2 + log 5 adalah ....",
            options = listOf("log 7", "1", "log 10²", "0"),
            correct = "B",
            explanation = "log 2 + log 5 = log (2 × 5) = log 10 = 1.",
        ),
    )

/** Builds [count] questions for a package, numbering them 1..count. */
internal fun questionsFor(
    packageId: String,
    count: Int,
): List<Question> =
    List(count) { index ->
        QuestionBank[index % QuestionBank.size].copy(
            id = "${packageId}_q${index + 1}",
            number = index + 1,
        )
    }
