package com.novelverse.app.data.local.database;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

/**
 * Room Type Converters for complex data types
 */
public class Converters {

    private static final Gson gson = new Gson();

    // Date converters
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    // String List converters
    @TypeConverter
    public static List<String> fromStringList(String value) {
        if (value == null) return null;
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String toStringList(List<String> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }

    // Integer List converters
    @TypeConverter
    public static List<Integer> fromIntegerList(String value) {
        if (value == null) return null;
        Type listType = new TypeToken<List<Integer>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String toIntegerList(List<Integer> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }

    // Boolean converters (for SQL compatibility)
    @TypeConverter
    public static Boolean fromInteger(Integer value) {
        return value == null ? null : value == 1;
    }

    @TypeConverter
    public static Integer booleanToInteger(Boolean value) {
        return value == null ? null : (value ? 1 : 0);
    }
}
