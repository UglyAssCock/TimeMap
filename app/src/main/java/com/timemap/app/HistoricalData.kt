package com.timemap.app

import kotlin.math.abs

data class HistoricalEvent(
    val id: String,
    val title: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val lat: Double,
    val lon: Double,
    val category: String,
    val summary: String
)

data class SearchPlace(
    val name: String,
    val year: Int,
    val lat: Double,
    val lon: Double,
    val zoom: Double,
    val description: String
)

data class EraPack(
    val id: String,
    val titleEn: String,
    val titleFr: String,
    val titleAr: String,
    val years: List<Int>
)

data class QuizQuestion(
    val areaName: String,
    val year: Int,
    val lat: Double,
    val lon: Double,
    val correctEmpire: String,
    val options: List<String>,
    val explanation: String
)

data class BookmarkItem(
    val title: String,
    val year: Int,
    val lat: Double,
    val lon: Double,
    val zoom: Double
)

object HistoricalData {

    // Verified available GeoJSON snapshot years in aourednik/historical-basemaps/geojson/
    val AVAILABLE_YEARS: List<Int> = listOf(
        -3000, -2000, -1500, -1000, -500, -400, -323, -300, -200, -100, -1,
        100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100, 1200, 1279,
        1300, 1400, 1492, 1500, 1530, 1600, 1650, 1700, 1715, 1783, 1800,
        1815, 1880, 1900, 1914, 1920, 1930, 1938, 1945, 1960, 1994, 2000, 2010
    )

    fun snapToAvailableYear(inputYear: Int): Int {
        return AVAILABLE_YEARS.minByOrNull { abs(it - inputYear) } ?: 1500
    }

    fun formatYear(year: Int): String {
        return if (year < 0) "${abs(year)} BC" else "$year AD"
    }

    fun getFileNameForYear(year: Int): String {
        val snapped = snapToAvailableYear(year)
        return if (snapped < 0) {
            "world_bc${abs(snapped)}.geojson"
        } else {
            "world_${snapped}.geojson"
        }
    }

    fun getGeoJsonUrl(year: Int): String {
        val fileName = getFileNameForYear(year)
        return "https://raw.githubusercontent.com/aourednik/historical-basemaps/master/geojson/$fileName"
    }

    val ERA_PACKS: List<EraPack> = listOf(
        EraPack(
            id = "ancient",
            titleEn = "Ancient World (3000 BC – 1 BC)",
            titleFr = "Monde Antique (3000 av. J.-C. – 1 av. J.-C.)",
            titleAr = "العالم القديم (3000 ق.م - 1 ق.م)",
            years = listOf(-3000, -2000, -1500, -1000, -500, -400, -323, -300, -200, -100, -1)
        ),
        EraPack(
            id = "classical_medieval",
            titleEn = "Classical & Medieval (100 – 1492)",
            titleFr = "Antiquité Tardive & Moyen Âge (100 – 1492)",
            titleAr = "العصور الكلاسيكية والوسطى (100 - 1492)",
            years = listOf(100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100, 1200, 1279, 1300, 1400, 1492)
        ),
        EraPack(
            id = "early_modern",
            titleEn = "Age of Sail & Empires (1500 – 1783)",
            titleFr = "Grandes Découvertes & Empires (1500 – 1783)",
            titleAr = "عصر الاستكشاف والإمبراطوريات (1500 - 1783)",
            years = listOf(1500, 1530, 1600, 1650, 1700, 1715, 1783)
        ),
        EraPack(
            id = "nineteenth",
            titleEn = "19th Century Pack (1800 – 1900)",
            titleFr = "Pack XIXe Siècle (1800 – 1900)",
            titleAr = "حزمة القرن التاسع عشر (1800 - 1900)",
            years = listOf(1800, 1815, 1880, 1900)
        ),
        EraPack(
            id = "twentieth",
            titleEn = "Modern World (1914 – 2010)",
            titleFr = "Monde Contemporain (1914 – 2010)",
            titleAr = "العالم الحديث (1914 - 2010)",
            years = listOf(1914, 1920, 1930, 1938, 1945, 1960, 1994, 2000, 2010)
        )
    )

