package com.codingwithmitch.cleannotes.business.domain.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateUtil
@Inject
constructor(
    private val dateFormat: SimpleDateFormat
){

    // Date format: "2019-07-23 HH:mm:ss"
    // 2019-07-23

    fun removeStringFromDateString(sd: String): String{
        return sd.substring(0, sd.indexOf(" "))
    }

    fun convertFirebaseTimeStampToStringDate(timeStamp: Timestamp): String{
        return dateFormat.format(timeStamp.toDate())
    }

    fun convertStringDateToFirebaseTimeStamp(date: String): Timestamp{
        return Timestamp(dateFormat.parse(date))
    }

    fun getCurrentTimestamp(): String{
        return dateFormat.format(Date())
    }
}