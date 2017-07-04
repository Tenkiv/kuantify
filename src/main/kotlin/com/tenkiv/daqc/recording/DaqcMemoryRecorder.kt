package com.tenkiv.daqc.recording

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.tenkiv.DAQC_CONTEXT
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import java.io.*
import java.time.Instant
import javax.measure.Quantity

class DaqcMemoryRecorder<T: DaqcQuantity<T>>(val samplesInMemory: Int = 10,
                                             val fileName: String = "TempFile.json",
                                             val updatable: Updatable<ValueInstant<T>>) {

    init { updatable.openNewCoroutineListener(DAQC_CONTEXT,{cache(it)}) }

    private val fileWriter = BufferedWriter(FileWriter(fileName, true))
    val jsonFactory = JsonFactory()
    val jsonWriter = jsonFactory.createGenerator(fileWriter)
    val jsonParser = jsonFactory.createParser(File(fileName))

    val VALUE = "value"
    val TIME = "time"

    var currentBlock = ArrayList<ValueInstant<DaqcQuantity<T>>>()

    private suspend fun cache(value: ValueInstant<DaqcQuantity<T>>): Unit {
        if(currentBlock.size >= samplesInMemory){ writeOut(currentBlock) } else { currentBlock.add(value) }
    }

    private fun writeOut(entry: List<ValueInstant<DaqcQuantity<T>>>){
        entry.forEach {
            jsonWriter.writeStartObject()
            jsonWriter.writeStringField(VALUE,it.value.toString())
            jsonWriter.writeNumberField(TIME,it.instant.toEpochMilli())
            jsonWriter.writeEndObject()
        }
        fileWriter.flush()
    }

    inline fun <reified Q: Quantity<Q>> getDaqcQuantityData(start: Instant, end: Instant):
            List<ValueInstant<DaqcQuantity<Q>>>{
        val typedList = ArrayList<ValueInstant<DaqcQuantity<Q>>>()
        currentBlock.filter { it.instant.isAfter(start) && it.instant.isBefore(end) }.forEach {
            typedList.add(it as ValueInstant<DaqcQuantity<Q>>)
        }

        while (!(jsonParser.nextValue() == JsonToken.END_ARRAY && jsonParser.currentName.isNullOrBlank())){
            if(jsonParser.currentToken() == JsonToken.START_ARRAY){
                var shouldTake: Boolean = false
                var lastInstant: Instant = Instant.now()
                while (jsonParser.nextValue() != JsonToken.END_ARRAY) {
                    if(jsonParser.currentName != null && jsonParser.currentName == TIME) {
                        lastInstant = Instant.ofEpochMilli(jsonParser.valueAsLong)
                        if(lastInstant.isBefore(end) && lastInstant.isAfter(start)){
                            shouldTake = true
                        }
                    }else if(jsonParser.currentName != null && jsonParser.currentName == VALUE) {
                        if(shouldTake){
                            shouldTake = false
                            val quant = DaqcValue.quantityFromString<Q>(jsonParser.valueAsString)
                            if(quant != null){
                                typedList.add(quant.at(lastInstant))
                            }
                        }
                    }
                }
            }
        }
        return typedList
    }
}