    // 44 Hand-written major historical events with coordinates, dates, and summaries
    val EVENTS: List<HistoricalEvent> = listOf(
        HistoricalEvent("ev1", "Unification of Upper & Lower Egypt", -3000, 5, 1, 29.85, 31.25, "Founding", "King Narmer unifies Upper and Lower Egypt, founding the First Dynasty at Memphis."),
        HistoricalEvent("ev2", "Code of Hammurabi Enacted", -1750, 6, 10, 32.54, 44.42, "Treaty", "Babylonian King Hammurabi establishes one of the earliest and most complete written legal codes."),
        HistoricalEvent("ev3", "Battle of Kadesh", -1274, 5, 12, 34.55, 36.52, "Battle", "Pharaoh Ramesses II and Hittite King Muwatalli II fight the largest chariot battle in antiquity, leading to the first recorded peace treaty."),
        HistoricalEvent("ev4", "Founding of Carthage", -814, 4, 15, 36.85, 10.32, "Founding", "Phoenician settlers from Tyre led by Queen Dido establish the maritime city-state of Carthage in North Africa."),
        HistoricalEvent("ev5", "Founding of Rome", -753, 4, 21, 41.89, 12.49, "Founding", "Traditional founding of Rome on the Palatine Hill by Romulus."),
        HistoricalEvent("ev6", "Cyrus the Great Conquers Babylon", -539, 10, 12, 32.54, 44.42, "Battle", "Cyrus the Great incorporates Babylonia into the Achaemenid Persian Empire and issues the Cyrus Cylinder."),
        HistoricalEvent("ev7", "Battle of Marathon", -490, 9, 12, 38.15, 23.97, "Battle", "Athenian forces repel the first Persian invasion of Greece on the plain of Marathon."),
        HistoricalEvent("ev8", "Battle of Gaugamela", -331, 10, 1, 36.36, 43.25, "Battle", "Alexander the Great decisively defeats Darius III of Persia, opening the East to Macedonian rule."),
        HistoricalEvent("ev9", "Death of Alexander at Babylon", -323, 6, 10, 32.54, 44.42, "Event", "Alexander the Great dies in Babylon; his empire fractures among the Diadochi into Hellenistic kingdoms."),
        HistoricalEvent("ev10", "Kalinga War & Ashoka's Edicts", -261, 10, 18, 20.19, 85.85, "Treaty", "Mauryan Emperor Ashoka embraces Buddhism and non-violence after the devastating Kalinga War."),
        HistoricalEvent("ev11", "Qin Shi Huang Unifies China", -221, 3, 25, 34.26, 108.94, "Founding", "Qin Shi Huang conquers the warring states, becomes First Emperor, and links the Great Wall."),
        HistoricalEvent("ev12", "Battle of Zama", -202, 10, 19, 36.11, 9.29, "Battle", "Scipio Africanus defeats Hannibal Barca in North Africa, ending the Second Punic War."),
        HistoricalEvent("ev13", "Fall of Carthage & Corinth", -146, 3, 20, 36.85, 10.32, "Battle", "Rome destroys Carthage at the close of the Third Punic War and establishes dominance over the Mediterranean."),
        HistoricalEvent("ev14", "Assassination of Julius Caesar", -44, 3, 15, 41.89, 12.47, "Event", "Julius Caesar is assassinated on the Ides of March in the Roman Senate, hastening the end of the Republic."),
        HistoricalEvent("ev15", "Battle of Actium", -31, 9, 2, 38.95, 20.76, "Battle", "Octavian defeats Mark Antony and Cleopatra, paving the way for the Roman Principate under Augustus."),
        HistoricalEvent("ev16", "Battle of the Teutoburg Forest", 9, 9, 9, 52.41, 8.13, "Battle", "Germanic tribes led by Arminius ambush and destroy three Roman legions, halting expansion east of the Rhine."),
        HistoricalEvent("ev17", "Eruption of Mount Vesuvius", 79, 8, 24, 40.75, 14.48, "Event", "Catastrophic volcanic eruption buries the Roman cities of Pompeii and Herculaneum."),
        HistoricalEvent("ev18", "Roman Empire Reaches Peak Extent", 117, 8, 8, 41.90, 12.49, "Founding", "Under Emperor Trajan, the Roman Empire reaches its greatest territorial extent from Scotland to Mesopotamia."),
        HistoricalEvent("ev19", "Battle of Red Cliffs", 208, 12, 15, 29.87, 113.62, "Battle", "Allied southern warlords Sun Quan and Liu Bei defeat Cao Cao on the Yangtze, ushering in the Three Kingdoms period."),
        HistoricalEvent("ev20", "Edict of Milan", 313, 2, 15, 45.46, 9.19, "Treaty", "Emperors Constantine I and Licinius proclaim religious tolerance across the Roman Empire."),
        HistoricalEvent("ev21", "Dedication of Constantinople", 330, 5, 11, 41.01, 28.98, "Founding", "Constantine the Great inaugurates Constantinople (New Rome) on the site of ancient Byzantium."),
        HistoricalEvent("ev22", "Fall of the Western Roman Empire", 476, 9, 4, 44.42, 12.20, "Event", "Odoacer deposes Emperor Romulus Augustulus in Ravenna, marking the end of the Western Roman Empire."),
        HistoricalEvent("ev23", "Hijrah to Medina", 622, 9, 24, 24.47, 39.61, "Founding", "Prophet Muhammad completes the Hijrah from Mecca to Medina, establishing the first Islamic community and Year 1 AH."),
        HistoricalEvent("ev24", "Battle of Tours / Poitiers", 732, 10, 10, 46.76, 0.54, "Battle", "Frankish forces under Charles Martel halt the northward advance of the Umayyad Caliphate in western Europe."),
        HistoricalEvent("ev25", "Battle of Talas", 751, 7, 25, 42.52, 72.24, "Battle", "Abbasid Caliphate and Tang Dynasty forces clash in Central Asia; papermaking technology spreads westward."),
        HistoricalEvent("ev26", "Founding of Baghdad", 762, 7, 30, 33.31, 44.36, "Founding", "Abbasid Caliph al-Mansur founds the Round City of Baghdad, which becomes the center of the Islamic Golden Age."),
        HistoricalEvent("ev27", "Coronation of Charlemagne", 800, 12, 25, 41.90, 12.45, "Founding", "Charlemagne is crowned Emperor of the Romans in Rome, uniting much of Western Europe."),
        HistoricalEvent("ev28", "Battle of Hastings", 1066, 10, 14, 50.91, 0.49, "Battle", "William the Conqueror defeats King Harold II, initiating Norman rule over England."),
        HistoricalEvent("ev29", "Genghis Khan Unites the Mongol Steppe", 1206, 4, 10, 47.92, 106.91, "Founding", "Temüjin is proclaimed Genghis Khan at a Kurultai, launching the largest contiguous land empire in history."),
        HistoricalEvent("ev30", "Sealing of Magna Carta", 1215, 6, 15, 51.44, -0.56, "Treaty", "King John of England agrees to the Magna Carta at Runnymede, limiting royal authority under the law."),
        HistoricalEvent("ev31", "Siege of Baghdad", 1258, 2, 10, 33.31, 44.36, "Battle", "Hulagu Khan's Mongol forces capture Baghdad, ending the Abbasid Caliphate's rule there."),
        HistoricalEvent("ev32", "Kublai Khan Completes Yuan Conquest", 1279, 3, 19, 22.19, 113.12, "Battle", "The Battle of Yamen completes the Mongol Yuan Dynasty unification of all China."),
        HistoricalEvent("ev33", "Mansa Musa's Pilgrimage from Mali", 1324, 7, 15, 12.64, -8.00, "Event", "Emperor Mansa Musa of the Mali Empire journeys to Mecca with vast gold reserves, putting West Africa on world maps."),
        HistoricalEvent("ev34", "Fall of Constantinople", 1453, 5, 29, 41.01, 28.98, "Battle", "Ottoman Sultan Mehmed II captures Constantinople, ending the Byzantine Empire."),
        HistoricalEvent("ev35", "Columbus Reaches the Americas", 1492, 10, 12, 24.05, -74.53, "Event", "Christopher Columbus makes landfall in the Bahamas, initiating sustained contact between Afro-Eurasia and the Americas."),
        HistoricalEvent("ev36", "Treaty of Tordesillas", 1494, 6, 7, 41.50, -5.00, "Treaty", "Spain and Portugal divide newly discovered lands outside Europe along a meridian in the Atlantic."),
        HistoricalEvent("ev37", "Fall of Tenochtitlan", 1521, 8, 13, 19.43, -99.13, "Battle", "Hernán Cortés and Indigenous allies capture the Aztec capital Tenochtitlan."),
        HistoricalEvent("ev38", "Peace of Westphalia", 1648, 10, 24, 51.96, 7.63, "Treaty", "Treaties in Münster and Osnabrück end the Thirty Years' War and establish modern state sovereignty."),
        HistoricalEvent("ev39", "Treaty of Paris Ends American War", 1783, 9, 3, 48.86, 2.35, "Treaty", "Great Britain formally recognizes the independence of the United States of America."),
        HistoricalEvent("ev40", "French Revolution & Storming of the Bastille", 1789, 7, 14, 48.85, 2.37, "Event", "Parisians storm the Bastille fortress, igniting the French Revolution."),
        HistoricalEvent("ev41", "Battle of Waterloo & Congress of Vienna", 1815, 6, 18, 50.68, 4.41, "Battle", "Napoleon Bonaparte is defeated at Waterloo as European powers redraw post-Napoleonic borders."),
        HistoricalEvent("ev42", "Berlin Conference & Scramble for Africa", 1884, 11, 15, 52.52, 13.40, "Treaty", "European colonial powers formalize rules for partitioning Africa."),
        HistoricalEvent("ev43", "Outbreak of World War I", 1914, 7, 28, 43.86, 18.41, "Battle", "Following the assassination in Sarajevo, global alliances trigger the First World War."),
        HistoricalEvent("ev44", "End of World War II & UN Charter", 1945, 9, 2, 35.44, 139.65, "Treaty", "World War II concludes and the United Nations is founded, reshaping modern borders.")
    )

