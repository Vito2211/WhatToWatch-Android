package com.vp.whattowatch.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vp.whattowatch.data.local.AppDatabase
import com.vp.whattowatch.data.sharing.ShareCategory
import com.vp.whattowatch.data.sharing.ShareContentCategoryRef
import com.vp.whattowatch.data.sharing.ShareContentItem
import com.vp.whattowatch.data.sharing.ShareCustomList
import com.vp.whattowatch.data.sharing.ShareListItemRef
import com.vp.whattowatch.data.sharing.SharePayload
import com.vp.whattowatch.data.sharing.ShareType
import com.vp.whattowatch.data.sharing.SharingService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
class CrossPlatformSharingFixtureTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var sharingService: SharingService

    private val jsonSerializer = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        sharingService = SharingService(
            context = context,
            database = db,
            contentDao = db.contentDao(),
            categoryDao = db.categoryDao(),
            customListDao = db.customListDao(),
            fileUriProvider = { Uri.fromFile(it) }
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testGenerateAndVerifyCrossPlatformFixtures() {
        val fixtures = mutableMapOf<String, FixtureData>()

        // 1. Single Item (English with URL, notes, and category)
        val singleItemPayload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "Inception",
            contents = listOf(
                ShareContentItem(
                    id = "inc-001",
                    title = "Inception",
                    contentType = "MOVIE",
                    watchStatus = "WATCHED",
                    externalUrl = "https://www.imdb.com/title/tt1375666/",
                    description = "A thief who steals corporate secrets through the use of dream-sharing technology.",
                    createdAt = 1700000000000L,
                    updatedAt = 1700000000000L
                )
            ),
            categories = listOf(
                ShareCategory(id = "cat-scifi", name = "Sci-Fi", createdAt = 1700000000000L)
            ),
            contentCategories = listOf(
                ShareContentCategoryRef(contentId = "inc-001", categoryId = "cat-scifi")
            ),
            sharedAt = 1700000000000L
        )
        val singleItemEncoded = sharingService.compressAndEncode(singleItemPayload)
        val singleItemDecoded = sharingService.decodeAndDecompress(singleItemEncoded)
        assertEquals("Inception", singleItemDecoded.title)
        fixtures["single_item_en"] = FixtureData(
            name = "single_item_en",
            description = "Single movie item in English with notes, URL, and category",
            encoded = singleItemEncoded,
            expectedTitle = "Inception",
            expectedType = "SINGLE_ITEM",
            expectedContentsCount = 1,
            expectedCategoriesCount = 1,
            isValid = true
        )

        // 2. Custom List (Multiple items across diverse types and statuses)
        val listPayload = SharePayload(
            version = 1,
            type = ShareType.CUSTOM_LIST,
            title = "Weekend Watchlist 🎬",
            contents = listOf(
                ShareContentItem(
                    id = "list-item-1",
                    title = "Severance",
                    contentType = "SERIES",
                    watchStatus = "WATCHING",
                    externalUrl = "https://tv.apple.com/us/show/severance",
                    description = "Mark leads a team of office workers whose memories have been surgically divided.",
                    createdAt = 1700000001000L,
                    updatedAt = 1700000001000L
                ),
                ShareContentItem(
                    id = "list-item-2",
                    title = "Dune: Part Two",
                    contentType = "MOVIE",
                    watchStatus = "NOT_WATCHED",
                    externalUrl = null,
                    description = "Paul Atreides unites with Chani and the Fremen.",
                    createdAt = 1700000002000L,
                    updatedAt = 1700000002000L
                ),
                ShareContentItem(
                    id = "list-item-3",
                    title = "Kurzgesagt: Loneliness",
                    contentType = "VIDEO",
                    watchStatus = "WATCHED",
                    externalUrl = "https://www.youtube.com/watch?v=n3Xv_g3g-mA",
                    description = "Scientific examination of human connection.",
                    createdAt = 1700000003000L,
                    updatedAt = 1700000003000L
                )
            ),
            categories = listOf(
                ShareCategory(id = "cat-drama", name = "Drama", createdAt = 1700000000000L),
                ShareCategory(id = "cat-edu", name = "Education", createdAt = 1700000000000L)
            ),
            customLists = listOf(
                ShareCustomList(id = "c-list-1", name = "Weekend Watchlist 🎬", createdAt = 1700000000000L, updatedAt = 1700000000000L)
            ),
            contentCategories = listOf(
                ShareContentCategoryRef(contentId = "list-item-1", categoryId = "cat-drama"),
                ShareContentCategoryRef(contentId = "list-item-3", categoryId = "cat-edu")
            ),
            listItems = listOf(
                ShareListItemRef(listId = "c-list-1", contentId = "list-item-1"),
                ShareListItemRef(listId = "c-list-1", contentId = "list-item-2"),
                ShareListItemRef(listId = "c-list-1", contentId = "list-item-3")
            ),
            sharedAt = 1700000000000L
        )
        val listEncoded = sharingService.compressAndEncode(listPayload)
        val listDecoded = sharingService.decodeAndDecompress(listEncoded)
        assertEquals(3, listDecoded.contents.size)
        fixtures["custom_list_multi"] = FixtureData(
            name = "custom_list_multi",
            description = "Custom list containing 3 items of diverse content types",
            encoded = listEncoded,
            expectedTitle = "Weekend Watchlist 🎬",
            expectedType = "CUSTOM_LIST",
            expectedContentsCount = 3,
            expectedCategoriesCount = 2,
            isValid = true
        )

        // 3. Russian Cyrillic & Unicode & Emoji
        val ruPayload = SharePayload(
            version = 1,
            type = ShareType.CUSTOM_LIST,
            title = "Шедевры мирового кино 🍿 ★",
            contents = listOf(
                ShareContentItem(
                    id = "ru-1",
                    title = "Интерстеллар 🚀",
                    contentType = "MOVIE",
                    watchStatus = "WATCHED",
                    externalUrl = "https://www.kinopoisk.ru/film/258687/",
                    description = "Когда засуха, пыльные бури и вымирание растений приводят человечество к продовольственному кризису.",
                    createdAt = 1700000010000L,
                    updatedAt = 1700000010000L
                ),
                ShareContentItem(
                    id = "ru-2",
                    title = "Хижина в лесу",
                    contentType = "OTHER",
                    watchStatus = "WATCHING",
                    externalUrl = null,
                    description = "Группа студентов отправляется на уик-энд в уединённую лесную хижину.",
                    createdAt = 1700000020000L,
                    updatedAt = 1700000020000L
                )
            ),
            categories = listOf(
                ShareCategory(id = "cat-ru-sci", name = "Фантастика", createdAt = 1700000000000L),
                ShareCategory(id = "cat-ru-hor", name = "Ужасы", createdAt = 1700000000000L)
            ),
            contentCategories = listOf(
                ShareContentCategoryRef(contentId = "ru-1", categoryId = "cat-ru-sci"),
                ShareContentCategoryRef(contentId = "ru-2", categoryId = "cat-ru-hor")
            ),
            sharedAt = 1700000000000L
        )
        val ruEncoded = sharingService.compressAndEncode(ruPayload)
        val ruDecoded = sharingService.decodeAndDecompress(ruEncoded)
        assertEquals("Шедевры мирового кино 🍿 ★", ruDecoded.title)
        assertEquals("Интерстеллар 🚀", ruDecoded.contents[0].title)
        fixtures["russian_unicode_emoji"] = FixtureData(
            name = "russian_unicode_emoji",
            description = "Russian Cyrillic text with complex emojis and special characters",
            encoded = ruEncoded,
            expectedTitle = "Шедевры мирового кино 🍿 ★",
            expectedType = "CUSTOM_LIST",
            expectedContentsCount = 2,
            expectedCategoriesCount = 2,
            isValid = true
        )

        // 4. Missing Optional Fields (null description and URL)
        val missingFieldsPayload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "Minimalist Item",
            contents = listOf(
                ShareContentItem(
                    id = "min-1",
                    title = "Minimalist Item",
                    contentType = "MOVIE",
                    watchStatus = "NOT_WATCHED",
                    externalUrl = null,
                    description = null,
                    createdAt = 1700000030000L,
                    updatedAt = 1700000030000L
                )
            ),
            categories = emptyList(),
            contentCategories = emptyList(),
            sharedAt = 1700000000000L
        )
        val missingFieldsEncoded = sharingService.compressAndEncode(missingFieldsPayload)
        val missingFieldsDecoded = sharingService.decodeAndDecompress(missingFieldsEncoded)
        assertEquals("Minimalist Item", missingFieldsDecoded.title)
        fixtures["missing_optional_fields"] = FixtureData(
            name = "missing_optional_fields",
            description = "Payload with null optional fields (description, URL, categories)",
            encoded = missingFieldsEncoded,
            expectedTitle = "Minimalist Item",
            expectedType = "SINGLE_ITEM",
            expectedContentsCount = 1,
            expectedCategoriesCount = 0,
            isValid = true
        )

        // 5. Empty Descriptions and Links (blank strings)
        val emptyStringsPayload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "Empty String Fields",
            contents = listOf(
                ShareContentItem(
                    id = "empty-1",
                    title = "Empty String Fields",
                    contentType = "OTHER",
                    watchStatus = "WATCHING",
                    externalUrl = "",
                    description = "   ",
                    createdAt = 1700000040000L,
                    updatedAt = 1700000040000L
                )
            ),
            categories = emptyList(),
            contentCategories = emptyList(),
            sharedAt = 1700000000000L
        )
        val emptyStringsEncoded = sharingService.compressAndEncode(emptyStringsPayload)
        val emptyStringsDecoded = sharingService.decodeAndDecompress(emptyStringsEncoded)
        assertEquals("Empty String Fields", emptyStringsDecoded.title)
        fixtures["empty_descriptions_and_links"] = FixtureData(
            name = "empty_descriptions_and_links",
            description = "Payload with empty and whitespace-only strings for description and URL",
            encoded = emptyStringsEncoded,
            expectedTitle = "Empty String Fields",
            expectedType = "SINGLE_ITEM",
            expectedContentsCount = 1,
            expectedCategoriesCount = 0,
            isValid = true
        )

        // 6. Multilingual Asian Scripts (Japanese Kanji and Kana)
        val jaPayload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "千と千尋の神隠し 🐉",
            contents = listOf(
                ShareContentItem(
                    id = "ja-1",
                    title = "千と千尋の神隠し (Spirited Away)",
                    contentType = "MOVIE",
                    watchStatus = "WATCHED",
                    externalUrl = "https://www.ghibli.jp/works/chihiro/",
                    description = "宮崎駿監督による2001年の長編アニメーション映画。",
                    createdAt = 1700000050000L,
                    updatedAt = 1700000050000L
                )
            ),
            categories = listOf(
                ShareCategory(id = "cat-anime", name = "アニメ", createdAt = 1700000000000L)
            ),
            contentCategories = listOf(
                ShareContentCategoryRef(contentId = "ja-1", categoryId = "cat-anime")
            ),
            sharedAt = 1700000000000L
        )
        val jaEncoded = sharingService.compressAndEncode(jaPayload)
        val jaDecoded = sharingService.decodeAndDecompress(jaEncoded)
        assertEquals("千と千尋の神隠し 🐉", jaDecoded.title)
        fixtures["multilingual_japanese"] = FixtureData(
            name = "multilingual_japanese",
            description = "Multilingual Japanese text with Kanji, Kana, and Dragon emoji",
            encoded = jaEncoded,
            expectedTitle = "千と千尋の神隠し 🐉",
            expectedType = "SINGLE_ITEM",
            expectedContentsCount = 1,
            expectedCategoriesCount = 1,
            isValid = true
        )

        // 7. Unsupported Future Version (version 99)
        val futurePayload = SharePayload(
            version = 99,
            type = ShareType.SINGLE_ITEM,
            title = "Future Format Title",
            contents = listOf(
                ShareContentItem(
                    id = "fut-1",
                    title = "Future Film",
                    contentType = "MOVIE",
                    watchStatus = "WATCHED",
                    createdAt = 1700000060000L,
                    updatedAt = 1700000060000L
                )
            )
        )
        val futureEncoded = sharingService.compressAndEncode(futurePayload)
        var rejectedByAndroid = false
        try {
            sharingService.decodeAndDecompress(futureEncoded)
        } catch (_: IllegalArgumentException) {
            rejectedByAndroid = true
        }
        assertTrue("Android must reject version 99", rejectedByAndroid)
        fixtures["unsupported_future_version"] = FixtureData(
            name = "unsupported_future_version",
            description = "Payload with unsupported version (version 99) that must be rejected",
            encoded = futureEncoded,
            expectedTitle = "Future Format Title",
            expectedType = "SINGLE_ITEM",
            expectedContentsCount = 1,
            expectedCategoriesCount = 0,
            isValid = false,
            expectedError = "Unsupported sharing version 99"
        )

        // 8. Raw Uncompressed JSON (testing fallback compatibility)
        val rawJsonPayload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "Raw JSON Title",
            contents = listOf(
                ShareContentItem(
                    id = "raw-1",
                    title = "Raw JSON Item",
                    contentType = "MOVIE",
                    watchStatus = "NOT_WATCHED",
                    createdAt = 1700000070000L,
                    updatedAt = 1700000070000L
                )
            )
        )
        val rawJsonString = Json.encodeToString(SharePayload.serializer(), rawJsonPayload)
        val rawJsonEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(rawJsonString.toByteArray(Charsets.UTF_8))
        fixtures["uncompressed_raw_json"] = FixtureData(
            name = "uncompressed_raw_json",
            description = "Valid uncompressed JSON payload encoded in Base64",
            encoded = rawJsonEncoded,
            expectedTitle = "Raw JSON Title",
            expectedType = "SINGLE_ITEM",
            expectedContentsCount = 1,
            expectedCategoriesCount = 0,
            isValid = true
        )

        // 9. Corrupted Base64
        fixtures["corrupted_base64"] = FixtureData(
            name = "corrupted_base64",
            description = "Corrupted non-base64 characters",
            encoded = "!!!not_valid_base64_string@@@###",
            expectedTitle = "",
            expectedType = "",
            expectedContentsCount = 0,
            expectedCategoriesCount = 0,
            isValid = false,
            expectedError = "Invalid base64 encoding"
        )

        // 10. Corrupted GZIP Stream
        val nonGzipBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString("Not a gzip header".toByteArray(Charsets.UTF_8))
        fixtures["corrupted_gzip"] = FixtureData(
            name = "corrupted_gzip",
            description = "Valid Base64 containing invalid non-GZIP bytes",
            encoded = nonGzipBase64,
            expectedTitle = "",
            expectedType = "",
            expectedContentsCount = 0,
            expectedCategoriesCount = 0,
            isValid = false,
            expectedError = "Payload is not valid"
        )

        // Export fixtures to JSON
        val exportedJson = buildJsonObject {
            fixtures.forEach { (key, fixture) ->
                putJsonObject(key) {
                    put("name", fixture.name)
                    put("description", fixture.description)
                    put("encoded", fixture.encoded)
                    put("expectedTitle", fixture.expectedTitle)
                    put("expectedType", fixture.expectedType)
                    put("expectedContentsCount", fixture.expectedContentsCount)
                    put("expectedCategoriesCount", fixture.expectedCategoriesCount)
                    put("isValid", fixture.isValid)
                    fixture.expectedError?.let { put("expectedError", it) }
                }
            }
        }.toString()

        // Write to Android test resources
        val resourcesDir = File("src/test/resources").takeIf { it.exists() }
            ?: File("app/src/test/resources").takeIf { it.exists() }
            ?: File("src/test/resources").apply { mkdirs() }
        File(resourcesDir, "cross_platform_fixtures.json").writeText(exportedJson)

        // Write to Website tests fixtures directory
        val websiteCandidates = listOf(
            File("../website/tests/fixtures"),
            File("../../website/tests/fixtures"),
            File("/Users/mypc/Vibecode/WHATTOWATCH/website/tests/fixtures")
        )
        val websiteDir = websiteCandidates.firstOrNull { it.isDirectory || it.parentFile?.isDirectory == true }
        if (websiteDir != null) {
            websiteDir.mkdirs()
            File(websiteDir, "android_generated_payloads.json").writeText(exportedJson)
        }
    }

    private data class FixtureData(
        val name: String,
        val description: String,
        val encoded: String,
        val expectedTitle: String,
        val expectedType: String,
        val expectedContentsCount: Int,
        val expectedCategoriesCount: Int,
        val isValid: Boolean,
        val expectedError: String? = null
    )
}
