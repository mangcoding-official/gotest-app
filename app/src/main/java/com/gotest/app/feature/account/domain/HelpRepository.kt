package com.gotest.app.feature.account.domain

import kotlinx.coroutines.flow.Flow

/** Picks the icon badge of a help category; the UI maps it to an icon and colour. */
enum class HelpCategoryKind { Account, Practice, Payment }

data class HelpCategory(
    val id: String,
    val kind: HelpCategoryKind,
    val title: String,
    val subtitle: String,
    /** Argument for HelpTopicDestination. */
    val topicId: String,
)

data class HelpFaq(
    val id: String,
    val question: String,
    val answer: String,
)

enum class HelpContactType { Email, WhatsApp }

data class HelpContact(
    val type: HelpContactType,
    /** What the tile shows, e.g. "+62 3142 2453". */
    val displayValue: String,
    /** `mailto:` or `https://wa.me/...` URI the app opens. */
    val uri: String,
)

/** Everything on the "Bantuan & Support" screen. */
data class HelpCenter(
    val categories: List<HelpCategory>,
    val faqs: List<HelpFaq>,
    val contacts: List<HelpContact>,
    val operatingHours: String,
)

/** A chip on the topic screen; the first one of a topic covers every article. */
data class HelpTopicTag(
    val id: String,
    val label: String,
)

data class HelpArticle(
    val id: String,
    val tagIds: Set<String>,
    val question: String,
    val answer: String,
)

data class HelpTopic(
    val id: String,
    val title: String,
    val subtitle: String,
    val tags: List<HelpTopicTag>,
    val articles: List<HelpArticle>,
    /** Opened by "Masih butuh bantuan? → Hubungi Kami". */
    val supportContact: HelpContact,
)

interface HelpRepository {
    fun observeHelpCenter(): Flow<HelpCenter>

    /** Emits an error for an unknown [topicId]. */
    fun observeTopic(topicId: String): Flow<HelpTopic>
}