    // Famous historical empires and capitals for instant Search jumps
    val SEARCH_PLACES: List<SearchPlace> = listOf(
        SearchPlace("Roman Empire (Peak)", 100, 41.90, 12.49, 3.3, "Rome & Mediterranean basin under the Pax Romana"),
        SearchPlace("Roman Republic", -100, 41.90, 12.49, 3.6, "Expanding Roman Republic across Italy, Greece, and North Africa"),
        SearchPlace("Byzantine Empire (Eastern Rome)", 600, 41.01, 28.98, 4.0, "Constantinople and Eastern Mediterranean territories"),
        SearchPlace("Achaemenid Persian Empire", -500, 29.93, 52.89, 3.5, "First Persian Empire stretching from Egypt and Anatolia to the Indus"),
        SearchPlace("Alexander the Great / Macedonian Empire", -323, 32.54, 44.42, 3.5, "Hellenistic world from Greece and Egypt to India"),
        SearchPlace("Carthage / Punic Empire", -300, 36.85, 10.32, 4.2, "Western Mediterranean maritime empire centered in Tunisia"),
        SearchPlace("Numidia & North Africa", -200, 36.36, 6.61, 4.8, "Ancient Berber kingdoms of Numidia centered at Cirta (Constantine)"),
        SearchPlace("Maurya Empire", -300, 25.61, 85.14, 3.8, "Iron Age Indian subcontinent unified under Chandragupta and Ashoka"),
        SearchPlace("Han Dynasty", 100, 34.26, 108.94, 3.4, "Golden age of imperial China and opening of the Silk Road"),
        SearchPlace("Tang Dynasty", 700, 34.26, 108.94, 3.4, "Cosmopolitan Chinese empire centered at Chang'an"),
        SearchPlace("Umayyad Caliphate", 700, 33.51, 36.29, 3.0, "Caliphate stretching from Iberia and the Maghreb to Sindh"),
        SearchPlace("Abbasid Caliphate", 800, 33.31, 44.36, 3.3, "Islamic Golden Age centered in Baghdad"),
        SearchPlace("Almoravid & Almohad Maghreb", 1100, 31.63, -8.00, 4.0, "North African and Andalusian dynasties"),
        SearchPlace("Mali Empire (Mansa Musa)", 1300, 12.64, -8.00, 4.0, "West African gold empire spanning the Niger River and Timbuktu"),
        SearchPlace("Mongol Empire", 1279, 47.00, 100.00, 2.5, "Largest contiguous land empire under Genghis Khan's successors"),
        SearchPlace("Ottoman Empire", 1600, 41.01, 28.98, 3.5, "Southeastern Europe, West Asia, and North Africa including Regency of Algiers"),
        SearchPlace("Regency of Algiers / Algeria", 1700, 36.75, 3.05, 4.6, "Autonomous North African maritime state in the Mediterranean"),
        SearchPlace("Aztec Empire", 1500, 19.43, -99.13, 4.5, "Triple Alliance centered at Tenochtitlan in Mesoamerica"),
        SearchPlace("Inca Empire (Tawantinsuyu)", 1500, -13.53, -71.97, 3.8, "Andean empire centered at Cusco"),
        SearchPlace("Spanish Empire", 1700, 20.00, -80.00, 2.5, "Global viceroyalties across the Americas, Iberia, and the Philippines"),
        SearchPlace("Mughal Empire", 1700, 27.18, 78.02, 3.8, "South Asian empire famous for economic wealth and monumental architecture"),
        SearchPlace("Qing Dynasty", 1800, 39.90, 116.40, 3.2, "Last imperial dynasty of China at its territorial zenith"),
        SearchPlace("Napoleonic French Empire", 1815, 48.85, 2.35, 4.0, "Europe during the Congress of Vienna and Napoleonic era"),
        SearchPlace("British Empire", 1920, 25.00, 30.00, 2.0, "Largest maritime empire in history after World War I"),
        SearchPlace("Cold War Bipolar World", 1960, 50.00, 20.00, 2.2, "NATO, Warsaw Pact, and the wave of African independence in 1960")
    )

