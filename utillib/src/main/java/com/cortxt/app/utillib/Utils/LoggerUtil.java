package com.cortxt.app.utillib.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.veracode.annotation.CRLFCleanser;


/**
 * This class is a replacement for the Log static class. The advantage of using this class
 * is the the TAG that is used for the log statements is the same for all the classes, but the 
 * class name is mentioned in the message. This means that filtering ACRA
 * to send the logcat for this application only is very simple. The class name is still maintained
 * in the message.
 * @author Abhin
 *
 */
public class LoggerUtil {
    /**
     * Path of file that {@link LoggerUtil#logToFile} write to.
     */
    public String LOG_FILE = "/sdcard/mmclog.txt";

    public static final String LOG_TRANSIT_FILE = "/sdcard/mmclogtransit.txt";

    public static final String TAG = "com.cortxt.app.MMC";
    public static File logFile = null;

    /**
     * Flag to hold whether the application is debug mode or not
     */
    public static boolean DEBUGGABLE = false;

    /**
     * Sets {@link LoggerUtil#DEBUGGABLE} to debug
     * @param debug
     */
    public static void setDebuggable (boolean debug) {
        LoggerUtil.DEBUGGABLE = debug;
    }

    /**
     * @return {@link LoggerUtil#DEBUGGABLE}
     */
    public static boolean isDebuggable() {
        return LoggerUtil.DEBUGGABLE;
    }

    private static Object logFileLock = new Object();
    /**
     * Logs to a file.
     * This is needed because the android log clears itself.
     * @param level
     * @param className
     * @param methodName
     * @param message
     */
    public static void logToFile(Level level, String className, String methodName, String message) {
        Date d = new Date(System.currentTimeMillis());
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        String timestamp = dateFormat.format(d);

        String logText = timestamp + " - " + level + " - " +  className + " - " + methodName + " - " + message;
        logText = sanitize (logText);
        writeToFile(logText);
        if(isDebuggable()){
            //message = org.owasp.esapi.StringUtilities.stripControls (methodName + " " + message);
            message = sanitize (methodName + " " + message);
            Log.i(className, message);
        }

    }

    public static void init (Context context){
        if (logFile == null) {
            logFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "mmclog");
            //logFile = new File(filePath);
        }
    }

    @CRLFCleanser
    private static String sanitize (String logText){
        if (logText.contains("\n") || logText.contains("\r"))
            logText = logText.replace ("\r", "").replace ("\n", "");
        return logText;
    }

    public static void logToTransitFile(Level level, String className, String methodName, String message) {
        Date d = new Date(System.currentTimeMillis());
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        String timestamp = dateFormat.format(d);

        String logText = timestamp + " - " + level + " - " +  className + " - " + methodName + " - " + message;
        logText = sanitize (logText);
        writeToFile(logText);
        if(isDebuggable()){
            message = sanitize (methodName + " " + message);
            //message = org.owasp.esapi.StringUtilities.stripControls (methodName + " " + message);
            Log.i(className, message);
        }
    }

    /**
     * Logs to a file.
     * @param level
     * @param className
     * @param methodName
     * @param message
     * @param e
     */
    public static void logToFile(Level level, String className, String methodName, String message, Exception e) {
        String stackTrace = Global.getStackTrace ( e);
//        String stackTrace = "\n\t" + e.toString();
//        StackTraceElement[] stackTraceElements = e.getStackTrace();
//        int len = stackTraceElements.length;
//        if (len > 3)
//            len = 3;
//        for(int i=0; i<len; i++) {
//            stackTrace += "\n\t" + stackTraceElements[i].getClassName() + "." + stackTraceElements[i].getMethodName() + " (" + stackTraceElements[i].getFileName() + " : " + stackTraceElements[i].getLineNumber() + ")";
//        }

        logToFile(level, className, methodName, message + stackTrace);
    }

    /**
     * Temporary method for logging events and samples to separate file
     * @param message
     */
//    public static void logToOtherFile(String message) {
//        writeToFile(message, "/sdcard/events.txt");
//    }

    private static void writeToFile(String text) {
        if(isDebuggable()) {
            synchronized(logFileLock) {
                try {
                    text = text + "\n";

//                    File file = new File(filePath);
//                    if(!file.exists()) {
//                        file.createNewFile();
//                    }
                    if (logFile == null)
                        return;
                    File file = logFile;
                    FileWriter fileWriter = new FileWriter(file, true);
                    BufferedWriter outputStream = new BufferedWriter(fileWriter);

                    try {
                        outputStream.append(text);
                    }
                    catch (IOException ioe_writingToFile) {
                        Log.e("MMCLogger", "error writing to log file ", ioe_writingToFile);
                    }
                    finally {
                        outputStream.close();
                        fileWriter.close();
                    }
                }
                catch (IOException ioe_openingFile) {
                    Log.e("MMCLogger", "error opening log file " + ioe_openingFile.getMessage(), null);
                }
            }
        }
    }

    public enum Level {
        DEBUG {
            public String toString() {
                return "DEBUG";
            }
        },
        WARNING{
            public String toString() {
                return "WARNING";
            }
        },
        ERROR {
            public String toString() {
                return "ERROR";
            }
        },
        WTF {
            public String toString() {
                return "WTF";
            }
        }

    }
}