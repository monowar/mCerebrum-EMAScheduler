package org.md2k.ema_scheduler.logger;

import android.content.Context;

import com.google.gson.Gson;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeString;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.utilities.Report.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by monowar on 3/14/16.
 */
public class LoggerManager {
    private static final String TAG = LoggerManager.class.getSimpleName();
    private static LoggerManager instance;
    Context context;
    DataKitAPI dataKitAPI;
    DataSourceBuilder dataSourceBuilderLogger;
    DataSourceClient dataSourceClientLogger;
    ArrayList<LogInfo> logInfos;

    private LoggerManager(Context context) {
        this.context = context;
        dataKitAPI = DataKitAPI.getInstance(context);
        dataSourceBuilderLogger = createDataSourceBuilderLogger();
        registerLogInfo();
        readLogInfosFromDataKit();
    }

    public static LoggerManager getInstance(Context context) {
        if (instance == null)
            instance = new LoggerManager(context);
        return instance;
    }

    long getToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void registerLogInfo() {
        dataSourceClientLogger = dataKitAPI.register(dataSourceBuilderLogger);
    }

    public void insert(LogInfo logInfo){
        Gson gson=new Gson();
        String string=gson.toJson(logInfo);
        Log.d(TAG, "insert()..." + string);
        DataTypeString dataTypeString=new DataTypeString(DateTime.getDateTime(), string);
        dataKitAPI.insert(dataSourceClientLogger, dataTypeString);
        logInfos.add(logInfo);
    }
    private void readLogInfosFromDataKit() {
        long startTimestamp = getToday();
        long endTimestamp = DateTime.getDateTime();
        logInfos=new ArrayList<>();
        Gson gson=new Gson();
        ArrayList<DataType> dataTypes=dataKitAPI.query(dataSourceClientLogger, startTimestamp, endTimestamp);
        for(int i=0;i<dataTypes.size();i++){
            DataTypeString dataTypeString= (DataTypeString) dataTypes.get(i);
            LogInfo logInfo=gson.fromJson(dataTypeString.getSample(),LogInfo.class);
            logInfos.add(logInfo);
        }
    }

    DataSourceBuilder createDataSourceBuilderLogger() {
        Platform platform = new PlatformBuilder().setType(PlatformType.PHONE).build();
        DataSourceBuilder dataSourceBuilder = new DataSourceBuilder().setType(DataSourceType.LOG).setPlatform(platform);
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.NAME, "Log");
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.DESCRIPTION, "Represents the log of EMA Scheduler");
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.DATA_TYPE, DataTypeString.class.getName());
        ArrayList<HashMap<String, String>> dataDescriptors = new ArrayList<>();
        HashMap<String, String> dataDescriptor = new HashMap<>();
        dataDescriptor.put(METADATA.NAME, "Log");
        dataDescriptor.put(METADATA.UNIT, "string");
        dataDescriptor.put(METADATA.DESCRIPTION, "Contains log");
        dataDescriptor.put(METADATA.DATA_TYPE, String.class.getName());
        dataDescriptors.add(dataDescriptor);
        dataSourceBuilder = dataSourceBuilder.setDataDescriptors(dataDescriptors);
        return dataSourceBuilder;
    }

    public ArrayList<LogInfo> getLogInfos() {
        return logInfos;
    }
    public ArrayList<LogInfo> getLogInfos(String operation, String type, String id){
        ArrayList<LogInfo> logInfosTemp=new ArrayList<>();
        for(int i=0;i<logInfos.size();i++){
            if(!logInfos.get(i).getOperation().equals(operation)) continue;
            if(!logInfos.get(i).getType().equals(type)) continue;
            if(!logInfos.get(i).getId().equals(id)) continue;
            logInfosTemp.add(logInfos.get(i));
        }
        return logInfosTemp;
    }
    public LogInfo getLogInfoLast(String operation, String type, String id) {
        LogInfo logInfo=null;
        for (int i = 0; i < logInfos.size(); i++) {
            if (!logInfos.get(i).getOperation().equals(operation)) continue;
            if (!logInfos.get(i).getType().equals(type)) continue;
            if (!logInfos.get(i).getId().equals(id)) continue;
            if (logInfo == null || logInfo.getTimestamp() < logInfos.get(i).getTimestamp())
                logInfo = logInfos.get(i);
        }
        return logInfo;
    }
    public ArrayList<LogInfo> getLogInfos(String operation, String type, String id, long startTime, long endTime){
        ArrayList<LogInfo> logInfosTemp=new ArrayList<>();
        for(int i=0;i<logInfos.size();i++){
            if(operation!=null && !logInfos.get(i).getOperation().equals(operation)) continue;
            if(type!=null && !logInfos.get(i).getType().equals(type)) continue;
            if(id!=null && !logInfos.get(i).getId().equals(id)) continue;
            if(startTime!=-1 && logInfos.get(i).getTimestamp()<startTime) continue;
            if(endTime!=-1 && logInfos.get(i).getTimestamp()>endTime) continue;
            logInfosTemp.add(logInfos.get(i));
        }
        return logInfosTemp;
    }
    public static void clear(){
        instance=null;
    }

}