    val QUIZ_QUESTIONS: List<QuizQuestion> = listOf(
        QuizQuestion(
            areaName = "Anatolia & Constantinople",
            year = 1500,
            lat = 41.01,
            lon = 28.98,
            correctEmpire = "Ottoman Empire",
            options = listOf("Ottoman Empire", "Byzantine Empire", "Safavid Empire", "Mamluk Sultanate"),
            explanation = "After capturing Constantinople in 1453, the Ottoman Empire consolidated rule over Anatolia and the Balkans."
        ),
        QuizQuestion(
            areaName = "The Mediterranean Basin & Egypt",
            year = 100,
            lat = 35.0,
            lon = 18.0,
            correctEmpire = "Roman Empire",
            options = listOf("Roman Empire", "Parthian Empire", "Carthaginian Republic", "Ptolemaic Kingdom"),
            explanation = "In 100 AD, the Roman Empire completely encircled the Mediterranean Sea ('Mare Nostrum')."
        ),
        QuizQuestion(
            areaName = "Northern China & the Eurasian Steppe",
            year = 1279,
            lat = 40.0,
            lon = 110.0,
            correctEmpire = "Mongol Empire / Yuan Dynasty",
            options = listOf("Mongol Empire / Yuan Dynasty", "Ming Dynasty", "Tang Dynasty", "Russian Empire"),
            explanation = "By 1279, Kublai Khan completed the conquest of Song China, uniting China under the Mongol-led Yuan Dynasty."
        ),
        QuizQuestion(
            areaName = "Andes Mountains & Cusco",
            year = 1492,
            lat = -13.53,
            lon = -71.97,
            correctEmpire = "Inca Empire",
            options = listOf("Inca Empire", "Spanish Empire", "Aztec Empire", "Portuguese Empire"),
            explanation = "In 1492, before Spanish arrival in South America, the Inca Empire (Tawantinsuyu) ruled the Andean region."
        ),
        QuizQuestion(
            areaName = "Persia & Mesopotamia",
            year = -500,
            lat = 32.0,
            lon = 48.0,
            correctEmpire = "Achaemenid Empire",
            options = listOf("Achaemenid Empire", "Seleucid Empire", "Assyrian Empire", "Sasanian Empire"),
            explanation = "Under Darius the Great in 500 BC, the Achaemenid Persian Empire ruled from Egypt to the Indus River."
        ),
        QuizQuestion(
            areaName = "Iberia, North Africa & Damascus",
            year = 700,
            lat = 33.5,
            lon = 10.0,
            correctEmpire = "Umayyad Caliphate",
            options = listOf("Umayyad Caliphate", "Abbasid Caliphate", "Fatimid Caliphate", "Eastern Roman Empire"),
            explanation = "Around 700 AD, the Damascus-based Umayyad Caliphate governed North Africa and the Middle East."
        ),
        QuizQuestion(
            areaName = "The Niger River Bend & Timbuktu",
            year = 1300,
            lat = 16.77,
            lon = -3.01,
            correctEmpire = "Mali Empire",
            options = listOf("Mali Empire", "Songhai Empire", "Sokoto Caliphate", "Kingdom of Aksum"),
            explanation = "In 1300, the Mali Empire controlled the lucrative trans-Saharan gold and salt trade routes."
        ),
        QuizQuestion(
            areaName = "Indian Subcontinent (Deccan & Ganges)",
            year = -300,
            lat = 23.0,
            lon = 80.0,
            correctEmpire = "Maurya Empire",
            options = listOf("Maurya Empire", "Mughal Empire", "Gupta Empire", "Chola Dynasty"),
            explanation = "Founded by Chandragupta Maurya in 322 BC, the Maurya Empire unified most of the Indian subcontinent."
        ),
        QuizQuestion(
            areaName = "Central Valley of Mexico",
            year = 1500,
            lat = 19.43,
            lon = -99.13,
            correctEmpire = "Aztec Empire",
            options = listOf("Aztec Empire", "Maya City-States", "Viceroyalty of New Spain", "Olmec Civilization"),
            explanation = "In 1500 AD, the Aztec Triple Alliance ruled central Mexico from its island capital of Tenochtitlan."
        ),
        QuizQuestion(
            areaName = "Northern India & Agra",
            year = 1700,
            lat = 27.18,
            lon = 78.02,
            correctEmpire = "Mughal Empire",
            options = listOf("Mughal Empire", "British East India Company", "Delhi Sultanate", "Maratha Confederacy"),
            explanation = "In 1700 under Emperor Aurangzeb, the Mughal Empire reached its greatest territorial extent across South Asia."
        )
    )

    fun buildHistoricalSummary(regionName: String, subjectTo: String, year: Int): String {
        val eraLabel = when {
            year <= -1000 -> "Bronze & Early Iron Age"
            year < 0 -> "Classical Antiquity"
            year < 500 -> "Imperial & Late Antiquity Era"
            year < 1500 -> "Medieval & Post-Classical Era"
            year < 1800 -> "Early Modern & Age of Sail Era"
            year <= 1914 -> "Industrial & Imperial Era"
            else -> "Modern Contemporary Era"
        }
        val rulerInfo = if (subjectTo.isNotBlank() && !subjectTo.equals(regionName, ignoreCase = true)) {
            "Part of the $subjectTo sphere of control."
        } else {
            "An independent or distinct polity/cultural territory."
        }
        val nearbyEvents = EVENTS.filter { abs(it.year - year) <= 60 }
            .take(2)
            .joinToString(" ") { "• ${formatYear(it.year)}: ${it.title}." }

        return buildString {
            append("In ${formatYear(year)} ($eraLabel), $regionName is recorded on the world map. ")
            append(rulerInfo)
            if (nearbyEvents.isNotBlank()) {
                append("\n\nKey era milestones:\n$nearbyEvents")
            }
        }
    }
}
