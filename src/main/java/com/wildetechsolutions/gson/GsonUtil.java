package com.wildetechsolutions.gson;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.wildetechsolutions.reflection.ReflectionUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class GsonUtil {
    private static Logger log = LoggerFactory.getLogger(GsonUtil.class);

    public static void patch(Object object, Map<String, Object> jsonMap) throws NoSuchFieldException{
        JsonObject jsonObject = new Gson().toJsonTree(jsonMap).getAsJsonObject();
        setJsonValues(jsonObject, object, jsonObject.getClass(), null);
    }

    private static Object convertObjectToTargetType(String key, JsonObject jsonObject, Class<?> fieldType) {
        Object value = null;

        if (String.class.isAssignableFrom(fieldType)) {
            value = jsonObject.get(key).getAsString();
        } else if (Long.class.isAssignableFrom(fieldType) || long.class.isAssignableFrom(fieldType)) {
            value = jsonObject.get(key).getAsLong();
        } else if (Integer.class.isAssignableFrom(fieldType) || int.class.isAssignableFrom(fieldType)) {
            value = jsonObject.get(key).getAsInt();
        } else if (Boolean.class.isAssignableFrom(fieldType) || boolean.class.isAssignableFrom(fieldType)) {
            value = jsonObject.get(key).getAsBoolean();
        } else if (Float.class.isAssignableFrom(fieldType) || float.class.isAssignableFrom(fieldType)) {
            value = jsonObject.get(key).getAsBoolean();
        } else if (LocalDate.class.isAssignableFrom(fieldType)) {
            value = LocalDate.parse(jsonObject.get(key).getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        return value;
    }

    private static void setJsonValues(JsonObject jsonObject, Object job, Class<?> classOfJsonObject, String nestedPath) throws NoSuchFieldException {
        for (String key : jsonObject.keySet()) {
            Class<?> fieldType = ReflectionUtils.getFieldType(classOfJsonObject, key);

            String pathToField = key;
            if (nestedPath != null && !nestedPath.isEmpty()) {
                pathToField = String.join(".", nestedPath, key);
            }

            if(fieldType != null){
                log.debug("key: {}, type: {}", pathToField, fieldType.getName());

                if (ReflectionUtils.isCommonType(fieldType)) {
                    log.debug("field was primitive");
                    Object value = convertObjectToTargetType(key, jsonObject, fieldType);

                    try {
                        log.debug("setting {} to {}", pathToField, value);
                        PropertyUtils.setProperty(job, pathToField, value);
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }else{
                    log.debug("{} was complex", fieldType.getName());
                    if (jsonObject.get(key).isJsonObject()) {
                        log.debug("{} was json object", fieldType.getName());

                        JsonObject subJsonObject = jsonObject.get(key).getAsJsonObject();
                        Class<?> subObjectType = ReflectionUtils.getFieldType(classOfJsonObject, key);
                        log.debug("{} is of type {}", fieldType.getName(), subObjectType.getName());

                        try {
                            Object object = subObjectType.getDeclaredConstructor().newInstance();

                            if (classOfJsonObject.isAssignableFrom(job.getClass())) {
                                log.debug("setting {} to {}", pathToField, object);
                                PropertyUtils.setProperty(job, pathToField, object);
                            }else{
                                log.debug("Doing nothing, classOfJson object was {} while job class was {}", classOfJsonObject.getName(), job.getClass().getName());

                            }

                            String subPath = null;
                            if (nestedPath != null && !nestedPath.isEmpty()) {
                                subPath = String.join(".", nestedPath, key);
                            }else{
                                subPath = key;
                            }

                            List<Field> idFields = FieldUtils.getFieldsListWithAnnotation(subObjectType, Id.class);
                            for(Field field : idFields){
                                Object value = convertObjectToTargetType(field.getName(), subJsonObject, field.getType());
                                log.debug("Setting id field {} of class {} of field {} to {}", field.getName(), subObjectType, pathToField, value);
                                PropertyUtils.setProperty(object, field.getName(), value);
                            }
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                                 NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }
}
