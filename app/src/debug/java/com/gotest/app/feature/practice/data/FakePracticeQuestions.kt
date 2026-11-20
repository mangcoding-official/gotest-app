package com.gotest.app.feature.practice.data

import com.gotest.app.feature.practice.domain.AnswerData
import com.gotest.app.feature.practice.domain.OptionText
import com.gotest.app.feature.practice.domain.QuestionContent
import com.gotest.app.feature.practice.domain.Stimulus

/** One question of the fake Latihan with its key and pembahasan (the server keeps these from the app). */
internal data class FakeQuestion(
    val id: Long,
    val type: String,
    val subject: String,
    val topic: String,
    val content: QuestionContent,
    val correct: AnswerData,
    val explanation: String,
)

/** Five questions of the three answer shapes of Latihan: single, multiple_select, statement_table. */
internal object FakePracticeQuestions {
    private fun options(vararg texts: String) = texts.map { OptionText(it) }

    val all =
        listOf(
            FakeQuestion(
                id = 5001,
                type = "multiple_choice",
                subject = "Penalaran Umum",
                topic = "Penalaran Kuantitatif",
                content =
                    QuestionContent(
                        prompt = "<p>Jika $2x + 3 = 11$, maka nilai \$x^2$ adalah ....</p>",
                        options = options("4", "8", "16", "22", "64"),
                    ),
                correct = AnswerData(selectedOption = 2),
                explanation = "<p>$2x = 8$, jadi \$x = 4$ dan \$x^2 = 16$.</p>",
            ),
            FakeQuestion(
                id = 5002,
                type = "multiple_select",
                subject = "Penalaran Umum",
                topic = "Bilangan",
                content =
                    QuestionContent(
                        prompt = "<p>Pilih semua bilangan prima di bawah ini.</p>",
                        options = options("2", "9", "11", "15", "17"),
                    ),
                correct = AnswerData(selectedOptions = listOf(0, 2, 4)),
                explanation = "<p>2, 11, dan 17 hanya habis dibagi 1 dan dirinya sendiri.</p>",
            ),
            FakeQuestion(
                id = 5003,
                type = "statement_table",
                subject = "Penalaran Umum",
                topic = "Logika",
                content =
                    QuestionContent(
                        prompt = "<p>Tentukan benar atau salah setiap pernyataan berikut.</p>",
                        statements =
                            options(
                                "Semua persegi adalah persegi panjang.",
                                "Setiap bilangan genap habis dibagi 4.",
                                "Jumlah sudut segitiga adalah 180°.",
                            ),
                        answerLabels = options("Benar", "Salah"),
                    ),
                correct = AnswerData(statementAnswers = listOf(0, 1, 0)),
                explanation = "<p>6 genap tetapi tidak habis dibagi 4, jadi pernyataan kedua salah.</p>",
            ),
            FakeQuestion(
                id = 5004,
                type = "multiple_choice",
                subject = "Penalaran Umum",
                topic = "Pemahaman Bacaan",
                content =
                    QuestionContent(
                        prompt = "<p>Gagasan utama bacaan di atas adalah ....</p>",
                        stimulus =
                            Stimulus(
                                name = "Bacaan 1",
                                text =
                                    "<p>Hutan mangrove menahan abrasi pantai dan menjadi tempat berkembang biak " +
                                        "berbagai jenis ikan. Karena itu, penanaman kembali mangrove penting bagi " +
                                        "nelayan di pesisir.</p>",
                            ),
                        options =
                            options(
                                "Ikan berkembang biak di laut lepas.",
                                "Mangrove penting bagi pantai dan nelayan.",
                                "Abrasi hanya terjadi di pantai berpasir.",
                                "Nelayan menanam mangrove setiap tahun.",
                            ),
                    ),
                correct = AnswerData(selectedOption = 1),
                explanation = "<p>Kedua kalimat bacaan menjelaskan manfaat mangrove bagi pantai dan nelayan.</p>",
            ),
            FakeQuestion(
                id = 5005,
                type = "multiple_choice",
                subject = "Penalaran Umum",
                topic = "Pola Bilangan",
                content =
                    QuestionContent(
                        prompt = "<p>Bilangan berikutnya dari pola 3, 6, 12, 24, ... adalah ....</p>",
                        options = options("30", "36", "48", "60"),
                    ),
                correct = AnswerData(selectedOption = 2),
                explanation = "<p>Setiap bilangan dikali 2, jadi $24 \\times 2 = 48$.</p>",
            ),
        )
}
