package com.gotest.app.feature.exam.ui.components

import com.gotest.app.core.model.AttemptResult
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.Question
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.core.model.QuestionOption
import com.gotest.app.core.model.QuestionReview
import com.gotest.app.core.model.QuestionStatement
import com.gotest.app.core.model.QuestionType
import com.gotest.app.core.model.TopicAccuracy
import java.time.LocalDate

/** Literal sample data for the exam feature's @Preview functions only. */
internal object ExamPreviewData {
    private const val QUESTION_COUNT = 20
    private const val DESIGN_INDEX = 3
    private const val ANSWERED_COUNT = 3
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

    private fun question(
        number: Int,
        topic: String,
        prompt: String,
        options: List<String>,
        correct: String,
        explanation: String,
    ) = Question(
        id = "preview_q$number",
        number = number,
        topic = topic,
        type = QuestionType.MultipleChoice,
        stimulusTitle = null,
        stimulusBody = null,
        imageUrl = null,
        prompt = prompt,
        instruction = null,
        options = options.mapIndexed { i, text -> QuestionOption(('A' + i).toString(), text) },
        statements = emptyList(),
        correctKeys = setOf(correct),
        correctStatements = emptyMap(),
        explanation = explanation,
    )

    val perimeterQuestion =
        question(
            number = 1,
            topic = "Matematika Dasar",
            prompt = "Keliling segitiga sama sisi dengan panjang sisi 8 cm adalah?",
            options = listOf("16 cm", "20 cm", "24 cm", "32 cm"),
            correct = "C",
            explanation = "Keliling = 3 × 8 = 24 cm.",
        )

    val permutationQuestion =
        question(
            number = 2,
            topic = "Matematika Diskrit",
            prompt = "Banyak susunan huruf berbeda dari kata \"TONI\" adalah ....",
            options = listOf("12", "120", "24", "4"),
            correct = "C",
            explanation =
                "Kata \"TONI\" terdiri dari 4 huruf yang semuanya berbeda. " +
                    "Banyak susunan = 4! = 4 × 3 × 2 × 1 = 24 susunan.",
        )

    val rectangleQuestion =
        question(
            number = 3,
            topic = "Matematika Dasar",
            prompt = "Keliling persegi panjang dengan panjang 7 cm dan lebar 5 cm adalah?",
            options = listOf("12 cm", "35 cm", "24 cm", "124 cm"),
            correct = "C",
            explanation = "Keliling = 2 × (7 + 5) = 24 cm.",
        )

    val routeQuestion =
        Question(
            id = "preview_q4",
            number = 4,
            topic = "Logaritma",
            type = QuestionType.MultipleChoice,
            stimulusTitle = ROUTE_TITLE,
            stimulusBody = ROUTE_BODY,
            imageUrl = null,
            prompt = ROUTE_PROMPT,
            instruction = null,
            options = routeOptions,
            statements = emptyList(),
            correctKeys = setOf("A"),
            correctStatements = emptyMap(),
            explanation = "Rute A memiliki penumpang terbanyak sehingga paling mungkin penuh lebih dulu.",
        )

    val complexQuestion =
        routeQuestion.copy(
            id = "preview_q4_complex",
            topic = "PG Kompleks",
            type = QuestionType.ComplexMultipleChoice,
            instruction =
                "Pilih semua jawaban yang tepat. Skor penuh hanya diberikan bila pilihanmu tepat sama " +
                    "dengan seluruh kunci.",
            correctKeys = setOf("A", "C"),
        )

    val trueFalseQuestion =
        Question(
            id = "preview_q4_tf",
            number = 4,
            topic = "Benar / Salah",
            type = QuestionType.TrueFalse,
            stimulusTitle = "Tandon Air Gedung",
            stimulusBody =
                "Tandon gedung belajar berkapasitas 840 liter dan terisi 504 liter pada pagi hari. " +
                    "Siang hari air terpakai 110 liter, lalu sore hari ditambah 150 liter.",
            imageUrl = null,
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
            explanation = "Volume akhir = 504 − 110 + 150 = 544 liter.",
        )

    /** Twenty questions where the fourth one is [fourth], like the design's "Soal 4 dari 20". */
    fun twentyQuestions(fourth: Question): List<Question> {
        val bank = listOf(perimeterQuestion, permutationQuestion, rectangleQuestion, fourth)
        return List(QUESTION_COUNT) { index ->
            val base = if (index == DESIGN_INDEX) fourth else bank[index % bank.size]
            base.copy(id = "preview_${index + 1}", number = index + 1)
        }
    }

    /** Questions 1–3 answered, matching the in-progress attempt in the design. */
    fun firstThreeAnswered(questions: List<Question>): Map<String, QuestionAnswer> =
        questions.take(ANSWERED_COUNT).associate { it.id to QuestionAnswer(selectedKeys = it.correctKeys) }

    val reviews: List<QuestionReview> =
        listOf(
            QuestionReview(perimeterQuestion, QuestionAnswer(selectedKeys = setOf("C"))),
            QuestionReview(permutationQuestion, QuestionAnswer(selectedKeys = setOf("B"))),
            QuestionReview(rectangleQuestion, QuestionAnswer(selectedKeys = setOf("D"))),
            QuestionReview(routeQuestion, QuestionAnswer(selectedKeys = setOf("A"))),
            QuestionReview(trueFalseQuestion, QuestionAnswer(statementAnswers = mapOf(1 to true, 2 to true))),
            QuestionReview(complexQuestion.copy(id = "preview_q6", number = 6), QuestionAnswer()),
        )

    val tryoutResult =
        AttemptResult(
            attemptId = "att_preview",
            packageId = "pkg_preview",
            title = "UTBK-SNBT Session 1",
            description = "Simulasi UTBK-SNBT.",
            examLabel = "UTBK-SNBT",
            type = PackageType.Tryout,
            score = 540,
            rank = 12,
            participants = 12_512,
            correct = 12,
            wrong = 5,
            empty = 3,
            topicAccuracy =
                listOf(
                    TopicAccuracy("Matematika Dasar", 80),
                    TopicAccuracy("Logaritma", 50),
                    TopicAccuracy("Aljabar", 25),
                ),
            weakestTopic = "Aljabar",
            sections = emptyList(),
            completedOn = LocalDate.of(2026, 9, 19),
            durationSeconds = 1_232,
        )
}
