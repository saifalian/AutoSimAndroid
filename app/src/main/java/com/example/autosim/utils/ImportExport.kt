package com.example.autosim.utils

import com.example.autosim.db.AppDatabase
import com.example.autosim.db.ClickSpot
import com.example.autosim.db.OcrGroup
import com.example.autosim.db.OcrPhrase
import com.example.autosim.db.OcrRegion
import com.example.autosim.db.Sequence
import com.example.autosim.db.SequenceStep
import com.example.autosim.db.StepType
import com.example.autosim.db.TextDetectionPhrase
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

object ImportExport {
    
    data class ExportData(
        val clickSpots: List<ClickSpot>,
        val ocrRegions: List<OcrRegion>,
        val ocrGroups: List<OcrGroup>,
        val ocrPhrases: List<OcrPhrase>,
        val textDetectionPhrases: List<TextDetectionPhrase>,
        val sequences: List<Sequence>,
        val sequenceSteps: List<SequenceStep>
    )
    
    suspend fun exportToJson(db: AppDatabase): String {
        val clickSpots = db.clickSpotDao().getAll().first()
        val ocrRegions = db.ocrRegionDao().getAll().first()
        val ocrGroups = db.ocrGroupDao().getAll().first()
        val ocrPhrases = db.ocrPhraseDao().getAll().first()
        val textDetectionPhrases = db.textDetectionPhraseDao().getAll().first()
        val sequences = db.sequenceDao().getAll().first()
        val sequenceSteps = db.sequenceStepDao().getAll().first()
        
        val json = JSONObject()
        
        // Export Click Spots
        val clickSpotsArray = JSONArray()
        clickSpots.forEach { spot ->
            val obj = JSONObject()
            obj.put("id", spot.id)
            obj.put("name", spot.name)
            obj.put("x", spot.x)
            obj.put("y", spot.y)
            obj.put("delay", spot.delay)
            obj.put("repeat", spot.repeat)
            clickSpotsArray.put(obj)
        }
        json.put("clickSpots", clickSpotsArray)
        
        // Export OCR Regions
        val ocrRegionsArray = JSONArray()
        ocrRegions.forEach { region ->
            val obj = JSONObject()
            obj.put("id", region.id)
            obj.put("name", region.name)
            obj.put("left", region.left)
            obj.put("top", region.top)
            obj.put("width", region.width)
            obj.put("height", region.height)
            ocrRegionsArray.put(obj)
        }
        json.put("ocrRegions", ocrRegionsArray)
        
        // Export OCR Groups
        val ocrGroupsArray = JSONArray()
        ocrGroups.forEach { group ->
            val obj = JSONObject()
            obj.put("id", group.id)
            obj.put("name", group.name)
            obj.put("regionId", group.regionId)
            ocrGroupsArray.put(obj)
        }
        json.put("ocrGroups", ocrGroupsArray)
        
        // Export OCR Phrases
        val ocrPhrasesArray = JSONArray()
        ocrPhrases.forEach { phrase ->
            val obj = JSONObject()
            obj.put("id", phrase.id)
            obj.put("groupId", phrase.groupId)
            obj.put("text", phrase.text)
            obj.put("actionId", phrase.actionId)
            ocrPhrasesArray.put(obj)
        }
        json.put("ocrPhrases", ocrPhrasesArray)
        
        // Export Text Detection Phrases
        val textDetectionArray = JSONArray()
        textDetectionPhrases.forEach { phrase ->
            val obj = JSONObject()
            obj.put("id", phrase.id)
            obj.put("text", phrase.text)
            obj.put("actionId", phrase.actionId)
            textDetectionArray.put(obj)
        }
        json.put("textDetectionPhrases", textDetectionArray)
        
        // Export Sequences
        val sequencesArray = JSONArray()
        sequences.forEach { sequence ->
            val obj = JSONObject()
            obj.put("id", sequence.id)
            obj.put("name", sequence.name)
            sequencesArray.put(obj)
        }
        json.put("sequences", sequencesArray)
        
        // Export Sequence Steps
        val sequenceStepsArray = JSONArray()
        sequenceSteps.forEach { step ->
            val obj = JSONObject()
            obj.put("id", step.id)
            obj.put("sequenceId", step.sequenceId)
            obj.put("stepNumber", step.stepNumber)
            obj.put("type", step.type.name)
            obj.put("targetId", step.targetId)
            obj.put("delay", step.delay)
            obj.put("repeatCount", step.repeatCount)
            obj.put("jumpToStep", step.jumpToStep)
            obj.put("conditionalPhrase", step.conditionalPhrase)
            sequenceStepsArray.put(obj)
        }
        json.put("sequenceSteps", sequenceStepsArray)
        
        return json.toString(2)
    }
    
    suspend fun importFromJson(db: AppDatabase, jsonString: String) {
        val json = JSONObject(jsonString)
        
        // Import Click Spots
        val clickSpotsArray = json.getJSONArray("clickSpots")
        for (i in 0 until clickSpotsArray.length()) {
            val obj = clickSpotsArray.getJSONObject(i)
            val spot = ClickSpot(
                id = 0, // Will be auto-generated
                name = obj.getString("name"),
                x = obj.getInt("x"),
                y = obj.getInt("y"),
                delay = obj.getLong("delay"),
                repeat = obj.getInt("repeat")
            )
            db.clickSpotDao().insert(spot)
        }
        
        // Import OCR Regions
        val ocrRegionsArray = json.getJSONArray("ocrRegions")
        for (i in 0 until ocrRegionsArray.length()) {
            val obj = ocrRegionsArray.getJSONObject(i)
            val region = OcrRegion(
                id = 0,
                name = obj.getString("name"),
                left = obj.getInt("left"),
                top = obj.getInt("top"),
                width = obj.getInt("width"),
                height = obj.getInt("height")
            )
            db.ocrRegionDao().insert(region)
        }
        
        // Import OCR Groups
        val ocrGroupsArray = json.getJSONArray("ocrGroups")
        for (i in 0 until ocrGroupsArray.length()) {
            val obj = ocrGroupsArray.getJSONObject(i)
            val group = OcrGroup(
                id = 0,
                name = obj.getString("name"),
                regionId = obj.getInt("regionId")
            )
            db.ocrGroupDao().insert(group)
        }
        
        // Import OCR Phrases
        val ocrPhrasesArray = json.getJSONArray("ocrPhrases")
        for (i in 0 until ocrPhrasesArray.length()) {
            val obj = ocrPhrasesArray.getJSONObject(i)
            val phrase = OcrPhrase(
                id = 0,
                groupId = obj.getInt("groupId"),
                text = obj.getString("text"),
                actionId = if (obj.isNull("actionId")) null else obj.getInt("actionId")
            )
            db.ocrPhraseDao().insert(phrase)
        }
        
        // Import Text Detection Phrases
        val textDetectionArray = json.getJSONArray("textDetectionPhrases")
        for (i in 0 until textDetectionArray.length()) {
            val obj = textDetectionArray.getJSONObject(i)
            val phrase = TextDetectionPhrase(
                id = 0,
                text = obj.getString("text"),
                actionId = if (obj.isNull("actionId")) null else obj.getInt("actionId")
            )
            db.textDetectionPhraseDao().insert(phrase)
        }
        
        // Import Sequences
        val sequencesArray = json.getJSONArray("sequences")
        for (i in 0 until sequencesArray.length()) {
            val obj = sequencesArray.getJSONObject(i)
            val sequence = Sequence(
                id = 0,
                name = obj.getString("name")
            )
            db.sequenceDao().insert(sequence)
        }
        
        // Import Sequence Steps
        val sequenceStepsArray = json.getJSONArray("sequenceSteps")
        for (i in 0 until sequenceStepsArray.length()) {
            val obj = sequenceStepsArray.getJSONObject(i)
            val step = SequenceStep(
                id = 0,
                sequenceId = obj.getInt("sequenceId"),
                stepNumber = obj.getInt("stepNumber"),
                type = StepType.valueOf(obj.getString("type")),
                targetId = if (obj.isNull("targetId")) null else obj.getInt("targetId"),
                delay = if (obj.isNull("delay")) null else obj.getLong("delay"),
                repeatCount = if (obj.isNull("repeatCount")) null else obj.getInt("repeatCount"),
                jumpToStep = if (obj.isNull("jumpToStep")) null else obj.getInt("jumpToStep"),
                conditionalPhrase = if (obj.isNull("conditionalPhrase")) null else obj.getString("conditionalPhrase")
            )
            db.sequenceStepDao().insert(step)
        }
    }
}

